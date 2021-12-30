// File: lzham_lzdecomp.cpp
// See Copyright Notice and license at the end of include/lzham.h
//
// See "Coroutines in C":
// http://www.chiark.greenend.org.uk/~sgtatham/coroutines.html
// Also see "Protothreads - Lightweight, Stackless Threads in C":
// http://www.sics.se/~adam/pt/
#include "lzham_core.h"
#include "lzham_decomp.h"
#include "lzham_symbol_codec.h"
#include "lzham_checksum.h"
#include "lzham_lzdecompbase.h"

using namespace lzham;

// We currently only really need ~35K, adding a few K as a fudge factor.
#define LZHAM_DECOMP_MEMORY_ARENA_SIZE (38*1024)

namespace lzham
{
   static const uint8 s_literal_next_state[24] =
   {
      0, 0, 0, 0, 1, 2, 3, // 0-6: literal states
      4, 5, 6, 4, 5,       // 7-11: match states
      7, 7, 7, 7, 7, 7, 7, 10, 10, 10, 10, 10   // 12-23: unused
   };
   
   static const uint s_huge_match_base_len[4] = { CLZDecompBase::cMaxMatchLen + 1, CLZDecompBase::cMaxMatchLen + 1 + 256, CLZDecompBase::cMaxMatchLen + 1 + 256 + 1024, CLZDecompBase::cMaxMatchLen + 1 + 256 + 1024 + 4096 };
   static const uint8 s_huge_match_code_len[4] = { 8, 10, 12, 16 };

   struct lzham_decompressor
   {
      lzham_decompressor(lzham_malloc_context malloc_context) : 
         m_malloc_context(malloc_context),
         m_codec(malloc_context),
         m_lit_table(malloc_context),
         m_delta_lit_table(malloc_context),
         m_main_table(malloc_context),
         m_dist_lsb_table(malloc_context)
      {
         for (uint i = 0; i < 2; i++)
         {
            m_rep_len_table[i].set_malloc_context(malloc_context);
            m_large_len_table[i].set_malloc_context(malloc_context);
         }
      }

      void init();
      
      template<bool unbuffered> lzham_decompress_status_t decompress();
      
      void reset_huff_tables();
      void reset_arith_tables();
      void reset_all_tables();
      void reset_huffman_table_update_rates();

      lzham_malloc_context m_malloc_context;

      int m_state;

      CLZDecompBase m_lzBase;
      symbol_codec m_codec;

      uint32 m_raw_decomp_buf_size;
      uint8 *m_pRaw_decomp_buf;
      uint8 *m_pDecomp_buf;
      uint32 m_decomp_adler32;

      const uint8 *m_pIn_buf;
      size_t *m_pIn_buf_size;
      uint8 *m_pOut_buf;
      size_t *m_pOut_buf_size;
      bool m_no_more_input_bytes_flag;

      uint8 *m_pOrig_out_buf;
      size_t m_orig_out_buf_size;

      lzham_decompress_params m_params;

      lzham_decompress_status_t m_status;
                              
      quasi_adaptive_huffman_data_model m_lit_table;
      quasi_adaptive_huffman_data_model m_delta_lit_table;
      quasi_adaptive_huffman_data_model m_main_table;
      quasi_adaptive_huffman_data_model m_rep_len_table[2];
      quasi_adaptive_huffman_data_model m_large_len_table[2];
      quasi_adaptive_huffman_data_model m_dist_lsb_table;

      adaptive_bit_model m_is_match_model[CLZDecompBase::cNumStates];
      adaptive_bit_model m_is_rep_model[CLZDecompBase::cNumStates];
      adaptive_bit_model m_is_rep0_model[CLZDecompBase::cNumStates];
      adaptive_bit_model m_is_rep0_single_byte_model[CLZDecompBase::cNumStates];
      adaptive_bit_model m_is_rep1_model[CLZDecompBase::cNumStates];
      adaptive_bit_model m_is_rep2_model[CLZDecompBase::cNumStates];
      
      uint m_dst_ofs;
      uint m_dst_highwater_ofs;

      uint m_step;
      uint m_block_step;
      uint m_initial_step;

      uint m_block_index;

      int m_match_hist0;
      int m_match_hist1;
      int m_match_hist2;
      int m_match_hist3;
      uint m_cur_state;

      uint m_start_block_dst_ofs;
            
      uint m_block_type;

      const uint8 *m_pFlush_src;
      size_t m_flush_num_bytes_remaining;
      size_t m_flush_n;

      uint m_seed_bytes_to_ignore_when_flushing;

      uint m_file_src_file_adler32;

      uint m_rep_lit0;
      uint m_match_len;
      uint m_match_slot;
      uint m_extra_bits;
      uint m_num_extra_bits;

      uint m_src_ofs;
      const uint8* m_pCopy_src;
      uint m_num_raw_bytes_remaining;

      uint m_debug_is_match;
      uint m_debug_match_len;
      uint m_debug_match_dist;
      uint m_debug_lit;

      lzham_decompress_status_t m_z_last_status;
      uint m_z_first_call;
      uint m_z_has_flushed;
      uint m_z_cmf;
      uint m_z_flg;
      uint m_z_dict_adler32;

      uint m_tmp;
   };

   // Ordinarily I dislike macros like this, but in this case I think using them makes the decompression function easier to follow.

   // Coroutine helpers.
   #define LZHAM_CR_INITIAL_STATE 0
   #define LZHAM_CR_BEGIN(state) switch( state ) { case LZHAM_CR_INITIAL_STATE:
   #define LZHAM_CR_RETURN(state, result) do { state = __LINE__; return (result); case __LINE__:; } while (0)
   #define LZHAM_CR_FINISH }

   // Helpers to save/restore local variables (hopefully CPU registers) to memory.
   #define LZHAM_RESTORE_STATE LZHAM_RESTORE_LOCAL_STATE \
      match_hist0 = m_match_hist0; match_hist1 = m_match_hist1; match_hist2 = m_match_hist2; match_hist3 = m_match_hist3; \
      cur_state = m_cur_state; dst_ofs = m_dst_ofs;
      
   #define LZHAM_SAVE_STATE LZHAM_SAVE_LOCAL_STATE \
      m_match_hist0 = match_hist0; m_match_hist1 = match_hist1; m_match_hist2 = match_hist2; m_match_hist3 = match_hist3; \
      m_cur_state = cur_state; m_dst_ofs = dst_ofs;
      
   // Helper that coroutine returns to the caller with a request for more input bytes.
   #define LZHAM_DECODE_NEEDS_BYTES \
      LZHAM_SAVE_STATE \
      for ( ; ; ) \
      { \
         *m_pIn_buf_size = static_cast<size_t>(m_codec.decode_get_bytes_consumed()); \
         *m_pOut_buf_size = 0; \
         LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_NEEDS_MORE_INPUT); \
         m_codec.decode_set_input_buffer(m_pIn_buf, *m_pIn_buf_size, m_pIn_buf, m_no_more_input_bytes_flag); \
         if ((m_codec.m_decode_buf_eof) || (m_codec.m_decode_buf_size)) break; \
      } \
      LZHAM_RESTORE_STATE

   #if LZHAM_PLATFORM_X360
      #define LZHAM_BULK_MEMCPY XMemCpy
      #define LZHAM_MEMCPY memcpy
   #else
      #define LZHAM_BULK_MEMCPY memcpy
      #define LZHAM_MEMCPY memcpy
   #endif

   // Flush the output buffer/dictionary by doing a coroutine return to the caller.
   // Buffered mode only.
   #define LZHAM_FLUSH_DICT_TO_OUTPUT_BUFFER(dict_ofs) \
      LZHAM_SAVE_STATE \
      m_pFlush_src = m_pDecomp_buf + m_seed_bytes_to_ignore_when_flushing + m_dst_highwater_ofs; \
      m_flush_num_bytes_remaining = dict_ofs - m_seed_bytes_to_ignore_when_flushing - m_dst_highwater_ofs; \
      m_seed_bytes_to_ignore_when_flushing = 0; \
      m_dst_highwater_ofs = dict_ofs & dict_size_mask; \
      while (m_flush_num_bytes_remaining) \
      { \
         m_flush_n = LZHAM_MIN(m_flush_num_bytes_remaining, *m_pOut_buf_size); \
         if (0 == (m_params.m_decompress_flags & LZHAM_DECOMP_FLAG_COMPUTE_ADLER32)) \
         { \
            LZHAM_BULK_MEMCPY(m_pOut_buf, m_pFlush_src, m_flush_n); \
         } \
         else \
         { \
            size_t copy_ofs = 0; \
            while (copy_ofs < m_flush_n) \
            { \
               const uint cBytesToMemCpyPerIteration = 8192U; \
               size_t bytes_to_copy = LZHAM_MIN((size_t)(m_flush_n - copy_ofs), cBytesToMemCpyPerIteration); \
               LZHAM_MEMCPY(m_pOut_buf + copy_ofs, m_pFlush_src + copy_ofs, bytes_to_copy); \
               m_decomp_adler32 = adler32(m_pFlush_src + copy_ofs, bytes_to_copy, m_decomp_adler32); \
               copy_ofs += bytes_to_copy; \
            } \
         } \
         *m_pIn_buf_size = static_cast<size_t>(m_codec.decode_get_bytes_consumed()); \
         *m_pOut_buf_size = m_flush_n; \
         LZHAM_CR_RETURN(m_state, m_flush_n ? LZHAM_DECOMP_STATUS_NOT_FINISHED : LZHAM_DECOMP_STATUS_HAS_MORE_OUTPUT); \
         m_codec.decode_set_input_buffer(m_pIn_buf, *m_pIn_buf_size, m_pIn_buf, m_no_more_input_bytes_flag); \
         m_pFlush_src += m_flush_n; \
         m_flush_num_bytes_remaining -= m_flush_n; \
      } \
      LZHAM_RESTORE_STATE

   #define LZHAM_DECOMPRESS_DECODE_ADAPTIVE_SYMBOL(codec, result, model) LZHAM_SYMBOL_CODEC_DECODE_ADAPTIVE_HUFFMAN(codec, result, model)
   
   //------------------------------------------------------------------------------------------------------------------
   void lzham_decompressor::init()
   {
      m_lzBase.init_position_slots(m_params.m_dict_size_log2);

#ifdef LZHAM_LZDEBUG
      if (m_pDecomp_buf)
         memset(m_pDecomp_buf, 0xCE, 1U << m_params.m_dict_size_log2);
#endif

      m_state = LZHAM_CR_INITIAL_STATE;
      m_step = 0;
      m_block_step = 0;
      m_block_index = 0;
      m_initial_step = 0;
            
      m_dst_ofs = 0;
      m_dst_highwater_ofs = 0;

      m_pIn_buf = NULL;
      m_pIn_buf_size = NULL;
      m_pOut_buf = NULL;
      m_pOut_buf_size = NULL;
      m_no_more_input_bytes_flag = false;
      m_status = LZHAM_DECOMP_STATUS_NOT_FINISHED;
      m_pOrig_out_buf = NULL;
      m_orig_out_buf_size = 0;
      m_decomp_adler32 = cInitAdler32;
      m_seed_bytes_to_ignore_when_flushing = 0;
      
      m_z_last_status = LZHAM_DECOMP_STATUS_NOT_FINISHED;
      m_z_first_call = 1;
      m_z_has_flushed = 0;
      m_z_cmf = 0;
      m_z_flg = 0;
      m_z_dict_adler32 = 0;

      m_tmp = 0;

      m_match_hist0 = 0;
      m_match_hist1 = 0;
      m_match_hist2 = 0;
      m_match_hist3 = 0;
      m_cur_state = 0;
      
      m_start_block_dst_ofs = 0;
      m_block_type = 0;
      m_flush_num_bytes_remaining = 0;
      m_flush_n = 0;
      m_file_src_file_adler32 = 0;
      m_rep_lit0 = 0;
      m_match_len = 0;
      m_match_slot = 0;
      m_extra_bits = 0;
      m_num_extra_bits = 0;
      m_src_ofs = 0;
      m_pCopy_src = NULL;
      m_num_raw_bytes_remaining = 0;
      
      m_codec.clear();
   }

   void lzham_decompressor::reset_huff_tables()
   {
      m_lit_table.reset();
     m_delta_lit_table.reset();
      
      m_main_table.reset();

      for (uint i = 0; i < LZHAM_ARRAY_SIZE(m_rep_len_table); i++)
         m_rep_len_table[i].reset();

      for (uint i = 0; i < LZHAM_ARRAY_SIZE(m_large_len_table); i++)
         m_large_len_table[i].reset();

      m_dist_lsb_table.reset();
   }

   void lzham_decompressor::reset_arith_tables()
   {
      for (uint i = 0; i < LZHAM_ARRAY_SIZE(m_is_match_model); i++)
         m_is_match_model[i].clear();

      for (uint i = 0; i < CLZDecompBase::cNumStates; i++)
      {
         m_is_rep_model[i].clear();
         m_is_rep0_model[i].clear();
         m_is_rep0_single_byte_model[i].clear();
         m_is_rep1_model[i].clear();
         m_is_rep2_model[i].clear();
      }
   }

   void lzham_decompressor::reset_all_tables()
   {
      reset_huff_tables();

      reset_arith_tables();
   }

   void lzham_decompressor::reset_huffman_table_update_rates()
   {
      m_lit_table.reset_update_rate();
      m_delta_lit_table.reset_update_rate();

      m_main_table.reset_update_rate();

      for (uint i = 0; i < LZHAM_ARRAY_SIZE(m_rep_len_table); i++)
         m_rep_len_table[i].reset_update_rate();

      for (uint i = 0; i < LZHAM_ARRAY_SIZE(m_large_len_table); i++)
         m_large_len_table[i].reset_update_rate();

      m_dist_lsb_table.reset_update_rate();
   }
      
   //------------------------------------------------------------------------------------------------------------------
   // Decompression method. Implemented as a coroutine so it can be paused and resumed to support streaming.
   //------------------------------------------------------------------------------------------------------------------
   template<bool unbuffered>
   lzham_decompress_status_t lzham_decompressor::decompress()
   {
      // Important: This function is a coroutine. ANY locals variables that need to be preserved across coroutine
      // returns must be either be a member variable, or a local which is saved/restored to a member variable at
      // the right times. (This makes this function difficult to follow and freaking ugly due to the macros of doom - but hey it works.)
      // The most often used variables are in locals so the compiler hopefully puts them into CPU registers.
      symbol_codec &codec = m_codec;
      const uint dict_size = 1U << m_params.m_dict_size_log2;
      const uint dict_size_mask = unbuffered ? UINT_MAX : (dict_size - 1);

      int match_hist0 = 0, match_hist1 = 0, match_hist2 = 0, match_hist3 = 0;
      uint cur_state = 0, dst_ofs = 0;
      
      const size_t out_buf_size = *m_pOut_buf_size;
      
      uint8* pDst = unbuffered ? reinterpret_cast<uint8*>(m_pOut_buf) : reinterpret_cast<uint8*>(m_pDecomp_buf);
      uint8* pDst_end = unbuffered ?  (reinterpret_cast<uint8*>(m_pOut_buf) + out_buf_size) : (reinterpret_cast<uint8*>(m_pDecomp_buf) + dict_size);      
      
      LZHAM_SYMBOL_CODEC_DECODE_DECLARE(codec);

#define LZHAM_SAVE_LOCAL_STATE
#define LZHAM_RESTORE_LOCAL_STATE

      // Important: Do not use any switch() statements below here.
      LZHAM_CR_BEGIN(m_state)

      if ((!unbuffered) && (m_params.m_num_seed_bytes))
      {
         LZHAM_BULK_MEMCPY(pDst, m_params.m_pSeed_bytes, m_params.m_num_seed_bytes);
         dst_ofs += m_params.m_num_seed_bytes;
         if (dst_ofs >= dict_size)
            dst_ofs = 0;
         else
            m_seed_bytes_to_ignore_when_flushing = dst_ofs;
      }
      
      if (!m_codec.start_decoding(m_pIn_buf, *m_pIn_buf_size, m_no_more_input_bytes_flag, NULL, NULL))
      {
         LZHAM_LOG_ERROR(1020);
         return LZHAM_DECOMP_STATUS_FAILED_INITIALIZING;
      }

      LZHAM_SYMBOL_CODEC_DECODE_BEGIN(codec);

      {
         if (m_params.m_decompress_flags & LZHAM_DECOMP_FLAG_READ_ZLIB_STREAM)
         {
            uint check;
            LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, m_z_cmf, 8);
            LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, m_z_flg, 8);
            check = ((m_z_cmf << 8) + m_z_flg) % 31;
            if ((check != 0) || ((m_z_cmf & 15) != LZHAM_Z_LZHAM))
            {
               LZHAM_LOG_ERROR(1021);
               return LZHAM_DECOMP_STATUS_FAILED_BAD_ZLIB_HEADER;
            }
            if (m_z_flg & 32)
            {
               if ((!m_params.m_pSeed_bytes) || (unbuffered))
               {
                  LZHAM_LOG_ERROR(1022);
                  return LZHAM_DECOMP_STATUS_FAILED_NEED_SEED_BYTES;
               }
               m_z_dict_adler32 = 0;
               for (m_tmp = 0; m_tmp < 4; ++m_tmp)
               {
                  uint n; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, n, 8);
                  m_z_dict_adler32 = (m_z_dict_adler32 << 8) | n;
               }
               if (adler32(m_params.m_pSeed_bytes, m_params.m_num_seed_bytes) != m_z_dict_adler32)
               {
                  LZHAM_LOG_ERROR(1023);
                  return LZHAM_DECOMP_STATUS_FAILED_BAD_SEED_BYTES;
               }
            }
         }

         {
            // Was written by lzcompressor::send_configuration().
            //uint tmp;
            //LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, tmp, 2);
         }

         uint max_update_interval = m_params.m_table_max_update_interval, update_interval_slow_rate =  m_params.m_table_update_interval_slow_rate;
         if (!max_update_interval && !update_interval_slow_rate)
         {
            uint rate = m_params.m_table_update_rate;
            if (!rate)
               rate = LZHAM_DEFAULT_TABLE_UPDATE_RATE;
            rate = math::clamp<uint>(rate, 1, LZHAM_FASTEST_TABLE_UPDATE_RATE) - 1;
            max_update_interval = g_table_update_settings[rate].m_max_update_interval;
            update_interval_slow_rate = g_table_update_settings[rate].m_slow_rate;
         }

         bool succeeded = m_lit_table.init2(m_malloc_context, false, 256, max_update_interval, update_interval_slow_rate, NULL);
         succeeded = succeeded && m_delta_lit_table.assign(m_lit_table);
         
         succeeded = succeeded && m_main_table.init2(m_malloc_context, false, CLZDecompBase::cLZXNumSpecialLengths + (m_lzBase.m_num_lzx_slots - CLZDecompBase::cLZXLowestUsableMatchSlot) * 8, max_update_interval, update_interval_slow_rate, NULL);

         succeeded = succeeded && m_rep_len_table[0].init2(m_malloc_context, false, CLZDecompBase::cNumHugeMatchCodes + (CLZDecompBase::cMaxMatchLen - CLZDecompBase::cMinMatchLen + 1), max_update_interval, update_interval_slow_rate, NULL);
         succeeded = succeeded && m_rep_len_table[1].assign(m_rep_len_table[0]);

         succeeded = succeeded && m_large_len_table[0].init2(m_malloc_context, false, CLZDecompBase::cNumHugeMatchCodes + CLZDecompBase::cLZXNumSecondaryLengths, max_update_interval, update_interval_slow_rate, NULL);
         succeeded = succeeded && m_large_len_table[1].assign(m_large_len_table[0]);

         succeeded = succeeded && m_dist_lsb_table.init2(m_malloc_context, false, 16, max_update_interval, update_interval_slow_rate, NULL);
         if (!succeeded)
         {
            LZHAM_LOG_ERROR(1024);
            return LZHAM_DECOMP_STATUS_FAILED_INITIALIZING;
         }
      }
      
      // Output block loop.
      do
      {
#ifdef LZHAM_LZDEBUG
         uint outer_sync_marker; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, k, 12);
         LZHAM_VERIFY(outer_sync_marker == 166);
#endif
         
         // Decode block type.
         LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, m_block_type, CLZDecompBase::cBlockHeaderBits);

         if (m_block_type == CLZDecompBase::cSyncBlock)
         {
            // Sync block
            // Reset either the symbol table update rates, or all statistics, then force a coroutine return to give the caller a chance to handle the output right now.
            LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, m_tmp, CLZDecompBase::cBlockFlushTypeBits);

            // See lzcompressor::send_sync_block() (TODO: make these an enum)
            if (m_tmp == 1)
               reset_huffman_table_update_rates();
            else if (m_tmp == 2)
               reset_all_tables();

            LZHAM_SYMBOL_CODEC_DECODE_ALIGN_TO_BYTE(codec);

            uint n; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, n, 16);
            if (n != 0)
            {
               LZHAM_SYMBOL_CODEC_DECODE_END(codec);
               *m_pIn_buf_size = static_cast<size_t>(codec.decode_get_bytes_consumed());
               *m_pOut_buf_size = 0;
               for ( ; ; ) { LZHAM_LOG_ERROR(1025); LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_FAILED_BAD_SYNC_BLOCK); }
            }

            LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, n, 16);
            if (n != 0xFFFF)
            {
               LZHAM_SYMBOL_CODEC_DECODE_END(codec);
               *m_pIn_buf_size = static_cast<size_t>(codec.decode_get_bytes_consumed());
               *m_pOut_buf_size = 0;
               for ( ; ; ) { LZHAM_LOG_ERROR(1026); LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_FAILED_BAD_SYNC_BLOCK); }
            }
            
            // See lzcompressor::send_sync_block() (TODO: make these an enum)            
            if ((m_tmp == 2) || (m_tmp == 3))
            {
               // It's a sync or full flush, so immediately give caller whatever output we have. Also gives the caller a chance to reposition the input stream ptr somewhere else before continuing.
               LZHAM_SYMBOL_CODEC_DECODE_END(codec);

               if ((!unbuffered) && (dst_ofs))
               {
                  LZHAM_FLUSH_DICT_TO_OUTPUT_BUFFER(dst_ofs);
               }
               else
               {
                  if (unbuffered)
                  {
                     LZHAM_ASSERT(dst_ofs >= m_dst_highwater_ofs);
                  }
                  else
                  {
                     LZHAM_ASSERT(!m_dst_highwater_ofs);
                  }
                  
                  // unbuffered, or dst_ofs==0
                  *m_pIn_buf_size = static_cast<size_t>(codec.decode_get_bytes_consumed());
                  *m_pOut_buf_size = dst_ofs - m_dst_highwater_ofs;
                  
                  // Partial/sync flushes in unbuffered mode details:
                  // We assume the caller doesn't move the output buffer between calls AND the pointer to the output buffer input parameter won't change between calls (i.e.
                  // it *always* points to the beginning of the decompressed stream). The caller will need to track the current output buffer offset.
                  m_dst_highwater_ofs = dst_ofs;
                  
                  LZHAM_SAVE_STATE
                  LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_NOT_FINISHED);
                  LZHAM_RESTORE_STATE
                  
                  m_codec.decode_set_input_buffer(m_pIn_buf, *m_pIn_buf_size, m_pIn_buf, m_no_more_input_bytes_flag);
               }
               
               LZHAM_SYMBOL_CODEC_DECODE_BEGIN(codec);
            }
         }
         else if (m_block_type == CLZDecompBase::cRawBlock)
         {
            // Raw block handling is complex because we ultimately want to (safely) handle as many bytes as possible using a small number of memcpy()'s.
            uint num_raw_bytes_remaining;
            num_raw_bytes_remaining = 0;
            
#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE m_num_raw_bytes_remaining = num_raw_bytes_remaining;
#define LZHAM_RESTORE_LOCAL_STATE num_raw_bytes_remaining = m_num_raw_bytes_remaining;

            // Determine how large this raw block is.
            LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, num_raw_bytes_remaining, 24);
            
            // Get and verify raw block length check bits.
            uint num_raw_bytes_check_bits; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, num_raw_bytes_check_bits, 8);
            uint raw_bytes_remaining0, raw_bytes_remaining1, raw_bytes_remaining2;
            raw_bytes_remaining0 = num_raw_bytes_remaining & 0xFF;
            raw_bytes_remaining1 = (num_raw_bytes_remaining >> 8) & 0xFF;
            raw_bytes_remaining2 = (num_raw_bytes_remaining >> 16) & 0xFF;
            if (num_raw_bytes_check_bits != ((raw_bytes_remaining0 ^ raw_bytes_remaining1) ^ raw_bytes_remaining2))
            {
               LZHAM_SYMBOL_CODEC_DECODE_END(codec);
               *m_pIn_buf_size = static_cast<size_t>(codec.decode_get_bytes_consumed());
               *m_pOut_buf_size = 0;
               for ( ; ; ) { LZHAM_LOG_ERROR(1027); LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_FAILED_BAD_RAW_BLOCK); }
            }
            
            num_raw_bytes_remaining++;
            
            // Discard any partial bytes from the bit buffer (align up to the next byte).
            LZHAM_SYMBOL_CODEC_DECODE_ALIGN_TO_BYTE(codec);
            
            // Flush any full bytes from the bit buffer.
            do
            {
               int b;
               LZHAM_SYMBOL_CODEC_DECODE_REMOVE_BYTE_FROM_BIT_BUF(codec, b);
               if (b < 0)
                  break;

               if ((unbuffered) && (dst_ofs >= out_buf_size))
               {
                  LZHAM_SYMBOL_CODEC_DECODE_END(codec);
                  *m_pIn_buf_size = static_cast<size_t>(codec.decode_get_bytes_consumed());
                  *m_pOut_buf_size = 0;
                  for ( ; ; ) { LZHAM_LOG_ERROR(1028); LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_FAILED_DEST_BUF_TOO_SMALL); }
               }

               pDst[dst_ofs++] = static_cast<uint8>(b);

               if ((!unbuffered) && (dst_ofs > dict_size_mask))
               {
                  LZHAM_SYMBOL_CODEC_DECODE_END(codec);
                  LZHAM_FLUSH_DICT_TO_OUTPUT_BUFFER(dict_size);
                  LZHAM_SYMBOL_CODEC_DECODE_BEGIN(codec);
                  dst_ofs = 0;
               }

               num_raw_bytes_remaining--;
            } while (num_raw_bytes_remaining);

            LZHAM_SYMBOL_CODEC_DECODE_END(codec);

            // Now handle the bulk of the raw data with memcpy().
            while (num_raw_bytes_remaining)
            {
               uint64 in_buf_ofs, in_buf_remaining;
               in_buf_ofs = codec.decode_get_bytes_consumed();
               in_buf_remaining = *m_pIn_buf_size - in_buf_ofs;

               while (!in_buf_remaining)
               {
                  // We need more bytes from the caller.
                  *m_pIn_buf_size = static_cast<size_t>(in_buf_ofs);
                  *m_pOut_buf_size = 0;

                  if (m_no_more_input_bytes_flag)
                  {
                     for ( ; ; ) { LZHAM_LOG_ERROR(1029); LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_FAILED_EXPECTED_MORE_RAW_BYTES); }
                  }

                  LZHAM_SAVE_STATE
                  LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_NEEDS_MORE_INPUT);
                  LZHAM_RESTORE_STATE

                  m_codec.decode_set_input_buffer(m_pIn_buf, *m_pIn_buf_size, m_pIn_buf, m_no_more_input_bytes_flag);

                  in_buf_ofs = 0;
                  in_buf_remaining = *m_pIn_buf_size;
               }

               // Determine how many bytes we can safely memcpy() in a single call.
               uint num_bytes_to_copy;
               num_bytes_to_copy = static_cast<uint>(LZHAM_MIN(num_raw_bytes_remaining, in_buf_remaining));
               if (!unbuffered)
                  num_bytes_to_copy = LZHAM_MIN(num_bytes_to_copy, dict_size - dst_ofs);

               if ((unbuffered) && ((dst_ofs + num_bytes_to_copy) > out_buf_size))
               {
                  // Output buffer is not large enough.
                  *m_pIn_buf_size = static_cast<size_t>(in_buf_ofs);
                  *m_pOut_buf_size = 0;
                  for ( ; ; ) { LZHAM_LOG_ERROR(1030); LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_FAILED_DEST_BUF_TOO_SMALL); }
               }

               // Copy the raw bytes.
               LZHAM_BULK_MEMCPY(pDst + dst_ofs, m_pIn_buf + in_buf_ofs, num_bytes_to_copy);

               in_buf_ofs += num_bytes_to_copy;
               num_raw_bytes_remaining -= num_bytes_to_copy;

               codec.decode_set_input_buffer(m_pIn_buf, *m_pIn_buf_size, m_pIn_buf + in_buf_ofs, m_no_more_input_bytes_flag);

               dst_ofs += num_bytes_to_copy;

               if ((!unbuffered) && (dst_ofs > dict_size_mask))
               {
                  LZHAM_ASSERT(dst_ofs == dict_size);

                  LZHAM_FLUSH_DICT_TO_OUTPUT_BUFFER(dict_size);

                  dst_ofs = 0;
               }
            }

            LZHAM_SYMBOL_CODEC_DECODE_BEGIN(codec);

#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE
#define LZHAM_RESTORE_LOCAL_STATE
         }
         else if (m_block_type == CLZDecompBase::cCompBlock)
         {
            LZHAM_SYMBOL_CODEC_DECODE_ARITH_START(codec)

            match_hist0 = 1;
            match_hist1 = 1;
            match_hist2 = 1;
            match_hist3 = 1;
            cur_state = 0;
                        
            m_start_block_dst_ofs = dst_ofs;

            {
               uint block_flush_type; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, block_flush_type, CLZDecompBase::cBlockFlushTypeBits);
               if (block_flush_type == 1)
                  reset_huffman_table_update_rates();
               else if (block_flush_type == 2)
                  reset_all_tables();
            }

#ifdef LZHAM_LZDEBUG
            m_initial_step = m_step;
            m_block_step = 0;
            for ( ; ; m_step++, m_block_step++)
#else
            for ( ; ; )
#endif
            {
#ifdef LZHAM_LZDEBUG
               uint sync_marker; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, x, CLZDecompBase::cLZHAMDebugSyncMarkerBits);
               LZHAM_VERIFY(sync_marker == CLZDecompBase::cLZHAMDebugSyncMarkerValue);

               LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, m_debug_is_match, 1);
               LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, m_debug_match_len, 17);

               uint debug_cur_state; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, debug_cur_state, 4);
               LZHAM_VERIFY(cur_state == debug_cur_state);
#endif

               // Read "is match" bit.
               uint match_model_index;
               match_model_index = LZHAM_IS_MATCH_MODEL_INDEX(cur_state);
               LZHAM_ASSERT(match_model_index < LZHAM_ARRAY_SIZE(m_is_match_model));

               uint is_match_bit; LZHAM_SYMBOL_CODEC_DECODE_ARITH_BIT(codec, is_match_bit, m_is_match_model[match_model_index]);

#ifdef LZHAM_LZDEBUG
               LZHAM_VERIFY(is_match_bit == m_debug_is_match);
#endif

               if (LZHAM_BUILTIN_EXPECT(!is_match_bit, 0))
               {
                  // Handle literal.

#ifdef LZHAM_LZDEBUG
                  LZHAM_VERIFY(m_debug_match_len == 1);
#endif

#ifdef LZHAM_LZDEBUG
                  LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, m_debug_lit, 8);
#endif
                  
                  if ((unbuffered) && (LZHAM_BUILTIN_EXPECT(dst_ofs >= out_buf_size, 0)))
                  {
                     LZHAM_SYMBOL_CODEC_DECODE_END(codec);
                     *m_pIn_buf_size = static_cast<size_t>(codec.decode_get_bytes_consumed());
                     *m_pOut_buf_size = 0;
                     for ( ; ; ) { LZHAM_LOG_ERROR(1031); LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_FAILED_DEST_BUF_TOO_SMALL); }
                  }

                  if (LZHAM_BUILTIN_EXPECT(cur_state < CLZDecompBase::cNumLitStates, 1))
                  {
                     // Regular literal
                     uint r; LZHAM_DECOMPRESS_DECODE_ADAPTIVE_SYMBOL(codec, r, m_lit_table);
                     pDst[dst_ofs] = static_cast<uint8>(r);

#ifdef LZHAM_LZDEBUG
                     LZHAM_VERIFY(pDst[dst_ofs] == m_debug_lit);
#endif
                  }
                  else
                  {
                     // Delta literal
                     uint match_hist0_ofs, rep_lit0;

                     // Determine delta literal's partial context.
                     match_hist0_ofs = dst_ofs - match_hist0;
                     rep_lit0 = pDst[match_hist0_ofs & dict_size_mask];
                                          
#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE m_rep_lit0 = rep_lit0;
#define LZHAM_RESTORE_LOCAL_STATE rep_lit0 = m_rep_lit0;

#ifdef LZHAM_LZDEBUG
                     uint debug_rep_lit0; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, debug_rep_lit0, 8);
                     LZHAM_VERIFY(debug_rep_lit0 == rep_lit0);
#endif

                     uint r; LZHAM_DECOMPRESS_DECODE_ADAPTIVE_SYMBOL(codec, r, m_delta_lit_table);
                     r ^= rep_lit0;
                     pDst[dst_ofs] = static_cast<uint8>(r);

#ifdef LZHAM_LZDEBUG
                     LZHAM_VERIFY(pDst[dst_ofs] == m_debug_lit);
#endif

#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE
#define LZHAM_RESTORE_LOCAL_STATE
                  }

                  cur_state = s_literal_next_state[cur_state];

                  dst_ofs++;
                  if ((!unbuffered) && (LZHAM_BUILTIN_EXPECT(dst_ofs > dict_size_mask, 0)))
                  {
                     LZHAM_SYMBOL_CODEC_DECODE_END(codec);
                     LZHAM_FLUSH_DICT_TO_OUTPUT_BUFFER(dict_size);
                     LZHAM_SYMBOL_CODEC_DECODE_BEGIN(codec);
                     dst_ofs = 0;
                  }
               }
               else
               {
                  // Handle match.
                  uint match_len;
                  match_len = 1;

#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE m_match_len = match_len;
#define LZHAM_RESTORE_LOCAL_STATE match_len = m_match_len;

                  // Determine if match is a rep_match, and if so what type.
                  uint is_rep; LZHAM_SYMBOL_CODEC_DECODE_ARITH_BIT(codec, is_rep, m_is_rep_model[cur_state]);
                  if (LZHAM_BUILTIN_EXPECT(is_rep, 1))
                  {
                     uint is_rep0; LZHAM_SYMBOL_CODEC_DECODE_ARITH_BIT(codec, is_rep0, m_is_rep0_model[cur_state]);
                     if (LZHAM_BUILTIN_EXPECT(is_rep0, 1))
                     {
                        uint is_rep0_len1; LZHAM_SYMBOL_CODEC_DECODE_ARITH_BIT(codec, is_rep0_len1, m_is_rep0_single_byte_model[cur_state]);
                        if (LZHAM_BUILTIN_EXPECT(is_rep0_len1, 1))
                        {
                           cur_state = (cur_state < CLZDecompBase::cNumLitStates) ? 9 : 11;
                        }
                        else
                        {
                           LZHAM_DECOMPRESS_DECODE_ADAPTIVE_SYMBOL(codec, match_len, m_rep_len_table[cur_state >= CLZDecompBase::cNumLitStates]);
                           match_len += CLZDecompBase::cMinMatchLen;
                           
                           if (match_len == (CLZDecompBase::cMaxMatchLen + 1))
                           {
                              // Decode "huge" match length.
                              match_len = 0;
                              do 
                              {
                                 uint b; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, b, 1); 
                                 if (!b)
                                    break;
                                 match_len++;
                              } while (match_len < 3);
                              uint k; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, k, s_huge_match_code_len[match_len]);
                              match_len = s_huge_match_base_len[match_len] + k;
                           }

                           cur_state = (cur_state < CLZDecompBase::cNumLitStates) ? 8 : 11;
                        }
                     }
                     else
                     {
                        LZHAM_DECOMPRESS_DECODE_ADAPTIVE_SYMBOL(codec, match_len, m_rep_len_table[cur_state >= CLZDecompBase::cNumLitStates]);
                        match_len += CLZDecompBase::cMinMatchLen;
                        
                        if (match_len == (CLZDecompBase::cMaxMatchLen + 1))
                        {
                           // Decode "huge" match length.
                           match_len = 0;
                           do 
                           {
                              uint b; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, b, 1); 
                              if (!b)
                                 break;
                              match_len++;
                           } while (match_len < 3);
                           uint k; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, k, s_huge_match_code_len[match_len]);
                           match_len = s_huge_match_base_len[match_len] + k;
                        }

                        uint is_rep1; LZHAM_SYMBOL_CODEC_DECODE_ARITH_BIT(codec, is_rep1, m_is_rep1_model[cur_state]);
                        if (LZHAM_BUILTIN_EXPECT(is_rep1, 1))
                        {
                           uint temp = match_hist1;
                           match_hist1 = match_hist0;
                           match_hist0 = temp;
                        }
                        else
                        {
                           uint is_rep2; LZHAM_SYMBOL_CODEC_DECODE_ARITH_BIT(codec, is_rep2, m_is_rep2_model[cur_state]);

                           if (LZHAM_BUILTIN_EXPECT(is_rep2, 1))
                           {
                              // rep2
                              uint temp = match_hist2;
                              match_hist2 = match_hist1;
                              match_hist1 = match_hist0;
                              match_hist0 = temp;
                           }
                           else
                           {
                              // rep3
                              uint temp = match_hist3;
                              match_hist3 = match_hist2;
                              match_hist2 = match_hist1;
                              match_hist1 = match_hist0;
                              match_hist0 = temp;
                           }
                        }

                        cur_state = (cur_state < CLZDecompBase::cNumLitStates) ? 8 : 11;
                     }
                  }
                  else
                  {
                     // Handle normal/full match.
                     uint sym; LZHAM_DECOMPRESS_DECODE_ADAPTIVE_SYMBOL(codec, sym, m_main_table);
                     sym -= CLZDecompBase::cLZXNumSpecialLengths;

                     if (LZHAM_BUILTIN_EXPECT(static_cast<int>(sym) < 0, 0))
                     {
                        // Handle special symbols.
                        if (static_cast<int>(sym) == (CLZDecompBase::cLZXSpecialCodeEndOfBlockCode - CLZDecompBase::cLZXNumSpecialLengths))
                           break;
                        else
                        {
                           // Must be cLZXSpecialCodePartialStateReset.
                           match_hist0 = 1;
                           match_hist1 = 1;
                           match_hist2 = 1;
                           match_hist3 = 1;
                           cur_state = 0;
                           continue;
                        }
                     }

                     // Low 3 bits of symbol = match length category, higher bits = distance category.
                     match_len = (sym & 7) + 2;

                     uint match_slot;
                     match_slot = (sym >> 3) + CLZDecompBase::cLZXLowestUsableMatchSlot;

#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE m_match_len = match_len; m_match_slot = match_slot;
#define LZHAM_RESTORE_LOCAL_STATE match_len = m_match_len; match_slot = m_match_slot;

                     if (LZHAM_BUILTIN_EXPECT(match_len == 9, 0))
                     {
                        // Match is >= 9 bytes, decode the actual length.
                        uint e; LZHAM_DECOMPRESS_DECODE_ADAPTIVE_SYMBOL(codec, e, m_large_len_table[cur_state >= CLZDecompBase::cNumLitStates]);
                        match_len += e;
                        
                        if (match_len == (CLZDecompBase::cMaxMatchLen + 1))
                        {
                           // Decode "huge" match length.
                           match_len = 0;
                           do 
                           {
                              uint b; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, b, 1); 
                              if (!b)
                                 break;
                              match_len++;
                           } while (match_len < 3);
                           uint k; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, k, s_huge_match_code_len[match_len]);
                           match_len = s_huge_match_base_len[match_len] + k;
                        }
                     }

                     uint num_extra_bits;
                     num_extra_bits = m_lzBase.m_lzx_position_extra_bits[match_slot];

                     uint extra_bits;

#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE m_match_len = match_len; m_match_slot = match_slot; m_num_extra_bits = num_extra_bits;
#define LZHAM_RESTORE_LOCAL_STATE match_len = m_match_len; match_slot = m_match_slot; num_extra_bits = m_num_extra_bits;

                     if (LZHAM_BUILTIN_EXPECT(num_extra_bits < 3, 0))
                     {
                        LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, extra_bits, num_extra_bits);
                     }
                     else
                     {
                        extra_bits = 0;
                        if (LZHAM_BUILTIN_EXPECT(num_extra_bits > 4, 1))
                        {
                           LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, extra_bits, num_extra_bits - 4);
                           extra_bits <<= 4;
                        }

#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE m_match_len = match_len; m_match_slot = match_slot; m_extra_bits = extra_bits;
#define LZHAM_RESTORE_LOCAL_STATE match_len = m_match_len; match_slot = m_match_slot; extra_bits = m_extra_bits;

                        uint j; LZHAM_DECOMPRESS_DECODE_ADAPTIVE_SYMBOL(codec, j, m_dist_lsb_table);
                        extra_bits += j;
                     }

                     match_hist3 = match_hist2;
                     match_hist2 = match_hist1;
                     match_hist1 = match_hist0;
                     match_hist0 = m_lzBase.m_lzx_position_base[match_slot] + extra_bits;

                     cur_state = (cur_state < CLZDecompBase::cNumLitStates) ? CLZDecompBase::cNumLitStates : CLZDecompBase::cNumLitStates + 3;

#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE m_match_len = match_len;
#define LZHAM_RESTORE_LOCAL_STATE match_len = m_match_len;
                  }

                  // We have the match's length and distance, now do the copy.

#ifdef LZHAM_LZDEBUG
                  LZHAM_VERIFY(match_len == m_debug_match_len);
                  LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, m_debug_match_dist, 25);
                  uint d; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, d, 4);
                  m_debug_match_dist = (m_debug_match_dist << 4) | d;
                  LZHAM_VERIFY((uint)match_hist0 == m_debug_match_dist);
#endif
                  if ( (unbuffered) && LZHAM_BUILTIN_EXPECT((((size_t)match_hist0 > dst_ofs) || ((dst_ofs + match_len) > out_buf_size)), 0) )
                  {
                     LZHAM_LOG_ERROR(1032);
                     LZHAM_SYMBOL_CODEC_DECODE_END(codec);
                     *m_pIn_buf_size = static_cast<size_t>(codec.decode_get_bytes_consumed());
                     *m_pOut_buf_size = 0;
                     for ( ; ; ) { LZHAM_CR_RETURN(m_state, LZHAM_DECOMP_STATUS_FAILED_BAD_CODE); }
                  }

                  uint src_ofs;
                  const uint8* pCopy_src;
                  src_ofs = (dst_ofs - match_hist0) & dict_size_mask;
                  pCopy_src = pDst + src_ofs;
                                    
#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE m_match_len = match_len; m_src_ofs = src_ofs; m_pCopy_src = pCopy_src;
#define LZHAM_RESTORE_LOCAL_STATE match_len = m_match_len; src_ofs = m_src_ofs; pCopy_src = m_pCopy_src;

                  if ( (!unbuffered) && LZHAM_BUILTIN_EXPECT( ((LZHAM_MAX(src_ofs, dst_ofs) + match_len) > dict_size_mask), 0) )
                  {
                     // Match source or destination wraps around the end of the dictionary to the beginning, so handle the copy one byte at a time.
                     do
                     {
                        pDst[dst_ofs++] = *pCopy_src++;

                        if (LZHAM_BUILTIN_EXPECT(pCopy_src == pDst_end, 0))
                           pCopy_src = pDst;

                        if (LZHAM_BUILTIN_EXPECT(dst_ofs > dict_size_mask, 0))
                        {
                           LZHAM_SYMBOL_CODEC_DECODE_END(codec);
                           LZHAM_FLUSH_DICT_TO_OUTPUT_BUFFER(dict_size);
                           LZHAM_SYMBOL_CODEC_DECODE_BEGIN(codec);
                           dst_ofs = 0;
                        }

                        match_len--;
                     } while (LZHAM_BUILTIN_EXPECT(match_len > 0, 1));
                  }
                  else
                  {
                     uint8* pCopy_dst = pDst + dst_ofs;
                     if (LZHAM_BUILTIN_EXPECT(match_hist0 == 1, 0))
                     {
                        // Handle byte runs.
                        uint8 c = *pCopy_src;
                        if (LZHAM_BUILTIN_EXPECT(match_len < 8, 1))
                        {
                           for (int i = match_len; i > 0; i--)
                              *pCopy_dst++ = c;
                        }
                        else
                        {
                           memset(pCopy_dst, c, match_len);
                        }
                     }
                     else
                     {
                        // Handle matches of length 2 or higher.
                        if (LZHAM_BUILTIN_EXPECT(((match_len < 8) || ((int)match_len > match_hist0)), 1))
                        {
                           for (int i = match_len; i > 0; i--)
                              *pCopy_dst++ = *pCopy_src++;
                        }
                        else
                        {
                           LZHAM_MEMCPY(pCopy_dst, pCopy_src, match_len);
                        }
                     }
                     dst_ofs += match_len;
                  }
               } // lit or match

#undef LZHAM_SAVE_LOCAL_STATE
#undef LZHAM_RESTORE_LOCAL_STATE
#define LZHAM_SAVE_LOCAL_STATE
#define LZHAM_RESTORE_LOCAL_STATE
            } // for ( ; ; )

#ifdef LZHAM_LZDEBUG
            uint end_sync_marker; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, end_sync_marker, 12);
            LZHAM_VERIFY(end_sync_marker == 366);
#endif
            LZHAM_SYMBOL_CODEC_DECODE_ALIGN_TO_BYTE(codec);
         }
         else if (m_block_type == CLZDecompBase::cEOFBlock)
         {
            // Received EOF.
            m_status = LZHAM_DECOMP_STATUS_SUCCESS;
         }
         else
         {
            LZHAM_LOG_ERROR(1033);
            // This block type is currently undefined.
            m_status = LZHAM_DECOMP_STATUS_FAILED_BAD_CODE;
         }

         m_block_index++;

      } while (m_status == LZHAM_DECOMP_STATUS_NOT_FINISHED);

      if ((!unbuffered) && (dst_ofs))
      {
         LZHAM_SYMBOL_CODEC_DECODE_END(codec);
         LZHAM_FLUSH_DICT_TO_OUTPUT_BUFFER(dst_ofs);
         LZHAM_SYMBOL_CODEC_DECODE_BEGIN(codec);
      }

      if (m_status == LZHAM_DECOMP_STATUS_SUCCESS)
      {
         LZHAM_SYMBOL_CODEC_DECODE_ALIGN_TO_BYTE(codec);

         LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, m_file_src_file_adler32, 16);
         uint l; LZHAM_SYMBOL_CODEC_DECODE_GET_BITS(codec, l, 16);
         m_file_src_file_adler32 = (m_file_src_file_adler32 << 16) | l;

         if (m_params.m_decompress_flags & LZHAM_DECOMP_FLAG_COMPUTE_ADLER32)
         {
            if (unbuffered)
            {
               m_decomp_adler32 = adler32(pDst, dst_ofs, cInitAdler32);
            }

            if (m_file_src_file_adler32 != m_decomp_adler32)
            {
               LZHAM_LOG_ERROR(1034);
               m_status = LZHAM_DECOMP_STATUS_FAILED_ADLER32;
            }
         }
         else
         {
            m_decomp_adler32 = m_file_src_file_adler32;
         }
      }

      LZHAM_SYMBOL_CODEC_DECODE_END(codec);

      *m_pIn_buf_size = static_cast<size_t>(codec.stop_decoding());
      *m_pOut_buf_size = unbuffered ? (dst_ofs - m_dst_highwater_ofs) : 0;
      m_dst_highwater_ofs = dst_ofs;

      LZHAM_CR_RETURN(m_state, m_status);

      for ( ; ; )
      {
         *m_pIn_buf_size = 0;
         *m_pOut_buf_size = 0;
         LZHAM_CR_RETURN(m_state, m_status);
      }

      LZHAM_CR_FINISH

      return m_status;
   }

   static bool check_params(const lzham_decompress_params *pParams)
   {
      if ((!pParams) || (pParams->m_struct_size != sizeof(lzham_decompress_params)))
      {
         LZHAM_LOG_ERROR(1000);
         return false;
      }

      if ((pParams->m_dict_size_log2 < CLZDecompBase::cMinDictSizeLog2) || (pParams->m_dict_size_log2 > CLZDecompBase::cMaxDictSizeLog2))
      {
         LZHAM_LOG_ERROR(1001);
         return false;
      }

      if (pParams->m_num_seed_bytes)
      {
         if (((pParams->m_decompress_flags & LZHAM_DECOMP_FLAG_OUTPUT_UNBUFFERED) != 0) || (!pParams->m_pSeed_bytes))
         {
            LZHAM_LOG_ERROR(1002);
            return false;
         }
         if (pParams->m_num_seed_bytes > (1U << pParams->m_dict_size_log2))
         {
            LZHAM_LOG_ERROR(1003);
            return false;
         }
      }
      return true;
   }
   
   lzham_decompress_state_ptr LZHAM_CDECL lzham_lib_decompress_init(const lzham_decompress_params *pParams)
   {
      LZHAM_ASSUME(CLZDecompBase::cMinDictSizeLog2 == LZHAM_MIN_DICT_SIZE_LOG2);
      LZHAM_ASSUME(CLZDecompBase::cMaxDictSizeLog2 == LZHAM_MAX_DICT_SIZE_LOG2_X64);

      if (!check_params(pParams))
         return NULL;

      lzham_malloc_context malloc_context = lzham_create_malloc_context(LZHAM_DECOMP_MEMORY_ARENA_SIZE);
      
      lzham_decompressor *pState = lzham_new<lzham_decompressor>(malloc_context, malloc_context);
      if (!pState)
      {
         lzham_destroy_malloc_context(malloc_context);
         return NULL;
      }
      
      pState->m_params = *pParams;

      if (pState->m_params.m_decompress_flags & LZHAM_DECOMP_FLAG_OUTPUT_UNBUFFERED)
      {
         pState->m_pRaw_decomp_buf = NULL;
         pState->m_raw_decomp_buf_size = 0;
         pState->m_pDecomp_buf = NULL;
      }
      else
      {
         uint32 decomp_buf_size = 1U << pState->m_params.m_dict_size_log2;
         pState->m_pRaw_decomp_buf = static_cast<uint8*>(lzham_malloc(malloc_context, decomp_buf_size + 15));
         if (!pState->m_pRaw_decomp_buf)
         {
            lzham_delete(malloc_context, pState);
            lzham_destroy_malloc_context(malloc_context);
            return NULL;
         }
         pState->m_raw_decomp_buf_size = decomp_buf_size;
         pState->m_pDecomp_buf = math::align_up_pointer(pState->m_pRaw_decomp_buf, 16);
      }

      pState->init();
      
      return pState;
   }

   lzham_decompress_state_ptr LZHAM_CDECL lzham_lib_decompress_reinit(lzham_decompress_state_ptr p, const lzham_decompress_params *pParams)
   {
      if (!p)
         return lzham_lib_decompress_init(pParams);
      
      lzham_decompressor *pState = static_cast<lzham_decompressor *>(p);

      if (!check_params(pParams))
         return NULL;
      
      if (pState->m_params.m_decompress_flags & LZHAM_DECOMP_FLAG_OUTPUT_UNBUFFERED)
      {
         lzham_free(pState->m_malloc_context, pState->m_pRaw_decomp_buf);
         pState->m_pRaw_decomp_buf = NULL;
         pState->m_raw_decomp_buf_size = 0;
         pState->m_pDecomp_buf = NULL;
      }
      else
      {
         uint32 new_dict_size = 1U << pState->m_params.m_dict_size_log2;
         if ((!pState->m_pRaw_decomp_buf) || (pState->m_raw_decomp_buf_size < new_dict_size))
         {
            uint8 *pNew_dict = static_cast<uint8*>(lzham_realloc(pState->m_malloc_context, pState->m_pRaw_decomp_buf, new_dict_size + 15));
            if (!pNew_dict)
               return NULL;
            pState->m_pRaw_decomp_buf = pNew_dict;
            pState->m_raw_decomp_buf_size = new_dict_size;
            pState->m_pDecomp_buf = math::align_up_pointer(pState->m_pRaw_decomp_buf, 16);
         }
      }
      
      pState->m_params = *pParams;

      pState->init();

      pState->reset_arith_tables();
      return pState;
   }

   uint32 LZHAM_CDECL lzham_lib_decompress_deinit(lzham_decompress_state_ptr p)
   {
      lzham_decompressor *pState = static_cast<lzham_decompressor *>(p);
      if (!pState)
         return 0;

      uint32 adler32 = pState->m_decomp_adler32;

      lzham_malloc_context malloc_context = pState->m_malloc_context;

      lzham_free(malloc_context, pState->m_pRaw_decomp_buf);
      lzham_delete(malloc_context, pState);
      lzham_destroy_malloc_context(malloc_context);

      return adler32;
   }

   lzham_decompress_status_t LZHAM_CDECL lzham_lib_decompress(
      lzham_decompress_state_ptr p,
      const lzham_uint8 *pIn_buf, size_t *pIn_buf_size,
      lzham_uint8 *pOut_buf, size_t *pOut_buf_size,
      lzham_bool no_more_input_bytes_flag)
   {
      lzham_decompressor *pState = static_cast<lzham_decompressor *>(p);

      if ((!pState) || (!pState->m_params.m_dict_size_log2) || (!pIn_buf_size) || (!pOut_buf_size))
      {
         return LZHAM_DECOMP_STATUS_INVALID_PARAMETER;
      }

      if ((*pIn_buf_size) && (!pIn_buf))
      {
         return LZHAM_DECOMP_STATUS_INVALID_PARAMETER;
      }

      if ((*pOut_buf_size) && (!pOut_buf))
      {
         return LZHAM_DECOMP_STATUS_INVALID_PARAMETER;
      }

      pState->m_pIn_buf = pIn_buf;
      pState->m_pIn_buf_size = pIn_buf_size;
      pState->m_pOut_buf = pOut_buf;
      pState->m_pOut_buf_size = pOut_buf_size;
      pState->m_no_more_input_bytes_flag = (no_more_input_bytes_flag != 0);

      if (pState->m_params.m_decompress_flags & LZHAM_DECOMP_FLAG_OUTPUT_UNBUFFERED)
      {
         if (!pState->m_pOrig_out_buf)
         {
            pState->m_pOrig_out_buf = pOut_buf;
            pState->m_orig_out_buf_size = *pOut_buf_size;
         }
         else
         {
            // In unbuffered mode, the caller is not allowed to move the output buffer and the output pointer MUST always point to the beginning of the output buffer.
            // Also, the output buffer size must indicate the full size of the output buffer. The decompressor will track the current output offset, and during partial/sync
            // flushes it'll report how many bytes it has written since the call. 
            if ((pState->m_pOrig_out_buf != pOut_buf) || (pState->m_orig_out_buf_size != *pOut_buf_size))
            {
               return LZHAM_DECOMP_STATUS_INVALID_PARAMETER;
            }
         }
      }

      lzham_decompress_status_t status;

      if (pState->m_params.m_decompress_flags & LZHAM_DECOMP_FLAG_OUTPUT_UNBUFFERED)
         status = pState->decompress<true>();
      else
         status = pState->decompress<false>();
      
      return status;
   }

   lzham_decompress_status_t LZHAM_CDECL lzham_lib_decompress_memory(const lzham_decompress_params *pParams, lzham_uint8* pDst_buf, size_t *pDst_len, const lzham_uint8* pSrc_buf, size_t src_len, lzham_uint32 *pAdler32)
   {
      if (!pParams)
         return LZHAM_DECOMP_STATUS_INVALID_PARAMETER;

      lzham_decompress_params params(*pParams);
      params.m_decompress_flags |= LZHAM_DECOMP_FLAG_OUTPUT_UNBUFFERED;

      lzham_decompress_state_ptr pState = lzham_lib_decompress_init(&params);
      if (!pState)
         return LZHAM_DECOMP_STATUS_FAILED_INITIALIZING;

      lzham_decompress_status_t status = lzham_lib_decompress(pState, pSrc_buf, &src_len, pDst_buf, pDst_len, true);

      uint32 adler32 = lzham_lib_decompress_deinit(pState);
      if (pAdler32)
         *pAdler32 = adler32;

      return status;
   }

   // ----------------- zlib-style API's

   int LZHAM_CDECL lzham_lib_z_inflateInit(lzham_z_streamp pStream)
   {
      return lzham_lib_z_inflateInit2(pStream, LZHAM_Z_DEFAULT_WINDOW_BITS);
   }

   int LZHAM_CDECL lzham_lib_z_inflateInit2(lzham_z_streamp pStream, int window_bits)
   {
      if (!pStream) 
         return LZHAM_Z_STREAM_ERROR;
      
#ifdef LZHAM_Z_API_FORCE_WINDOW_BITS
      window_bits = LZHAM_Z_API_FORCE_WINDOW_BITS;
#endif      

      int max_window_bits = LZHAM_64BIT_POINTERS ? LZHAM_MAX_DICT_SIZE_LOG2_X64 : LZHAM_MAX_DICT_SIZE_LOG2_X86;
      if (labs(window_bits) > max_window_bits)
         return LZHAM_Z_PARAM_ERROR;

      if (labs(window_bits) < LZHAM_MIN_DICT_SIZE_LOG2)
         window_bits = (window_bits < 0) ? -LZHAM_MIN_DICT_SIZE_LOG2 : LZHAM_MIN_DICT_SIZE_LOG2;
      
      lzham_decompress_params params;
      utils::zero_object(params);
      params.m_struct_size = sizeof(lzham_decompress_params);
      params.m_dict_size_log2 = static_cast<lzham_uint32>(labs(window_bits));
      
      params.m_decompress_flags = LZHAM_DECOMP_FLAG_COMPUTE_ADLER32;
      if (window_bits > 0)
         params.m_decompress_flags |= LZHAM_DECOMP_FLAG_READ_ZLIB_STREAM;
      
      lzham_decompress_state_ptr pState = lzham_lib_decompress_init(&params);
      if (!pState)
         return LZHAM_Z_MEM_ERROR;
      pStream->state = static_cast<lzham_z_internal_state *>(pState);

      pStream->data_type = 0;
      pStream->adler = LZHAM_Z_ADLER32_INIT;
      pStream->msg = NULL;
      pStream->total_in = 0;
      pStream->total_out = 0;
      pStream->reserved = 0;
            
      return LZHAM_Z_OK;
   }
   
   int LZHAM_CDECL lzham_lib_z_inflateReset(lzham_z_streamp pStream)
   {
      if ((!pStream) || (!pStream->state)) 
         return LZHAM_Z_STREAM_ERROR;

      lzham_decompress_state_ptr pState = static_cast<lzham_decompress_state_ptr>(pStream->state);
      lzham_decompressor *pDecomp = static_cast<lzham_decompressor *>(pState);
      
      lzham_decompress_params params(pDecomp->m_params);

      if (!lzham_lib_decompress_reinit(pState, &params))
         return LZHAM_Z_STREAM_ERROR;

      return LZHAM_Z_OK;
   }

   int LZHAM_CDECL lzham_lib_z_inflate(lzham_z_streamp pStream, int flush)
   {
      if ((!pStream) || (!pStream->state)) 
         return LZHAM_Z_STREAM_ERROR;
            
      if ((flush == LZHAM_Z_PARTIAL_FLUSH) || (flush == LZHAM_Z_FULL_FLUSH))
         flush = LZHAM_Z_SYNC_FLUSH;
      if (flush)
      {
         if ((flush != LZHAM_Z_SYNC_FLUSH) && (flush != LZHAM_Z_FINISH)) 
            return LZHAM_Z_STREAM_ERROR;
      }

      size_t orig_avail_in = pStream->avail_in;

      lzham_decompress_state_ptr pState = static_cast<lzham_decompress_state_ptr>(pStream->state);
      lzham_decompressor *pDecomp = static_cast<lzham_decompressor *>(pState);
      if (pDecomp->m_z_last_status >= LZHAM_DECOMP_STATUS_FIRST_SUCCESS_OR_FAILURE_CODE)
         return LZHAM_Z_DATA_ERROR;

      if (pDecomp->m_z_has_flushed && (flush != LZHAM_Z_FINISH)) 
         return LZHAM_Z_STREAM_ERROR;
      pDecomp->m_z_has_flushed |= (flush == LZHAM_Z_FINISH);

      lzham_decompress_status_t status;
      for ( ; ; )
      {
         size_t in_bytes = pStream->avail_in;
         size_t out_bytes = pStream->avail_out;
         lzham_bool no_more_input_bytes_flag = (flush == LZHAM_Z_FINISH);
         status = lzham_lib_decompress(pState, pStream->next_in, &in_bytes, pStream->next_out, &out_bytes, no_more_input_bytes_flag);
         
         pDecomp->m_z_last_status = status;

         pStream->next_in += (uint)in_bytes; 
         pStream->avail_in -= (uint)in_bytes;
         pStream->total_in += (uint)in_bytes; 
         pStream->adler = pDecomp->m_decomp_adler32;

         pStream->next_out += (uint)out_bytes;
         pStream->avail_out -= (uint)out_bytes;
         pStream->total_out += (uint)out_bytes;
                  
         if (status >= LZHAM_DECOMP_STATUS_FIRST_FAILURE_CODE)
         {
            if (status == LZHAM_DECOMP_STATUS_FAILED_NEED_SEED_BYTES)
               return LZHAM_Z_NEED_DICT;
            else 
               return LZHAM_Z_DATA_ERROR; // Stream is corrupted (there could be some uncompressed data left in the output dictionary - oh well).
         }
         
         if ((status == LZHAM_DECOMP_STATUS_NEEDS_MORE_INPUT) && (!orig_avail_in))
            return LZHAM_Z_BUF_ERROR; // Signal caller that we can't make forward progress without supplying more input, or by setting flush to LZHAM_Z_FINISH.
         else if (flush == LZHAM_Z_FINISH)
         {
            // Caller has indicated that all remaining input was at next_in, and all remaining output will fit entirely in next_out.
            // (The output buffer at next_out MUST be large to hold the remaining uncompressed data when flush==LZHAM_Z_FINISH).
            if (status == LZHAM_DECOMP_STATUS_SUCCESS)
               return LZHAM_Z_STREAM_END;
            // If status is LZHAM_DECOMP_STATUS_HAS_MORE_OUTPUT, there must be at least 1 more byte on the way but the caller to lzham_decompress() supplied an empty output buffer.
            // Something is wrong because the caller's output buffer should be large enough to hold the entire decompressed stream when flush==LZHAM_Z_FINISH.
            else if (status == LZHAM_DECOMP_STATUS_HAS_MORE_OUTPUT)
               return LZHAM_Z_BUF_ERROR;
         }
         else if ((status == LZHAM_DECOMP_STATUS_SUCCESS) || (!pStream->avail_in) || (!pStream->avail_out))
            break;
      }

      return (status == LZHAM_DECOMP_STATUS_SUCCESS) ? LZHAM_Z_STREAM_END : LZHAM_Z_OK;
   }

   int LZHAM_CDECL lzham_lib_z_inflateEnd(lzham_z_streamp pStream)
   {
      if (!pStream)
         return LZHAM_Z_STREAM_ERROR;

      lzham_decompress_state_ptr pState = static_cast<lzham_decompress_state_ptr>(pStream->state);
      if (pState)
      {
         pStream->adler = lzham_lib_decompress_deinit(pState);
         pStream->state = NULL;
      }

      return LZHAM_Z_OK;
   }

   int LZHAM_CDECL lzham_lib_z_uncompress(unsigned char *pDest, lzham_z_ulong *pDest_len, const unsigned char *pSource, lzham_z_ulong source_len)
   {
      lzham_z_stream stream;
      int status;
      memset(&stream, 0, sizeof(stream));

      // In case lzham_z_ulong is 64-bits (argh I hate longs).
      if ((source_len | *pDest_len) > 0xFFFFFFFFU) 
         return LZHAM_Z_PARAM_ERROR;

      stream.next_in = pSource;
      stream.avail_in = (uint)source_len;
      stream.next_out = pDest;
      stream.avail_out = (uint)*pDest_len;

      status = lzham_lib_z_inflateInit(&stream);
      if (status != LZHAM_Z_OK)
         return status;

      status = lzham_lib_z_inflate(&stream, LZHAM_Z_FINISH);
      if (status != LZHAM_Z_STREAM_END)
      {
         lzham_lib_z_inflateEnd(&stream);
         return ((status == LZHAM_Z_BUF_ERROR) && (!stream.avail_in)) ? LZHAM_Z_DATA_ERROR : status;
      }
      *pDest_len = stream.total_out;

      return lzham_lib_z_inflateEnd(&stream);
   }

   const char * LZHAM_CDECL lzham_lib_z_error(int err)
   {
      static struct 
      { 
         int m_err; 
         const char *m_pDesc; 
      } 
      s_error_descs[] =
      {
         { LZHAM_Z_OK, "" }, 
         { LZHAM_Z_STREAM_END, "stream end" }, 
         { LZHAM_Z_NEED_DICT, "need dictionary" }, 
         { LZHAM_Z_ERRNO, "file error" }, 
         { LZHAM_Z_STREAM_ERROR, "stream error" },
         { LZHAM_Z_DATA_ERROR, "data error" }, 
         { LZHAM_Z_MEM_ERROR, "out of memory" }, 
         { LZHAM_Z_BUF_ERROR, "buf error" }, 
         { LZHAM_Z_VERSION_ERROR, "version error" }, 
         { LZHAM_Z_PARAM_ERROR, "parameter error" }
      };
      for (uint i = 0; i < sizeof(s_error_descs) / sizeof(s_error_descs[0]); ++i) 
         if (s_error_descs[i].m_err == err) 
            return s_error_descs[i].m_pDesc;
      return NULL;
   }

   lzham_z_ulong lzham_lib_z_adler32(lzham_z_ulong adler, const unsigned char *ptr, size_t buf_len)
   {
      return adler32(ptr, buf_len, static_cast<uint>(adler));
   }

   lzham_z_ulong LZHAM_CDECL lzham_lib_z_crc32(lzham_z_ulong crc, const lzham_uint8 *ptr, size_t buf_len)
   {
      return crc32(static_cast<uint>(crc), ptr, buf_len);
   }

} // namespace lzham
