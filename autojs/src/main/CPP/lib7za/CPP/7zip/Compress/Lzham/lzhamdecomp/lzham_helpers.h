// File: lzham_helpers.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

#define LZHAM_NO_COPY_OR_ASSIGNMENT_OP(c) c(const c&); c& operator= (const c&);

namespace lzham
{
   namespace helpers
   {
      template<typename T> struct rel_ops
      {
         friend inline bool operator!=(const T& x, const T& y) { return (!(x == y)); }
         friend inline bool operator> (const T& x, const T& y) { return (y < x); }
         friend inline bool operator<=(const T& x, const T& y) { return (!(y < x)); }
         friend inline bool operator>=(const T& x, const T& y) { return (!(x < y)); }
      };

      template <typename T>
      inline T* construct(T* p)
      {
         return new (static_cast<void*>(p)) T;
      }

      template <typename T, typename U>
      inline T* construct(T* p, const U& init)
      {
         return new (static_cast<void*>(p)) T(init);
      }

      template <typename T> 
      inline void construct_array(T* p, uint n);
      
      template <typename T, typename U>
      inline void construct_array(T* p, uint n, const U& init)
      {
         T* q = p + n;
         for ( ; p != q; ++p)
            new (static_cast<void*>(p)) T(init);
      }

      template <typename T>
      inline void destruct(T* p)
      {
         LZHAM_NOTE_UNUSED(p);
         p->~T();
      }

      template <typename T> 
      inline void destruct_array(T* p, uint n);

   }  // namespace helpers

}  // namespace lzham
