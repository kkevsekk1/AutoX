// File: lzham_lzdecompbase.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

//#define LZHAM_LZDEBUG

#define LZHAM_IS_MATCH_MODEL_INDEX(cur_state) (cur_state)

namespace lzham
{
	struct table_update_settings
	{
		uint16 m_max_update_interval;
		uint16 m_slow_rate;
	};
	extern table_update_settings g_table_update_settings[];

   struct CLZDecompBase
   {
      enum 
      {
         cMinMatchLen = 2U,
         cMaxMatchLen = 257U,
         
         cMaxHugeMatchLen = 65536,
                           
         cMinDictSizeLog2 = 15,
         cMaxDictSizeLog2 = 29,
                  
         cMatchHistSize = 4,
         cMaxLen2MatchDist = 2047
      };
         
      enum 
      {
         cLZXNumSecondaryLengths = 249,
         
         cNumHugeMatchCodes = 1,
         cMaxHugeMatchCodeBits = 16,
                                    
         cLZXNumSpecialLengths = 2,
         
         cLZXLowestUsableMatchSlot = 1,
         cLZXMaxPositionSlots = 128
      };
      
      enum
      {
         cLZXSpecialCodeEndOfBlockCode = 0,
         cLZXSpecialCodePartialStateReset = 1
      };
      
      enum
      {  
         cLZHAMDebugSyncMarkerValue = 666,
         cLZHAMDebugSyncMarkerBits = 12
      };

      enum
      {
         cBlockHeaderBits = 2,
         cBlockCheckBits = 4,
         cBlockFlushTypeBits = 2,
         
         cSyncBlock = 0,
         cCompBlock = 1,
         cRawBlock = 2,
         cEOFBlock = 3
      };
      
      enum
      {
         cNumStates = 12,
         cNumLitStates = 7,
      };
				      
      uint m_dict_size_log2;
      uint m_dict_size;
      
      uint m_num_lzx_slots;

      static uint m_lzx_position_base[cLZXMaxPositionSlots];
      static uint m_lzx_position_extra_mask[cLZXMaxPositionSlots];
      static uint8 m_lzx_position_extra_bits[cLZXMaxPositionSlots];
		            
      void init_position_slots(uint dict_size_log2);
   };
   
} // namespace lzham
