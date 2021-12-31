// File: platform.cpp
// See Copyright Notice and license at the end of include/lzham.h
#include "lzham_core.h"
#include "lzham_timer.h"
#include <assert.h>

#if LZHAM_PLATFORM_X360
#include <xbdm.h>
#endif

#define LZHAM_FORCE_DEBUGGER_PRESENT 0

#ifndef _MSC_VER
int sprintf_s(char *buffer, size_t sizeOfBuffer, const char *format, ...)
{
   if (!sizeOfBuffer)
      return 0;

   va_list args;
   va_start(args, format);
   int c = vsnprintf(buffer, sizeOfBuffer, format, args);
   va_end(args);

   buffer[sizeOfBuffer - 1] = '\0';

   if (c < 0)
      return static_cast<int>(sizeOfBuffer - 1);

   return LZHAM_MIN(c, (int)sizeOfBuffer - 1);
}
int vsprintf_s(char *buffer, size_t sizeOfBuffer, const char *format, va_list args)
{
   if (!sizeOfBuffer)
      return 0;

   int c = vsnprintf(buffer, sizeOfBuffer, format, args);

   buffer[sizeOfBuffer - 1] = '\0';

   if (c < 0)
      return static_cast<int>(sizeOfBuffer - 1);

   return LZHAM_MIN(c, (int)sizeOfBuffer - 1);
}
#endif // __GNUC__

bool lzham_is_debugger_present(void)
{
#if LZHAM_PLATFORM_X360
   return DmIsDebuggerPresent() != 0;
#elif LZHAM_USE_WIN32_API
   return IsDebuggerPresent() != 0;
#elif LZHAM_FORCE_DEBUGGER_PRESENT
   return true;
#else
   return false;
#endif
}

void lzham_debug_break(void)
{
#if LZHAM_USE_WIN32_API
   DebugBreak();
#elif (TARGET_OS_MAC == 1) && (TARGET_IPHONE_SIMULATOR == 0) && (TARGET_OS_IPHONE == 0)
//   __asm {int 3}
//   __asm("int $3")
   assert(0);   
#else
   assert(0);
#endif
}

void lzham_output_debug_string(const char* p)
{
   LZHAM_NOTE_UNUSED(p);
#if LZHAM_USE_WIN32_API
   OutputDebugStringA(p);
#else
   fputs(p, stderr);
#endif
}

#if LZHAM_BUFFERED_PRINTF
#include <vector>
// This stuff was a quick hack only intended for debugging/development.
namespace lzham
{
   struct buffered_str
   {
      enum { cBufSize = 256 };
      char m_buf[cBufSize];
   };

   static std::vector<buffered_str> g_buffered_strings;
   static volatile long g_buffered_string_locked;
   
   static void lock_buffered_strings()
   {
      while (atomic_exchange32(&g_buffered_string_locked, 1) == 1)
      {
         lzham_yield_processor();
         lzham_yield_processor();
         lzham_yield_processor();
         lzham_yield_processor();
      }

      LZHAM_MEMORY_IMPORT_BARRIER
   }
   
   static void unlock_buffered_strings()
   {
      LZHAM_MEMORY_EXPORT_BARRIER

      atomic_exchange32(&g_buffered_string_locked, 0);
   }

   void lzham_buffered_printf(const char *format, ...)
   {
      format;
   
      char buf[lzham::buffered_str::cBufSize];
   
      va_list args;
      va_start(args, format);
      vsnprintf_s(buf, sizeof(buf), sizeof(buf), format, args);
      va_end(args);   

      buf[sizeof(buf) - 1] = '\0';
   
      lzham::lock_buffered_strings();
   
      if (!lzham::g_buffered_strings.capacity())
      {
         lzham::g_buffered_strings.reserve(2048);
      }

      lzham::g_buffered_strings.resize(lzham::g_buffered_strings.size() + 1);
      memcpy(lzham::g_buffered_strings.back().m_buf, buf, sizeof(buf));

      lzham::unlock_buffered_strings();
   }

   void lzham_flush_buffered_printf()
   {
      lzham::lock_buffered_strings();

      for (lzham::uint i = 0; i < lzham::g_buffered_strings.size(); i++)
      {
         printf("%s", lzham::g_buffered_strings[i].m_buf);
      }

      lzham::g_buffered_strings.resize(0);

      lzham::unlock_buffered_strings();
   }

} // namespace lzham
#endif   

