// File: lzham_mem.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

namespace lzham
{
   typedef void *lzham_malloc_context;

   lzham_malloc_context lzham_create_malloc_context(uint arena_size);
   void lzham_destroy_malloc_context(lzham_malloc_context context);
   
   void*    lzham_malloc(lzham_malloc_context context, size_t size, size_t* pActual_size = NULL);
   void*    lzham_realloc(lzham_malloc_context context, void* p, size_t size, size_t* pActual_size = NULL, bool movable = true);
   void     lzham_free(lzham_malloc_context context, void* p);
   size_t   lzham_msize(lzham_malloc_context context, void* p);

   template<typename T>
   inline T* lzham_new(lzham_malloc_context context)
   {
      T* p = static_cast<T*>(lzham_malloc(context, sizeof(T)));
      if (!p) return NULL;
      if (LZHAM_IS_SCALAR_TYPE(T))
         return p;
      return helpers::construct(p);
   }
     
   template<typename T, typename A>
   inline T* lzham_new(lzham_malloc_context context, const A& init0)
   {
      T* p = static_cast<T*>(lzham_malloc(context, sizeof(T)));
      if (!p) return NULL;
      return new (static_cast<void*>(p)) T(init0); 
   }
   
   template<typename T, typename A, typename B>
   inline T* lzham_new(lzham_malloc_context context, const A& init0, const B& init1)
   {
      T* p = static_cast<T*>(lzham_malloc(context, sizeof(T)));
      if (!p) return NULL;
      return new (static_cast<void*>(p)) T(init0, init1); 
   }
   
   template<typename T, typename A, typename B, typename C>
   inline T* lzham_new(lzham_malloc_context context, const A& init0, const B& init1, const C& init2)
   {
      T* p = static_cast<T*>(lzham_malloc(context, sizeof(T)));
      if (!p) return NULL;
      return new (static_cast<void*>(p)) T(init0, init1, init2); 
   }
   
   template<typename T, typename A, typename B, typename C, typename D>
   inline T* lzham_new(lzham_malloc_context context, const A& init0, const B& init1, const C& init2, const D& init3)
   {
      T* p = static_cast<T*>(lzham_malloc(context, sizeof(T)));
      if (!p) return NULL;
      return new (static_cast<void*>(p)) T(init0, init1, init2, init3); 
   }

   template<typename T>
   inline T* lzham_new_array(lzham_malloc_context context, uint32 num)
   {
      if (!num) num = 1;

      uint8* q = static_cast<uint8*>(lzham_malloc(context, LZHAM_MIN_ALLOC_ALIGNMENT + sizeof(T) * num));
      if (!q)
         return NULL;

      T* p = reinterpret_cast<T*>(q + LZHAM_MIN_ALLOC_ALIGNMENT);

      reinterpret_cast<uint32*>(p)[-1] = num;
      reinterpret_cast<uint32*>(p)[-2] = ~num;

      if (!LZHAM_IS_SCALAR_TYPE(T))
      {
         helpers::construct_array(p, num);
      }
      return p;
   }

   template<typename T> 
   inline void lzham_delete(lzham_malloc_context context, T* p)
   {
      if (p) 
      {
         if (!LZHAM_IS_SCALAR_TYPE(T))
         {
            helpers::destruct(p);
         }
         lzham_free(context, p);
      }         
   }

   template<typename T> 
   inline void lzham_delete_array(lzham_malloc_context context, T* p)
   {
      if (p)
      {
         const uint32 num = reinterpret_cast<uint32*>(p)[-1];
         const uint32 num_check = reinterpret_cast<uint32*>(p)[-2];
         LZHAM_ASSERT(num && (num == ~num_check));
         if (num == ~num_check)
         {
            if (!LZHAM_IS_SCALAR_TYPE(T))
            {
               helpers::destruct_array(p, num);
            }

            lzham_free(context, reinterpret_cast<uint8*>(p) - LZHAM_MIN_ALLOC_ALIGNMENT);
         }
      }
   }   
   
   void lzham_print_mem_stats(lzham_malloc_context context);

} // namespace lzham
