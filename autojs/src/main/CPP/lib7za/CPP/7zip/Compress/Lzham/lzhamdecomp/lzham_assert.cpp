// File: lzham_assert.cpp
// See Copyright Notice and license at the end of include/lzham.h
#include "lzham_core.h"

static bool g_fail_exceptions;
static bool g_exit_on_failure = true;

void lzham_enable_fail_exceptions(bool enabled)
{
   g_fail_exceptions = enabled;
}

void lzham_assert(const char* pExp, const char* pFile, unsigned line)
{
   char buf[512];

   sprintf_s(buf, sizeof(buf), "%s(%u): Assertion failed: \"%s\"\n", pFile, line, pExp);

   lzham_output_debug_string(buf);

   printf("%s", buf);

   if (lzham_is_debugger_present())
      lzham_debug_break();
}

void lzham_fail(const char* pExp, const char* pFile, unsigned line)
{
   char buf[512];

   sprintf_s(buf, sizeof(buf), "%s(%u): Failure: \"%s\"\n", pFile, line, pExp);

   lzham_output_debug_string(buf);

   printf("%s", buf);

   if (lzham_is_debugger_present())
      lzham_debug_break();

#if LZHAM_USE_WIN32_API
   if (g_fail_exceptions)
      RaiseException(LZHAM_FAIL_EXCEPTION_CODE, 0, 0, NULL);
   else
#endif
   if (g_exit_on_failure)
      exit(EXIT_FAILURE);
}

void lzham_trace(const char* pFmt, va_list args)
{
   if (lzham_is_debugger_present())
   {
      char buf[512];
      vsprintf_s(buf, sizeof(buf), pFmt, args);

      lzham_output_debug_string(buf);
   }
}

void lzham_trace(const char* pFmt, ...)
{
   va_list args;
   va_start(args, pFmt);
   lzham_trace(pFmt, args);
   va_end(args);
}

#if LZHAM_ERROR_LOGGING
#if LZHAM_VERBOSE_ERROR_LOGGING
void lzham_log_error(const char *pFunc, const char *pFile, int line, const char *pMsg, int idx)
{
   fprintf(stderr, "\nlzham_log_error: %i %s file: %s line: %u func %s\n", idx, pMsg ? pMsg : "", pFile, line, pFunc);
}
#else
void lzham_log_error(int idx)
{
   fprintf(stderr, "\nlzham_log_error: %i\n", idx);
}
#endif
#endif
