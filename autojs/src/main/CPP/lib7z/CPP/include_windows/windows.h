/*
	windows.h - main header file for the Win32 API

	Written by Anders Norlander <anorland@hem2.passagen.se>

	This file is part of a free library for the Win32 API.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

*/
#ifndef _WINDOWS_H
#define _WINDOWS_H

#include <stdarg.h>

/* BEGIN #include <windef.h> */

#include "Common/MyWindows.h" // FIXED

#ifndef CONST
#define CONST const
#endif

#undef MAX_PATH
#define MAX_PATH 4096  /* Linux : 4096  - Windows : 260 */

#ifndef FALSE
#define FALSE 0
#endif
#ifndef TRUE
#define TRUE 1
#endif

#define WINAPI 

#undef BOOL
typedef int BOOL;


#define CREATE_NEW	  1
#define CREATE_ALWAYS	  2
#define OPEN_EXISTING	  3
#define OPEN_ALWAYS	  4
/* #define TRUNCATE_EXISTING 5 */



/* BEGIN #include <winnt.h> */
/* BEGIN <winerror.h> */
#define NO_ERROR                    0L
#define ERROR_ALREADY_EXISTS        EEXIST
#define ERROR_FILE_EXISTS           EEXIST
#define ERROR_INVALID_HANDLE        EBADF
#define ERROR_PATH_NOT_FOUND        ENOENT
#define ERROR_DISK_FULL             ENOSPC
#define ERROR_NO_MORE_FILES         0x100018 // FIXME
#define ERROR_DIRECTORY             267 // FIXME

// #define ERROR_NEGATIVE_SEEK         0x100131 // FIXME


/* see Common/WyWindows.h
#define S_OK ((HRESULT)0x00000000L)
#define S_FALSE ((HRESULT)0x00000001L)
#define E_INVALIDARG ((HRESULT)0x80070057L)
#define E_NOTIMPL ((HRESULT)0x80004001L)
#define E_NOINTERFACE ((HRESULT)0x80004002L)
#define E_ABORT ((HRESULT)0x80004004L)
#define E_FAIL ((HRESULT)0x80004005L)
#define E_OUTOFMEMORY ((HRESULT)0x8007000EL)
#define STG_E_INVALIDFUNCTION ((HRESULT)0x80030001L)
#define SUCCEEDED(Status) ((HRESULT)(Status) >= 0)
#define FAILED(Status) ((HRESULT)(Status)<0)
*/
#ifndef VOID
#define VOID void
#endif
typedef void *PVOID,*LPVOID;
typedef WCHAR *LPWSTR;
typedef CHAR *LPSTR;
typedef TCHAR *LPTSTR;

#ifdef UNICODE
/*
 * P7ZIP_TEXT is a private macro whose specific use is to force the expansion of a
 * macro passed as an argument to the macro TEXT.  DO NOT use this
 * macro within your programs.  It's name and function could change without
 * notice.
 */
#define P7ZIP_TEXT(q) L##q
#else
#define P7ZIP_TEXT(q) q
#endif
/*
 * UNICODE a constant string when UNICODE is defined, else returns the string
 * unmodified.
 * The corresponding macros  _TEXT() and _T() for mapping _UNICODE strings
 * passed to C runtime functions are defined in mingw/tchar.h
 */
#define TEXT(q) P7ZIP_TEXT(q)    

typedef BYTE BOOLEAN;

/* BEGIN #include <basetsd.h> */
#ifndef __int64
#define __int64 long long
#endif
typedef unsigned __int64 UINT64;
typedef __int64 INT64;
/* END #include <basetsd.h> */

#define FILE_ATTRIBUTE_READONLY             1
#define FILE_ATTRIBUTE_HIDDEN               2
#define FILE_ATTRIBUTE_SYSTEM               4
#define FILE_ATTRIBUTE_DIRECTORY           16
#define FILE_ATTRIBUTE_ARCHIVE             32
#define FILE_ATTRIBUTE_DEVICE              64
#define FILE_ATTRIBUTE_NORMAL             128
#define FILE_ATTRIBUTE_TEMPORARY          256
#define FILE_ATTRIBUTE_SPARSE_FILE        512
#define FILE_ATTRIBUTE_REPARSE_POINT     1024
#define FILE_ATTRIBUTE_COMPRESSED        2048
#define FILE_ATTRIBUTE_OFFLINE          0x1000
#define FILE_ATTRIBUTE_ENCRYPTED        0x4000
#define FILE_ATTRIBUTE_UNIX_EXTENSION   0x8000   /* trick for Unix */

/* END   <winerror.h> */

#include <string.h>
#include <stddef.h>

/* END #include <winnt.h> */

/* END #include <windef.h> */

/* BEGIN #include <winbase.h> */

#define WAIT_OBJECT_0 0
#define INFINITE	0xFFFFFFFF

typedef struct _SYSTEMTIME {
	WORD wYear;
	WORD wMonth;
	WORD wDayOfWeek;
	WORD wDay;
	WORD wHour;
	WORD wMinute;
	WORD wSecond;
	WORD wMilliseconds;
} SYSTEMTIME;

#ifdef __cplusplus
extern "C" {
#endif

BOOL WINAPI DosDateTimeToFileTime(WORD,WORD,FILETIME *);
BOOL WINAPI FileTimeToDosDateTime(CONST FILETIME *,WORD *, WORD *);
BOOL WINAPI FileTimeToLocalFileTime(CONST FILETIME *,FILETIME *);
BOOL WINAPI FileTimeToSystemTime(CONST FILETIME *,SYSTEMTIME *);
BOOL WINAPI LocalFileTimeToFileTime(CONST FILETIME *,FILETIME *);
VOID WINAPI GetSystemTime(SYSTEMTIME *);
BOOL WINAPI SystemTimeToFileTime(const SYSTEMTIME*,FILETIME *);
VOID WINAPI GetSystemTimeAsFileTime(FILETIME * time);

DWORD WINAPI GetTickCount(VOID);

#ifdef __cplusplus
}
#endif
/* END #include <winbase.h> */

/* BEGIN #include <winnls.h> */

#define CP_ACP   0
#define CP_OEMCP 1
#define CP_UTF8  65001

/* #include <unknwn.h> */
#include <basetyps.h>

#ifdef __cplusplus
extern "C" const IID IID_ISequentialStream;
struct ISequentialStream : public IUnknown
{
	STDMETHOD(QueryInterface)(REFIID,PVOID*) PURE;
	STDMETHOD_(ULONG,AddRef)(void) PURE;
	STDMETHOD_(ULONG,Release)(void) PURE;
	STDMETHOD(Read)(void*,ULONG,ULONG*) PURE;
	STDMETHOD(Write)(void const*,ULONG,ULONG*) PURE;
};
#else
extern const IID IID_ISequentialStream;
#endif  /* __cplusplus */


/* END #include <ole2.h> */

#endif

