// File: lzham_prefix_coding.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once
#include "lzham_huffman_codes.h"

namespace lzham
{
   namespace prefix_coding
   {
      const uint cMaxSupportedSyms = 1024;
      
      // This value can be tuned for a specific CPU.
      const uint cMaxTableBits = 11;

      bool limit_max_code_size(uint num_syms, uint8* pCodesizes, uint max_code_size);

      bool generate_codes(uint num_syms, const uint8* pCodesizes, uint16* pCodes);
            
      class decoder_tables
      {
      public:
         inline decoder_tables(lzham_malloc_context malloc_context) :
            m_malloc_context(malloc_context),
            m_table_shift(0), m_table_max_code(0), m_decode_start_code_size(0), m_cur_lookup_size(0), m_lookup(NULL), m_cur_sorted_symbol_order_size(0), m_sorted_symbol_order(NULL)
         {
         }

         inline decoder_tables(const decoder_tables& other) :
            m_malloc_context(other.m_malloc_context),
            m_table_shift(0), m_table_max_code(0), m_decode_start_code_size(0), m_cur_lookup_size(0), m_lookup(NULL), m_cur_sorted_symbol_order_size(0), m_sorted_symbol_order(NULL)
         {
            *this = other;
         }

         inline decoder_tables& operator= (const decoder_tables& rhs)
         {
            assign(rhs);
            return *this;
         }

         inline bool assign(const decoder_tables& rhs)
         {
            if (this == &rhs)
               return true;

            if (m_malloc_context != rhs.m_malloc_context)
            {
               clear();

               m_malloc_context = rhs.m_malloc_context;
            }

            uint32* pCur_lookup = m_lookup;
            uint16* pCur_sorted_symbol_order = m_sorted_symbol_order;

            memcpy(this, &rhs, sizeof(*this));

            if ((pCur_lookup) && (pCur_sorted_symbol_order) && (rhs.m_cur_lookup_size == m_cur_lookup_size) && (rhs.m_cur_sorted_symbol_order_size == m_cur_sorted_symbol_order_size))
            {
               m_lookup = pCur_lookup;
               m_sorted_symbol_order = pCur_sorted_symbol_order;

               memcpy(m_lookup, rhs.m_lookup, sizeof(m_lookup[0]) * m_cur_lookup_size);
               memcpy(m_sorted_symbol_order, rhs.m_sorted_symbol_order, sizeof(m_sorted_symbol_order[0]) * m_cur_sorted_symbol_order_size);
            }
            else
            {
               lzham_delete_array(m_malloc_context, pCur_lookup);
               m_lookup = NULL;

               if (rhs.m_lookup)
               {
                  m_lookup = lzham_new_array<uint32>(m_malloc_context, m_cur_lookup_size);
                  if (!m_lookup)
                     return false;
                  memcpy(m_lookup, rhs.m_lookup, sizeof(m_lookup[0]) * m_cur_lookup_size);
               }

               lzham_delete_array(m_malloc_context, pCur_sorted_symbol_order);
               m_sorted_symbol_order = NULL;

               if (rhs.m_sorted_symbol_order)
               {
                  m_sorted_symbol_order = lzham_new_array<uint16>(m_malloc_context, m_cur_sorted_symbol_order_size);
                  if (!m_sorted_symbol_order)
                     return false;
                  memcpy(m_sorted_symbol_order, rhs.m_sorted_symbol_order, sizeof(m_sorted_symbol_order[0]) * m_cur_sorted_symbol_order_size);
               }
            }

            return true;
         }
         
         inline void clear()
         {
            if (m_lookup)
            {
               lzham_delete_array(m_malloc_context, m_lookup);
               m_lookup = 0;
               m_cur_lookup_size = 0;
            }

            if (m_sorted_symbol_order)
            {
               lzham_delete_array(m_malloc_context, m_sorted_symbol_order);
               m_sorted_symbol_order = NULL;
               m_cur_sorted_symbol_order_size = 0;
            }
         }

         inline ~decoder_tables()
         {
            if (m_lookup)
               lzham_delete_array(m_malloc_context, m_lookup);

            if (m_sorted_symbol_order)
               lzham_delete_array(m_malloc_context, m_sorted_symbol_order);
         }

         // DO NOT use any complex classes here - it is bitwise copied.
         
         lzham_malloc_context m_malloc_context;

         uint                 m_num_syms;
         uint                 m_total_used_syms;
         uint                 m_table_bits;
         uint                 m_table_shift;
         uint                 m_table_max_code;
         uint                 m_decode_start_code_size;

         uint8                m_min_code_size;
         uint8                m_max_code_size;

         uint                 m_max_codes[cMaxExpectedHuffCodeSize + 1];
         int                  m_val_ptrs[cMaxExpectedHuffCodeSize + 1];

         uint                 m_cur_lookup_size;
         uint32*              m_lookup;

         uint                 m_cur_sorted_symbol_order_size;
         uint16*              m_sorted_symbol_order;

         inline uint get_unshifted_max_code(uint len) const
         {
            LZHAM_ASSERT( (len >= 1) && (len <= cMaxExpectedHuffCodeSize) );
            uint k = m_max_codes[len - 1];
            if (!k)
               return UINT_MAX;
            return (k - 1) >> (16 - len);
         }
      };

      bool generate_decoder_tables(uint num_syms, const uint8* pCodesizes, decoder_tables* pTables, uint table_bits, const code_size_histogram &code_size_histo, bool sym_freq_all_ones);

   } // namespace prefix_coding

} // namespace lzham
