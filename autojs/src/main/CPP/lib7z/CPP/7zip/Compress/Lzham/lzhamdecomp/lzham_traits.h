// File: lzham_traits.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

namespace lzham
{
   template<typename T>
   struct scalar_type
   {
      enum { cFlag = false };
      static inline void construct(T* p) { helpers::construct(p); }
      static inline void construct(T* p, const T& init) { helpers::construct(p, init); }
      static inline void construct_array(T* p, uint n) { helpers::construct_array(p, n); }
      static inline void destruct(T* p) { helpers::destruct(p); }
      static inline void destruct_array(T* p, uint n) { helpers::destruct_array(p, n); }
   };

   template<typename T> struct scalar_type<T*>
   {
      enum { cFlag = true };
      static inline void construct(T** p) { memset(p, 0, sizeof(T*)); }
      static inline void construct(T** p, T* init) { *p = init; }
      static inline void construct_array(T** p, uint n) { memset(p, 0, sizeof(T*) * n); }
      static inline void destruct(T** p) { LZHAM_NOTE_UNUSED(p); }
      static inline void destruct_array(T** p, uint n) { LZHAM_NOTE_UNUSED(p); LZHAM_NOTE_UNUSED(n); }
   };

#define LZHAM_DEFINE_BUILT_IN_TYPE(X) \
   template<> struct scalar_type<X> { \
   enum { cFlag = true }; \
   static inline void construct(X* p) { memset(p, 0, sizeof(X)); } \
   static inline void construct(X* p, const X& init) { memcpy(p, &init, sizeof(X)); } \
   static inline void construct_array(X* p, uint n) { memset(p, 0, sizeof(X) * n); } \
   static inline void destruct(X* p) { LZHAM_NOTE_UNUSED(p); } \
   static inline void destruct_array(X* p, uint n) { LZHAM_NOTE_UNUSED(p); LZHAM_NOTE_UNUSED(n); } };

   LZHAM_DEFINE_BUILT_IN_TYPE(bool)
   LZHAM_DEFINE_BUILT_IN_TYPE(char)
   LZHAM_DEFINE_BUILT_IN_TYPE(unsigned char)
   LZHAM_DEFINE_BUILT_IN_TYPE(short)
   LZHAM_DEFINE_BUILT_IN_TYPE(unsigned short)
   LZHAM_DEFINE_BUILT_IN_TYPE(int)
   LZHAM_DEFINE_BUILT_IN_TYPE(unsigned int)
   LZHAM_DEFINE_BUILT_IN_TYPE(long)
   LZHAM_DEFINE_BUILT_IN_TYPE(unsigned long)
   LZHAM_DEFINE_BUILT_IN_TYPE(float)
   LZHAM_DEFINE_BUILT_IN_TYPE(double)
   LZHAM_DEFINE_BUILT_IN_TYPE(long double)
   #if defined(WIN32)
      LZHAM_DEFINE_BUILT_IN_TYPE(__int64)
      LZHAM_DEFINE_BUILT_IN_TYPE(unsigned __int64)
   #endif

#undef LZHAM_DEFINE_BUILT_IN_TYPE

// See: http://erdani.org/publications/cuj-2004-06.pdf

   template<typename T>
   struct bitwise_movable { enum { cFlag = false }; };

// Defines type Q as bitwise movable.
#define LZHAM_DEFINE_BITWISE_MOVABLE(Q) template<> struct bitwise_movable<Q> { enum { cFlag = true }; };

   template<typename T>
   struct bitwise_copyable { enum { cFlag = false }; };

   // Defines type Q as bitwise copyable.
#define LZHAM_DEFINE_BITWISE_COPYABLE(Q) template<> struct bitwise_copyable<Q> { enum { cFlag = true }; };

#if (defined(__APPLE__) && (TARGET_OS_MAC != 1)) || defined(__NetBSD__)
   #define LZHAM_IS_POD(T) std::is_pod<T>::value
#else
   #define LZHAM_IS_POD(T) __is_pod(T)
#endif

#define LZHAM_IS_SCALAR_TYPE(T) (scalar_type<T>::cFlag)

#define LZHAM_IS_BITWISE_COPYABLE(T) ((scalar_type<T>::cFlag) || (bitwise_copyable<T>::cFlag) || LZHAM_IS_POD(T))

#define LZHAM_IS_BITWISE_MOVABLE(T) (LZHAM_IS_BITWISE_COPYABLE(T) || (bitwise_movable<T>::cFlag))

#define LZHAM_HAS_DESTRUCTOR(T) ((!scalar_type<T>::cFlag) && (!LZHAM_IS_POD(T)))

   // From yasli_traits.h:
   // Credit goes to Boost;
   // also found in the C++ Templates book by Vandevoorde and Josuttis

   typedef char (&yes_t)[1];
   typedef char (&no_t)[2];

   template <class U> yes_t class_test(int U::*);
   template <class U> no_t class_test(...);

   template <class T> struct is_class
   {
      enum { value = (sizeof(class_test<T>(0)) == sizeof(yes_t)) };
   };

   template <typename T> struct is_pointer
   {
      enum { value = false };
   };

   template <typename T> struct is_pointer<T*>
   {
      enum { value = true };
   };

   LZHAM_DEFINE_BITWISE_COPYABLE(empty_type);
   LZHAM_DEFINE_BITWISE_MOVABLE(empty_type);

   namespace helpers
   {
      template <typename T>
      inline void construct_array(T* p, uint n)
      {
         if (LZHAM_IS_SCALAR_TYPE(T))
         {
            memset(p, 0, sizeof(T) * n);
         }
         else
         {
            T* q = p + n;
            for ( ; p != q; ++p)
               new (static_cast<void*>(p)) T;
         }
      }

      template <typename T>
      inline void destruct_array(T* p, uint n)
      {
         if ( LZHAM_HAS_DESTRUCTOR(T) )
         {
            T* q = p + n;
            for ( ; p != q; ++p)
               p->~T();
         }
      }
   }

} // namespace lzham
