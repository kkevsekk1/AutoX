// File: lzham_match_accel.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once
#include "lzham_lzbase.h"
#include "lzham_threading.h"

namespace lzham
{
   const uint cMatchAccelMaxSupportedProbes = 128;
   const uint cMatchAccelMaxSupportedThreads = 32;
      
   struct node
   {
      uint m_left;
      uint m_right;
   };
   
   LZHAM_DEFINE_BITWISE_MOVABLE(node);
   
#pragma pack(push, 1)      
   struct dict_match
   {
      uint m_dist;
      uint16 m_len;

      inline uint get_dist() const { return m_dist & 0x7FFFFFFF; }
      inline uint get_len() const { return m_len + 2; }
      inline bool is_last() const { return (int)m_dist < 0; }
   };
#pragma pack(pop)  

   LZHAM_DEFINE_BITWISE_MOVABLE(dict_match);
   
   class search_accelerator
   {
      LZHAM_NO_COPY_OR_ASSIGNMENT_OP(search_accelerator);

   public:
      search_accelerator(lzham_malloc_context malloc_context);

      lzham_malloc_context get_malloc_context() const { return m_malloc_context; }

      // If all_matches is true, the match finder returns all found matches with no filtering.
      // Otherwise, the finder will tend to return lists of matches with mostly unique lengths.
      // For each length, it will discard matches with worse distances (in the coding sense).
      enum 
      {
         cFlagDeterministic = 1,
         cFlagLen2Matches = 2,
         cFlagHash24 = 4
      };

      bool init(CLZBase* pLZBase, task_pool* pPool, uint max_helper_threads, uint max_dict_size, uint max_matches, bool all_matches, uint max_probes, uint flags);
      
      void reset();
      void flush();
      
      inline uint get_max_dict_size() const { return m_max_dict_size; }
      inline uint get_max_dict_size_mask() const { return m_max_dict_size_mask; }
      inline uint get_cur_dict_size() const { return m_cur_dict_size; }
      
      inline uint get_lookahead_pos() const { return m_lookahead_pos; }
      inline uint get_lookahead_size() const { return m_lookahead_size; }
      
      inline uint get_char(int delta_pos) const { return m_dict[(m_lookahead_pos + delta_pos) & m_max_dict_size_mask]; }
      inline uint get_char(uint cur_dict_pos, int delta_pos) const { return m_dict[(cur_dict_pos + delta_pos) & m_max_dict_size_mask]; }
      inline const uint8* get_ptr(uint pos) const { return &m_dict[pos]; }
      
      uint get_max_helper_threads() const { return m_max_helper_threads; }
      
      inline uint operator[](uint pos) const { return m_dict[pos]; }
            
      uint get_max_add_bytes() const;
      bool add_bytes_begin(uint num_bytes, const uint8* pBytes);
      inline atomic32_t get_num_completed_helper_threads() const { return m_num_completed_helper_threads; }
      void add_bytes_end();

      // Returns the lookahead's raw position/size/dict_size at the time add_bytes_begin() is called.
      inline uint get_fill_lookahead_pos() const { return m_fill_lookahead_pos; }
      inline uint get_fill_lookahead_size() const { return m_fill_lookahead_size; }
      inline uint get_fill_dict_size() const { return m_fill_dict_size; }
      
      uint get_len2_match(uint lookahead_ofs);
      dict_match* find_matches(uint lookahead_ofs, bool spin = true);
            
      void advance_bytes(uint num_bytes);
      
      LZHAM_FORCE_INLINE uint get_match_len(uint lookahead_ofs, int dist, uint max_match_len, uint start_match_len = 0) const
      {
         LZHAM_ASSERT(lookahead_ofs < m_lookahead_size);
         LZHAM_ASSERT(start_match_len <= max_match_len);
         LZHAM_ASSERT(max_match_len <= (get_lookahead_size() - lookahead_ofs));

         const int find_dict_size = m_cur_dict_size + lookahead_ofs;
         if (dist > find_dict_size)
            return 0;

         const uint comp_pos = static_cast<uint>((m_lookahead_pos + lookahead_ofs - dist) & m_max_dict_size_mask);
         const uint lookahead_pos = (m_lookahead_pos + lookahead_ofs) & m_max_dict_size_mask;
         
         const uint8* pComp = &m_dict[comp_pos];
         const uint8* pLookahead = &m_dict[lookahead_pos];
         
         uint match_len;
         for (match_len = start_match_len; match_len < max_match_len; match_len++)
            if (pComp[match_len] != pLookahead[match_len])
               break;

         return match_len;
      }
                  
   public:
      lzham_malloc_context m_malloc_context;

      CLZBase* m_pLZBase;
      task_pool* m_pTask_pool;
      uint m_max_helper_threads;
   
      uint m_max_dict_size;
      uint m_max_dict_size_mask;
      
      uint m_lookahead_pos;
      uint m_lookahead_size;
                  
      uint m_cur_dict_size;
            
      lzham::vector<uint8> m_dict;
            
      lzham::vector<uint> m_hash;
      lzham::vector<node> m_nodes;

      lzham::vector<dict_match> m_matches;
      lzham::vector<atomic32_t> m_match_refs;
                  
      enum { cDigramHashSize = 4096 };
      lzham::vector<uint> m_digram_hash;
      lzham::vector<uint> m_digram_next;

      lzham::vector<uint> m_thread_dict_offsets[cMatchAccelMaxSupportedThreads];
                                          
      uint m_fill_lookahead_pos;
      uint m_fill_lookahead_size;
      uint m_fill_dict_size;
      
      uint m_max_probes;
      uint m_max_matches;
      
      bool m_all_matches;

      bool m_deterministic;
      bool m_len2_matches;
      bool m_hash24;
                  
      volatile atomic32_t m_next_match_ref;
      
      volatile atomic32_t m_num_completed_helper_threads;
                  
      void find_all_matches_callback_st(uint64 data, void* pData_ptr);
      void find_all_matches_callback_mt(uint64 data, void* pData_ptr);
      bool find_all_matches(uint num_bytes);
      bool find_len2_matches();
   };

} // namespace lzham
