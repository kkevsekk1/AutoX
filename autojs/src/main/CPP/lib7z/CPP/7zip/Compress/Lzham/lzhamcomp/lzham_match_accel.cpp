// File: lzham_match_accel.cpp
// See Copyright Notice and license at the end of include/lzham.h
#include "lzham_core.h"
#include "lzham_match_accel.h"
#include "lzham_timer.h"

static const int cHashSize24 = 0x1000000;
static const int cHashSize16 = 0x10000;

namespace lzham
{
   static inline uint32 hash2_to_12(uint c0, uint c1) { return c0 ^ (c1 << 4); }

   #define LZHAM_HASH3_16(c0, c1, c2) ((((uint)c0) | (((uint)c1) << 8U)) ^ (((uint)c2) << 4U))
   #define LZHAM_HASH3_24(c0, c1, c2) (((uint)c0) | (((uint)c1) << 8U) | (((uint)c2) << 16U))
      
   search_accelerator::search_accelerator(lzham_malloc_context malloc_context) :
      m_malloc_context(malloc_context),
      m_pLZBase(NULL),
      m_pTask_pool(NULL),
      m_max_helper_threads(0),
      m_max_dict_size(0),
      m_max_dict_size_mask(0),
      m_lookahead_pos(0),
      m_lookahead_size(0),
      m_cur_dict_size(0),
      m_dict(malloc_context), 
      m_hash(malloc_context), 
      m_nodes(malloc_context), 
      m_matches(malloc_context), 
      m_match_refs(malloc_context), 
      m_digram_hash(malloc_context),
      m_digram_next(malloc_context),
      m_fill_lookahead_pos(0),
      m_fill_lookahead_size(0),
      m_fill_dict_size(0),
      m_max_probes(0),
      m_max_matches(0),
      m_all_matches(false),
      m_deterministic(false),
      m_len2_matches(false),
      m_hash24(false),
      m_next_match_ref(0),
      m_num_completed_helper_threads(0)
   {
      for (uint i = 0; i < LZHAM_ARRAY_SIZE(m_thread_dict_offsets); i++)
         m_thread_dict_offsets[i].set_malloc_context(malloc_context);
   }

   bool search_accelerator::init(CLZBase* pLZBase, task_pool* pPool, uint max_helper_threads, uint max_dict_size, uint max_matches, bool all_matches, uint max_probes, uint flags)
   {
      LZHAM_ASSERT(pLZBase);
      LZHAM_ASSERT(max_dict_size && math::is_power_of_2(max_dict_size));
      LZHAM_ASSERT(max_probes);

      m_max_probes = LZHAM_MIN(cMatchAccelMaxSupportedProbes, max_probes);
      
      m_deterministic = (flags & cFlagDeterministic) != 0;
      m_len2_matches = (flags & cFlagLen2Matches) != 0;
      m_hash24 = (flags & cFlagHash24) != 0;

      m_pLZBase = pLZBase;
      m_pTask_pool = max_helper_threads ? pPool : NULL;
      m_max_helper_threads = m_pTask_pool ? max_helper_threads : 0;
      m_max_matches = LZHAM_MIN(m_max_probes, max_matches);
      m_all_matches = all_matches;

      m_max_dict_size = max_dict_size;
      m_max_dict_size_mask = m_max_dict_size - 1;
      m_cur_dict_size = 0;
      m_lookahead_size = 0;
      m_lookahead_pos = 0;
      m_fill_lookahead_pos = 0;
      m_fill_lookahead_size = 0;
      m_fill_dict_size = 0;
      m_num_completed_helper_threads = 0;

      if (!m_dict.try_resize_no_construct(max_dict_size + LZHAM_MIN(m_max_dict_size, static_cast<uint>(CLZBase::cMaxHugeMatchLen))))
      {
         LZHAM_LOG_ERROR(9000);
         return false;
      }

      if (!m_hash.try_resize_no_construct(m_hash24 ? cHashSize24 : cHashSize16))
      {
         LZHAM_LOG_ERROR(9001);
         return false;
      }

      memset(m_hash.get_ptr(), 0, m_hash.size_in_bytes());

      if (!m_nodes.try_resize_no_construct(max_dict_size))
      {
         LZHAM_LOG_ERROR(9002);
         return false;
      }

      for (uint i = 0; i < max_helper_threads; i++)
      {
         if (!m_thread_dict_offsets[i].try_reserve(256 * 1024))
         {
            LZHAM_LOG_ERROR(9003);
            return false;
         }
      }
               
      // Shouldn't be necessary
      //if (m_deterministic)
      //   memset(m_nodes.get_ptr(), 0, m_nodes.size_in_bytes());

      return true;
   }

   void search_accelerator::reset()
   {
      m_cur_dict_size = 0;
      m_lookahead_size = 0;
      m_lookahead_pos = 0;
      m_fill_lookahead_pos = 0;
      m_fill_lookahead_size = 0;
      m_fill_dict_size = 0;
      m_num_completed_helper_threads = 0;

      // Clearing the hash tables is only necessary for determinism (otherwise, it's possible the matches returned after a reset will depend on the data processes before the reset).
      if (m_hash.size()) 
         memset(m_hash.get_ptr(), 0, m_hash.size_in_bytes());
      
      if (m_digram_hash.size())
         memset(m_digram_hash.get_ptr(), 0, m_digram_hash.size_in_bytes());
      
      // Shouldn't be necessary
      //if (m_deterministic)
      //   memset(m_nodes.get_ptr(), 0, m_nodes.size_in_bytes());
   }

   void search_accelerator::flush()
   {
      m_cur_dict_size = 0;
   }

   uint search_accelerator::get_max_add_bytes() const
   {
      uint add_pos = static_cast<uint>(m_lookahead_pos & (m_max_dict_size - 1));
      return m_max_dict_size - add_pos;
   }

   static uint8 g_hamming_dist[256] =
   {
      0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3, 3, 4,
      1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
      1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
      1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
      2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6,
      3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
      3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7,
      4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7, 7, 8
   };
      
   void search_accelerator::find_all_matches_callback_st(uint64 data, void* pData_ptr)
   {
      scoped_perf_section find_all_matches_timer("find_all_matches_callback_st");

      LZHAM_NOTE_UNUSED(data);
      LZHAM_NOTE_UNUSED(pData_ptr);
      
      dict_match temp_matches[cMatchAccelMaxSupportedProbes * 2];

      uint fill_lookahead_pos = m_fill_lookahead_pos;
      uint fill_dict_size = m_fill_dict_size;
      uint fill_lookahead_size = m_fill_lookahead_size;

      uint c0 = 0, c1 = 0;
      if (fill_lookahead_size >= 2)
      {
         c0 = m_dict[fill_lookahead_pos & m_max_dict_size_mask];
         c1 = m_dict[(fill_lookahead_pos & m_max_dict_size_mask) + 1];
      }

      const uint8* pDict = m_dict.get_ptr();

      while (fill_lookahead_size >= 3)
      {
         uint insert_pos = fill_lookahead_pos & m_max_dict_size_mask;

         uint c2 = pDict[insert_pos + 2];

         uint h;
         if (m_hash24)
            h = LZHAM_HASH3_24(c0, c1, c2);
         else
            h = LZHAM_HASH3_16(c0, c1, c2);

         c0 = c1;
         c1 = c2;

         dict_match *pDstMatch = temp_matches;

         uint cur_pos = m_hash[h];
         m_hash[h] = static_cast<uint>(fill_lookahead_pos);

         uint *pLeft = &m_nodes[insert_pos].m_left;
         uint *pRight = &m_nodes[insert_pos].m_right;

         const uint max_match_len = LZHAM_MIN(static_cast<uint>(CLZBase::cMaxMatchLen), fill_lookahead_size);
         uint best_match_len = 2;

         const uint8* pIns = &pDict[insert_pos];

         uint n = m_max_probes;
         for ( ; ; )
         {
            uint delta_pos = fill_lookahead_pos - cur_pos;
            if ((n-- == 0) || (!delta_pos) || (delta_pos >= fill_dict_size))
            {
               *pLeft = 0;
               *pRight = 0;
               break;
            }

            uint pos = cur_pos & m_max_dict_size_mask;
                                                            
            // Unfortunately, the initial compare match_len must be 0 because of the way we hash and truncate matches at the end of each block.
            uint match_len = 0;
            const uint8* pComp = &pDict[pos];

#if LZHAM_PLATFORM_X360 || (LZHAM_USE_UNALIGNED_INT_LOADS == 0) || LZHAM_BIG_ENDIAN_CPU
            for ( ; match_len < max_match_len; match_len++)
               if (pComp[match_len] != pIns[match_len])
                  break;
#else
            // Compare a qword at a time for a bit more efficiency.
            uint64 x = *reinterpret_cast<const uint64 *>(pComp);
            uint64 y = *reinterpret_cast<const uint64 *>(pIns);
            if ((max_match_len >= 8) && (x == y))
            {
               const uint64* pComp_cur = reinterpret_cast<const uint64*>(pComp) + 1;
               const uint64* pIns_cur = reinterpret_cast<const uint64*>(pIns) + 1;
                              
               const uint64* pComp_end = reinterpret_cast<const uint64*>(pComp + max_match_len - 7);
               while (pComp_cur < pComp_end)
               {
                  if (*pComp_cur != *pIns_cur)
                     break;
                  ++pComp_cur;
                  ++pIns_cur;
               }

               uint alt_match_len = static_cast<uint>(reinterpret_cast<const uint8*>(pComp_cur) - reinterpret_cast<const uint8*>(pComp));
               for ( ; alt_match_len < max_match_len; alt_match_len++)
                  if (pComp[alt_match_len] != pIns[alt_match_len])
                     break;

               match_len = alt_match_len;
            }
            else
            {
               if ((uint32)x == (uint32)y)
               {
                  x >>= 32;
                  y >>= 32;
                  match_len += 4;
               }

               if ((uint16)x == (uint16)y)
               {
                  x >>= 16;
                  y >>= 16;
                  match_len += 2;
               }
               
               if ((uint8)x == (uint8)y)
                  match_len++;
               
               match_len = math::minimum(match_len, max_match_len);
            }
#endif

#ifdef LZVERIFY
            uint check_match_len;
            for (check_match_len = 0; check_match_len < max_match_len; check_match_len++)
               if (pComp[check_match_len] != pIns[check_match_len])
                  break;
            LZHAM_VERIFY(match_len == check_match_len);
#endif

            node *pNode = &m_nodes[pos];

            if (match_len > best_match_len)
            {
               pDstMatch->m_len = static_cast<uint16>(match_len - CLZBase::cMinMatchLen);
               pDstMatch->m_dist = delta_pos;
               pDstMatch++;

               best_match_len = match_len;

               if (match_len == max_match_len)
               {
                  *pLeft = pNode->m_left;
                  *pRight = pNode->m_right;
                  break;
               }
            }
            else if (m_all_matches)
            {
               pDstMatch->m_len = static_cast<uint16>(match_len - CLZBase::cMinMatchLen);
               pDstMatch->m_dist = delta_pos;
               pDstMatch++;
            }
            else if ((best_match_len > 2) && (best_match_len == match_len))
            {
               uint bestMatchDist = pDstMatch[-1].m_dist;
               uint compMatchDist = delta_pos;

               uint bestMatchSlot, bestMatchSlotOfs;
               m_pLZBase->compute_lzx_position_slot(bestMatchDist, bestMatchSlot, bestMatchSlotOfs);

               uint compMatchSlot, compMatchOfs;
               m_pLZBase->compute_lzx_position_slot(compMatchDist, compMatchSlot, compMatchOfs);

               // If both matches uses the same match slot, choose the one with the offset containing the lowest nibble as these bits separately entropy coded.
               // This could choose a match which is further away in the absolute sense, but closer in a coding sense.
               if ( (compMatchSlot < bestMatchSlot) ||
                  ((compMatchSlot >= 8) && (compMatchSlot == bestMatchSlot) && ((compMatchOfs & 15) < (bestMatchSlotOfs & 15))) )
               {
                  LZHAM_ASSERT((pDstMatch[-1].m_len + (uint)CLZBase::cMinMatchLen) == best_match_len);
                  pDstMatch[-1].m_dist = delta_pos;
               }
               else if ((match_len < max_match_len) && (compMatchSlot <= bestMatchSlot))
               {
                  // Choose the match which has lowest hamming distance in the mismatch byte for a tiny win on binary files.
                  // TODO: This competes against the prev. optimization.
                  uint desired_mismatch_byte = pIns[match_len];

                  uint cur_mismatch_byte = pDict[(insert_pos - bestMatchDist + match_len) & m_max_dict_size_mask];
                  uint cur_mismatch_dist = g_hamming_dist[cur_mismatch_byte ^ desired_mismatch_byte];

                  uint new_mismatch_byte = pComp[match_len];
                  uint new_mismatch_dist = g_hamming_dist[new_mismatch_byte ^ desired_mismatch_byte];
                  if (new_mismatch_dist < cur_mismatch_dist)
                  {
                     LZHAM_ASSERT((pDstMatch[-1].m_len + (uint)CLZBase::cMinMatchLen) == best_match_len);
                     pDstMatch[-1].m_dist = delta_pos;
                  }
               }
            }

            uint new_pos;
            if (pComp[match_len] < pIns[match_len])
            {
               *pLeft = cur_pos;
               pLeft = &pNode->m_right;
               new_pos = pNode->m_right;
            }
            else
            {
               *pRight = cur_pos;
               pRight = &pNode->m_left;
               new_pos = pNode->m_left;
            }
            if (new_pos == cur_pos)
               break;
            cur_pos = new_pos;
         }

         const uint num_matches = (uint)(pDstMatch - temp_matches);

         if (num_matches)
         {
            pDstMatch[-1].m_dist |= 0x80000000;

            const uint num_matches_to_write = LZHAM_MIN(num_matches, m_max_matches);

            const uint match_ref_ofs = m_next_match_ref;
            
            m_next_match_ref += num_matches_to_write;

            memcpy(&m_matches[match_ref_ofs],
                   temp_matches + (num_matches - num_matches_to_write),
                   sizeof(temp_matches[0]) * num_matches_to_write);

            m_match_refs[static_cast<uint>(fill_lookahead_pos - m_fill_lookahead_pos)] = match_ref_ofs;
         }
         else
         {
            m_match_refs[static_cast<uint>(fill_lookahead_pos - m_fill_lookahead_pos)] = -2;
         }

         fill_lookahead_pos++;
         fill_lookahead_size--;
         fill_dict_size++;
      }

      while (fill_lookahead_size)
      {
         uint insert_pos = fill_lookahead_pos & m_max_dict_size_mask;
         m_nodes[insert_pos].m_left = 0;
         m_nodes[insert_pos].m_right = 0;

         m_match_refs[static_cast<uint>(fill_lookahead_pos - m_fill_lookahead_pos)] = -2;

         fill_lookahead_pos++;
         fill_lookahead_size--;
         fill_dict_size++;
      }
      
      m_num_completed_helper_threads++;
   }

   void search_accelerator::find_all_matches_callback_mt(uint64 data, void* pData_ptr)
   {
      scoped_perf_section find_all_matches_timer(cVarArgs, "find_all_matches_callback_mt %u", (uint)data);

      LZHAM_NOTE_UNUSED(pData_ptr);
      const uint thread_index = (uint)data;

      dict_match temp_matches[cMatchAccelMaxSupportedProbes * 2];
            
      const uint8* pDict = m_dict.get_ptr();
            
      const uint *pDict_ofsets = m_thread_dict_offsets[thread_index].get_ptr();
      const uint num_dict_offsets = m_thread_dict_offsets[thread_index].size();

      for (uint i = 0; i < num_dict_offsets; i++)
      {
         uint lookahead_ofs = *pDict_ofsets++;
         uint fill_lookahead_pos = m_fill_lookahead_pos + lookahead_ofs;
         uint fill_dict_size = m_fill_dict_size + lookahead_ofs;
         LZHAM_ASSERT(m_fill_lookahead_size > lookahead_ofs);
         uint fill_lookahead_size = m_fill_lookahead_size - lookahead_ofs;
         
         const uint max_match_len = LZHAM_MIN(static_cast<uint>(CLZBase::cMaxMatchLen), fill_lookahead_size);

         uint insert_pos = fill_lookahead_pos & m_max_dict_size_mask;

         uint c0 = pDict[insert_pos];
         uint c1 = pDict[insert_pos + 1];
         uint c2 = pDict[insert_pos + 2];

         uint h;
         if (m_hash24)
            h = LZHAM_HASH3_24(c0, c1, c2);
         else
            h = LZHAM_HASH3_16(c0, c1, c2);

         dict_match* pDstMatch = temp_matches;

         uint cur_pos = m_hash[h];
         m_hash[h] = static_cast<uint>(fill_lookahead_pos);

         uint *pLeft = &m_nodes[insert_pos].m_left;
         uint *pRight = &m_nodes[insert_pos].m_right;
                  
         uint best_match_len = 2;

         const uint8* pIns = &pDict[insert_pos];

         uint n = m_max_probes;
         for ( ; ; )
         {
            uint delta_pos = fill_lookahead_pos - cur_pos;
            if ((n-- == 0) || (!delta_pos) || (delta_pos >= fill_dict_size))
            {
               *pLeft = 0;
               *pRight = 0;
               break;
            }

            uint pos = cur_pos & m_max_dict_size_mask;
                                                            
            // Unfortunately, the initial compare match_len must be 0 because of the way we hash and truncate matches at the end of each block.
            uint match_len = 0;
            const uint8* pComp = &pDict[pos];

#if LZHAM_PLATFORM_X360 || (LZHAM_USE_UNALIGNED_INT_LOADS == 0) || LZHAM_BIG_ENDIAN_CPU
            for ( ; match_len < max_match_len; match_len++)
               if (pComp[match_len] != pIns[match_len])
                  break;
#else
            // Compare a qword at a time for a bit more efficiency.
            uint64 x = *reinterpret_cast<const uint64 *>(pComp);
            uint64 y = *reinterpret_cast<const uint64 *>(pIns);
            if ((max_match_len >= 8) && (x == y))
            {
               const uint64* pComp_cur = reinterpret_cast<const uint64*>(pComp) + 1;
               const uint64* pIns_cur = reinterpret_cast<const uint64*>(pIns) + 1;
                              
               const uint64* pComp_end = reinterpret_cast<const uint64*>(pComp + max_match_len - 7);
               while (pComp_cur < pComp_end)
               {
                  if (*pComp_cur != *pIns_cur)
                     break;
                  ++pComp_cur;
                  ++pIns_cur;
               }

               uint alt_match_len = static_cast<uint>(reinterpret_cast<const uint8*>(pComp_cur) - reinterpret_cast<const uint8*>(pComp));
               for ( ; alt_match_len < max_match_len; alt_match_len++)
                  if (pComp[alt_match_len] != pIns[alt_match_len])
                     break;

               match_len = alt_match_len;
            }
            else
            {
               if ((uint32)x == (uint32)y)
               {
                  x >>= 32;
                  y >>= 32;
                  match_len += 4;
               }

               if ((uint16)x == (uint16)y)
               {
                  x >>= 16;
                  y >>= 16;
                  match_len += 2;
               }
               
               if ((uint8)x == (uint8)y)
                  match_len++;
               
               match_len = math::minimum(match_len, max_match_len);
            }
#endif

#ifdef LZVERIFY
            uint check_match_len;
            for (check_match_len = 0; check_match_len < max_match_len; check_match_len++)
               if (pComp[check_match_len] != pIns[check_match_len])
                  break;
            LZHAM_VERIFY(match_len == check_match_len);
#endif

            node *pNode = &m_nodes[pos];

            if (match_len > best_match_len)
            {
               pDstMatch->m_len = static_cast<uint16>(match_len - CLZBase::cMinMatchLen);
               pDstMatch->m_dist = delta_pos;
               pDstMatch++;

               best_match_len = match_len;

               if (match_len == max_match_len)
               {
                  *pLeft = pNode->m_left;
                  *pRight = pNode->m_right;
                  break;
               }
            }
            else if (m_all_matches)
            {
               pDstMatch->m_len = static_cast<uint16>(match_len - CLZBase::cMinMatchLen);
               pDstMatch->m_dist = delta_pos;
               pDstMatch++;
            }
            else if ((best_match_len > 2) && (best_match_len == match_len))
            {
               uint bestMatchDist = pDstMatch[-1].m_dist;
               uint compMatchDist = delta_pos;

               uint bestMatchSlot, bestMatchSlotOfs;
               m_pLZBase->compute_lzx_position_slot(bestMatchDist, bestMatchSlot, bestMatchSlotOfs);

               uint compMatchSlot, compMatchOfs;
               m_pLZBase->compute_lzx_position_slot(compMatchDist, compMatchSlot, compMatchOfs);

               // If both matches uses the same match slot, choose the one with the offset containing the lowest nibble as these bits separately entropy coded.
               // This could choose a match which is further away in the absolute sense, but closer in a coding sense.
               if ( (compMatchSlot < bestMatchSlot) ||
                  ((compMatchSlot >= 8) && (compMatchSlot == bestMatchSlot) && ((compMatchOfs & 15) < (bestMatchSlotOfs & 15))) )
               {
                  LZHAM_ASSERT((pDstMatch[-1].m_len + (uint)CLZBase::cMinMatchLen) == best_match_len);
                  pDstMatch[-1].m_dist = delta_pos;
               }
               else if ((match_len < max_match_len) && (compMatchSlot <= bestMatchSlot))
               {
                  // Choose the match which has lowest hamming distance in the mismatch byte for a tiny win on binary files.
                  // TODO: This competes against the prev. optimization.
                  uint desired_mismatch_byte = pIns[match_len];

                  uint cur_mismatch_byte = pDict[(insert_pos - bestMatchDist + match_len) & m_max_dict_size_mask];
                  uint cur_mismatch_dist = g_hamming_dist[cur_mismatch_byte ^ desired_mismatch_byte];

                  uint new_mismatch_byte = pComp[match_len];
                  uint new_mismatch_dist = g_hamming_dist[new_mismatch_byte ^ desired_mismatch_byte];
                  if (new_mismatch_dist < cur_mismatch_dist)
                  {
                     LZHAM_ASSERT((pDstMatch[-1].m_len + (uint)CLZBase::cMinMatchLen) == best_match_len);
                     pDstMatch[-1].m_dist = delta_pos;
                  }
               }
            }

            uint new_pos;
            if (pComp[match_len] < pIns[match_len])
            {
               *pLeft = cur_pos;
               pLeft = &pNode->m_right;
               new_pos = pNode->m_right;
            }
            else
            {
               *pRight = cur_pos;
               pRight = &pNode->m_left;
               new_pos = pNode->m_left;
            }
            if (new_pos == cur_pos)
               break;
            cur_pos = new_pos;
         }

         const uint num_matches = (uint)(pDstMatch - temp_matches);

         if (num_matches)
         {
            pDstMatch[-1].m_dist |= 0x80000000;

            const uint num_matches_to_write = LZHAM_MIN(num_matches, m_max_matches);

            const uint match_ref_ofs = static_cast<uint>(atomic_exchange_add(&m_next_match_ref, num_matches_to_write));

            memcpy(&m_matches[match_ref_ofs],
                   temp_matches + (num_matches - num_matches_to_write),
                   sizeof(temp_matches[0]) * num_matches_to_write);

            // FIXME: This is going to really hurt on platforms requiring export barriers.
            LZHAM_MEMORY_EXPORT_BARRIER

            atomic_exchange32((atomic32_t*)&m_match_refs[static_cast<uint>(fill_lookahead_pos - m_fill_lookahead_pos)], match_ref_ofs);
         }
         else
         {
            atomic_exchange32((atomic32_t*)&m_match_refs[static_cast<uint>(fill_lookahead_pos - m_fill_lookahead_pos)], -2);
         }
      }
                  
      atomic_increment32(&m_num_completed_helper_threads);
   }

   bool search_accelerator::find_len2_matches()
   {
      if (!m_digram_hash.size())
      {
         if (!m_digram_hash.try_resize(cDigramHashSize))
         {
            LZHAM_LOG_ERROR(9004);
            return false;
         }
      }

      if (m_digram_next.size() < m_lookahead_size)
      {
         if (!m_digram_next.try_resize(m_lookahead_size))
         {
            LZHAM_LOG_ERROR(9005);
            return false;
         }
      }

      uint lookahead_dict_pos = m_lookahead_pos & m_max_dict_size_mask;

      for (int lookahead_ofs = 0; lookahead_ofs < ((int)m_lookahead_size - 1); ++lookahead_ofs, ++lookahead_dict_pos)
      {
         uint c0 = m_dict[lookahead_dict_pos];
         uint c1 = m_dict[lookahead_dict_pos + 1];

         uint h = hash2_to_12(c0, c1) & (cDigramHashSize - 1);

         m_digram_next[lookahead_ofs] = m_digram_hash[h];
         m_digram_hash[h] = m_lookahead_pos + lookahead_ofs;
      }

      m_digram_next[m_lookahead_size - 1] = 0;

      return true;
   }

   uint search_accelerator::get_len2_match(uint lookahead_ofs)
   {
      if ((m_fill_lookahead_size - lookahead_ofs) < 2)
         return 0;
      if (!m_digram_next.size())
         return 0;

      uint cur_pos = m_lookahead_pos + lookahead_ofs;

      uint next_match_pos = m_digram_next[cur_pos - m_fill_lookahead_pos];

      uint match_dist = cur_pos - next_match_pos;

      if ((!match_dist) || (match_dist > CLZBase::cMaxLen2MatchDist) || (match_dist > (m_cur_dict_size + lookahead_ofs)))
         return 0;

      const uint8* pCur = &m_dict[cur_pos & m_max_dict_size_mask];
      const uint8* pMatch = &m_dict[next_match_pos & m_max_dict_size_mask];

      if ((pCur[0] == pMatch[0]) && (pCur[1] == pMatch[1]))
         return match_dist;

      return 0;
   }

   static inline uint32 bitmix32(uint32 a)                    
   {                      
      a -= (a << 6);                                  
      a ^= (a >> 17);                                 
      a -= (a << 9);                                  
      a ^= (a << 4);                                  
      a -= (a << 3);                                  
      a ^= (a << 10);                                 
      a ^= (a >> 15);                                 
      return a;                                       
   }                                                   
   
   bool search_accelerator::find_all_matches(uint num_bytes)
   {
      if (!m_matches.try_resize_no_construct(m_max_probes * num_bytes))
      {
         LZHAM_LOG_ERROR(9006);
         return false;
      }

      if (!m_match_refs.try_resize_no_construct(num_bytes))
      {
         LZHAM_LOG_ERROR(9007);
         return false;
      }

      memset(m_match_refs.get_ptr(), 0xFF, m_match_refs.size_in_bytes());

      m_fill_lookahead_pos = m_lookahead_pos;
      m_fill_lookahead_size = num_bytes;
      m_fill_dict_size = m_cur_dict_size;

      m_next_match_ref = 0;

      if ((!m_pTask_pool) || (m_max_helper_threads < 1) || (num_bytes < 1024))
      {
         find_all_matches_callback_st(0, NULL);
         
         m_num_completed_helper_threads = 0;
      }
      else
      {
         for (uint i = num_bytes - 2; i < num_bytes; i++)
         {
            uint fill_lookahead_pos = m_fill_lookahead_pos + i;
            uint insert_pos = fill_lookahead_pos & m_max_dict_size_mask;
            m_nodes[insert_pos].m_left = 0;
            m_nodes[insert_pos].m_right = 0;

            m_match_refs[static_cast<uint>(fill_lookahead_pos - m_fill_lookahead_pos)] = -2;
         }

         for (uint i = 0; i < m_max_helper_threads; i++)
            m_thread_dict_offsets[i].try_resize(0);

         uint bytes_to_add = num_bytes - 2;

         scoped_perf_section sect(cVarArgs, "****** find_all_matches_prep %u", bytes_to_add);
                       
         const uint8* pDict = &m_dict[m_lookahead_pos & m_max_dict_size_mask];
                     
         if (m_hash24)
         {
            uint t = (pDict[0] << 8) | (pDict[1] << 16);

            if (math::is_power_of_2(m_max_helper_threads))
            {
               const uint bitmask = (m_max_helper_threads - 1);
               for (uint i = 0; i < bytes_to_add; i++)
               {
                  t = (t >> 8) | (pDict[2] << 16);

                  LZHAM_ASSERT(t == LZHAM_HASH3_24(pDict[0], pDict[1], pDict[2]));

                  uint thread_index = bitmix32(t) & bitmask;

                  if (!m_thread_dict_offsets[thread_index].try_push_back(i))
                  {
                     LZHAM_LOG_ERROR(9008);
                     return false;
                  }

                  pDict++;
               }
            }
            else
            {
               for (uint i = 0; i < bytes_to_add; i++)
               {
                  t = (t >> 8) | (pDict[2] << 16);

                  LZHAM_ASSERT(t == LZHAM_HASH3_24(pDict[0], pDict[1], pDict[2]));

                  uint thread_index = bitmix32(t) % m_max_helper_threads;

                  if (!m_thread_dict_offsets[thread_index].try_push_back(i))
                  {
                     LZHAM_LOG_ERROR(9009);
                     return false;
                  }

                  pDict++;
               }
            }
         }
         else
         {
            uint c0 = pDict[0];
            uint c1 = pDict[1];
                  
            for (uint i = 0; i < bytes_to_add; i++)
            {
               uint c2 = pDict[2];
               
               uint t = LZHAM_HASH3_16(c0, c1, c2);

               c0 = c1;
               c1 = c2;
                                                            
               uint thread_index = bitmix32(t) % m_max_helper_threads;

               if (!m_thread_dict_offsets[thread_index].try_push_back(i))
               {
                  LZHAM_LOG_ERROR(9010);
                  return false;
               }
               
               pDict++;
            }
         }
         
         m_num_completed_helper_threads = 0;
         
         if (!m_pTask_pool->queue_multiple_object_tasks(this, &search_accelerator::find_all_matches_callback_mt, 0, m_max_helper_threads))
         {
            LZHAM_LOG_ERROR(9011);
            return false;
         }
      }

      return m_len2_matches ? find_len2_matches() : true;
   }

   bool search_accelerator::add_bytes_begin(uint num_bytes, const uint8* pBytes)
   {
      LZHAM_ASSERT(num_bytes <= m_max_dict_size);
      LZHAM_ASSERT(!m_lookahead_size);

      uint add_pos = m_lookahead_pos & m_max_dict_size_mask;
      LZHAM_ASSERT((add_pos + num_bytes) <= m_max_dict_size);

      memcpy(&m_dict[add_pos], pBytes, num_bytes);

      uint dict_bytes_to_mirror = LZHAM_MIN(static_cast<uint>(CLZBase::cMaxHugeMatchLen), m_max_dict_size);
      if (add_pos < dict_bytes_to_mirror)
         memcpy(&m_dict[m_max_dict_size], &m_dict[0], dict_bytes_to_mirror);

      m_lookahead_size = num_bytes;

      uint max_possible_dict_size = m_max_dict_size - num_bytes;
      m_cur_dict_size = LZHAM_MIN(m_cur_dict_size, max_possible_dict_size);

      m_next_match_ref = 0;

      return find_all_matches(num_bytes);
   }

   void search_accelerator::add_bytes_end()
   {
      if (m_pTask_pool)
      {
         m_pTask_pool->join();
      }

      LZHAM_ASSERT((uint)m_next_match_ref <= m_matches.size());
   }

   dict_match* search_accelerator::find_matches(uint lookahead_ofs, bool spin)
   {
      LZHAM_ASSERT(lookahead_ofs < m_lookahead_size);

      const uint match_ref_ofs = static_cast<uint>(m_lookahead_pos - m_fill_lookahead_pos + lookahead_ofs);

      int match_ref;
      uint spin_count = 0;

      // This may spin until the match finder job(s) catch up to the caller's lookahead position.
      for ( ; ; )
      {
         match_ref = static_cast<int>(m_match_refs[match_ref_ofs]);
         if (match_ref == -2)
            return NULL;
         else if (match_ref != -1)
            break;

         spin_count++;
         const uint cMaxSpinCount = 1000;
         if ((spin) && (spin_count < cMaxSpinCount))
         {
            lzham_yield_processor();
            lzham_yield_processor();
            lzham_yield_processor();
            lzham_yield_processor();
            lzham_yield_processor();
            lzham_yield_processor();
            lzham_yield_processor();
            lzham_yield_processor();

            LZHAM_MEMORY_IMPORT_BARRIER
         }
         else
         {
            scoped_perf_section sect("find_matches_sleep");

            spin_count = cMaxSpinCount;

            lzham_sleep(1);
         }
      }

      LZHAM_MEMORY_IMPORT_BARRIER

      return &m_matches[match_ref];
   }

   void search_accelerator::advance_bytes(uint num_bytes)
   {
      LZHAM_ASSERT(num_bytes <= m_lookahead_size);

      m_lookahead_pos += num_bytes;
      m_lookahead_size -= num_bytes;

      m_cur_dict_size += num_bytes;
      LZHAM_ASSERT(m_cur_dict_size <= m_max_dict_size);
   }
}
