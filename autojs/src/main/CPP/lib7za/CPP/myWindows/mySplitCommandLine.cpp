#include "StdAfx.h"

#include "../Common/StringConvert.h"

#include "myPrivate.h"
#include "Windows/System.h"
#include "7zip/MyVersion.h"
#include "Common/StdOutStream.h"
#include "Common/IntToString.h"
#include "../C/CpuArch.h"

#ifdef ENV_HAVE_LOCALE
#include <locale.h>
#endif

#include <string.h> // memset

extern void my_windows_split_path(const AString &p_path, AString &dir , AString &base);


#if defined(MY_CPU_X86_OR_AMD64) && defined(_7ZIP_ASM)
static void PrintCpuChars(AString &s, UInt32 v)
{
  for (int j = 0; j < 4; j++)
  {
    Byte b = (Byte)(v & 0xFF);
    v >>= 8;
    if (b == 0)
      break;
    s += (char)b;
  }
}

static void x86cpuid_to_String(const Cx86cpuid &c, AString &s)
{
  s.Empty();

  UInt32 maxFunc2 = 0;
  UInt32 t[3];

  MyCPUID(0x80000000, &maxFunc2, &t[0], &t[1], &t[2]);

  bool fullNameIsAvail = (maxFunc2 >= 0x80000004);

  if (!fullNameIsAvail)
  {
    for (int i = 0; i < 3; i++)
      PrintCpuChars(s, c.vendor[i]);
  }
  else
  {
    for (int i = 0; i < 3; i++)
    {
      UInt32 c[4] = { 0 };
      MyCPUID(0x80000002 + i, &c[0], &c[1], &c[2], &c[3]);
      for (int j = 0; j < 4; j++)
        PrintCpuChars(s, c[j]);
    }
  }

  s.Add_Space_if_NotEmpty();
  {
    char temp[32];
    ConvertUInt32ToHex(c.ver, temp);
    s += '(';
    s += temp;
    s += ')';
  }
}
#endif

static void GetCpuName(AString &s)
{
  s.Empty();

  #ifdef MY_CPU_X86_OR_AMD64
  {
    #ifdef _7ZIP_ASM
    Cx86cpuid cpuid;
    if (x86cpuid_CheckAndRead(&cpuid))
    {
      x86cpuid_to_String(cpuid, s);
      return;
    }
    #endif
    #ifdef MY_CPU_AMD64
    s = "x64";
    #else
    s = "x86";
    #endif
  }
  #else

    #ifdef MY_CPU_LE
      s = "LE";
    #elif defined(MY_CPU_BE)
      s = "BE";
    #else
      #error ENDIANNESS
    #endif

  #endif
}

void mySplitCommandLine(int numArguments, char *arguments[],UStringVector &parts) {

  { // define P7ZIP_HOME_DIR
    static char p7zip_home_dir[MAX_PATH];
    AString dir,name;
    my_windows_split_path(arguments[0],dir,name);
    snprintf(p7zip_home_dir,sizeof(p7zip_home_dir),"P7ZIP_HOME_DIR=%s/",(const char *)dir);
    p7zip_home_dir[sizeof(p7zip_home_dir)-1] = 0;
    putenv(p7zip_home_dir);
  }

#ifdef ENV_HAVE_LOCALE
  // set the program's current locale from the user's environment variables
  setlocale(LC_ALL,"");

  // auto-detect which conversion p7zip should use
  char *locale = setlocale(LC_CTYPE,0);
  if (locale) {
    size_t len = strlen(locale);
    char *locale_upper = (char *)malloc(len+1);
    if (locale_upper) {
      strcpy(locale_upper,locale);

      for(size_t i=0;i<len;i++)
        locale_upper[i] = toupper(locale_upper[i] & 255);

      if (    (strcmp(locale_upper,"") != 0)
              && (strcmp(locale_upper,"C") != 0)
              && (strcmp(locale_upper,"POSIX") != 0) ) {
        global_use_utf16_conversion = 1;
      }
      free(locale_upper);
    }
  }
#elif defined(LOCALE_IS_UTF8)
  global_use_utf16_conversion = 1; // assume LC_CTYPE="utf8"
#else
  global_use_utf16_conversion = 0; // assume LC_CTYPE="C"
#endif

  parts.Clear();
  for(int ind=0;ind < numArguments; ind++) {
    if ((ind <= 2) && (strcmp(arguments[ind],"-no-utf16") == 0)) {
      global_use_utf16_conversion = 0;
    } else if ((ind <= 2) && (strcmp(arguments[ind],"-utf16") == 0)) {
      global_use_utf16_conversion = 1;
    } else {
      UString tmp = MultiByteToUnicodeString(arguments[ind]);
      // tmp.Trim(); " " is a valid filename ...
      if (!tmp.IsEmpty()) {
        parts.Add(tmp);
      }
      // try to hide the password
      {
        char * arg = arguments[ind];
    size_t len = strlen(arg);
        if ( (len > 2) && (arg[0] == '-') && ( (arg[1]=='p') || (arg[1]=='P') ) )
        {
          memset(arg+2,'*',len-2);
        }
      }
    }
  }
}

const char *my_getlocale(void) {
#ifdef ENV_HAVE_LOCALE
  const char* ret = setlocale(LC_CTYPE,0);
  if (ret == 0)
    ret ="C";
  return ret;
#elif defined(LOCALE_IS_UTF8)
  return "utf8";
#else
  return "C";
#endif
}

void showP7zipInfo(CStdOutStream *so)
{
  if (!so)
    return;

  AString cpu_name;
  GetCpuName(cpu_name);
  cpu_name.Trim();

  int bits = int(sizeof(void *)) * 8;

  *so << "p7zip Version " << P7ZIP_VERSION << " (locale=" << my_getlocale() <<",Utf16=";
  if (global_use_utf16_conversion) *so << "on";
  else                             *so << "off";
  *so << ",HugeFiles=";
  if (sizeof(off_t) >= 8) *so << "on,";
  else                    *so << "off,";
  *so << bits << " bits,";
  int nbcpu = NWindows::NSystem::GetNumberOfProcessors();
  if (nbcpu > 1) *so << nbcpu << " CPUs ";
  else           *so << nbcpu << " CPU ";
  *so << cpu_name;

#ifdef _7ZIP_ASM
{
  const char * txt =",ASM";
  #ifdef MY_CPU_X86_OR_AMD64
  if (CPU_Is_Aes_Supported()) { txt =",ASM,AES-NI"; }
  #endif
  *so << txt;
}
#endif
    *so << ")\n\n";

}
