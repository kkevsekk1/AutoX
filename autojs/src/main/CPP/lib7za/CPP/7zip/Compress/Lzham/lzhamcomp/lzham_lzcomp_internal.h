// File: lzham_lzcomp_internal.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once
#include "lzham_match_accel.h"
#include "lzham_symbol_codec.h"
#include "lzham_lzbase.h"

namespace lzham
{
   typedef lzham::vector<uint8> byte_vec;

   const uint cMaxParseGraphNodes = 3072;
   const uint cMaxParseThreads = 8;
      
   const uint cMaxParseNodeStates = LZHAM_EXTREME_PARSING_MAX_BEST_ARRIVALS_MAX;
   const uint cDefaultMaxParseNodeStates = 4;
   
   enum compression_level
   {
      cCompressionLevelFastest,
      cCompressionLevelFaster,
      cCompressionLevelDefault,
      cCompressionLevelBetter,
      cCompressionLevelUber,

      cCompressionLevelCount
   };

   struct comp_settings
   {
      uint m_fast_bytes;
      uint m_match_accel_max_matches_per_probe;
      uint m_match_accel_max_probes;
   };
     
   class lzcompressor : public CLZBase
   {
      LZHAM_NO_COPY_OR_ASSIGNMENT_OP(lzcompressor);

   public:
      lzcompressor(lzham_malloc_context malloc_context);

      struct init_params
      {
         enum
         {
            cMinDictSizeLog2 = CLZBase::cMinDictSizeLog2,
            cMaxDictSizeLog2 = CLZBase::cMaxDictSizeLog2,
            cDefaultBlockSize = 1024U*512U
         };

         init_params() :
            m_pTask_pool(NULL),
            m_max_helper_threads(0),
            m_compression_level(cCompressionLevelDefault),
            m_dict_size_log2(22),
            m_block_size(cDefaultBlockSize),
            m_lzham_compress_flags(0),
            m_pSeed_bytes(0),
            m_num_seed_bytes(0),
            m_table_max_update_interval(0),
            m_table_update_interval_slow_rate(0),
            m_extreme_parsing_max_best_arrivals(cDefaultMaxParseNodeStates),
            m_fast_bytes_override(0)
         {
         }

         task_pool* m_pTask_pool;
         uint m_max_helper_threads;

         compression_level m_compression_level;
         uint m_dict_size_log2;

         uint m_block_size;
			                  
         uint m_lzham_compress_flags;

         const void *m_pSeed_bytes;
         uint m_num_seed_bytes;
			         
         uint m_table_max_update_interval;
         uint m_table_update_interval_slow_rate;

         uint m_extreme_parsing_max_best_arrivals;
         uint m_fast_bytes_override;
      };

      bool init(const init_params& params);
      void clear();

      // sync, or sync+dictionary flush 
      bool flush(lzham_flush_t flush_type);

      bool reset();

      bool put_bytes(const void* pBuf, uint buf_len);

      const byte_vec& get_compressed_data() const   { return m_comp_buf; }
            byte_vec& get_compressed_data()         { return m_comp_buf; }

      uint32 get_src_adler32() const { return m_src_adler32; }

   private:
      lzham_malloc_context m_malloc_context;

      class state;
      
      enum
      {
         cLitComplexity = 1,
         cRep0Complexity = 2,
         cRep3Complexity = 5,
         
         cLongMatchComplexity = 6,
         cLongMatchComplexityLenThresh = 9,
         
         cShortMatchComplexity = 7
      };

      struct lzdecision
      {
         int m_pos;  // dict position where decision was evaluated
         int m_len;  // 0 if literal, 1+ if match
         int m_dist; // <0 if match rep, else >=1 is match dist
         
         inline lzdecision() { }
         inline lzdecision(int pos, int len, int dist) : m_pos(pos), m_len(len), m_dist(dist) { }
         
         inline void init(int pos, int len, int dist) { m_pos = pos; m_len = len; m_dist = dist; }

         inline bool is_lit() const { return !m_len; }
         inline bool is_match() const { return m_len > 0; } // may be a rep or full match
         inline bool is_full_match() const { return (m_len > 0) && (m_dist >= 1); }
         inline uint get_len() const { return math::maximum<uint>(m_len, 1); }
         inline bool is_rep() const { return m_dist < 0; }
         inline bool is_rep0() const { return m_dist == -1; }

         uint get_match_dist(const state& s) const;

         inline uint get_complexity() const
         {
            if (is_lit())
               return cLitComplexity;
            else if (is_rep())
            {
               LZHAM_ASSUME(cRep0Complexity == 2);
               return 1 + -m_dist;  // 2, 3, 4, or 5
            }
            else if (get_len() >= cLongMatchComplexityLenThresh)
               return cLongMatchComplexity;
            else
               return cShortMatchComplexity;
         }

         inline uint get_min_codable_len() const
         {
            if (is_lit() || is_rep0())
               return 1;
            else
               return CLZBase::cMinMatchLen;
         }
      };

      struct lzpriced_decision : lzdecision
      {
         lzpriced_decision() { }

         inline lzpriced_decision(int pos, int len, int dist) : lzdecision(pos, len, dist) { }
         inline lzpriced_decision(int pos, int len, int dist, bit_cost_t cost) : lzdecision(pos, len, dist), m_cost(cost) { }
         
         inline void init(int pos, int len, int dist, bit_cost_t cost) { lzdecision::init(pos, len, dist); m_cost = cost; }

         inline bit_cost_t get_cost() const { return m_cost; }

         bit_cost_t m_cost;
      };
      
      struct state_base
      {
         uint m_cur_ofs;
         uint m_cur_state;
         uint m_match_hist[CLZBase::cMatchHistSize];
         
         inline bool operator== (const state_base &rhs) const
         {
            if (m_cur_state != rhs.m_cur_state)
               return false;
            for (uint i = 0; i < CLZBase::cMatchHistSize; i++)
               if (m_match_hist[i] != rhs.m_match_hist[i])
                  return false;
            return true;
         }

         void partial_advance(const lzdecision& lzdec);
         
         inline void save_partial_state(state_base& dst) const
         {
            dst.m_cur_ofs = m_cur_ofs;
            dst.m_cur_state = m_cur_state;
            memcpy(dst.m_match_hist, m_match_hist, sizeof(m_match_hist));
         }

         inline void restore_partial_state(const state_base& src)
         {
            m_cur_ofs = src.m_cur_ofs;
            m_cur_state = src.m_cur_state;
            memcpy(m_match_hist, src.m_match_hist, sizeof(m_match_hist));
         }
      };

      class state : public state_base
      {
      public:
         state(lzham_malloc_context malloc_context = NULL);

         lzham_malloc_context get_malloc_context() const { return m_malloc_context; }
         
         void set_malloc_context(lzham_malloc_context context)
         {
            m_malloc_context = context;
          
            m_lit_table.set_malloc_context(m_malloc_context);
            m_delta_lit_table.set_malloc_context(m_malloc_context);
            m_main_table.set_malloc_context(m_malloc_context);
            for (uint i = 0; i < 2; i++)
            {
               m_rep_len_table[i].set_malloc_context(m_malloc_context);
               m_large_len_table[i].set_malloc_context(m_malloc_context);
            }
            m_dist_lsb_table.set_malloc_context(m_malloc_context);
         }

         void clear();
         
         bool init(CLZBase& lzbase, uint table_max_update_interval, uint table_update_interval_slow_rate);
         void reset();
         
         bit_cost_t get_cost(CLZBase& lzbase, const search_accelerator& dict, const lzdecision& lzdec) const;
         bit_cost_t get_len2_match_cost(CLZBase& lzbase, uint dict_pos, uint len2_match_dist, uint is_match_model_index);
         bit_cost_t get_lit_cost(CLZBase& lzbase, const search_accelerator& dict, uint dict_pos, uint lit_pred0, uint is_match_model_index) const;

         // Returns actual cost.
         void get_rep_match_costs(uint dict_pos, bit_cost_t *pBitcosts, uint match_hist_index, int min_len, int max_len, uint is_match_model_index) const;
         void get_full_match_costs(CLZBase& lzbase, uint dict_pos, bit_cost_t *pBitcosts, uint match_dist, int min_len, int max_len, uint is_match_model_index) const;

         bit_cost_t update_stats(CLZBase& lzbase, const search_accelerator& dict, const lzdecision& lzdec);

         bool advance(CLZBase& lzbase, const search_accelerator& dict, const lzdecision& lzdec);
         bool encode(symbol_codec& codec, CLZBase& lzbase, const search_accelerator& dict, const lzdecision& lzdec);

         void print(symbol_codec& codec, CLZBase& lzbase, const search_accelerator& dict, const lzdecision& lzdec);

         bool encode_eob(symbol_codec& codec, const search_accelerator& dict, uint dict_pos);
         bool encode_reset_state_partial(symbol_codec& codec, const search_accelerator& dict, uint dict_pos);

         void update_match_hist(uint match_dist);
         int find_match_dist(uint match_hist) const;

         void reset_state_partial();
         void start_of_block(const search_accelerator& dict, uint cur_ofs, uint block_index);
         
         void reset_update_rate();
         void reset_tables();

         uint get_pred_char(const search_accelerator& dict, int pos, int backward_ofs) const;

         inline bool will_reference_last_match(const lzdecision& lzdec) const
         {
            return (!lzdec.is_match()) &&  (m_cur_state >= CLZBase::cNumLitStates);
         }

         lzham_malloc_context m_malloc_context;
         
         uint m_block_start_dict_ofs;

         adaptive_bit_model m_is_match_model[CLZBase::cNumStates];

         adaptive_bit_model m_is_rep_model[CLZBase::cNumStates];
         adaptive_bit_model m_is_rep0_model[CLZBase::cNumStates];
         adaptive_bit_model m_is_rep0_single_byte_model[CLZBase::cNumStates];
         adaptive_bit_model m_is_rep1_model[CLZBase::cNumStates];
         adaptive_bit_model m_is_rep2_model[CLZBase::cNumStates];
         
         quasi_adaptive_huffman_data_model m_lit_table;
         quasi_adaptive_huffman_data_model m_delta_lit_table;

         quasi_adaptive_huffman_data_model m_main_table;
         quasi_adaptive_huffman_data_model m_rep_len_table[2];
         quasi_adaptive_huffman_data_model m_large_len_table[2];
         quasi_adaptive_huffman_data_model m_dist_lsb_table;
      };

      class tracked_stat
      {
      public:
         tracked_stat() { clear(); }

         void clear() { m_num = 0; m_total = 0.0f; m_total2 = 0.0f; m_min_val = 9e+99; m_max_val = -9e+99; }
         
         void update(double val) { m_num++; m_total += val; m_total2 += val * val; m_min_val = LZHAM_MIN(m_min_val, val); m_max_val = LZHAM_MAX(m_max_val, val); }

         tracked_stat &operator += (double val) { update(val); return *this; }
         operator double() const { return m_total; }
         
         uint64 get_number_of_values() { return m_num; }
         uint32 get_number_of_values32() { return static_cast<uint32>(LZHAM_MIN(UINT_MAX, m_num)); }
         double get_total() const { return m_total; }
         double get_average() const { return m_num ? m_total / m_num : 0.0f; };
         double get_std_dev() const { return m_num ? sqrt( m_num * m_total2 - m_total * m_total ) / m_num: 0.0f; }
         double get_min_val() const { return m_num ? m_min_val : 0.0f; }
         double get_max_val() const { return m_num ? m_max_val : 0.0f; }

      private:
         uint64 m_num;
         double m_total;
         double m_total2;
         double m_min_val;
         double m_max_val;
      };

      struct coding_stats
      {
         coding_stats() { clear(); }

         void clear();

         void update(const lzdecision& lzdec, const state& cur_state, const search_accelerator& dict, bit_cost_t cost);
         void print();

         uint m_total_bytes;
         uint m_total_contexts;
         double m_total_cost;

         tracked_stat m_context_stats;

         double m_total_match_bits_cost;
         double m_worst_match_bits_cost;
         double m_total_is_match0_bits_cost;
         double m_total_is_match1_bits_cost;
         
         uint m_total_truncated_matches;
         uint m_match_truncation_len_hist[CLZBase::cMaxMatchLen + 1];
         uint m_match_truncation_hist[CLZBase::cMaxMatchLen + 1];
         uint m_match_type_truncation_hist[CLZBase::cNumStates][5];
         uint m_match_type_was_not_truncated_hist[CLZBase::cNumStates][5];
                           
         uint m_total_nonmatches;
         uint m_total_matches;
         
         tracked_stat m_lit_stats;
         tracked_stat m_delta_lit_stats;
         
         tracked_stat m_rep_stats[CLZBase::cMatchHistSize];
         tracked_stat m_rep0_len1_stats;
         tracked_stat m_rep0_len2_plus_stats;

         tracked_stat m_full_match_stats[cMaxMatchLen + 1];
                  
         uint m_total_far_len2_matches;
         uint m_total_near_len2_matches;

         uint m_total_update_rate_resets;

         uint m_max_len2_dist;
      };

      init_params m_params;
      comp_settings m_settings;

      int64 m_src_size;
      uint32 m_src_adler32;

      search_accelerator m_accel;

      symbol_codec m_codec;

      coding_stats m_stats;

      byte_vec m_block_buf;
      byte_vec m_comp_buf;

      uint m_step;

      uint m_block_start_dict_ofs;

      uint m_block_index;

      bool m_finished;
      bool m_use_task_pool;
      bool m_use_extreme_parsing;
            
      struct node_state
      {
         LZHAM_FORCE_INLINE void clear()
         {
            m_total_cost = cBitCostMax; //math::cNearlyInfinite;
            m_total_complexity = UINT_MAX;
         }
         
         // the lzdecision that led from parent to this node_state
         lzdecision m_lzdec;                 
         
         // This is either the state of the parent node (optimal parsing), or the state of the child node (extreme parsing).
         state::state_base m_saved_state;     
         
         // Total cost to arrive at this node state.
         bit_cost_t m_total_cost;                 
         uint m_total_complexity;
         
         // Parent node index.
         int16 m_parent_index;               
         
         // Parent node state index (only valid when extreme parsing).
         int8 m_parent_state_index;          
      };

      struct node
      {
         LZHAM_FORCE_INLINE void clear()
         {
            m_num_node_states = 0;
         }
         
         uint m_num_node_states;                                    
         
         node_state m_node_states[cMaxParseNodeStates];
         
         void add_state(int parent_index, int parent_state_index, const lzdecision &lzdec, const state &parent_state, bit_cost_t total_cost, uint total_complexity, uint max_parse_node_states);
      };

      state m_start_of_block_state;             // state at start of block
      
      state m_state;                            // main thread's current coding state

      struct raw_parse_thread_state
      {
         uint m_start_ofs;
         uint m_bytes_to_match;

         state m_state;
         state *m_pState;

         node m_nodes[cMaxParseGraphNodes + 1];
                                   
         lzham::vector<lzdecision> m_best_decisions;
         bool m_emit_decisions_backwards;

         lzham::vector<lzpriced_decision> m_temp_decisions;

         uint m_max_parse_node_states;
         uint m_max_greedy_decisions;
         uint m_greedy_parse_total_bytes_coded;

         uint m_parse_early_out_thresh;
         uint m_bytes_actually_parsed;

         bool m_greedy_parse_gave_up;
         
         bool m_issue_reset_state_partial;
         bool m_failed;
         bool m_use_semaphore;

         semaphore m_finished;

         bool init(lzcompressor& lzcomp, const lzcompressor::init_params &params);
                  
         void set_malloc_context(lzham_malloc_context malloc_context)
         {
            m_state.set_malloc_context(malloc_context);
            m_best_decisions.set_malloc_context(malloc_context);
            m_temp_decisions.set_malloc_context(malloc_context);
         }
      };

      struct parse_thread_state : raw_parse_thread_state
      {
         uint8 m_unused_alignment_array[128 - (sizeof(raw_parse_thread_state) & 127)];
      };

      uint m_fast_bytes;

      uint m_num_parse_threads;
      parse_thread_state m_parse_thread_state[cMaxParseThreads + 1]; // +1 extra for the greedy parser thread (only used for delta compression)
                                    
      bool send_zlib_header();
      bool init_seed_bytes();
      bool send_final_block();
      bool send_configuration();
      bool extreme_parse(parse_thread_state &parse_state);
      bool optimal_parse(parse_thread_state &parse_state);
      int enumerate_lz_decisions(uint ofs, const state& cur_state, lzham::vector<lzpriced_decision>& decisions, uint min_match_len, uint max_match_len);
      bool greedy_parse(parse_thread_state &parse_state);
      void parse_job_callback(uint64 data, void* pData_ptr);
      bool compress_block(const void* pBuf, uint buf_len);
      bool compress_block_internal(const void* pBuf, uint buf_len);
      bool code_decision(lzdecision lzdec, uint& cur_ofs, uint& bytes_to_match);
      bool send_sync_block(lzham_flush_t flush_type);
   };

} // namespace lzham



