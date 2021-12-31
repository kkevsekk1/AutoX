// File: lzham_assert.h
// See Copyright Notice and license at the end of include/lzham.h
#pragma once

#if LZHAM_ERROR_LOGGING
   #if LZHAM_VERBOSE_ERROR_LOGGING
      #define LZHAM_LOG_ERROR(idx) do { lzham_log_error(__FUNCTION__, __FILE__, __LINE__, "", idx); } while(0)
   #else
      #define LZHAM_LOG_ERROR(idx) do { lzham_log_error(idx); } while(0)
   #endif
#else
   #define LZHAM_LOG_ERROR(idx)
#endif

#if LZHAM_ERROR_LOGGING
   #if LZHAM_VERBOSE_ERROR_LOGGING
      void lzham_log_error(const char *pFunc, const char *pFile, int line, const char *pMsg, int idx);
   #else
      void lzham_log_error(int idx);
   #endif
#endif

const unsigned int LZHAM_FAIL_EXCEPTION_CODE = 256U;
void lzham_enable_fail_exceptions(bool enabled);

void lzham_assert(const char* pExp, const char* pFile, unsigned line);
void lzham_fail(const char* pExp, const char* pFile, unsigned line);

#ifdef NDEBUG
   #define LZHAM_ASSERT(x) ((void)0)
#else
   #define LZHAM_ASSERT(_exp) (void)( (!!(_exp)) || (lzham_assert(#_exp, __FILE__, __LINE__), 0) )
   #define LZHAM_ASSERTS_ENABLED 1
#endif

#define LZHAM_VERIFY(_exp) (void)( (!!(_exp)) || (lzham_assert(#_exp, __FILE__, __LINE__), 0) )

#define LZHAM_FAIL(msg) do { lzham_fail(#msg, __FILE__, __LINE__); } while(0)

#define LZHAM_ASSERT_OPEN_RANGE(x, l, h) LZHAM_ASSERT((x >= l) && (x < h))
#define LZHAM_ASSERT_CLOSED_RANGE(x, l, h) LZHAM_ASSERT((x >= l) && (x <= h))

void lzham_trace(const char* pFmt, va_list args);
void lzham_trace(const char* pFmt, ...);

// Borrowed from boost libraries.
template <bool x>  struct assume_failure;
template <> struct assume_failure<true> { enum { blah = 1 }; };
template<int x> struct assume_try { };

#define LZHAM_JOINER_FINAL(a, b) a##b
#define LZHAM_JOINER(a, b) LZHAM_JOINER_FINAL(a, b)
#define LZHAM_JOIN(a, b) LZHAM_JOINER(a, b)
#if defined(__GNUC__)
   #define LZHAM_ASSUME(p) typedef assume_try < sizeof(assume_failure< (bool)(p) > ) > LZHAM_JOIN(assume_typedef, __COUNTER__) __attribute__((unused))
#else
   #define LZHAM_ASSUME(p) typedef assume_try < sizeof(assume_failure< (bool)(p) > ) > LZHAM_JOIN(assume_typedef, __COUNTER__)
#endif
