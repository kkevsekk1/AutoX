// stdafx.h

#ifndef __STDAFX_H
#define __STDAFX_H


#include "config.h"

#define MAXIMUM_WAIT_OBJECTS 64

#define NO_INLINE /* FIXME */

#ifdef ENV_HAVE_PTHREAD
#include <pthread.h>
#endif

#include "Common/Common.h"
#include "Common/MyWindows.h"
#include "Common/MyTypes.h"
#include "Common/MyString.h" // FIXME

#include <windows.h>

#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <tchar.h>
#include <wchar.h>
#include <stddef.h>
#include <ctype.h>
#include <unistd.h>
#include <errno.h>
#include <math.h>

#ifdef __NETWARE__
#include <sys/types.h>
#endif

#undef CS /* fix for Solaris 10 x86 */


#ifdef __cplusplus
# define EXTERN_C    extern "C"
#else
# define EXTERN_C    extern
#endif


/***************************/

#ifndef ENV_HAVE_WCHAR__H

EXTERN_C_BEGIN

size_t	wcslen(const wchar_t *);
wchar_t *wcscpy(wchar_t * , const wchar_t * );
wchar_t *wcscat(wchar_t * , const wchar_t * );

EXTERN_C_END

#endif

/***************************/

#define CLASS_E_CLASSNOTAVAILABLE        ((HRESULT)0x80040111L)

/************************* LastError *************************/
inline DWORD WINAPI GetLastError(void) { return errno; }
inline void WINAPI SetLastError( DWORD err ) { errno = err; }

#define AreFileApisANSI() (1)

void Sleep(unsigned millisleep);

typedef pid_t t_processID;

t_processID GetCurrentProcess(void);

#define  NORMAL_PRIORITY_CLASS (0)
#define  IDLE_PRIORITY_CLASS   (10)
void SetPriorityClass(t_processID , int priority);

#ifdef __cplusplus
class wxWindow;
typedef wxWindow *HWND;

#define MB_ICONERROR (0x00000200) // wxICON_ERROR
#define MB_YESNOCANCEL (0x00000002 | 0x00000008 | 0x00000010) // wxYES | wxNO | wxCANCEL
#define MB_ICONQUESTION (0x00000400) // wxICON_QUESTION
#define MB_TASKMODAL  (0) // FIXME
#define MB_SYSTEMMODAL (0) // FIXME

#define MB_OK (0x00000004) // wxOK
#define MB_ICONSTOP (0x00000200) // wxICON_STOP
#define MB_OKCANCEL (0x00000004 | 0x00000010) // wxOK | wxCANCEL

#define MessageBox MessageBoxW
int MessageBoxW(wxWindow * parent, const TCHAR * mes, const TCHAR * title,int flag);


// FIXME
#define IDCLOSE   (5001) // wxID_CLOSE
#define IDEXIT    (5006) // wxID_EXIT
#define IDOK      (5100) // wxID_OK
#define IDCANCEL  (5101) // wxID_CANCEL
#define IDABORT   (5115) // wxID_ABORT
#define IDYES     (5103) // wxID_YES
#define IDNO      (5104) // wxID_NO
#define IDHELP    (5009) // wxID_HELP

// Show
#define SW_HIDE             0
#define SW_SHOW             5



typedef void *HINSTANCE;

// gcc / clang on Unix  : sizeof(long==sizeof(void*) in 32 or 64 bits)
//typedef          int   INT_PTR;
typedef          long   INT_PTR;
// typedef unsigned int  UINT_PTR;
typedef unsigned long  UINT_PTR;

typedef          long LONG_PTR;
typedef unsigned long DWORD_PTR;

typedef UINT_PTR WPARAM;

/* WARNING
 LPARAM shall be 'long' because of CListView::SortItems and wxListCtrl::SortItems :
*/
typedef LONG_PTR LPARAM;
typedef LONG_PTR LRESULT;


#define LOWORD(l)              ((WORD)((DWORD_PTR)(l) & 0xFFFF))
#define HIWORD(l)              ((WORD)((DWORD_PTR)(l) >> 16))


#define CALLBACK /* */

#define ERROR_NEGATIVE_SEEK         0x100131 // FIXME
#define FACILITY_WIN32                        7 // FIXME
#define __HRESULT_FROM_WIN32(x)   ((HRESULT)(x) > 0 ? ((HRESULT) (((x) & 0x0000FFFF) | (FACILITY_WIN32 << 16) | 0x80000000)) : (HRESULT)(x) ) // FIXME

static inline HRESULT HRESULT_FROM_WIN32(unsigned int x)
{
    return (HRESULT)x > 0 ? ((HRESULT) ((x & 0x0000FFFF) | (FACILITY_WIN32 << 16) | 0x80000000)) : (HRESULT)x;
}

/************ Windows2.h ***********/

typedef void * WNDPROC;
typedef void * CREATESTRUCT;
typedef struct
{
	HWND  hwndFrom;

	UINT  code;
#define NM_DBLCLK       1
#define LVN_ITEMCHANGED 2
#define LVN_COLUMNCLICK 3	
#define CBEN_BEGINEDIT  10
#define CBEN_ENDEDITW   11
	
	
} NMHDR;
typedef NMHDR * LPNMHDR;

typedef struct tagNMLISTVIEW
{
    NMHDR hdr;
    INT iItem;
    INT iSubItem;
    UINT uNewState;
    UINT uOldState;
    // UINT uChanged;
    // POINT ptAction;
    LPARAM  lParam;
} NMLISTVIEW, *LPNMLISTVIEW;

typedef void * LPNMITEMACTIVATE;

#define NM_RCLICK 1234 /* FIXME */

// FIXME
#define WM_CREATE 1
#define WM_COMMAND 2
#define WM_NOTIFY 3
#define WM_DESTROY 4
#define WM_CLOSE 5

#define HIWORD(l)              ((WORD)((DWORD_PTR)(l) >> 16))
#define LOWORD(l)              ((WORD)((DWORD_PTR)(l) & 0xFFFF))


/************ LANG ***********/
typedef WORD            LANGID;

LANGID GetUserDefaultLangID(void);
LANGID GetSystemDefaultLangID(void);

#define PRIMARYLANGID(l)        ((WORD)(l) & 0x3ff)
#define SUBLANGID(l)            ((WORD)(l) >> 10)

#if defined( __x86_64__ )

#define _WIN64 1

#endif

#endif

#endif 

