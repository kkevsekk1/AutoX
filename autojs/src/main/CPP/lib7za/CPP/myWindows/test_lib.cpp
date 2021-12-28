#undef BIG_ENDIAN
#undef LITTLE_ENDIAN

#include "StdAfx.h"

#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <dirent.h>
#include <unistd.h>

#ifdef __APPLE_CC__
#define UInt32  macUIn32
#include <CoreFoundation/CoreFoundation.h>
#undef UInt32
#endif

#ifdef ENV_HAVE_WCHAR__H
#include <wchar.h>
#endif
#ifdef ENV_HAVE_LOCALE
#include <locale.h>
#endif

#include <windows.h>

#define NEED_NAME_WINDOWS_TO_UNIX
// #include "myPrivate.h"

#include "Common/StringConvert.h"
#include "Common/StdOutStream.h"

#undef NDEBUG
#include <assert.h>

#include "Common/StringConvert.cpp"
#include "Common/StdOutStream.cpp"
#include "Common/IntToString.cpp"
#include "Common/UTFConvert.cpp"

#include "Windows/Synchronization.cpp"
#include "Windows/FileFind.cpp"
#include "Windows/TimeUtils.cpp"
#include "Windows/System.cpp"
#include "../C/Threads.c"
#include "../../C/Ppmd.h"

int g_CodePage = -1;

int global_use_lstat = 0; // FIXME

/* FIXME */

LPSTR WINAPI CharNextA( LPCSTR ptr ) {
  if (!*ptr)
    return (LPSTR)ptr;
  return (LPSTR)(ptr + 1); // p7zip search only for ASCII characters like '/' so no need to worry about current locale
}


int MyStringCompare(const char *s1, const char *s2)
{
  while (true)
  {
    unsigned char c1 = (unsigned char)*s1++;
    unsigned char c2 = (unsigned char)*s2++;
    if (c1 < c2) return -1;
    if (c1 > c2) return 1;
    if (c1 == 0) return 0;
  }
}

int MyStringCompare(const wchar_t *s1, const wchar_t *s2)
{
  while (true)
  {
    wchar_t c1 = *s1++;
    wchar_t c2 = *s2++;
    if (c1 < c2) return -1;
    if (c1 > c2) return 1;
    if (c1 == 0) return 0;
  }
}



/* FIXME */

using namespace NWindows;

#if  defined(ENV_HAVE_WCHAR__H) && defined(ENV_HAVE_MBSTOWCS) && defined(ENV_HAVE_WCSTOMBS)
void test_mbs(void) {
  wchar_t wstr1[256] = {
                         L'e',
                         0xE8, // latin small letter e with grave
                         0xE9, // latin small letter e with acute
                         L'a',
                         0xE0, // latin small letter a with grave
                         0x20AC, // euro sign
                         L'b',
                         0 };
  wchar_t wstr2[256];
  char    astr[256];

  global_use_utf16_conversion = 1;

  size_t len1 = wcslen(wstr1);

  printf("wstr1 - %d - '%ls'\n",(int)len1,wstr1);

  size_t len0 = wcstombs(astr,wstr1,sizeof(astr));
  printf("astr - %d - '%s'\n",(int)len0,astr);

  size_t len2 = mbstowcs(wstr2,astr,sizeof(wstr2)/sizeof(*wstr2));
  printf("wstr - %d - '%ls'\n",(int)len2,wstr2);

  if (wcscmp(wstr1,wstr2) != 0) {
    printf("ERROR during conversions wcs -> mbs -> wcs\n");
    exit(EXIT_FAILURE);
  }

  char *ptr = astr;
  size_t len = 0;
  while (*ptr) {
    ptr = CharNextA(ptr);
    len += 1;
  }
  if ((len != len1) && (len != 12)) { // 12 = when locale is UTF8 instead of ISO8859-15
    printf("ERROR CharNextA : len=%d, len1=%d\n",(int)len,(int)len1);
    exit(EXIT_FAILURE);
  }

  UString ustr(wstr1);
  assert(ustr.Len() == (int)len1);

  AString  ansistr(astr);
  assert(ansistr.Len() == (int)len0);

  ansistr = UnicodeStringToMultiByte(ustr);
  assert(ansistr.Len() == (int)len0);

  assert(strcmp(ansistr,astr) == 0);
  assert(wcscmp(ustr,wstr1) == 0);

  UString ustr2 = MultiByteToUnicodeString(astr);
  assert(ustr2.Len() == (int)len1);
  assert(wcscmp(ustr2,wstr1) == 0);
}
void test_mbs_2(void) {
  wchar_t wstr1[256] = { 
	0x1F388,  // Ballon
        0 };
  wchar_t wstr1_7zip[256] = { // 7-zip use UTF16 wide string
      0xd83c,
      0xdf88,
        0 };
  char astr1[]= {
    char(0xf0), char(0x9f), char(0x8e), char(0x88), 0

  };
  wchar_t wstr2[256];
  char    astr[256];

  printf("\nTest Ballon character\n");

  global_use_utf16_conversion = 1;

  size_t len1 = wcslen(wstr1);

  printf("wstr1 - %d - '%ls'\n",(int)len1,wstr1);

  size_t len0 = wcstombs(astr,wstr1,sizeof(astr));
  printf("astr - %d - '%s'\n",(int)len0,astr);

  printf("strlen(astr)=%d\n",(int)strlen(astr));
  printf("strlen(astr1)=%d\n",(int)strlen(astr1));
  assert(strlen(astr) == strlen(astr1));

  assert(strcmp(astr,astr1) == 0);

  size_t len2 = mbstowcs(wstr2,astr,sizeof(wstr2)/sizeof(*wstr2));
  printf("wstr - %d - '%ls'\n",(int)len2,wstr2);

  if (wcscmp(wstr1,wstr2) != 0) {
    printf("ERROR during conversions wcs -> mbs -> wcs\n");
    exit(EXIT_FAILURE);
  }

  AString  ansistr(astr);
  assert(ansistr.Len() == (int)len0);

  UString ustr2 = MultiByteToUnicodeString(ansistr);
  assert(ustr2.Len() == wcslen(wstr1_7zip));
  assert(wcscmp(ustr2,wstr1_7zip) == 0);
}
#endif

static void test_astring(int num) {
  AString strResult;

  strResult = "first part : ";
  char number[256];
  sprintf(number,"%d",num);
  strResult += AString(number);

  strResult += " : last part";

  printf("strResult -%s-\n",(const char *)strResult);

}


extern void my_windows_split_path(const AString &p_path, AString &dir , AString &base);

static struct {
  const char *path;
  const char *dir;
  const char *base;
}
tabSplit[]=
  {
    { "",".","." },
    { "/","/","/" },
    { ".",".","." },
    { "//","/","/" },
    { "///","/","/" },
    { "dir",".","dir" },
    { "/dir","/","dir" },
    { "/dir/","/","dir" },
    { "/dir/base","/dir","base" },
    { "/dir//base","/dir","base" },
    { "/dir///base","/dir","base" },
    { "//dir/base","//dir","base" },
    { "///dir/base","///dir","base" },
    { "/dir/base/","/dir","base" },
    { 0,0,0 }
  };

static void test_split_astring() {
  int ind = 0;
  while (tabSplit[ind].path) {
    AString path(tabSplit[ind].path);
    AString dir;
    AString base;

    my_windows_split_path(path,dir,base);

    if ((dir != tabSplit[ind].dir) || (base != tabSplit[ind].base)) {
      printf("ERROR : '%s' '%s' '%s'\n",(const char *)path,(const char *)dir,(const char *)base);
    }
    ind++;
  }
  printf("test_split_astring : done\n");
}

 // Number of 100 nanosecond units from 1/1/1601 to 1/1/1970
#define EPOCH_BIAS  116444736000000000LL
static LARGE_INTEGER UnixTimeToUL(time_t tps_unx)
{
	LARGE_INTEGER ul;
	ul.QuadPart = tps_unx * 10000000LL + EPOCH_BIAS;
	return ul;
}

static LARGE_INTEGER FileTimeToUL(FILETIME fileTime)
{
	LARGE_INTEGER lFileTime;
	lFileTime.QuadPart = fileTime.dwHighDateTime;
	lFileTime.QuadPart = (lFileTime.QuadPart << 32) | fileTime.dwLowDateTime;
	return lFileTime;
}

static void display(const char *txt,SYSTEMTIME systime)
{
	FILETIME fileTime;
	BOOL ret = SystemTimeToFileTime(&systime,&fileTime);
	assert(ret == TRUE);
	LARGE_INTEGER ulFileTime = FileTimeToUL(fileTime);
	
	const char * day="";
	switch (systime.wDayOfWeek)
	{
        	case 0:day = "Sunday";break;
        	case 1:day = "Monday";break;
        	case 2:day = "Tuesday";break;
        	case 3:day = "Wednesday";break;
        	case 4:day = "Thursday";break;
        	case 5:day = "Friday";break;
        	case 6:day = "Saturday";break;
	}
	g_StdOut<< txt << day << " " 
		<< (int)systime.wYear << "/" <<  (int)systime.wMonth << "/" << (int)systime.wDay << " "
		<< (int)systime.wHour << ":" << (int)systime.wMinute << ":" <<  (int)systime.wSecond << ":" 
        	<<     (int)systime.wMilliseconds
		<< " (" << (UInt64)ulFileTime.QuadPart << ")\n";
}

static void test_time()
{
	time_t tps_unx = time(0);

	printf("Test Time (1):\n");
	printf("===========\n");
	SYSTEMTIME systimeGM;
	GetSystemTime(&systimeGM);
	
	LARGE_INTEGER ul = UnixTimeToUL(tps_unx);
	g_StdOut<<"  unix time = " << (UInt64)tps_unx << " (" << (UInt64)ul.QuadPart << ")\n";

	g_StdOut<<"  gmtime    : " << asctime(gmtime(&tps_unx))<<"\n";
	g_StdOut<<"  localtime : " << asctime(localtime(&tps_unx))<<"\n";

	display("  GetSystemTime : ", systimeGM);
}

static void test_time2()
{
        UInt32 dosTime = 0x30d0094C;
        FILETIME utcFileTime;
        FILETIME localFileTime;
        FILETIME localFileTime2;
        UInt32 dosTime2 = 0;

        printf("Test Time (2):\n");
        printf("===========\n");
        NTime::DosTimeToFileTime(dosTime, localFileTime);
        NTime::FileTimeToDosTime(localFileTime, dosTime2);
        assert(dosTime == dosTime2);

        printf("Test Time (3):\n");
	printf("===========\n");
	/* DosTime To utcFileTime */

	if (NTime::DosTimeToFileTime(dosTime, localFileTime)) /* DosDateTimeToFileTime */
	{
		if (!LocalFileTimeToFileTime(&localFileTime, &utcFileTime))
			utcFileTime.dwHighDateTime = utcFileTime.dwLowDateTime = 0;
	}

	printf("  - 0x%x => 0x%x 0x%x => 0x%x 0x%x\n",(unsigned)dosTime,
		(unsigned)localFileTime.dwHighDateTime,(unsigned)localFileTime.dwLowDateTime,
		(unsigned)utcFileTime.dwHighDateTime,(unsigned)utcFileTime.dwLowDateTime);


	/* utcFileTime to DosTime */

        FileTimeToLocalFileTime(&utcFileTime, &localFileTime2);
        NTime::FileTimeToDosTime(localFileTime2, dosTime2);  /* FileTimeToDosDateTime */

	printf("  - 0x%x <= 0x%x 0x%x <= 0x%x 0x%x\n",(unsigned)dosTime2,
		(unsigned)localFileTime2.dwHighDateTime,(unsigned)localFileTime2.dwLowDateTime,
		(unsigned)utcFileTime.dwHighDateTime,(unsigned)utcFileTime.dwLowDateTime);

	assert(dosTime == dosTime2);
	assert(localFileTime.dwHighDateTime == localFileTime2.dwHighDateTime);
	assert(localFileTime.dwLowDateTime  == localFileTime2.dwLowDateTime);
}

static void test_semaphore()
{
	g_StdOut << "\nTEST SEMAPHORE :\n";

	NWindows::NSynchronization::CSynchro sync;
	NWindows::NSynchronization::CSemaphoreWFMO sema;
	bool bres;
	DWORD waitResult;
	int i;

	sync.Create();
	sema.Create(&sync,2,10);

	g_StdOut << "   - Release(1)\n";
	for(i = 0 ;i < 8;i++)
	{
		// g_StdOut << "     - Release(1) : "<< i << "\n";
		bres = sema.Release(1);
		assert(bres == S_OK);
	}
	// g_StdOut << "     - Release(1) : done\n";
	bres = sema.Release(1);
	assert(bres == S_FALSE);

	g_StdOut << "   - WaitForMultipleObjects(INFINITE)\n";
	HANDLE events[1] = { sema };
	for(i=0;i<10;i++)
	{
		waitResult = ::WaitForMultipleObjects(1, events, FALSE, INFINITE);
		assert(waitResult == WAIT_OBJECT_0);
	}

	g_StdOut << "   Done\n";
}


/****************************************************************************************/


static int threads_count = 0;

static THREAD_FUNC_RET_TYPE thread_fct(void *  /* param */ ) {
	threads_count++;
	return 0;
}

#define MAX_THREADS 100000

int test_thread(void) {
	::CThread thread;

	Thread_Construct(&thread);

	threads_count = 0;
	
	printf("test_thread : %d threads\n",MAX_THREADS);

	for(int i=0;i<MAX_THREADS;i++) {
		Thread_Create(&thread, thread_fct, 0); 

		Thread_Wait(&thread);

		Thread_Close(&thread);
	}

	assert(threads_count == MAX_THREADS);

	return 0;
}


void dumpStr(const char *title,const char *txt)
{
  size_t i,len = strlen(txt);

  printf("%s - %d :",title,(int)len);

  for(i  = 0 ; i<len;i++) {
    printf(" 0x%02x",(unsigned)(txt[i] & 255)); 
  }

  printf("\n");
}


void dumpWStr(const char *title,const wchar_t *txt)
{
  size_t i,len = wcslen(txt);

  printf("%s - %d :",title,(int)len);

  for(i  = 0 ; i<len;i++) {
    printf(" 0x%02x",(unsigned)(txt[i])); 
  }

  printf("\n");
}

#ifdef __APPLE_CC__

void  testMaxOSX_stringConvert()
{
/*
                         0xE8, // latin small letter e with grave
                         0xE9, // latin small letter e with acute
                         L'a',
                         0xE0, // latin small letter a with grave
                         0x20AC, // euro sign
*/
   struct
   {
     char astr [256];
     wchar_t ustr [256];
   }
   tab [] =
   {
      {
      //   'a' , 'e with acute'       , 'e with grave'     ,  'a with grave'    ,  'u with grave'    ,  'b' , '.'  ,  't' , 'x'  , 't'  
         { 0x61,  0x65,  0xcc,  0x81  ,  0x65,  0xcc,  0x80,  0x61,  0xcc,  0x80,  0x75,  0xcc,  0x80,  0x62,  0x2e,  0x74,  0x78, 0x74,  0 },
         { 0x61,  0xe9,                  0xe8,                0xe0,                0xf9,                0x62,  0x2e,  0x74,  0x78, 0x74, 0 }
      },
      {
      //   'a' , 'euro sign'        ,  'b' , '.'  ,  't' , 'x'  , 't'  , '\n' 
         { 0x61,  0xe2,  0x82,  0xac,  0x62,  0x2e,  0x74,  0x78,  0x74,  0x0a, 0 },
         { 0x61,  0x20AC,              0x62,  0x2e,  0x74,  0x78,  0x74,  0x0a, 0 }  
      },
      {
         { 0 },
         { 0 }
      }
   };

   int i;

   printf("testMaxOSX_stringConvert : \n");

   i = 0;
   while (tab[i].astr[0])
   {
     printf("  %s\n",tab[i].astr);

     UString ustr = GetUnicodeString(tab[i].astr);

     // dumpWStr("1",&ustr[0]);

     assert(MyStringCompare(&ustr[0],tab[i].ustr) == 0);
     assert(ustr.Len() == wcslen(tab[i].ustr) );


     AString astr = GetAnsiString(ustr);
     assert(MyStringCompare(&astr[0],tab[i].astr) == 0);
     assert(astr.Len() == strlen(tab[i].astr) );

     i++;
   }
}

void  testMacOSX()
{
//  char texte1[]= { 0xc3 , 0xa9  , 0xc3, 0xa0, 0};

  wchar_t wpath1[4096] = {
                         0xE9, // latin small letter e with acute
                         0xE0,
                         0xc7,
                         0x25cc,
                         0x327,
                         0xe4,
                         0xe2,
                         0xc2,
                         0xc3,
                         0x2e,
                         0x74,
                         0x78,
                         0x74,
/*
                         L'e',
                         0xE8, // latin small letter e with grave
                         0xE9, // latin small letter e with acute
                         L'a',
                         0xE0, // latin small letter a with grave
                         0x20AC, // euro sign
                         L'b',
*/
                         0 };

    char utf8[4096];
    wchar_t wpath2[4096];



 // dumpStr("UTF8 standart",texte1);

    dumpWStr("UCS32 standard",wpath1);

// Translate into FS pathname
 {
    const wchar_t * wcs = wpath1;

    UniChar unipath[4096];

    long n = wcslen(wcs);

    for(long i =   0 ; i<= n ;i++) {
      unipath[i] = wcs[i];
    }

    CFStringRef cfpath = CFStringCreateWithCharacters(NULL,unipath,n);

    CFMutableStringRef cfpath2 = CFStringCreateMutableCopy(NULL,0,cfpath);
    CFRelease(cfpath);
    CFStringNormalize(cfpath2,kCFStringNormalizationFormD);
    
    CFStringGetCString(cfpath2,(char *)utf8,4096,kCFStringEncodingUTF8);

    CFRelease(cfpath2);  
  }

  dumpStr("UTF8 MacOSX",utf8);

// Translate from FS pathname
 {
    const char * path = utf8;

    long n = strlen(path);

    CFStringRef cfpath = CFStringCreateWithCString(NULL,path,kCFStringEncodingUTF8);

    if (cfpath)
    {

       CFMutableStringRef cfpath2 = CFStringCreateMutableCopy(NULL,0,cfpath);
       CFRelease(cfpath);
       CFStringNormalize(cfpath2,kCFStringNormalizationFormC);
    
       n = CFStringGetLength(cfpath2);
       for(long i =   0 ; i<= n ;i++) {
         wpath2[i] = CFStringGetCharacterAtIndex(cfpath2,i);
       }
       wpath2[n] = 0;

       CFRelease(cfpath2);  
    }
    else
    {
       wpath2[0] = 0;
    }
  }

  dumpWStr("UCS32 standard (2)",wpath2);

/*
 {
   CFStringRef cfpath;

    cfpath = CFStringCreateWithCString(kCFAllocatorDefault, texte1, kCFStringEncodingUTF8);

    // TODO str = null ?

    CFMutableStringRef cfpath2 = CFStringCreateMutableCopy(NULL,0,cfpaht);
    CFRealease(cfpath);

    

    
  }
*/


}
#endif // __APPLE_CC__


static const TCHAR *kMainDll = TEXT("7z.dll");

static CSysString ConvertUInt32ToString(UInt32 value)
{
  TCHAR buffer[32];
  ConvertUInt32ToString(value, buffer);
  return buffer;
}


void test_csystring(void)
{
	{
		const CSysString baseFolder = TEXT("bin/");
		const CSysString b2 = baseFolder + kMainDll;

		assert(MyStringCompare(&b2[0],TEXT("bin/7z.dll")) == 0);
	}

	{
		LPCTSTR dirPath=TEXT("/tmp/");
		LPCTSTR prefix=TEXT("foo");
		CSysString resultPath;

		UINT   number = 12345;
		UInt32 count  = 6789;
		
/*
		TCHAR * buf = resultPath.GetBuffer(MAX_PATH);
		::swprintf(buf,MAX_PATH,L"%ls%ls#%d@%d.tmp",dirPath,prefix,(unsigned)number,count);
		buf[MAX_PATH-1]=0;
		resultPath.ReleaseBuffer();
*/
		resultPath  = dirPath;
		resultPath += prefix;
		resultPath += TEXT('#');
		resultPath += ConvertUInt32ToString(number);
		resultPath += TEXT('@');
		resultPath += ConvertUInt32ToString(count);
		resultPath += TEXT(".tmp");

		// printf("##%ls##\n",&resultPath[0]);

		assert(MyStringCompare(&resultPath[0],TEXT("/tmp/foo#12345@6789.tmp")) == 0);
	}
	
}

static void  test_AString()
{
   AString a;

   a = "abc";
   assert(MyStringCompare(&a[0],"abc") == 0);
   assert(a.Len() == 3);

   a = GetAnsiString(L"abc");
   assert(MyStringCompare(&a[0],"abc") == 0);
   assert(a.Len() == 3);
}


const TCHAR kAnyStringWildcard = '*';

static void test_UString2(const UString &phyPrefix)
{
  UString tmp = phyPrefix + wchar_t(kAnyStringWildcard);
  printf("Enum(%ls-%ls-%lc)\n",&tmp[0],&phyPrefix[0],wchar_t(kAnyStringWildcard));
}



static void test_UString()
{
  UString us = L"7za433_tar";

   test_UString2(L"7za433_tar");

   UString u1(us);
   test_UString2(u1);
   u1 = L"";
   test_UString2(u1);
   u1 = us;
   test_UString2(u1);

   UString u2 = us;
   test_UString2(u2);
   u2 = L"";
   test_UString2(u2);
   u2 = u1;
   test_UString2(u2);

   u1 = L"abc";
   assert(MyStringCompare(&u1[0],L"abc") == 0);
   assert(u1.Len() == 3);

   u1 = GetUnicodeString("abc");
   assert(MyStringCompare(&u1[0],L"abc") == 0);
   assert(u1.Len() == 3);
}

/****************************************************************************************/
int main() {

  // return test_thread();


#ifdef ENV_HAVE_LOCALE
  setlocale(LC_ALL,"");
#endif

#if defined(BIG_ENDIAN)
  printf("BIG_ENDIAN : %d\n",(int)BIG_ENDIAN);
#endif
#if defined(LITTLE_ENDIAN)
  printf("LITTLE_ENDIAN : %d\n",(int)LITTLE_ENDIAN);
#endif

  printf("sizeof(Byte)   : %d\n",(int)sizeof(Byte));
  printf("sizeof(UInt16) : %d\n",(int)sizeof(UInt16));
  printf("sizeof(UInt32) : %d\n",(int)sizeof(UInt32));
  printf("sizeof(UINT32) : %d\n",(int)sizeof(UINT32));
  printf("sizeof(UInt64) : %d\n",(int)sizeof(UInt64));
  printf("sizeof(UINT64) : %d\n",(int)sizeof(UINT64));
  printf("sizeof(void *) : %d\n",(int)sizeof(void *));
  printf("sizeof(size_t) : %d\n",(int)sizeof(size_t));
  printf("sizeof(ptrdiff_t) : %d\n",(int)sizeof(ptrdiff_t));
  printf("sizeof(off_t) : %d\n",(int)sizeof(off_t));
  printf("sizeof(wchar_t) : %d\n",(int)sizeof(wchar_t));
#ifdef __APPLE_CC__
  printf("sizeof(UniChar) : %d\n",(int)sizeof(UniChar));
#endif
  printf("sizeof(CPpmd_See) : %d\n",(int)sizeof(CPpmd_See));
  printf("sizeof(CPpmd_State) : %d\n",(int)sizeof(CPpmd_State));

  // size tests
  assert(sizeof(Byte)==1);
  assert(sizeof(UInt16)==2);
  assert(sizeof(UInt32)==4);
  assert(sizeof(UINT32)==4);
  assert(sizeof(UInt64)==8);
  assert(sizeof(UINT64)==8);

  // alignement tests
  assert(sizeof(CPpmd_See)==4);
  assert(sizeof(CPpmd_State)==6);

  union {
	Byte b[2];
	UInt16 s;
  } u;
  u.s = 0x1234;

  if ((u.b[0] == 0x12) && (u.b[1] == 0x34)) {
    printf("CPU : big endian\n");
  } else if ((u.b[0] == 0x34) && (u.b[1] == 0x12)) {
    printf("CPU : little endian\n");
  } else {
    printf("CPU : unknown endianess\n");
  }

#if  defined(ENV_HAVE_WCHAR__H) && defined(ENV_HAVE_MBSTOWCS) && defined(ENV_HAVE_WCSTOMBS)
  test_mbs();
  test_mbs_2();
#endif

  test_astring(12345);
  test_split_astring();

  test_csystring();
  test_AString();
  test_UString();

  test_time();

  test_time2();

  test_semaphore();

#ifdef __APPLE_CC__
  testMacOSX();
  testMaxOSX_stringConvert();
#endif


{
	LANGID langID;
	WORD primLang;
	WORD subLang;

	langID = GetUserDefaultLangID();
	printf("langID=0x%x\n",langID);

	primLang = (WORD)(PRIMARYLANGID(langID));
	subLang = (WORD)(SUBLANGID(langID));

	printf("primLang=%d subLang=%d\n",(unsigned)primLang,(unsigned)subLang);  
}

  printf("\n### All Done ###\n\n");

  return 0;
}

