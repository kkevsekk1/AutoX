// File: lzham_huffman_codes.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

namespace lzham
{
   //const uint cHuffmanMaxSupportedSyms = 600;
   const uint cHuffmanMaxSupportedSyms = 1024;

   const uint cMaxExpectedHuffCodeSize = 16;

   struct code_size_histogram
   {
      enum { cMaxUnlimitedHuffCodeSize = 32 };
      uint m_num_codes[cMaxUnlimitedHuffCodeSize + 1];

      void clear() { utils::zero_object(m_num_codes); }

      void init(uint num_syms, const uint8* pCodesizes);

      inline void init(uint code_size0, uint total_syms0, uint code_size1, uint total_syms1)
      {
         m_num_codes[code_size0] += total_syms0;
         m_num_codes[code_size1] += total_syms1;
      }
   };
   
   uint get_generate_huffman_codes_table_size();
   
   bool generate_huffman_codes(void* pContext, uint num_syms, const uint16* pFreq, uint8* pCodesizes, uint& max_code_size, uint& total_freq_ret, code_size_histogram &code_size_hist);

} // namespace lzham
