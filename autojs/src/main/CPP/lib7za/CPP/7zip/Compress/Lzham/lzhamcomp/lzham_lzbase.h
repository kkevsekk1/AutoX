// File: lzham_lzbase.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

#include "lzham_lzdecompbase.h"

//#define LZHAM_LZVERIFY
//#define LZHAM_DISABLE_RAW_BLOCKS

namespace lzham
{

   struct CLZBase : CLZDecompBase
   {
      static uint8 m_slot_tab0[4096];
      static uint8 m_slot_tab1[512];
      static uint8 m_slot_tab2[256];

      void init_slot_tabs();

      inline void compute_lzx_position_slot(uint dist, uint& slot, uint& ofs)
      {
         uint s;
         if (dist < 0x1000)
            s = m_slot_tab0[dist];
         else if (dist < 0x100000)
            s = m_slot_tab1[dist >> 11];
         else if (dist < 0x1000000)
            s = m_slot_tab2[dist >> 16];
         else if (dist < 0x2000000)
            s = 48 + ((dist - 0x1000000) >> 23);
         else if (dist < 0x4000000)
            s = 50 + ((dist - 0x2000000) >> 24);
         else 
            s = 52 + ((dist - 0x4000000) >> 25);

         ofs = (dist - m_lzx_position_base[s]) & m_lzx_position_extra_mask[s];
         slot = s;

         LZHAM_ASSERT(s < m_num_lzx_slots);
         LZHAM_ASSERT((m_lzx_position_base[slot] + ofs) == dist);
         LZHAM_ASSERT(ofs < (1U << m_lzx_position_extra_bits[slot]));
      }
   };

} // namespace lzham
