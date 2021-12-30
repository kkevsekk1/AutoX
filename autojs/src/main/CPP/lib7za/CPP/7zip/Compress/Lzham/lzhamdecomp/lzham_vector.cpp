// File: lzham_vector.cpp
// See Copyright Notice and license at the end of include/lzham.h
#include "lzham_core.h"
#include "lzham_vector.h"

namespace lzham
{
   bool elemental_vector::increase_capacity(uint min_new_capacity, bool grow_hint, uint element_size, object_mover pMover, bool nofail)
   {
      LZHAM_ASSERT(m_size <= m_capacity);
      
#if LZHAM_64BIT_POINTERS
      LZHAM_ASSUME(sizeof(void*) == sizeof(uint64));
      LZHAM_ASSERT(min_new_capacity < (0x400000000ULL / element_size));
#else      
      LZHAM_ASSUME(sizeof(void*) == sizeof(uint32));
      LZHAM_ASSERT(min_new_capacity < (0x7FFF0000U / element_size));
#endif      

      if (m_capacity >= min_new_capacity)
         return true;

      // new_capacity must be 64-bit when compiling on x64.
		size_t new_capacity = (size_t)min_new_capacity;
      if ((grow_hint) && (!math::is_power_of_2(static_cast<uint64>(new_capacity))))
         new_capacity = static_cast<uint>(math::next_pow2(static_cast<uint64>(new_capacity)));

      LZHAM_ASSERT(new_capacity && (new_capacity > m_capacity));

      const size_t desired_size = element_size * new_capacity;
      size_t actual_size;
      if (!pMover)
      {
         void* new_p = lzham_realloc(m_malloc_context, m_p, desired_size, &actual_size, true);
         if (!new_p)
         {
            if (nofail)
            {
               LZHAM_LOG_ERROR(5000);
               return false;
            }
               
            char buf[256];
            sprintf_s(buf, sizeof(buf), "vector: lzham_realloc() failed allocating %u bytes", desired_size);
            LZHAM_FAIL(buf);
         }
         m_p = new_p;
      }
      else
      {
         void* new_p = lzham_malloc(m_malloc_context, desired_size, &actual_size);
         if (!new_p)
         {
            if (nofail)
            {
               LZHAM_LOG_ERROR(5001);
               return false;
            }
               
            LZHAM_LOG_ERROR(5002);

            char buf[256];
            sprintf_s(buf, sizeof(buf), "vector: lzham_malloc() failed allocating %u bytes", desired_size);
            LZHAM_FAIL(buf);
         }
         
         (*pMover)(new_p, m_p, m_size);
         
         if (m_p)
            lzham_free(m_malloc_context, m_p);

         m_p = new_p;
      }            
      
      if (actual_size > desired_size)
         m_capacity = static_cast<uint>(actual_size / element_size);
      else
         m_capacity = static_cast<uint>(new_capacity);
    
      return true;
   }

} // namespace lzham
