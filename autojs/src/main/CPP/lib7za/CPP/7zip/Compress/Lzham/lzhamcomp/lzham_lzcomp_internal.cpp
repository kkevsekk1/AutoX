// File: lzham_lzcomp_internal.cpp
// See Copyright Notice and license at the end of include/lzham.h
#include "lzham_core.h"
#include "lzham_lzcomp_internal.h"
#include "lzham_checksum.h"
#include "lzham_timer.h"
#include "lzham_lzbase.h"
#include <string.h>

// Update and print high-level coding statistics if set to 1.
// TODO: Add match distance coding statistics.
#define LZHAM_UPDATE_STATS                   0

// Only parse on the main thread, for easier debugging.
#define LZHAM_FORCE_SINGLE_THREADED_PARSING  0

// Verify all computed match costs against the generic/slow state::get_cost() method.
#define LZHAM_VERIFY_MATCH_COSTS             0

// Set to 1 to force all blocks to be uncompressed (raw).
#define LZHAM_FORCE_ALL_RAW_BLOCKS           0

#define LZHAM_EXTREME_PARSING_FAST_BYTES     96

namespace lzham
{
   static comp_settings s_level_settings[cCompressionLevelCount] =
   {
      // cCompressionLevelFastest
      {
         8,                               // m_fast_bytes
         1,                               // m_match_accel_max_matches_per_probe
         2,                               // m_match_accel_max_probes
      },
      // cCompressionLevelFaster
      {
         24,                              // m_fast_bytes
         6,                               // m_match_accel_max_matches_per_probe
         12,                              // m_match_accel_max_probes
      },
      // cCompressionLevelDefault
      {
         32,                              // m_fast_bytes
         UINT_MAX,                        // m_match_accel_max_matches_per_probe
         16,                              // m_match_accel_max_probes
      },
      // cCompressionLevelBetter
      {
         48,                              // m_fast_bytes
         UINT_MAX,                        // m_match_accel_max_matches_per_probe
         32,                              // m_match_accel_max_probes
      },
      // cCompressionLevelUber
      {
         64,                              // m_fast_bytes
         UINT_MAX,                        // m_match_accel_max_matches_per_probe
         cMatchAccelMaxSupportedProbes,   // m_match_accel_max_probes
      }
   };

   lzcompressor::lzcompressor(lzham_malloc_context malloc_context) :
      m_malloc_context(malloc_context),
      m_src_size(-1),
      m_src_adler32(0),
      m_accel(malloc_context),
      m_codec(malloc_context),
      m_block_buf(malloc_context),
      m_comp_buf(malloc_context),
      m_step(0),
      m_block_start_dict_ofs(0),
      m_block_index(0),
      m_finished(false),
      m_use_task_pool(false),
      m_use_extreme_parsing(false),
      m_start_of_block_state(malloc_context),
      m_state(malloc_context),
      m_fast_bytes(128),
      m_num_parse_threads(0)
   {
      LZHAM_VERIFY( ((uint32_ptr)this & (LZHAM_GET_ALIGNMENT(lzcompressor) - 1)) == 0);

      for (uint i = 0; i < LZHAM_ARRAY_SIZE(m_parse_thread_state); i++)
         m_parse_thread_state[i].set_malloc_context(malloc_context);
   }

   bool lzcompressor::init_seed_bytes()
   {
      uint cur_seed_ofs = 0;

      while (cur_seed_ofs < m_params.m_num_seed_bytes)
      {
         uint total_bytes_remaining = m_params.m_num_seed_bytes - cur_seed_ofs;
         uint num_bytes_to_add = math::minimum(total_bytes_remaining, m_params.m_block_size);

         if (!m_accel.add_bytes_begin(num_bytes_to_add, static_cast<const uint8*>(m_params.m_pSeed_bytes) + cur_seed_ofs))
         {
            LZHAM_LOG_ERROR(7000);
            return false;
         }
         m_accel.add_bytes_end();

         m_accel.advance_bytes(num_bytes_to_add);

         cur_seed_ofs += num_bytes_to_add;
      }

      return true;
   }

   bool lzcompressor::raw_parse_thread_state::init(lzcompressor& lzcomp, const lzcompressor::init_params &params)
   {
      if (!m_state.init(lzcomp, params.m_table_max_update_interval, params.m_table_update_interval_slow_rate))
         return false;

      if (lzcomp.m_use_extreme_parsing)
      {
         for (uint j = 0; j < LZHAM_ARRAY_SIZE(m_nodes); j++)
            m_nodes[j].clear();
      }
      else
      {
         node_state *pNodes = reinterpret_cast<node_state*>(m_nodes);

         memset(pNodes, 0xFF, (1 + cMaxParseGraphNodes) * sizeof(node_state));
      }

      return true;
   }

   bool lzcompressor::init(const init_params& params)
   {
      clear();

      if ((params.m_dict_size_log2 < CLZBase::cMinDictSizeLog2) || (params.m_dict_size_log2 > CLZBase::cMaxDictSizeLog2))
      {
         LZHAM_LOG_ERROR(7001);
         return false;
      }

      if ((params.m_compression_level < 0) || (params.m_compression_level > cCompressionLevelCount))
      {
         LZHAM_LOG_ERROR(7002);
         return false;
      }

      m_params = params;
      m_use_task_pool = (m_params.m_pTask_pool) && (m_params.m_pTask_pool->get_num_threads() != 0) && (m_params.m_max_helper_threads > 0);

      m_use_extreme_parsing = ((m_params.m_lzham_compress_flags & LZHAM_COMP_FLAG_EXTREME_PARSING) && (m_params.m_compression_level == cCompressionLevelUber));

      if (!m_use_task_pool)
         m_params.m_max_helper_threads = 0;

      m_settings = s_level_settings[params.m_compression_level];

      m_fast_bytes = m_use_extreme_parsing ? LZHAM_EXTREME_PARSING_FAST_BYTES : m_settings.m_fast_bytes;
      if (m_params.m_fast_bytes_override)
      {
         m_fast_bytes = math::clamp<uint>(m_params.m_fast_bytes_override, 8, CLZBase::cMaxMatchLen + 1);
      }

      const uint dict_size = 1U << m_params.m_dict_size_log2;

      if (params.m_num_seed_bytes)
      {
         if (!params.m_pSeed_bytes)
         {
            LZHAM_LOG_ERROR(7003);
            return false;
         }
         if (params.m_num_seed_bytes > dict_size)
         {
            LZHAM_LOG_ERROR(7004);
            return false;
         }
      }

      uint max_block_size = dict_size / 8;
      if (m_params.m_block_size > max_block_size)
      {
         m_params.m_block_size = max_block_size;
      }

      m_num_parse_threads = 1;

#if !LZHAM_FORCE_SINGLE_THREADED_PARSING
      if ((m_params.m_max_helper_threads > 0) && ((m_params.m_lzham_compress_flags & LZHAM_COMP_FLAG_FORCE_SINGLE_THREADED_PARSING) == 0))
      {
         LZHAM_ASSUME(cMaxParseThreads >= 4);

         if (m_params.m_block_size < 16384)
            m_num_parse_threads = LZHAM_MIN(cMaxParseThreads, m_params.m_max_helper_threads + 1);
         else if ((m_params.m_max_helper_threads <= 5) || (m_params.m_compression_level == cCompressionLevelFastest))
            m_num_parse_threads = 1;
         else
            m_num_parse_threads = m_use_extreme_parsing ? 4 : 2;
      }
#endif

      int num_parse_jobs = m_num_parse_threads - 1;
      uint match_accel_helper_threads = LZHAM_MAX(0, (int)m_params.m_max_helper_threads - num_parse_jobs);
      match_accel_helper_threads = LZHAM_MIN(match_accel_helper_threads, cMatchAccelMaxSupportedThreads);

      LZHAM_ASSERT(m_num_parse_threads >= 1);
      LZHAM_ASSERT(m_num_parse_threads <= cMaxParseThreads);

      if (!m_use_task_pool)
      {
         LZHAM_ASSERT(!match_accel_helper_threads && (m_num_parse_threads == 1));
      }
      else
      {
         LZHAM_ASSERT((match_accel_helper_threads + (m_num_parse_threads - 1)) <= m_params.m_max_helper_threads);
      }

      uint accel_flags = 0;
      if (m_params.m_lzham_compress_flags & LZHAM_COMP_FLAG_DETERMINISTIC_PARSING)
         accel_flags |= search_accelerator::cFlagDeterministic;

      if (m_params.m_compression_level > cCompressionLevelFastest)
      {
         if ((m_params.m_lzham_compress_flags & LZHAM_COMP_FLAG_USE_LOW_MEMORY_MATCH_FINDER) == 0)
            accel_flags |= search_accelerator::cFlagHash24;

         accel_flags |= search_accelerator::cFlagLen2Matches;
      }

      if (!m_accel.init(this, params.m_pTask_pool, match_accel_helper_threads, dict_size, m_settings.m_match_accel_max_matches_per_probe, false, m_settings.m_match_accel_max_probes, accel_flags))
      {
         LZHAM_LOG_ERROR(7005);
         return false;
      }

      init_position_slots(params.m_dict_size_log2);
      init_slot_tabs();

      if (!m_state.init(*this, m_params.m_table_max_update_interval, m_params.m_table_update_interval_slow_rate))
      {
         LZHAM_LOG_ERROR(7006);
         return false;
      }

      if (!m_block_buf.try_reserve(m_params.m_block_size))
      {
         LZHAM_LOG_ERROR(7007);
         return false;
      }

      if (!m_comp_buf.try_reserve(m_params.m_block_size*2))
      {
         LZHAM_LOG_ERROR(7008);
         return false;
      }

      for (uint i = 0; i < LZHAM_ARRAY_SIZE(m_parse_thread_state); i++)
      {
         if (!m_parse_thread_state[i].init(*this, m_params))
         {
            LZHAM_LOG_ERROR(7009);
            return false;
         }
      }

      if (params.m_num_seed_bytes)
      {
         if (!init_seed_bytes())
         {
            LZHAM_LOG_ERROR(7010);
            return false;
         }
      }

      if (!send_zlib_header())
      {
         LZHAM_LOG_ERROR(7011);
         return false;
      }

      m_src_size = 0;

      return true;
   }

   // See http://www.gzip.org/zlib/rfc-zlib.html
   // Method is set to 14 (LZHAM) and CINFO is (window_size - 15).
   bool lzcompressor::send_zlib_header()
   {
      if ((m_params.m_lzham_compress_flags & LZHAM_COMP_FLAG_WRITE_ZLIB_STREAM) == 0)
         return true;

      // set CM (method) and CINFO (dictionary size) fields
      int cmf = LZHAM_Z_LZHAM | ((m_params.m_dict_size_log2 - 15) << 4);

      // set FLEVEL by mapping LZHAM's compression level to zlib's
      int flg = 0;
      switch (m_params.m_compression_level)
      {
         case LZHAM_COMP_LEVEL_FASTEST:
         {
            flg = 0 << 6;
            break;
         }
         case LZHAM_COMP_LEVEL_FASTER:
         {
            flg = 1 << 6;
            break;
         }
         case LZHAM_COMP_LEVEL_DEFAULT:
         case LZHAM_COMP_LEVEL_BETTER:
         {
            flg = 2 << 6;
            break;
         }
         default:
         {
            flg = 3 << 6;
            break;
         }
      }

      // set FDICT flag
      if (m_params.m_pSeed_bytes)
         flg |= 32;

      int check = ((cmf << 8) + flg) % 31;
      if (check)
         flg += (31 - check);

      LZHAM_ASSERT(0 == (((cmf << 8) + flg) % 31));
      if (!m_comp_buf.try_push_back(static_cast<uint8>(cmf)))
         return false;
      if (!m_comp_buf.try_push_back(static_cast<uint8>(flg)))
         return false;

      if (m_params.m_pSeed_bytes)
      {
         // send adler32 of DICT
         uint dict_adler32 = adler32(m_params.m_pSeed_bytes, m_params.m_num_seed_bytes);
         for (uint i = 0; i < 4; i++)
         {
            if (!m_comp_buf.try_push_back(static_cast<uint8>(dict_adler32 >> 24)))
               return false;
            dict_adler32 <<= 8;
         }
      }

      return true;
   }

   void lzcompressor::clear()
   {
      m_codec.clear();
      m_src_size = -1;
      m_src_adler32 = cInitAdler32;
      m_block_buf.clear();
      m_comp_buf.clear();

      m_step = 0;
      m_finished = false;
      m_use_task_pool = false;
      m_use_extreme_parsing = false;
      m_block_start_dict_ofs = 0;
      m_block_index = 0;
      m_state.clear();
      m_num_parse_threads = 0;
      m_fast_bytes = 128;

      for (uint i = 0; i < cMaxParseThreads; i++)
      {
         parse_thread_state &parse_state = m_parse_thread_state[i];
         parse_state.m_state.clear();

         for (uint j = 0; j <= cMaxParseGraphNodes; j++)
            parse_state.m_nodes[j].clear();

         parse_state.m_start_ofs = 0;
         parse_state.m_bytes_to_match = 0;
         parse_state.m_best_decisions.clear();
         parse_state.m_issue_reset_state_partial = false;
         parse_state.m_emit_decisions_backwards = false;
         parse_state.m_failed = false;
         parse_state.m_parse_early_out_thresh = UINT_MAX;
         parse_state.m_bytes_actually_parsed = 0;
      }
   }

   bool lzcompressor::reset()
   {
      if (m_src_size < 0)
         return false;

      m_accel.reset();
      m_codec.reset();
      m_stats.clear();
      m_src_size = 0;
      m_src_adler32 = cInitAdler32;
      m_block_buf.try_resize(0);
      m_comp_buf.try_resize(0);

      m_step = 0;
      m_finished = false;
      m_block_start_dict_ofs = 0;
      m_block_index = 0;
      m_state.reset();

      if (m_params.m_num_seed_bytes)
      {
         if (!init_seed_bytes())
         {
            LZHAM_LOG_ERROR(7012);
            return false;
         }
      }

      return send_zlib_header();
   }

   bool lzcompressor::code_decision(lzdecision lzdec, uint& cur_ofs, uint& bytes_to_match)
   {
#ifdef LZHAM_LZDEBUG
      if (!m_codec.encode_bits(CLZBase::cLZHAMDebugSyncMarkerValue, CLZBase::cLZHAMDebugSyncMarkerBits)) return false;
      if (!m_codec.encode_bits(lzdec.is_match(), 1)) return false;
      if (!m_codec.encode_bits(lzdec.get_len(), 17)) return false;
      if (!m_codec.encode_bits(m_state.m_cur_state, 4)) return false;
#endif

#ifdef LZHAM_LZVERIFY
      if (lzdec.is_match())
      {
         uint match_dist = lzdec.get_match_dist(m_state);

         LZHAM_VERIFY(m_accel[cur_ofs] == m_accel[(cur_ofs - match_dist) & (m_accel.get_max_dict_size() - 1)]);
      }
#endif

      const uint len = lzdec.get_len();

      if (!m_state.encode(m_codec, *this, m_accel, lzdec))
      {
         LZHAM_LOG_ERROR(7013);
         return false;
      }

      cur_ofs += len;
      LZHAM_ASSERT(bytes_to_match >= len);
      bytes_to_match -= len;

      //m_accel.advance_bytes(len);

      m_step++;

      return true;
   }

   bool lzcompressor::send_sync_block(lzham_flush_t flush_type)
   {
      m_codec.reset();

      if (!m_codec.start_encoding(128))
      {
         LZHAM_LOG_ERROR(7014);
         return false;
      }

#ifdef LZHAM_LZDEBUG
      if (!m_codec.encode_bits(166, 12))
      {
         LZHAM_LOG_ERROR(7015);
         return false;
      }
#endif

      if (!m_codec.encode_bits(cSyncBlock, cBlockHeaderBits))
      {
         LZHAM_LOG_ERROR(7016);
         return false;
      }

      int flush_code = 0;
      switch (flush_type)
      {
         case LZHAM_FULL_FLUSH:
            flush_code = 2;
            break;
         case LZHAM_TABLE_FLUSH:
            flush_code = 1;
            break;
         case LZHAM_SYNC_FLUSH:
            flush_code = 3;
            break;
         case LZHAM_NO_FLUSH:
         case LZHAM_FINISH:
            flush_code = 0;
            break;
      }
      if (!m_codec.encode_bits(flush_code, cBlockFlushTypeBits))
      {
         LZHAM_LOG_ERROR(7017);
         return false;
      }

      if (!m_codec.encode_align_to_byte())
      {
         LZHAM_LOG_ERROR(7018);
         return false;
      }
      if (!m_codec.encode_bits(0x0000, 16))
      {
         LZHAM_LOG_ERROR(7019);
         return false;
      }
      if (!m_codec.encode_bits(0xFFFF, 16))
      {
         LZHAM_LOG_ERROR(7020);
         return false;
      }
      if (!m_codec.stop_encoding(true))
      {
         LZHAM_LOG_ERROR(7021);
         return false;
      }
      if (!m_comp_buf.append(m_codec.get_encoding_buf()))
      {
         LZHAM_LOG_ERROR(7022);
         return false;
      }

      m_block_index++;
      return true;
   }

   bool lzcompressor::flush(lzham_flush_t flush_type)
   {
      LZHAM_ASSERT(!m_finished);
      if (m_finished)
      {
         LZHAM_LOG_ERROR(7023);
         return false;
      }

      bool status = true;
      if (m_block_buf.size())
      {
         status = compress_block(m_block_buf.get_ptr(), m_block_buf.size());

         m_block_buf.try_resize(0);
      }

      if (status)
      {
         status = send_sync_block(flush_type);

         if (LZHAM_FULL_FLUSH == flush_type)
         {
            m_accel.flush();
            m_state.reset();
         }
      }

      lzham_flush_buffered_printf();

      return status;
   }

   bool lzcompressor::put_bytes(const void* pBuf, uint buf_len)
   {
      LZHAM_ASSERT(!m_finished);
      if (m_finished)
      {
         LZHAM_LOG_ERROR(7024);
         return false;
      }

      bool status = true;

      if (!pBuf)
      {
         // Last block - flush whatever's left and send the final block.
         if (m_block_buf.size())
         {
            status = compress_block(m_block_buf.get_ptr(), m_block_buf.size());

            m_block_buf.try_resize(0);
         }

         if (status)
         {
            if (!send_final_block())
            {
               status = false;
               LZHAM_LOG_ERROR(7025);
            }
         }

         m_finished = true;
      }
      else
      {
         // Compress blocks.
         const uint8 *pSrcBuf = static_cast<const uint8*>(pBuf);
         uint num_src_bytes_remaining = buf_len;

         while (num_src_bytes_remaining)
         {
            const uint num_bytes_to_copy = LZHAM_MIN(num_src_bytes_remaining, m_params.m_block_size - m_block_buf.size());

            if (num_bytes_to_copy == m_params.m_block_size)
            {
               LZHAM_ASSERT(!m_block_buf.size());

               // Full-block available - compress in-place.
               status = compress_block(pSrcBuf, num_bytes_to_copy);
            }
            else
            {
               // Less than a full block available - append to already accumulated bytes.
               if (!m_block_buf.append(static_cast<const uint8 *>(pSrcBuf), num_bytes_to_copy))
               {
                  LZHAM_LOG_ERROR(7026);
                  return false;
               }

               LZHAM_ASSERT(m_block_buf.size() <= m_params.m_block_size);

               if (m_block_buf.size() == m_params.m_block_size)
               {
                  status = compress_block(m_block_buf.get_ptr(), m_block_buf.size());

                  m_block_buf.try_resize(0);
               }
            }

            if (!status)
            {
               LZHAM_LOG_ERROR(7027);
               return false;
            }

            pSrcBuf += num_bytes_to_copy;
            num_src_bytes_remaining -= num_bytes_to_copy;
         }
      }

      lzham_flush_buffered_printf();

      return status;
   }

   bool lzcompressor::send_final_block()
   {
      if (!m_codec.start_encoding(16))
      {
         LZHAM_LOG_ERROR(7028);
         return false;
      }

#ifdef LZHAM_LZDEBUG
      if (!m_codec.encode_bits(166, 12))
      {
         LZHAM_LOG_ERROR(7029);
         return false;
      }
#endif

      if (!m_block_index)
      {
         if (!send_configuration())
         {
            LZHAM_LOG_ERROR(7030);
            return false;
         }
      }

      if (!m_codec.encode_bits(cEOFBlock, cBlockHeaderBits))
      {
         LZHAM_LOG_ERROR(7031);
         return false;
      }

      if (!m_codec.encode_align_to_byte())
      {
         LZHAM_LOG_ERROR(7032);
         return false;
      }

      if (!m_codec.encode_bits(m_src_adler32, 32))
      {
         LZHAM_LOG_ERROR(7033);
         return false;
      }

      if (!m_codec.stop_encoding(true))
      {
         LZHAM_LOG_ERROR(7034);
         return false;
      }

      if (m_comp_buf.empty())
      {
         m_comp_buf.swap(m_codec.get_encoding_buf());
      }
      else
      {
         if (!m_comp_buf.append(m_codec.get_encoding_buf()))
         {
            LZHAM_LOG_ERROR(7035);
            return false;
         }
      }

      m_block_index++;

#if LZHAM_UPDATE_STATS
      m_stats.print();
#endif

      return true;
   }

   bool lzcompressor::send_configuration()
   {
      // TODO: Currently unused.
      //if (!m_codec.encode_bits(m_settings.m_fast_adaptive_huffman_updating, 1))
      //   return false;
      //if (!m_codec.encode_bits(0, 1))
      //   return false;

      return true;
   }

   void lzcompressor::node::add_state(
      int parent_index, int parent_state_index,
      const lzdecision &lzdec, const state &parent_state,
      bit_cost_t total_cost,
      uint total_complexity, uint max_parse_node_states)
   {
      state_base trial_state;
      parent_state.save_partial_state(trial_state);
      trial_state.partial_advance(lzdec);

      for (int i = m_num_node_states - 1; i >= 0; i--)
      {
         node_state &cur_node_state = m_node_states[i];
         if (cur_node_state.m_saved_state == trial_state)
         {
            if ( (total_cost < cur_node_state.m_total_cost) ||
                 ((total_cost == cur_node_state.m_total_cost) && (total_complexity < cur_node_state.m_total_complexity)) )
            {
               cur_node_state.m_parent_index = static_cast<int16>(parent_index);
               cur_node_state.m_parent_state_index = static_cast<int8>(parent_state_index);
               cur_node_state.m_lzdec = lzdec;
               cur_node_state.m_total_cost = total_cost;
               cur_node_state.m_total_complexity = total_complexity;

               while (i > 0)
               {
                  if ((m_node_states[i].m_total_cost < m_node_states[i - 1].m_total_cost) ||
                      ((m_node_states[i].m_total_cost == m_node_states[i - 1].m_total_cost) && (m_node_states[i].m_total_complexity < m_node_states[i - 1].m_total_complexity)))
                  {
                     std::swap(m_node_states[i], m_node_states[i - 1]);
                     i--;
                  }
                  else
                     break;
               }
            }

            return;
         }
      }

      int insert_index;
      for (insert_index = m_num_node_states; insert_index > 0; insert_index--)
      {
         node_state &cur_node_state = m_node_states[insert_index - 1];

         if ( (total_cost > cur_node_state.m_total_cost) ||
              ((total_cost == cur_node_state.m_total_cost) && (total_complexity >= cur_node_state.m_total_complexity)) )
         {
            break;
         }
      }

      if (insert_index == static_cast<int>(max_parse_node_states))
         return;

      uint num_behind = m_num_node_states - insert_index;
      uint num_to_move = (m_num_node_states < max_parse_node_states) ? num_behind : (num_behind - 1);
      if (num_to_move)
      {
         LZHAM_ASSERT((insert_index + 1 + num_to_move) <= max_parse_node_states);
         memmove(&m_node_states[insert_index + 1], &m_node_states[insert_index], sizeof(node_state) * num_to_move);
      }

      node_state *pNew_node_state = &m_node_states[insert_index];
      pNew_node_state->m_parent_index = static_cast<int16>(parent_index);
      pNew_node_state->m_parent_state_index = static_cast<uint8>(parent_state_index);
      pNew_node_state->m_lzdec = lzdec;
      pNew_node_state->m_total_cost = total_cost;
      pNew_node_state->m_total_complexity = total_complexity;
      pNew_node_state->m_saved_state = trial_state;

      m_num_node_states = LZHAM_MIN(m_num_node_states + 1, static_cast<uint>(max_parse_node_states));

#ifdef LZHAM_LZVERIFY
      for (uint i = 0; i < (m_num_node_states - 1); ++i)
      {
         node_state &a = m_node_states[i];
         node_state &b = m_node_states[i + 1];
         LZHAM_VERIFY(
            (a.m_total_cost < b.m_total_cost) ||
            ((a.m_total_cost == b.m_total_cost) && (a.m_total_complexity <= b.m_total_complexity)) );
      }
#endif
   }

   // The "extreme" parser tracks the best cMaxParseNodeStates (default 4) candidate LZ decisions per lookahead character.
   // This allows the compressor to make locally suboptimal decisions that ultimately result in a better parse.
   // It assumes the input statistics are locally stationary over the input block to parse.
   bool lzcompressor::extreme_parse(parse_thread_state &parse_state)
   {
      LZHAM_ASSERT(parse_state.m_bytes_to_match <= cMaxParseGraphNodes);

      parse_state.m_failed = false;
      parse_state.m_emit_decisions_backwards = true;

      node *pNodes = parse_state.m_nodes;

#ifdef LZHAM_BUILD_DEBUG
      for (uint i = 0; i < (cMaxParseGraphNodes + 1); i++)
      {
         LZHAM_ASSERT(pNodes[i].m_num_node_states == 0);
      }
#endif

      state &approx_state = *parse_state.m_pState;

      pNodes[0].m_num_node_states = 1;
      node_state &first_node_state = pNodes[0].m_node_states[0];
      approx_state.save_partial_state(first_node_state.m_saved_state);
      first_node_state.m_parent_index = -1;
      first_node_state.m_parent_state_index = -1;
      first_node_state.m_total_cost = 0;
      first_node_state.m_total_complexity = 0;

      const uint bytes_to_parse = parse_state.m_bytes_to_match;

      const uint lookahead_start_ofs = m_accel.get_lookahead_pos() & m_accel.get_max_dict_size_mask();

      uint cur_dict_ofs = parse_state.m_start_ofs;
      uint cur_lookahead_ofs = cur_dict_ofs - lookahead_start_ofs;
      uint cur_node_index = 0;

      enum { cMaxFullMatches = cMatchAccelMaxSupportedProbes };
      uint match_lens[cMaxFullMatches];
      uint match_distances[cMaxFullMatches];

      bit_cost_t lzdec_bitcosts[cMaxMatchLen + 1];

      node prev_lit_node;
      prev_lit_node.clear();

      node *pMax_node_in_graph = &pNodes[0];

      while (cur_node_index < bytes_to_parse)
      {
         node* pCur_node = &pNodes[cur_node_index];

         if ((cur_node_index >= parse_state.m_parse_early_out_thresh) && (pCur_node == pMax_node_in_graph))
         {
            // If the best path *must* pass through this node, and we're far enough along, and we're parsing using a single thread, then exit so we can move all our state forward.
            if (pCur_node->m_num_node_states == 1)
               break;
         }

         const uint max_admissable_match_len = LZHAM_MIN(static_cast<uint>(CLZBase::cMaxMatchLen), bytes_to_parse - cur_node_index);
         const uint find_dict_size = m_accel.get_cur_dict_size() + cur_lookahead_ofs;

         const uint lit_pred0 = approx_state.get_pred_char(m_accel, cur_dict_ofs, 1);

         const uint8* pLookahead = &m_accel.m_dict[cur_dict_ofs];

         // full matches
         uint max_full_match_len = 0;
         uint num_full_matches = 0;
         uint len2_match_dist = 0;

         if (max_admissable_match_len >= CLZBase::cMinMatchLen)
         {
            const dict_match* pMatches = m_accel.find_matches(cur_lookahead_ofs);
            if (pMatches)
            {
               for ( ; ; )
               {
                  uint match_len = pMatches->get_len();
                  LZHAM_ASSERT((pMatches->get_dist() > 0) && (pMatches->get_dist() <= m_dict_size));
                  match_len = LZHAM_MIN(match_len, max_admissable_match_len);

                  if (match_len > max_full_match_len)
                  {
                     max_full_match_len = match_len;

                     match_lens[num_full_matches] = match_len;
                     match_distances[num_full_matches] = pMatches->get_dist();
                     num_full_matches++;
                  }

                  if (pMatches->is_last())
                     break;
                  pMatches++;
               }
            }

            len2_match_dist = m_accel.get_len2_match(cur_lookahead_ofs);
         }

         uint ahead_bytes = 1;
         for (uint cur_node_state_index = 0; cur_node_state_index < pCur_node->m_num_node_states; cur_node_state_index++)
         {
            node_state &cur_node_state = pCur_node->m_node_states[cur_node_state_index];

            if (cur_node_index)
            {
               LZHAM_ASSERT(cur_node_state.m_parent_index >= 0);

               approx_state.restore_partial_state(cur_node_state.m_saved_state);
            }

            uint is_match_model_index = LZHAM_IS_MATCH_MODEL_INDEX(approx_state.m_cur_state);

            const bit_cost_t cur_node_total_cost = cur_node_state.m_total_cost;
            const uint cur_node_total_complexity = cur_node_state.m_total_complexity;

            // rep matches
            uint match_hist_max_len = 0;
            uint match_hist_min_match_len = 1;
            for (uint rep_match_index = 0; rep_match_index < cMatchHistSize; rep_match_index++)
            {
               uint hist_match_len = 0;

               uint dist = approx_state.m_match_hist[rep_match_index];
               if (dist <= find_dict_size)
               {
                  const uint comp_pos = static_cast<uint>((m_accel.m_lookahead_pos + cur_lookahead_ofs - dist) & m_accel.m_max_dict_size_mask);
                  const uint8* pComp = &m_accel.m_dict[comp_pos];

                  for (hist_match_len = 0; hist_match_len < max_admissable_match_len; hist_match_len++)
                     if (pComp[hist_match_len] != pLookahead[hist_match_len])
                        break;
               }

               if (hist_match_len >= match_hist_min_match_len)
               {
                  match_hist_max_len = math::maximum(match_hist_max_len, hist_match_len);

                  approx_state.get_rep_match_costs(cur_dict_ofs, lzdec_bitcosts, rep_match_index, match_hist_min_match_len, hist_match_len, is_match_model_index);

                  uint rep_match_total_complexity = cur_node_total_complexity + (cRep0Complexity + rep_match_index);
                  for (uint l = match_hist_min_match_len; l <= hist_match_len; l++)
                  {
#if LZHAM_VERIFY_MATCH_COSTS
                     {
                        lzdecision actual_dec(cur_dict_ofs, l, -((int)rep_match_index + 1));
                        bit_cost_t actual_cost = approx_state.get_cost(*this, m_accel, actual_dec);
                        LZHAM_ASSERT(actual_cost == lzdec_bitcosts[l]);
                     }
#endif
                     node& dst_node = pCur_node[l];

                     bit_cost_t rep_match_total_cost = cur_node_total_cost + lzdec_bitcosts[l];

                     dst_node.add_state(cur_node_index, cur_node_state_index, lzdecision(cur_dict_ofs, l, -((int)rep_match_index + 1)), approx_state, rep_match_total_cost, rep_match_total_complexity, parse_state.m_max_parse_node_states);
                     pMax_node_in_graph = LZHAM_MAX(pMax_node_in_graph, &dst_node);
                  }
               }

               match_hist_min_match_len = CLZBase::cMinMatchLen;
            }

            if (match_hist_max_len >= m_fast_bytes)
            {
               ahead_bytes = match_hist_max_len;
               break;
            }

            uint min_truncate_match_len = match_hist_max_len;

            // nearest len2 match
            if (len2_match_dist)
            {
               lzdecision lzdec(cur_dict_ofs, 2, len2_match_dist);
               bit_cost_t actual_cost = approx_state.get_cost(*this, m_accel, lzdec);
               pCur_node[2].add_state(cur_node_index, cur_node_state_index, lzdec, approx_state, cur_node_total_cost + actual_cost, cur_node_total_complexity + cShortMatchComplexity, parse_state.m_max_parse_node_states);
               pMax_node_in_graph = LZHAM_MAX(pMax_node_in_graph, &pCur_node[2]);

               min_truncate_match_len = LZHAM_MAX(min_truncate_match_len, 2);
            }

            // full matches
            if (max_full_match_len > min_truncate_match_len)
            {
               uint prev_max_match_len = LZHAM_MAX(1, min_truncate_match_len);
               for (uint full_match_index = 0; full_match_index < num_full_matches; full_match_index++)
               {
                  uint end_len = match_lens[full_match_index];
                  if (end_len <= min_truncate_match_len)
                     continue;

                  uint start_len = prev_max_match_len + 1;
                  uint match_dist = match_distances[full_match_index];

                  LZHAM_ASSERT(start_len <= end_len);

                  approx_state.get_full_match_costs(*this, cur_dict_ofs, lzdec_bitcosts, match_dist, start_len, end_len, is_match_model_index);

                  for (uint l = start_len; l <= end_len; l++)
                  {
                     uint match_complexity = (l >= cLongMatchComplexityLenThresh) ? cLongMatchComplexity : cShortMatchComplexity;

#if LZHAM_VERIFY_MATCH_COSTS
                     {
                        lzdecision actual_dec(cur_dict_ofs, l, match_dist);
                        bit_cost_t actual_cost = approx_state.get_cost(*this, m_accel, actual_dec);
                        LZHAM_ASSERT(actual_cost == lzdec_bitcosts[l]);
                     }
#endif
                     node& dst_node = pCur_node[l];

                     bit_cost_t match_total_cost = cur_node_total_cost + lzdec_bitcosts[l];
                     uint match_total_complexity = cur_node_total_complexity + match_complexity;

                     dst_node.add_state( cur_node_index, cur_node_state_index, lzdecision(cur_dict_ofs, l, match_dist), approx_state, match_total_cost, match_total_complexity, parse_state.m_max_parse_node_states);
                     pMax_node_in_graph = LZHAM_MAX(pMax_node_in_graph, &dst_node);
                  }

                  prev_max_match_len = end_len;
               }

               if (max_full_match_len >= m_fast_bytes)
               {
                  ahead_bytes = max_full_match_len;
                  break;
               }
            }

            // literal
            bit_cost_t lit_cost = approx_state.get_lit_cost(*this, m_accel, cur_dict_ofs, lit_pred0, is_match_model_index);
            bit_cost_t lit_total_cost = cur_node_total_cost + lit_cost;
            uint lit_total_complexity = cur_node_total_complexity + cLitComplexity;
#if LZHAM_VERIFY_MATCH_COSTS
            {
               lzdecision actual_dec(cur_dict_ofs, 0, 0);
               bit_cost_t actual_cost = approx_state.get_cost(*this, m_accel, actual_dec);
               LZHAM_ASSERT(actual_cost == lit_cost);
            }
#endif

            pCur_node[1].add_state(cur_node_index, cur_node_state_index, lzdecision(cur_dict_ofs, 0, 0), approx_state, lit_total_cost, lit_total_complexity, parse_state.m_max_parse_node_states);
            pMax_node_in_graph = LZHAM_MAX(pMax_node_in_graph, &pCur_node[1]);

         } // cur_node_state_index

         cur_dict_ofs += ahead_bytes;
         cur_lookahead_ofs += ahead_bytes;
         cur_node_index += ahead_bytes;
      }

      LZHAM_ASSERT(static_cast<int>(cur_node_index) == (pMax_node_in_graph - pNodes));
      uint bytes_actually_parsed = cur_node_index;

      // Now get the optimal decisions by starting from the goal node.
      // m_best_decisions is filled backwards.
      if (!parse_state.m_best_decisions.try_reserve(bytes_actually_parsed))
      {
         parse_state.m_failed = true;

         for (uint i = 0; i <= bytes_actually_parsed; i++)
            pNodes[i].clear();

         LZHAM_LOG_ERROR(7036);

         return false;
      }

      bit_cost_t lowest_final_cost = cBitCostMax; //math::cNearlyInfinite;
      int node_state_index = 0;
      node_state *pLast_node_states = pNodes[bytes_actually_parsed].m_node_states;
      for (uint i = 0; i < pNodes[bytes_actually_parsed].m_num_node_states; i++)
      {
         if (pLast_node_states[i].m_total_cost < lowest_final_cost)
         {
            lowest_final_cost = pLast_node_states[i].m_total_cost;
            node_state_index = i;
         }
      }

      int node_index = bytes_actually_parsed;
      lzdecision *pDst_dec = parse_state.m_best_decisions.get_ptr();
      do
      {
         LZHAM_ASSERT((node_index >= 0) && (node_index <= (int)cMaxParseGraphNodes));

         node& cur_node = pNodes[node_index];
         const node_state &cur_node_state = cur_node.m_node_states[node_state_index];

         *pDst_dec++ = cur_node_state.m_lzdec;

         node_index = cur_node_state.m_parent_index;
         node_state_index = cur_node_state.m_parent_state_index;

      } while (node_index > 0);

      parse_state.m_best_decisions.try_resize_no_construct(static_cast<uint>(pDst_dec - parse_state.m_best_decisions.get_ptr()));
      parse_state.m_bytes_actually_parsed = bytes_actually_parsed;

      for (uint i = 0; i <= bytes_actually_parsed; i++)
         pNodes[i].clear();

      return true;
   }

   // Parsing notes:
   // The regular "optimal" parser only tracks the single cheapest candidate LZ decision per lookahead character.
   // This function finds the shortest path through an extremely dense node graph using a streamlined/simplified Dijkstra's algorithm with some coding heuristics.
   // Graph edges are LZ "decisions", cost is measured in fractional bits needed to code each graph edge, and graph nodes are lookahead characters.
   // There is no need to track visited/unvisted nodes, or find the next cheapest unvisted node in each iteration. The search always proceeds sequentially, visiting each lookahead character in turn from left/right.
   // The major CPU expense of this function is the complexity of LZ decision cost evaluation, so a lot of implementation effort is spent here reducing this overhead.
   // To simplify the problem, it assumes the input statistics are locally stationary over the input block to parse. (Otherwise, it would need to store, track, and update
   // unique symbol statistics for each lookahead character, which would be very costly.)
   // This function always sequentially pushes "forward" the unvisited node horizon. This horizon frequently collapses to a single node, which guarantees that the shortest path through the
   // graph must pass through this node. LZMA tracks cumulative bitprices relative to this node, while LZHAM currently always tracks cumulative bitprices relative to the first node in the lookahead buffer.
   // In very early versions of LZHAM the parse was much more understandable (straight Dijkstra with almost no bit price optimizations or coding heuristics).
   bool lzcompressor::optimal_parse(parse_thread_state &parse_state)
   {
      LZHAM_ASSERT(parse_state.m_bytes_to_match <= cMaxParseGraphNodes);

      parse_state.m_failed = false;
      parse_state.m_emit_decisions_backwards = true;

      node_state *pNodes = reinterpret_cast<node_state*>(parse_state.m_nodes);
      pNodes[0].m_parent_index = -1;
      pNodes[0].m_total_cost = 0;
      pNodes[0].m_total_complexity = 0;

#ifdef LZHAM_BUILD_DEBUG
      for (uint i = 1; i < (cMaxParseGraphNodes + 1); i++)
      {
         LZHAM_ASSERT(pNodes[i].m_total_cost == cUINT64_MAX);
         LZHAM_ASSERT(pNodes[i].m_total_complexity == UINT_MAX);
         LZHAM_ASSERT(pNodes[i].m_parent_index == -1);
      }
#endif

      state &approx_state = *parse_state.m_pState;

      const uint bytes_to_parse = parse_state.m_bytes_to_match;

      const uint lookahead_start_ofs = m_accel.get_lookahead_pos() & m_accel.get_max_dict_size_mask();

      uint cur_dict_ofs = parse_state.m_start_ofs;
      uint cur_lookahead_ofs = cur_dict_ofs - lookahead_start_ofs;
      uint cur_node_index = 0;

      enum { cMaxFullMatches = cMatchAccelMaxSupportedProbes };
      uint match_lens[cMaxFullMatches];
      uint match_distances[cMaxFullMatches];

      bit_cost_t lzdec_bitcosts[cMaxMatchLen + 1];

      node_state *pMax_node_in_graph = &pNodes[0];

      while (cur_node_index < bytes_to_parse)
      {
         node_state* pCur_node = &pNodes[cur_node_index];

         if ((cur_node_index >= parse_state.m_parse_early_out_thresh) && (pCur_node == pMax_node_in_graph))
         {
            // If the best path *must* pass through this node, and we're far enough along, and we're parsing using a single thread, then exit so we can move all our state forward.
            break;
         }

         const uint max_admissable_match_len = LZHAM_MIN(static_cast<uint>(CLZBase::cMaxMatchLen), bytes_to_parse - cur_node_index);
         const uint find_dict_size = m_accel.m_cur_dict_size + cur_lookahead_ofs;

         if (cur_node_index)
         {
            LZHAM_ASSERT(pCur_node->m_parent_index >= 0);

            // Move to this node's state using the lowest cost LZ decision found.
            approx_state.restore_partial_state(pCur_node->m_saved_state);
            approx_state.partial_advance(pCur_node->m_lzdec);
         }

         const bit_cost_t cur_node_total_cost = pCur_node->m_total_cost;
         // This assert includes a fudge factor - make sure we don't overflow our scaled costs.
         LZHAM_ASSERT((cBitCostMax - cur_node_total_cost) > (cBitCostScale * 64));
         const uint cur_node_total_complexity = pCur_node->m_total_complexity;

         const uint lit_pred0 = approx_state.get_pred_char(m_accel, cur_dict_ofs, 1);
         uint is_match_model_index = LZHAM_IS_MATCH_MODEL_INDEX(approx_state.m_cur_state);

         const uint8* pLookahead = &m_accel.m_dict[cur_dict_ofs];

         // rep matches
         uint match_hist_max_len = 0;
         uint match_hist_min_match_len = 1;
         for (uint rep_match_index = 0; rep_match_index < cMatchHistSize; rep_match_index++)
         {
            uint hist_match_len = 0;

            uint dist = approx_state.m_match_hist[rep_match_index];
            if (dist <= find_dict_size)
            {
               const uint comp_pos = static_cast<uint>((m_accel.m_lookahead_pos + cur_lookahead_ofs - dist) & m_accel.m_max_dict_size_mask);
               const uint8* pComp = &m_accel.m_dict[comp_pos];

               for (hist_match_len = 0; hist_match_len < max_admissable_match_len; hist_match_len++)
                  if (pComp[hist_match_len] != pLookahead[hist_match_len])
                     break;
            }

            if (hist_match_len >= match_hist_min_match_len)
            {
               match_hist_max_len = math::maximum(match_hist_max_len, hist_match_len);

               approx_state.get_rep_match_costs(cur_dict_ofs, lzdec_bitcosts, rep_match_index, match_hist_min_match_len, hist_match_len, is_match_model_index);

               uint rep_match_total_complexity = cur_node_total_complexity + (cRep0Complexity + rep_match_index);
               for (uint l = match_hist_min_match_len; l <= hist_match_len; l++)
               {
#if LZHAM_VERIFY_MATCH_COSTS
                  {
                     lzdecision actual_dec(cur_dict_ofs, l, -((int)rep_match_index + 1));
                     bit_cost_t actual_cost = approx_state.get_cost(*this, m_accel, actual_dec);
                     LZHAM_ASSERT(actual_cost == lzdec_bitcosts[l]);
                  }
#endif
                  node_state& dst_node = pCur_node[l];

                  bit_cost_t rep_match_total_cost = cur_node_total_cost + lzdec_bitcosts[l];

                  if ((rep_match_total_cost > dst_node.m_total_cost) || ((rep_match_total_cost == dst_node.m_total_cost) && (rep_match_total_complexity >= dst_node.m_total_complexity)))
                     continue;

                  dst_node.m_total_cost = rep_match_total_cost;
                  dst_node.m_total_complexity = rep_match_total_complexity;
                  dst_node.m_parent_index = (uint16)cur_node_index;
                  approx_state.save_partial_state(dst_node.m_saved_state);
                  dst_node.m_lzdec.init(cur_dict_ofs, l, -((int)rep_match_index + 1));
                  dst_node.m_lzdec.m_len = l;

                  pMax_node_in_graph = LZHAM_MAX(pMax_node_in_graph, &dst_node);
               }
            }

            match_hist_min_match_len = CLZBase::cMinMatchLen;
         }

         uint max_match_len = match_hist_max_len;

         if (max_match_len >= m_fast_bytes)
         {
            cur_dict_ofs += max_match_len;
            cur_lookahead_ofs += max_match_len;
            cur_node_index += max_match_len;
            continue;
         }

         // full matches
         if (max_admissable_match_len >= CLZBase::cMinMatchLen)
         {
            uint num_full_matches = 0;

            if (match_hist_max_len < 2)
            {
               // Get the nearest len2 match if we didn't find a rep len2.
               uint len2_match_dist = m_accel.get_len2_match(cur_lookahead_ofs);
               if (len2_match_dist)
               {
                  bit_cost_t cost = approx_state.get_len2_match_cost(*this, cur_dict_ofs, len2_match_dist, is_match_model_index);

#if LZHAM_VERIFY_MATCH_COSTS
                  {
                     lzdecision actual_dec(cur_dict_ofs, 2, len2_match_dist);
                     bit_cost_t actual_cost = approx_state.get_cost(*this, m_accel, actual_dec);
                     LZHAM_ASSERT(actual_cost == cost);
                  }
#endif

                  node_state& dst_node = pCur_node[2];

                  bit_cost_t match_total_cost = cur_node_total_cost + cost;
                  uint match_total_complexity = cur_node_total_complexity + cShortMatchComplexity;

                  if ((match_total_cost < dst_node.m_total_cost) || ((match_total_cost == dst_node.m_total_cost) && (match_total_complexity < dst_node.m_total_complexity)))
                  {
                     dst_node.m_total_cost = match_total_cost;
                     dst_node.m_total_complexity = match_total_complexity;
                     dst_node.m_parent_index = (uint16)cur_node_index;
                     approx_state.save_partial_state(dst_node.m_saved_state);
                     dst_node.m_lzdec.init(cur_dict_ofs, 2, len2_match_dist);

                     pMax_node_in_graph = LZHAM_MAX(pMax_node_in_graph, &dst_node);
                  }

                  max_match_len = 2;
               }
            }

            const uint min_truncate_match_len = max_match_len;

            // Now get all full matches: the nearest matches at each match length. (Actually, we don't
            // always get the nearest match. The match finder favors those matches which have the lowest value
            // in the nibble of each match distance, all other things being equal, to help exploit how the lowest
            // nibble of match distances is separately coded.)
            const dict_match* pMatches = m_accel.find_matches(cur_lookahead_ofs);
            if (pMatches)
            {
               for ( ; ; )
               {
                  uint match_len = pMatches->get_len();
                  LZHAM_ASSERT((pMatches->get_dist() > 0) && (pMatches->get_dist() <= m_dict_size));
                  match_len = LZHAM_MIN(match_len, max_admissable_match_len);

                  if (match_len > max_match_len)
                  {
                     max_match_len = match_len;

                     match_lens[num_full_matches] = match_len;
                     match_distances[num_full_matches] = pMatches->get_dist();
                     num_full_matches++;
                  }

                  if (pMatches->is_last())
                     break;
                  pMatches++;
               }
            }

            if (num_full_matches)
            {
               uint prev_max_match_len = LZHAM_MAX(1, min_truncate_match_len);
               for (uint full_match_index = 0; full_match_index < num_full_matches; full_match_index++)
               {
                  uint start_len = prev_max_match_len + 1;
                  uint end_len = match_lens[full_match_index];
                  uint match_dist = match_distances[full_match_index];

                  LZHAM_ASSERT(start_len <= end_len);

                  approx_state.get_full_match_costs(*this, cur_dict_ofs, lzdec_bitcosts, match_dist, start_len, end_len, is_match_model_index);

                  for (uint l = start_len; l <= end_len; l++)
                  {
                     uint match_complexity = (l >= cLongMatchComplexityLenThresh) ? cLongMatchComplexity : cShortMatchComplexity;

#if LZHAM_VERIFY_MATCH_COSTS
                     {
                        lzdecision actual_dec(cur_dict_ofs, l, match_dist);
                        bit_cost_t actual_cost = approx_state.get_cost(*this, m_accel, actual_dec);
                        LZHAM_ASSERT(actual_cost == lzdec_bitcosts[l]);
                     }
#endif
                     node_state& dst_node = pCur_node[l];

                     bit_cost_t match_total_cost = cur_node_total_cost + lzdec_bitcosts[l];
                     uint match_total_complexity = cur_node_total_complexity + match_complexity;

                     if ((match_total_cost > dst_node.m_total_cost) || ((match_total_cost == dst_node.m_total_cost) && (match_total_complexity >= dst_node.m_total_complexity)))
                        continue;

                     dst_node.m_total_cost = match_total_cost;
                     dst_node.m_total_complexity = match_total_complexity;
                     dst_node.m_parent_index = (uint16)cur_node_index;
                     approx_state.save_partial_state(dst_node.m_saved_state);
                     dst_node.m_lzdec.init(cur_dict_ofs, l, match_dist);

                     pMax_node_in_graph = LZHAM_MAX(pMax_node_in_graph, &dst_node);
                  }

                  prev_max_match_len = end_len;
               }
            }
         }

         if (max_match_len >= m_fast_bytes)
         {
            cur_dict_ofs += max_match_len;
            cur_lookahead_ofs += max_match_len;
            cur_node_index += max_match_len;
            continue;
         }

         // literal
         bit_cost_t lit_cost = approx_state.get_lit_cost(*this, m_accel, cur_dict_ofs, lit_pred0, is_match_model_index);
         bit_cost_t lit_total_cost = cur_node_total_cost + lit_cost;
         uint lit_total_complexity = cur_node_total_complexity + cLitComplexity;
#if LZHAM_VERIFY_MATCH_COSTS
         {
            lzdecision actual_dec(cur_dict_ofs, 0, 0);
            bit_cost_t actual_cost = approx_state.get_cost(*this, m_accel, actual_dec);
            LZHAM_ASSERT(actual_cost == lit_cost);
         }
#endif
         if ((lit_total_cost < pCur_node[1].m_total_cost) || ((lit_total_cost == pCur_node[1].m_total_cost) && (lit_total_complexity < pCur_node[1].m_total_complexity)))
         {
            pCur_node[1].m_total_cost = lit_total_cost;
            pCur_node[1].m_total_complexity = lit_total_complexity;
            pCur_node[1].m_parent_index = (int16)cur_node_index;
            approx_state.save_partial_state(pCur_node[1].m_saved_state);
            pCur_node[1].m_lzdec.init(cur_dict_ofs, 0, 0);

            pMax_node_in_graph = LZHAM_MAX(pMax_node_in_graph, &pCur_node[1]);
         }

         cur_dict_ofs++;
         cur_lookahead_ofs++;
         cur_node_index++;

      } // graph search

      LZHAM_ASSERT(static_cast<int>(cur_node_index) == (pMax_node_in_graph - pNodes));
      uint bytes_actually_parsed = cur_node_index;

      // Now get the optimal decisions by starting from the goal node.
      // m_best_decisions is filled backwards.
      if (!parse_state.m_best_decisions.try_reserve(bytes_actually_parsed))
      {
         parse_state.m_failed = true;

         memset(pNodes, 0xFF, (pMax_node_in_graph - pNodes + 1) * sizeof(node_state));

         LZHAM_LOG_ERROR(7037);

         return false;
      }

      int node_index = bytes_actually_parsed;
      lzdecision *pDst_dec = parse_state.m_best_decisions.get_ptr();
      do
      {
         LZHAM_ASSERT((node_index >= 0) && (node_index <= (int)cMaxParseGraphNodes));
         node_state& cur_node = pNodes[node_index];

         *pDst_dec++ = cur_node.m_lzdec;

         node_index = cur_node.m_parent_index;

      } while (node_index > 0);

      parse_state.m_best_decisions.try_resize_no_construct(static_cast<uint>(pDst_dec - parse_state.m_best_decisions.get_ptr()));

      parse_state.m_bytes_actually_parsed = bytes_actually_parsed;

      memset(pNodes, 0xFF, (pMax_node_in_graph - pNodes + 1) * sizeof(node_state));

      return true;
   }

   void lzcompressor::parse_job_callback(uint64 data, void* pData_ptr)
   {
      const uint parse_job_index = (uint)data;
      parse_thread_state &parse_state = m_parse_thread_state[parse_job_index];

      scoped_perf_section parse_job_timer(cVarArgs, "parse_job_callback %u", parse_job_index);

      LZHAM_NOTE_UNUSED(pData_ptr);

      if (m_use_extreme_parsing)
         extreme_parse(parse_state);
      else
         optimal_parse(parse_state);

      if (parse_state.m_use_semaphore)
      {
         parse_state.m_finished.release();
      }
   }

   // ofs is the absolute dictionary offset, must be >= the lookahead offset.
   // TODO: Doesn't find len2 matches
   int lzcompressor::enumerate_lz_decisions(uint ofs, const state& cur_state, lzham::vector<lzpriced_decision>& decisions, uint min_match_len, uint max_match_len)
   {
      LZHAM_ASSERT(min_match_len >= 1);

      uint start_ofs = m_accel.get_lookahead_pos() & m_accel.get_max_dict_size_mask();
      LZHAM_ASSERT(ofs >= start_ofs);
      const uint lookahead_ofs = ofs - start_ofs;

      uint largest_index = 0;
      uint largest_len;
      bit_cost_t largest_cost;

      if (min_match_len <= 1)
      {
         if (!decisions.try_resize(1))
         {
            LZHAM_LOG_ERROR(7038);
            return -1;
         }

         lzpriced_decision& lit_dec = decisions[0];
         lit_dec.init(ofs, 0, 0, 0);
         lit_dec.m_cost = cur_state.get_cost(*this, m_accel, lit_dec);
         largest_cost = lit_dec.m_cost;

         largest_len = 1;
      }
      else
      {
         if (!decisions.try_resize(0))
         {
            LZHAM_LOG_ERROR(7039);
            return -1;
         }

         largest_len = 0;
         largest_cost = cBitCostMax;
      }

      uint match_hist_max_len = 0;

      // Add rep matches.
      for (uint i = 0; i < cMatchHistSize; i++)
      {
         uint hist_match_len = m_accel.get_match_len(lookahead_ofs, cur_state.m_match_hist[i], max_match_len);
         if (hist_match_len < min_match_len)
            continue;

         if ( ((hist_match_len == 1) && (i == 0)) || (hist_match_len >= CLZBase::cMinMatchLen) )
         {
            match_hist_max_len = math::maximum(match_hist_max_len, hist_match_len);

            lzpriced_decision dec(ofs, hist_match_len, -((int)i + 1));
            dec.m_cost = cur_state.get_cost(*this, m_accel, dec);

            if (!decisions.try_push_back(dec))
            {
               LZHAM_LOG_ERROR(7040);
               return -1;
            }

            if ( (hist_match_len > largest_len) || ((hist_match_len == largest_len) && (dec.m_cost < largest_cost)) )
            {
               largest_index = decisions.size() - 1;
               largest_len = hist_match_len;
               largest_cost = dec.m_cost;
            }
         }
      }

      // Now add full matches.
      if ((max_match_len >= CLZBase::cMinMatchLen) && (match_hist_max_len < m_fast_bytes))
      {
         const dict_match* pMatches = m_accel.find_matches(lookahead_ofs);

         if (pMatches)
         {
            for ( ; ; )
            {
               uint match_len = math::minimum(pMatches->get_len(), max_match_len);
               LZHAM_ASSERT((pMatches->get_dist() > 0) && (pMatches->get_dist() <= m_dict_size));

               // Full matches are very likely to be more expensive than rep matches of the same length, so don't bother evaluating them.
               if ((match_len >= min_match_len) && (match_len > match_hist_max_len))
               {
                  if ((max_match_len > CLZBase::cMaxMatchLen) && (match_len == CLZBase::cMaxMatchLen))
                  {
                     match_len = m_accel.get_match_len(lookahead_ofs, pMatches->get_dist(), max_match_len, CLZBase::cMaxMatchLen);
                  }

                  lzpriced_decision dec(ofs, match_len, pMatches->get_dist());
                  dec.m_cost = cur_state.get_cost(*this, m_accel, dec);

                  if (!decisions.try_push_back(dec))
                  {
                     LZHAM_LOG_ERROR(7041);
                     return -1;
                  }

                  if ( (match_len > largest_len) || ((match_len == largest_len) && (dec.get_cost() < largest_cost)) )
                  {
                     largest_index = decisions.size() - 1;
                     largest_len = match_len;
                     largest_cost = dec.get_cost();
                  }
               }
               if (pMatches->is_last())
                  break;
               pMatches++;
            }
         }
      }

      return largest_index;
   }

   bool lzcompressor::greedy_parse(parse_thread_state &parse_state)
   {
      parse_state.m_failed = true;
      parse_state.m_emit_decisions_backwards = false;

      const uint bytes_to_parse = parse_state.m_bytes_to_match;

      const uint lookahead_start_ofs = m_accel.get_lookahead_pos() & m_accel.get_max_dict_size_mask();

      uint cur_dict_ofs = parse_state.m_start_ofs;
      uint cur_lookahead_ofs = cur_dict_ofs - lookahead_start_ofs;
      uint cur_ofs = 0;

      state &approx_state = *parse_state.m_pState;

      lzham::vector<lzpriced_decision> &decisions = parse_state.m_temp_decisions;

      if (!decisions.try_reserve(384))
      {
         LZHAM_LOG_ERROR(7042);
         return false;
      }

      if (!parse_state.m_best_decisions.try_resize(0))
      {
         LZHAM_LOG_ERROR(7043);
         return false;
      }

      while (cur_ofs < bytes_to_parse)
      {
         const uint max_admissable_match_len = LZHAM_MIN(static_cast<uint>(CLZBase::cMaxHugeMatchLen), bytes_to_parse - cur_ofs);

         int largest_dec_index = enumerate_lz_decisions(cur_dict_ofs, approx_state, decisions, 1, max_admissable_match_len);
         if (largest_dec_index < 0)
         {
            LZHAM_LOG_ERROR(7044);
            return false;
         }

         const lzpriced_decision &dec = decisions[largest_dec_index];

         if (!parse_state.m_best_decisions.try_push_back(dec))
         {
            LZHAM_LOG_ERROR(7045);
            return false;
         }

         approx_state.partial_advance(dec);

         uint match_len = dec.get_len();
         LZHAM_ASSERT(match_len <= max_admissable_match_len);
         cur_dict_ofs += match_len;
         cur_lookahead_ofs += match_len;
         cur_ofs += match_len;

         if (parse_state.m_best_decisions.size() >= parse_state.m_max_greedy_decisions)
         {
            parse_state.m_greedy_parse_total_bytes_coded = cur_ofs;
            parse_state.m_bytes_actually_parsed = cur_ofs;
            parse_state.m_greedy_parse_gave_up = true;
            return false;
         }
      }

      parse_state.m_greedy_parse_total_bytes_coded = cur_ofs;

      LZHAM_ASSERT(cur_ofs == bytes_to_parse);

      parse_state.m_failed = false;
      parse_state.m_bytes_actually_parsed = parse_state.m_bytes_to_match;

      return true;
   }

   bool lzcompressor::compress_block(const void* pBuf, uint buf_len)
   {
      uint cur_ofs = 0;
      uint bytes_remaining = buf_len;
      while (bytes_remaining)
      {
         uint bytes_to_compress = math::minimum(m_accel.get_max_add_bytes(), bytes_remaining);
         if (!compress_block_internal(static_cast<const uint8*>(pBuf) + cur_ofs, bytes_to_compress))
         {
            LZHAM_LOG_ERROR(7046);
            return false;
         }

         cur_ofs += bytes_to_compress;
         bytes_remaining -= bytes_to_compress;
      }
      return true;
   }

   bool lzcompressor::compress_block_internal(const void* pBuf, uint buf_len)
   {
      scoped_perf_section compress_block_timer(cVarArgs, "****** compress_block %u", m_block_index);

      LZHAM_ASSERT(pBuf);
      LZHAM_ASSERT(buf_len <= m_params.m_block_size);

      LZHAM_ASSERT(m_src_size >= 0);
      if (m_src_size < 0)
         return false;

      m_src_size += buf_len;

      // Important: Don't do any expensive work until after add_bytes_begin() is called, to increase parallelism.
      if (!m_accel.add_bytes_begin(buf_len, static_cast<const uint8*>(pBuf)))
      {
         LZHAM_LOG_ERROR(7047);
         return false;
      }

      bool computed_adler32 = false;

      m_start_of_block_state = m_state;

      m_block_start_dict_ofs = m_accel.get_lookahead_pos() & (m_accel.get_max_dict_size() - 1);

      uint cur_dict_ofs = m_block_start_dict_ofs;

      uint bytes_to_match = buf_len;

      if (!m_codec.start_encoding((buf_len * 9) / 8))
      {
         LZHAM_LOG_ERROR(7048);
         return false;
      }

      if (!m_block_index)
      {
         if (!send_configuration())
         {
            LZHAM_LOG_ERROR(7049);
            return false;
         }
      }

#ifdef LZHAM_LZDEBUG
      m_codec.encode_bits(166, 12);
#endif

      if (!m_codec.encode_bits(cCompBlock, cBlockHeaderBits))
      {
         LZHAM_LOG_ERROR(7050);
         return false;
      }

      if (!m_codec.encode_arith_init())
      {
         LZHAM_LOG_ERROR(7051);
         return false;
      }

      m_state.start_of_block(m_accel, cur_dict_ofs, m_block_index);

      bool emit_reset_update_rate_command = false;

      if (m_params.m_lzham_compress_flags & LZHAM_COMP_FLAG_TRADEOFF_DECOMPRESSION_RATE_FOR_COMP_RATIO)
      {
         emit_reset_update_rate_command = true;

         m_state.reset_update_rate();
      }

      // TODO: We could also issue a full huff/arith table reset (code 2), and see if that actually improves the block's compression.
      m_codec.encode_bits(emit_reset_update_rate_command ? 1 : 0, cBlockFlushTypeBits);

      //coding_stats initial_stats(m_stats);

      uint initial_step = m_step;

      while (bytes_to_match)
      {
         const uint cAvgAcceptableGreedyMatchLen = 384;
         if ((m_params.m_pSeed_bytes) && (bytes_to_match >= cAvgAcceptableGreedyMatchLen))
         {
            parse_thread_state &greedy_parse_state = m_parse_thread_state[cMaxParseThreads];

            greedy_parse_state.m_pState = &greedy_parse_state.m_state;
            greedy_parse_state.m_state = m_state;
            greedy_parse_state.m_state.m_cur_ofs = cur_dict_ofs;

            greedy_parse_state.m_issue_reset_state_partial = false;
            greedy_parse_state.m_start_ofs = cur_dict_ofs;
            greedy_parse_state.m_bytes_to_match = LZHAM_MIN(bytes_to_match, static_cast<uint>(CLZBase::cMaxHugeMatchLen));

            greedy_parse_state.m_max_greedy_decisions = LZHAM_MAX((bytes_to_match / cAvgAcceptableGreedyMatchLen), 2);
            greedy_parse_state.m_greedy_parse_gave_up = false;
            greedy_parse_state.m_greedy_parse_total_bytes_coded = 0;

            greedy_parse_state.m_parse_early_out_thresh = UINT_MAX;
            greedy_parse_state.m_bytes_actually_parsed = 0;

            greedy_parse_state.m_use_semaphore = false;

            if (!greedy_parse(greedy_parse_state))
            {
               if (!greedy_parse_state.m_greedy_parse_gave_up)
               {
                  LZHAM_LOG_ERROR(7052);
                  return false;
               }
            }

            uint num_greedy_decisions_to_code = 0;

            const lzham::vector<lzdecision> &best_decisions = greedy_parse_state.m_best_decisions;

            if (!greedy_parse_state.m_greedy_parse_gave_up)
               num_greedy_decisions_to_code = best_decisions.size();
            else
            {
               uint num_small_decisions = 0;
               uint total_match_len = 0;
               uint max_match_len = 0;

               uint i;
               for (i = 0; i < best_decisions.size(); i++)
               {
                  const lzdecision &dec = best_decisions[i];
                  if (dec.get_len() <= CLZBase::cMaxMatchLen)
                  {
                     num_small_decisions++;
                     if (num_small_decisions > 16)
                        break;
                  }

                  total_match_len += dec.get_len();
                  max_match_len = LZHAM_MAX(max_match_len, dec.get_len());
               }

               if (max_match_len > CLZBase::cMaxMatchLen)
               {
                  if ((total_match_len / i) >= cAvgAcceptableGreedyMatchLen)
                  {
                     num_greedy_decisions_to_code = i;
                  }
               }
            }

            if (num_greedy_decisions_to_code)
            {
               for (uint i = 0; i < num_greedy_decisions_to_code; i++)
               {
                  LZHAM_ASSERT(best_decisions[i].m_pos == (int)cur_dict_ofs);
                  //LZHAM_ASSERT(i >= 0);
                  LZHAM_ASSERT(i < best_decisions.size());

#if LZHAM_UPDATE_STATS
                  bit_cost_t cost = m_state.get_cost(*this, m_accel, best_decisions[i]);
                  m_stats.update(best_decisions[i], m_state, m_accel, cost);
#endif

                  if (!code_decision(best_decisions[i], cur_dict_ofs, bytes_to_match))
                  {
                     LZHAM_LOG_ERROR(7053);
                     return false;
                  }

                  m_accel.advance_bytes(best_decisions[i].get_len());
               }

               if ((!greedy_parse_state.m_greedy_parse_gave_up) || (!bytes_to_match))
			   {
				   if (!computed_adler32)
				   {
					   computed_adler32 = true;

					   scoped_perf_section add_bytes_timer("adler32");
					   m_src_adler32 = adler32(pBuf, buf_len, m_src_adler32);
				   }

                  continue;
			   }
            }
         }

         uint num_parse_jobs = LZHAM_MIN(m_num_parse_threads, (bytes_to_match + cMaxParseGraphNodes - 1) / cMaxParseGraphNodes);
         if ((m_params.m_lzham_compress_flags & LZHAM_COMP_FLAG_DETERMINISTIC_PARSING) == 0)
         {
            if (m_use_task_pool && m_accel.get_max_helper_threads())
            {
               // Increase the number of active parse jobs as the match finder finishes up to keep CPU utilization up.
               num_parse_jobs += m_accel.get_num_completed_helper_threads();
               num_parse_jobs = LZHAM_MIN(num_parse_jobs, cMaxParseThreads);
            }
         }

         // Don't bother threading if the remaining bytes to parse is too small.
         if ((bytes_to_match < 1536) || (m_params.m_lzham_compress_flags & LZHAM_COMP_FLAG_FORCE_SINGLE_THREADED_PARSING))
            num_parse_jobs = 1;

         // Update the coding statistics more frequently near the beginning of streams.
         if ((!m_block_index) && ((cur_dict_ofs - m_block_start_dict_ofs) < cMaxParseGraphNodes * 4))
            num_parse_jobs = 1;

         uint parse_thread_start_ofs = cur_dict_ofs;
         uint parse_thread_total_size = LZHAM_MIN(bytes_to_match, cMaxParseGraphNodes * num_parse_jobs);

         uint parse_thread_remaining = parse_thread_total_size;

         state_base saved_state;
         if (num_parse_jobs == 1)
            m_state.save_partial_state(saved_state);

         for (uint parse_thread_index = 0; parse_thread_index < num_parse_jobs; parse_thread_index++)
         {
            parse_thread_state &parse_thread = m_parse_thread_state[parse_thread_index];

            if (num_parse_jobs == 1)
            {
               parse_thread.m_pState = &m_state;
            }
            else
            {
               parse_thread.m_pState = &parse_thread.m_state;
               parse_thread.m_state = m_state;
            }

            parse_thread.m_pState->m_cur_ofs = parse_thread_start_ofs;

            if (parse_thread_index > 0)
            {
               parse_thread.m_pState->reset_state_partial();
               parse_thread.m_issue_reset_state_partial = true;
            }
            else
            {
               parse_thread.m_issue_reset_state_partial = false;
            }

            parse_thread.m_start_ofs = parse_thread_start_ofs;
            if (parse_thread_index == (num_parse_jobs - 1))
               parse_thread.m_bytes_to_match = parse_thread_remaining;
            else
               parse_thread.m_bytes_to_match = parse_thread_total_size / num_parse_jobs;

            parse_thread.m_bytes_to_match = LZHAM_MIN(parse_thread.m_bytes_to_match, cMaxParseGraphNodes);
            LZHAM_ASSERT(parse_thread.m_bytes_to_match > 0);

            parse_thread.m_max_parse_node_states = m_params.m_extreme_parsing_max_best_arrivals;
            parse_thread.m_max_greedy_decisions = UINT_MAX;
            parse_thread.m_greedy_parse_gave_up = false;

            parse_thread.m_parse_early_out_thresh = UINT_MAX;
            parse_thread.m_bytes_actually_parsed = 0;

            parse_thread.m_use_semaphore = ((m_use_task_pool) && (num_parse_jobs > 1)) && (parse_thread_index > 0);

            if ((m_params.m_compression_level == cCompressionLevelUber) && (num_parse_jobs == 1))
            {
               // Allow the parsers to exit early if they encounter a graph bottleneck, so we can move the coding statistics forward before parsing again.
               parse_thread.m_parse_early_out_thresh = (m_params.m_lzham_compress_flags & LZHAM_COMP_FLAG_EXTREME_PARSING) ? 16 : 64;
            }

            parse_thread_start_ofs += parse_thread.m_bytes_to_match;
            parse_thread_remaining -= parse_thread.m_bytes_to_match;
         }

         {
            scoped_perf_section parse_timer("parsing");

            if ((m_use_task_pool) && (num_parse_jobs > 1))
            {
               {
                  scoped_perf_section queue_task_timer("queuing parse tasks");

                  if (!m_params.m_pTask_pool->queue_multiple_object_tasks(this, &lzcompressor::parse_job_callback, 1, num_parse_jobs - 1))
                     return false;
               }

               parse_job_callback(0, NULL);
            }
            else
            {
               for (uint parse_thread_index = 0; parse_thread_index < num_parse_jobs; parse_thread_index++)
               {
                  parse_job_callback(parse_thread_index, NULL);
               }
            }
         }

         if (num_parse_jobs == 1)
            m_state.restore_partial_state(saved_state);

         if (!computed_adler32)
         {
            computed_adler32 = true;

            scoped_perf_section add_bytes_timer("adler32");
            m_src_adler32 = adler32(pBuf, buf_len, m_src_adler32);
         }

#define LZHAM_RELEASE_SEMAPHORES for (uint pti = 1; pti < num_parse_jobs; pti++) if (m_parse_thread_state[pti].m_use_semaphore) { m_parse_thread_state[pti].m_finished.wait(); m_parse_thread_state[pti].m_use_semaphore = false; }

         {
            scoped_perf_section coding_timer("coding");

            uint total_bytes_parsed = 0;

            for (uint parse_thread_index = 0; parse_thread_index < num_parse_jobs; parse_thread_index++)
            {
               parse_thread_state &parse_thread = m_parse_thread_state[parse_thread_index];

               if (parse_thread.m_use_semaphore)
               {
                  scoped_perf_section sect(cVarArgs, "Waiting for parser %u", parse_thread_index);
                  m_parse_thread_state[parse_thread_index].m_finished.wait();
                  m_parse_thread_state[parse_thread_index].m_use_semaphore = false;
               }

               if (parse_thread.m_failed)
               {
                  LZHAM_RELEASE_SEMAPHORES
                  LZHAM_LOG_ERROR(7054);
                  return false;
               }

               const lzham::vector<lzdecision> &best_decisions = parse_thread.m_best_decisions;

               if (parse_thread.m_issue_reset_state_partial)
               {
                  if (!m_state.encode_reset_state_partial(m_codec, m_accel, cur_dict_ofs))
                  {
                     LZHAM_RELEASE_SEMAPHORES
                     LZHAM_LOG_ERROR(7055);
                     return false;
                  }
                  m_step++;
               }

               if (best_decisions.size())
               {
                  int i = 0;
                  int end_dec_index = static_cast<int>(best_decisions.size()) - 1;
                  int dec_step = 1;
                  if (parse_thread.m_emit_decisions_backwards)
                  {
                     i = static_cast<int>(best_decisions.size()) - 1;
                     end_dec_index = 0;
                     dec_step = -1;
                     LZHAM_ASSERT(best_decisions.back().m_pos == (int)parse_thread.m_start_ofs);
                  }
                  else
                  {
                     LZHAM_ASSERT(best_decisions.front().m_pos == (int)parse_thread.m_start_ofs);
                  }

                  // Loop rearranged to avoid bad x64 codegen problem with MSVC2008.
                  for ( ; ; )
                  {
                     LZHAM_ASSERT(best_decisions[i].m_pos == (int)cur_dict_ofs);
                     LZHAM_ASSERT(i >= 0);
                     LZHAM_ASSERT(i < (int)best_decisions.size());

#if LZHAM_UPDATE_STATS
                     bit_cost_t cost = m_state.get_cost(*this, m_accel, best_decisions[i]);
                     m_stats.update(best_decisions[i], m_state, m_accel, cost);
                     //m_state.print(m_codec, *this, m_accel, best_decisions[i]);
#endif

                     if (!code_decision(best_decisions[i], cur_dict_ofs, bytes_to_match))
                     {
                        LZHAM_RELEASE_SEMAPHORES
                        LZHAM_LOG_ERROR(7056);
                        return false;
                     }

                     total_bytes_parsed += best_decisions[i].get_len();

                     if (i == end_dec_index)
                        break;
                     i += dec_step;
                  }

                  LZHAM_NOTE_UNUSED(i);
               }

               LZHAM_ASSERT(cur_dict_ofs == parse_thread.m_start_ofs + parse_thread.m_bytes_actually_parsed);

            } // parse_thread_index

            m_accel.advance_bytes(total_bytes_parsed);

         } // coding

      } // while (bytes_to_match)

      {
         scoped_perf_section add_bytes_timer("add_bytes_end");
         m_accel.add_bytes_end();
      }

      if (!m_state.encode_eob(m_codec, m_accel, cur_dict_ofs))
      {
         LZHAM_LOG_ERROR(7057);
         return false;
      }

#ifdef LZHAM_LZDEBUG
      if (!m_codec.encode_bits(366, 12))
      {
         LZHAM_LOG_ERROR(7058);
         return false;
      }
#endif

      {
         scoped_perf_section stop_encoding_timer("stop_encoding");
         if (!m_codec.stop_encoding(true))
         {
            LZHAM_LOG_ERROR(7059);
            return false;
         }
      }

      // Coded the entire block - now see if it makes more sense to just send a raw/uncompressed block.

      uint compressed_size = m_codec.get_encoding_buf().size();
      LZHAM_NOTE_UNUSED(compressed_size);

      //bool used_raw_block = false;

#if !LZHAM_FORCE_ALL_RAW_BLOCKS
   #if (defined(LZHAM_DISABLE_RAW_BLOCKS) || defined(LZHAM_LZDEBUG))
       if (0)
   #else
       // TODO: Allow the user to control this threshold, i.e. if less than 1% then just store uncompressed.
       if (compressed_size >= buf_len)
   #endif
#endif
      {
         // Failed to compress the block, so go back to our original state and just code a raw block.
         m_state = m_start_of_block_state;
         m_step = initial_step;
         //m_stats = initial_stats;

         m_codec.reset();

         if (!m_codec.start_encoding(buf_len + 16))
         {
            LZHAM_LOG_ERROR(7060);
            return false;
         }

         if (!m_block_index)
         {
            if (!send_configuration())
            {
               LZHAM_LOG_ERROR(7061);
               return false;
            }
         }

#ifdef LZHAM_LZDEBUG
         if (!m_codec.encode_bits(166, 12))
         {
            LZHAM_LOG_ERROR(7062);
            return false;
         }
#endif

         if (!m_codec.encode_bits(cRawBlock, cBlockHeaderBits))
         {
            LZHAM_LOG_ERROR(7063);
            return false;
         }

         LZHAM_ASSERT(buf_len <= 0x1000000);
         if (!m_codec.encode_bits(buf_len - 1, 24))
         {
            LZHAM_LOG_ERROR(7064);
            return false;
         }

         // Write buf len check bits, to help increase the probability of detecting corrupted data more early.
         uint buf_len0 = (buf_len - 1) & 0xFF;
         uint buf_len1 = ((buf_len - 1) >> 8) & 0xFF;
         uint buf_len2 = ((buf_len - 1) >> 16) & 0xFF;
         if (!m_codec.encode_bits((buf_len0 ^ buf_len1) ^ buf_len2, 8))
         {
            LZHAM_LOG_ERROR(7065);
            return false;
         }

         if (!m_codec.encode_align_to_byte())
         {
            LZHAM_LOG_ERROR(7066);
            return false;
         }

         const uint8* pSrc = m_accel.get_ptr(m_block_start_dict_ofs);

         for (uint i = 0; i < buf_len; i++)
         {
            if (!m_codec.encode_bits(*pSrc++, 8))
            {
               LZHAM_LOG_ERROR(7067);
               return false;
            }
         }

         if (!m_codec.stop_encoding(true))
         {
            LZHAM_LOG_ERROR(7068);
            return false;
         }

         //used_raw_block = true;
         emit_reset_update_rate_command = false;
      }

      {
         scoped_perf_section append_timer("append");

         if (m_comp_buf.empty())
         {
            m_comp_buf.swap(m_codec.get_encoding_buf());
         }
         else
         {
            if (!m_comp_buf.append(m_codec.get_encoding_buf()))
            {
               LZHAM_LOG_ERROR(7069);
               return false;
            }
         }
      }

#if LZHAM_UPDATE_STATS
      LZHAM_VERIFY(m_stats.m_total_bytes == m_src_size);
      if (emit_reset_update_rate_command)
         m_stats.m_total_update_rate_resets++;
#endif

      m_block_index++;

      return true;
   }

} // namespace lzham
