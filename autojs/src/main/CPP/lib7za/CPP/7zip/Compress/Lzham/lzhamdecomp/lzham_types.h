// File: types.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

namespace lzham
{
   typedef unsigned char      uint8;
   typedef signed char        int8;
   typedef unsigned char      uint8;
   typedef unsigned short     uint16;
   typedef signed short       int16;
   typedef unsigned int       uint32;
   typedef uint32             uint;
   typedef signed int         int32;

   #ifdef _MSC_VER
      typedef unsigned __int64      uint64;
      typedef signed __int64        int64;
   #else
      typedef unsigned long long    uint64;
      typedef long long             int64;
   #endif

   const uint8  cUINT8_MIN  = 0;
   const uint8  cUINT8_MAX  = 0xFFU;
   const uint16 cUINT16_MIN = 0;
   const uint16 cUINT16_MAX = 0xFFFFU;
   const uint32 cUINT32_MIN = 0;
   const uint32 cUINT32_MAX = 0xFFFFFFFFU;
   const uint64 cUINT64_MIN = 0;
   const uint64 cUINT64_MAX = 0xFFFFFFFFFFFFFFFFULL;    //0xFFFFFFFFFFFFFFFFui64;

   const int8  cINT8_MIN  = -128;
   const int8  cINT8_MAX  = 127;
   const int16 cINT16_MIN = -32768;
   const int16 cINT16_MAX = 32767;
   const int32 cINT32_MIN = (-2147483647 - 1);
   const int32 cINT32_MAX = 2147483647;
   const int64 cINT64_MIN = (int64)0x8000000000000000ULL; //(-9223372036854775807i64 - 1);
   const int64 cINT64_MAX = (int64)0x7FFFFFFFFFFFFFFFULL; //9223372036854775807i64;

#if LZHAM_64BIT_POINTERS
   typedef uint64 uint_ptr;
   typedef uint64 uint32_ptr;
   typedef int64 signed_size_t;
   typedef uint64 ptr_bits_t;
#else
   typedef unsigned int uint_ptr;
   typedef unsigned int uint32_ptr;
   typedef signed int signed_size_t;
   typedef uint32 ptr_bits_t;
#endif
   
   enum
   {
      cInvalidIndex = -1
   };

   const uint cIntBits = sizeof(uint) * CHAR_BIT;

   template<typename T> struct int_traits { enum { cMin = INT_MIN, cMax = INT_MAX, cSigned = true }; };
   template<> struct int_traits<int8> { enum { cMin = cINT8_MIN, cMax = cINT8_MAX, cSigned = true }; };
   template<> struct int_traits<int16> { enum { cMin = cINT16_MIN, cMax = cINT16_MAX, cSigned = true }; };
   template<> struct int_traits<int32> { enum { cMin = cINT32_MIN, cMax = cINT32_MAX, cSigned = true }; };

   template<> struct int_traits<uint> { enum { cMin = 0, cMax = UINT_MAX, cSigned = false }; };
   template<> struct int_traits<uint8> { enum { cMin = 0, cMax = cUINT8_MAX, cSigned = false }; };
   template<> struct int_traits<uint16> { enum { cMin = 0, cMax = cUINT16_MAX, cSigned = false }; };

   struct empty_type { };

} // namespace lzham
