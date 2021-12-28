// File: lzham_platform.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

bool lzham_is_debugger_present(void);
void lzham_debug_break(void);
void lzham_output_debug_string(const char* p);

// actually in lzham_assert.cpp
void lzham_assert(const char* pExp, const char* pFile, unsigned line);
void lzham_fail(const char* pExp, const char* pFile, unsigned line);

#ifdef WIN32
   #define LZHAM_BREAKPOINT DebuggerBreak();
   #define LZHAM_BUILTIN_EXPECT(c, v) c
#elif defined(__GNUC__)
   #define LZHAM_BREAKPOINT asm("int $3");
   #define LZHAM_BUILTIN_EXPECT(c, v) __builtin_expect(c, v)
#else
   #define LZHAM_BREAKPOINT
   #define LZHAM_BUILTIN_EXPECT(c, v) c
#endif

#if defined(__GNUC__) && LZHAM_PLATFORM_PC
extern __inline__ __attribute__((__always_inline__,__gnu_inline__)) void lzham_yield_processor()
{
   __asm__ __volatile__("pause");
}
#elif LZHAM_PLATFORM_X360
#define lzham_yield_processor() \
   YieldProcessor(); \
   __asm { or r0, r0, r0 } \
   YieldProcessor(); \
   __asm { or r1, r1, r1 } \
   YieldProcessor(); \
   __asm { or r0, r0, r0 } \
   YieldProcessor(); \
   __asm { or r1, r1, r1 } \
   YieldProcessor(); \
   __asm { or r0, r0, r0 } \
   YieldProcessor(); \
   __asm { or r1, r1, r1 } \
   YieldProcessor(); \
   __asm { or r0, r0, r0 } \
   YieldProcessor(); \
   __asm { or r1, r1, r1 }
#else
LZHAM_FORCE_INLINE void lzham_yield_processor()
{
#if LZHAM_USE_MSVC_INTRINSICS
   #if LZHAM_PLATFORM_PC_X64
      _mm_pause();
   #else
      YieldProcessor();
   #endif
#else
   // No implementation
#endif
}
#endif

#ifndef _MSC_VER
   int sprintf_s(char *buffer, size_t sizeOfBuffer, const char *format, ...);
   int vsprintf_s(char *buffer, size_t sizeOfBuffer, const char *format, va_list args);
#endif

#if LZHAM_PLATFORM_X360
   #define LZHAM_MEMORY_EXPORT_BARRIER MemoryBarrier();
#else
   // Barriers shouldn't be necessary on x86/x64.
   // TODO: Should use __sync_synchronize() on other platforms that support GCC.
   #define LZHAM_MEMORY_EXPORT_BARRIER
#endif

#if LZHAM_PLATFORM_X360
   #define LZHAM_MEMORY_IMPORT_BARRIER MemoryBarrier();
#else
   // Barriers shouldn't be necessary on x86/x64.
   // TODO: Should use __sync_synchronize() on other platforms that support GCC.
   #define LZHAM_MEMORY_IMPORT_BARRIER
#endif

// Note: It's very important that LZHAM_READ_BIG_ENDIAN_UINT32() is fast on the target platform.
// This is used to read every DWORD from the input stream.

#if LZHAM_USE_UNALIGNED_INT_LOADS
   #if LZHAM_BIG_ENDIAN_CPU
      #define LZHAM_READ_BIG_ENDIAN_UINT32(p) *reinterpret_cast<const uint32*>(p)
   #else
      #if defined(LZHAM_USE_MSVC_INTRINSICS)
         #define LZHAM_READ_BIG_ENDIAN_UINT32(p) _byteswap_ulong(*reinterpret_cast<const uint32*>(p))
      #elif defined(__GNUC__)
         #define LZHAM_READ_BIG_ENDIAN_UINT32(p) __builtin_bswap32(*reinterpret_cast<const uint32*>(p))
      #else
         #define LZHAM_READ_BIG_ENDIAN_UINT32(p) utils::swap32(*reinterpret_cast<const uint32*>(p))
      #endif
   #endif
#else
   #define LZHAM_READ_BIG_ENDIAN_UINT32(p) ((reinterpret_cast<const uint8*>(p)[0] << 24) | (reinterpret_cast<const uint8*>(p)[1] << 16) | (reinterpret_cast<const uint8*>(p)[2] << 8) | (reinterpret_cast<const uint8*>(p)[3]))
#endif

#if LZHAM_USE_WIN32_ATOMIC_FUNCTIONS
   extern "C" __int64 _InterlockedCompareExchange64(__int64 volatile * Destination, __int64 Exchange, __int64 Comperand);
   #if defined(_MSC_VER)
      #pragma intrinsic(_InterlockedCompareExchange64)
   #endif
#endif // LZHAM_USE_WIN32_ATOMIC_FUNCTIONS

namespace lzham
{
#if LZHAM_USE_WIN32_ATOMIC_FUNCTIONS
   typedef LONG atomic32_t;
   typedef LONGLONG atomic64_t;

   // Returns the original value.
   inline atomic32_t atomic_compare_exchange32(atomic32_t volatile *pDest, atomic32_t exchange, atomic32_t comparand)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return InterlockedCompareExchange(pDest, exchange, comparand);
   }

   // Returns the original value.
   inline atomic64_t atomic_compare_exchange64(atomic64_t volatile *pDest, atomic64_t exchange, atomic64_t comparand)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 7) == 0);
      return _InterlockedCompareExchange64(pDest, exchange, comparand);
   }

   // Returns the resulting incremented value.
   inline atomic32_t atomic_increment32(atomic32_t volatile *pDest)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return InterlockedIncrement(pDest);
   }

   // Returns the resulting decremented value.
   inline atomic32_t atomic_decrement32(atomic32_t volatile *pDest)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return InterlockedDecrement(pDest);
   }

   // Returns the original value.
   inline atomic32_t atomic_exchange32(atomic32_t volatile *pDest, atomic32_t val)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return InterlockedExchange(pDest, val);
   }

   // Returns the resulting value.
   inline atomic32_t atomic_add32(atomic32_t volatile *pDest, atomic32_t val)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return InterlockedExchangeAdd(pDest, val) + val;
   }

   // Returns the original value.
   inline atomic32_t atomic_exchange_add(atomic32_t volatile *pDest, atomic32_t val)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return InterlockedExchangeAdd(pDest, val);
   }
#elif LZHAM_USE_GCC_ATOMIC_BUILTINS
   typedef long atomic32_t;
   typedef long long atomic64_t;

   // Returns the original value.
   inline atomic32_t atomic_compare_exchange32(atomic32_t volatile *pDest, atomic32_t exchange, atomic32_t comparand)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return __sync_val_compare_and_swap(pDest, comparand, exchange);
   }

   // Returns the original value.
   inline atomic64_t atomic_compare_exchange64(atomic64_t volatile *pDest, atomic64_t exchange, atomic64_t comparand)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 7) == 0);
      return __sync_val_compare_and_swap(pDest, comparand, exchange);
   }

   // Returns the resulting incremented value.
   inline atomic32_t atomic_increment32(atomic32_t volatile *pDest)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return __sync_add_and_fetch(pDest, 1);
   }

   // Returns the resulting decremented value.
   inline atomic32_t atomic_decrement32(atomic32_t volatile *pDest)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return __sync_sub_and_fetch(pDest, 1);
   }

   // Returns the original value.
   inline atomic32_t atomic_exchange32(atomic32_t volatile *pDest, atomic32_t val)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return __sync_lock_test_and_set(pDest, val);
   }

   // Returns the resulting value.
   inline atomic32_t atomic_add32(atomic32_t volatile *pDest, atomic32_t val)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return __sync_add_and_fetch(pDest, val);
   }

   // Returns the original value.
   inline atomic32_t atomic_exchange_add(atomic32_t volatile *pDest, atomic32_t val)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return __sync_fetch_and_add(pDest, val);
   }
#else
   #define LZHAM_NO_ATOMICS 1

   // Atomic ops not supported - but try to do something reasonable. Assumes no threading at all.
   typedef long atomic32_t;
   typedef long long atomic64_t;

   inline atomic32_t atomic_compare_exchange32(atomic32_t volatile *pDest, atomic32_t exchange, atomic32_t comparand)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      atomic32_t cur = *pDest;
      if (cur == comparand)
         *pDest = exchange;
      return cur;
   }

   inline atomic64_t atomic_compare_exchange64(atomic64_t volatile *pDest, atomic64_t exchange, atomic64_t comparand)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 7) == 0);
      atomic64_t cur = *pDest;
      if (cur == comparand)
         *pDest = exchange;
      return cur;
   }

   inline atomic32_t atomic_increment32(atomic32_t volatile *pDest)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return (*pDest += 1);
   }

   inline atomic32_t atomic_decrement32(atomic32_t volatile *pDest)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return (*pDest -= 1);
   }

   inline atomic32_t atomic_exchange32(atomic32_t volatile *pDest, atomic32_t val)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      atomic32_t cur = *pDest;
      *pDest = val;
      return cur;
   }

   inline atomic32_t atomic_add32(atomic32_t volatile *pDest, atomic32_t val)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      return (*pDest += val);
   }

   inline atomic32_t atomic_exchange_add(atomic32_t volatile *pDest, atomic32_t val)
   {
      LZHAM_ASSERT((reinterpret_cast<ptr_bits_t>(pDest) & 3) == 0);
      atomic32_t cur = *pDest;
      *pDest += val;
      return cur;
   }

#endif

#if LZHAM_BUFFERED_PRINTF
   void lzham_buffered_printf(const char *format, ...);
   void lzham_flush_buffered_printf();
#else
   inline void lzham_buffered_printf(const char *format, ...) { (void)format; }
   inline void lzham_flush_buffered_printf() { }
#endif

} // namespace lzham
