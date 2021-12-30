// File: lzham_mem.cpp
// See Copyright Notice and license at the end of include/lzham.h
#include "lzham_core.h"

#ifdef __APPLE__
   #include <malloc/malloc.h>
#elif defined(__FreeBSD__) || defined(__NetBSD__)
   #include <malloc_np.h>
   #if defined(__FreeBSD__)
      #define malloc(size) aligned_alloc((LZHAM_MIN_ALLOC_ALIGNMENT), (size))
   #endif
#else
   #include <malloc.h>
#endif

using namespace lzham;

// Purposely less than 2^15, the min dictionary size.
#define LZHAM_MAX_ARENA_ALLOC_SIZE 32767

#define LZHAM_MEM_STATS 0

#ifndef LZHAM_USE_WIN32_API
   #ifdef _MSC_VER
      // LZHAM currently needs _msize/malloc_size/malloc_usable_size to function, so it can't be 100% ANSI C++.
   #elif defined(__APPLE__)
      #define _msize malloc_size
   #else
      #define _msize malloc_usable_size
   #endif
#endif

namespace lzham
{
   #if LZHAM_64BIT_POINTERS
      const uint64 MAX_POSSIBLE_BLOCK_SIZE = 0x400000000ULL;
   #else
      const uint32 MAX_POSSIBLE_BLOCK_SIZE = 0x7FFF0000U;
   #endif

   class simple_spinlock
   {
      volatile atomic32_t m_lock;

   public:
      simple_spinlock() : m_lock(0) { }

      ~simple_spinlock() { }

      void lock()
      {
         for ( ; ; )
         {
            if (atomic_compare_exchange32(&m_lock, 1, 0) == 0)
               break;
         }

         LZHAM_MEMORY_IMPORT_BARRIER;
      }

      void unlock()
      {
         LZHAM_MEMORY_EXPORT_BARRIER;

         atomic_decrement32(&m_lock);
      }
   };
      
   static void* lzham_default_realloc(void* p, size_t size, size_t* pActual_size, lzham_bool movable, void* pUser_data)
   {
      LZHAM_NOTE_UNUSED(pUser_data);

      void* p_new;

      if (!p)
      {
         p_new = malloc(size);
         LZHAM_ASSERT( (reinterpret_cast<ptr_bits_t>(p_new) & (LZHAM_MIN_ALLOC_ALIGNMENT - 1)) == 0 );

         if (pActual_size)
            *pActual_size = p_new ? _msize(p_new) : 0;
      }
      else if (!size)
      {
         free(p);
         p_new = NULL;

         if (pActual_size)
            *pActual_size = 0;
      }
      else
      {
         void* p_final_block = p;
#ifdef WIN32
         p_new = _expand(p, size);
#else
         p_new = NULL;
#endif

         if (p_new)
         {
            LZHAM_ASSERT( (reinterpret_cast<ptr_bits_t>(p_new) & (LZHAM_MIN_ALLOC_ALIGNMENT - 1)) == 0 );
            p_final_block = p_new;
         }
         else if (movable)
         {
            p_new = realloc(p, size);

            if (p_new)
            {
               LZHAM_ASSERT( (reinterpret_cast<ptr_bits_t>(p_new) & (LZHAM_MIN_ALLOC_ALIGNMENT - 1)) == 0 );
               p_final_block = p_new;
            }
         }

         if (pActual_size)
            *pActual_size = _msize(p_final_block);
      }

      return p_new;
   }

   static size_t lzham_default_msize(void* p, void* pUser_data)
   {
      LZHAM_NOTE_UNUSED(pUser_data);
      return p ? _msize(p) : 0;
   }

   static lzham_realloc_func        g_pRealloc = lzham_default_realloc;
   static lzham_msize_func          g_pMSize   = lzham_default_msize;
   static void*                     g_pUser_data;

   void LZHAM_CDECL lzham_lib_set_memory_callbacks(lzham_realloc_func pRealloc, lzham_msize_func pMSize, void* pUser_data)
   {
      if ((!pRealloc) || (!pMSize))
      {
         g_pRealloc = lzham_default_realloc;
         g_pMSize = lzham_default_msize;
         g_pUser_data = NULL;
      }
      else
      {
         g_pRealloc = pRealloc;
         g_pMSize = pMSize;
         g_pUser_data = pUser_data;
      }
   }

   static inline void lzham_mem_error(lzham_malloc_context context, const char* p_msg)
   {
      LZHAM_NOTE_UNUSED(context);
      lzham_assert(p_msg, __FILE__, __LINE__);
      
      LZHAM_LOG_ERROR(2000);
   }

   struct malloc_context
   {
      enum { cSig = 0x5749ABCD };
      uint m_sig;

      uint m_arena_size;
      
      simple_spinlock m_lock;

      int64 m_total_blocks;
      int64 m_cur_allocated;
      int64 m_max_blocks;
      int64 m_max_allocated;

      uint m_arena_ofs;

      uint8 m_arena[1];

      void init(uint arena_size)
      {
         m_sig = cSig;

         m_arena_size = arena_size;
         m_arena_ofs = 0;
                  
         m_total_blocks = 0;
         m_cur_allocated = 0;
         m_max_blocks = 0;
         m_max_allocated = 0;

         if (arena_size)
         {
            uint alignment_mask = (LZHAM_MIN_ALLOC_ALIGNMENT - 1);
            m_arena_ofs = (LZHAM_MIN_ALLOC_ALIGNMENT - (reinterpret_cast<uint64>(m_arena) & alignment_mask)) & alignment_mask;
         }
      }

      void lock() { m_lock.lock(); }
      void unlock() { m_lock.unlock(); }

      bool ptr_is_in_arena(void *p) const
      {
         if ((p < m_arena) || (p >= (m_arena + m_arena_size)))
            return false;
         return true;
      }

      // Important: only the decompressor uses an arena, and it's only single threaded, so this DOES NOT take the context lock.
      void *arena_alloc(size_t size)
      {
         if ((!m_arena_size) || (size > LZHAM_MAX_ARENA_ALLOC_SIZE))
            return NULL;
         
         uint arena_remaining = m_arena_size - m_arena_ofs;
         
         size_t total_needed = (size + LZHAM_MIN_ALLOC_ALIGNMENT + (LZHAM_MIN_ALLOC_ALIGNMENT - 1)) & ~(LZHAM_MIN_ALLOC_ALIGNMENT - 1);
         if (arena_remaining < total_needed)
            return NULL;

         void *p = m_arena + (m_arena_ofs + LZHAM_MIN_ALLOC_ALIGNMENT);
         static_cast<uint32 *>(p)[-1] = static_cast<uint32>(size);
         
         m_arena_ofs += static_cast<uint>(total_needed);
         
         return p;
      }

      uint arena_msize(const void *p) const
      {
         return static_cast<const uint32 *>(p)[-1];
      }

#if LZHAM_MEM_STATS
      void update_total_allocated(int block_delta, int64 byte_delta)
      {
         lock();

         m_total_blocks += block_delta;
         m_cur_allocated += byte_delta;
         m_max_blocks = math::maximum(m_max_blocks, m_total_blocks);
         m_max_allocated = math::maximum(m_max_allocated, m_cur_allocated);

         unlock();
      }
#endif // LZHAM_MEM_STATS
   };

   lzham_malloc_context lzham_create_malloc_context(uint arena_size)
   {
      malloc_context *p = static_cast<malloc_context *>((*g_pRealloc)(NULL, (sizeof(malloc_context) - 1) + arena_size + LZHAM_MIN_ALLOC_ALIGNMENT, NULL, true, g_pUser_data));
      helpers::construct(p);
      p->init(arena_size);
      return p;
   }

   void lzham_destroy_malloc_context(lzham_malloc_context context)
   {
      LZHAM_ASSERT(context);
      if (context)
      {
         malloc_context *p = static_cast<malloc_context *>(context);
         LZHAM_VERIFY(p->m_sig == malloc_context::cSig);

         lzham_print_mem_stats(p);
                           
         helpers::destruct(p);
         p->m_sig = 0xDEADDEAD;

         (*g_pRealloc)(p, 0, NULL, true, g_pUser_data);
      }
   }
            
   void* lzham_malloc(lzham_malloc_context context, size_t size, size_t* pActual_size)
   {
      LZHAM_VERIFY(context);
      
      size = (size + sizeof(uint32) - 1U) & ~(sizeof(uint32) - 1U);
      if (!size)
         size = sizeof(uint32);

      if (size > MAX_POSSIBLE_BLOCK_SIZE)
      {
         lzham_mem_error(context, "lzham_malloc: size too big");
         return NULL;
      }

      size_t actual_size = size;
      uint8* p_new;

      malloc_context *pContext = static_cast<malloc_context *>(context);

      LZHAM_VERIFY(pContext->m_sig == malloc_context::cSig);

      p_new = static_cast<uint8 *>(pContext->arena_alloc(size));
      if (!p_new)
      {
         p_new = static_cast<uint8*>((*g_pRealloc)(NULL, size, &actual_size, true, g_pUser_data));
      }

      if (pActual_size)
         *pActual_size = actual_size;

      if ((!p_new) || (actual_size < size))
      {
         lzham_mem_error(context, "lzham_malloc: out of memory");
         return NULL;
      }

      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(p_new) & (LZHAM_MIN_ALLOC_ALIGNMENT - 1)) == 0);

#if LZHAM_MEM_STATS
      pContext->update_total_allocated(1, static_cast<int64>(actual_size));
#endif

      return p_new;
   }

   void* lzham_realloc(lzham_malloc_context context, void* p, size_t size, size_t* pActual_size, bool movable)
   {
      LZHAM_VERIFY(context);

      if ((ptr_bits_t)p & (LZHAM_MIN_ALLOC_ALIGNMENT - 1))
      {
         lzham_mem_error(context, "lzham_realloc: bad ptr");
         return NULL;
      }

      if (size > MAX_POSSIBLE_BLOCK_SIZE)
      {
         lzham_mem_error(context, "lzham_malloc: size too big");
         return NULL;
      }

#if LZHAM_MEM_STATS
      size_t cur_size = p ? (*g_pMSize)(p, g_pUser_data) : 0;
#endif

      size_t actual_size = size;
      void *p_new;
      
      malloc_context *pContext = static_cast<malloc_context *>(context);
      
      LZHAM_VERIFY(pContext->m_sig == malloc_context::cSig);

      if (pContext->ptr_is_in_arena(p))
      {
         if (!movable)
            return NULL;
         
         uint prev_size = pContext->arena_msize(p);
         if (size <= prev_size)
         {
            p_new = p;
         }
         else
         {
            p_new = static_cast<uint8 *>(pContext->arena_alloc(size));
            if (!p_new)
            {
               p_new = (*g_pRealloc)(NULL, size, &actual_size, true, g_pUser_data);
            }
          
            memcpy(p_new, p, prev_size);
         }
      }
      else
      {
         p_new = NULL;

         if (!p)
            p_new = static_cast<uint8 *>(pContext->arena_alloc(size));
         
         if (!p_new)
            p_new = (*g_pRealloc)(p, size, &actual_size, movable, g_pUser_data);
      }

      if (pActual_size)
         *pActual_size = actual_size;

      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(p_new) & (LZHAM_MIN_ALLOC_ALIGNMENT - 1)) == 0);

#if LZHAM_MEM_STATS
      int num_new_blocks = 0;
      if (p)
      {
         if (!p_new)
            num_new_blocks = -1;
      }
      else if (p_new)
      {
         num_new_blocks = 1;
      }
      pContext->update_total_allocated(num_new_blocks, static_cast<int64>(actual_size) - static_cast<int64>(cur_size));
#endif

      return p_new;
   }

   void lzham_free(lzham_malloc_context context, void* p)
   {
      if (!p)
         return;
      
      LZHAM_VERIFY(context);

      if (reinterpret_cast<ptr_bits_t>(p) & (LZHAM_MIN_ALLOC_ALIGNMENT - 1))
      {
         lzham_mem_error(context, "lzham_free: bad ptr");
         return;
      }
      
      malloc_context *pContext = static_cast<malloc_context *>(context);
      
      LZHAM_VERIFY(pContext->m_sig == malloc_context::cSig);

#if LZHAM_MEM_STATS
      size_t cur_size = lzham_msize(context, p);
      pContext->update_total_allocated(-1, -static_cast<int64>(cur_size));
#endif
      
      if (!pContext->ptr_is_in_arena(p))
      {
         (*g_pRealloc)(p, 0, NULL, true, g_pUser_data);
      }
   }

   size_t lzham_msize(lzham_malloc_context context, void* p)
   {
      if (!p)
         return 0;

      if (reinterpret_cast<ptr_bits_t>(p) & (LZHAM_MIN_ALLOC_ALIGNMENT - 1))
      {
         lzham_mem_error(context, "lzham_msize: bad ptr");
         return 0;
      }

      malloc_context *pContext = static_cast<malloc_context *>(context);
      LZHAM_VERIFY(pContext->m_sig == malloc_context::cSig);

      if (pContext->ptr_is_in_arena(p))
      {
         return pContext->arena_msize(p);
      }
      else
      {
         return (*g_pMSize)(p, g_pUser_data);
      }
   }
      
   void lzham_print_mem_stats(lzham_malloc_context context)
   {
      LZHAM_VERIFY(context);

#if LZHAM_MEM_STATS
      malloc_context *pContext = static_cast<malloc_context *>(context);
      
      LZHAM_VERIFY(pContext->m_sig == malloc_context::cSig);

      printf("Current blocks: %u, allocated: %" LZHAM_PRIu64 ", max ever allocated: %" LZHAM_PRIi64 "\n", pContext->m_total_blocks, (int64)pContext->m_cur_allocated, (int64)pContext->m_max_allocated);
      printf("Max used arena: %u\n", pContext->m_arena_ofs);
      printf("Max blocks: %" LZHAM_PRIu64 "\n", pContext->m_max_blocks);
#endif
   }

} // namespace lzham

