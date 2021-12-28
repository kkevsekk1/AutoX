// Windows/FileName.cpp

#include "StdAfx.h"

#include "Windows/FileName.h"
#include "Common/Wildcard.h"

#include "Common/StringConvert.h"

namespace NWindows {
namespace NFile {
namespace NDir {
bool MyGetFullPathName(CFSTR path, FString &resFullPath);
}}}

namespace NWindows {
namespace NFile {
namespace NName {

#define IS_SEPAR(c) IS_PATH_SEPAR(c)

int FindSepar(const wchar_t *s) throw()
{
  for (const wchar_t *p = s;; p++)
  {
    const wchar_t c = *p;
    if (c == 0)
      return -1;
    if (IS_SEPAR(c))
      return (int)(p - s);
  }
}

#ifndef USE_UNICODE_FSTRING
int FindSepar(const FChar *s) throw()
{
  for (const FChar *p = s;; p++)
  {
    const FChar c = *p;
    if (c == 0)
      return -1;
    if (IS_SEPAR(c))
      return (int)(p - s);
  }
}
#endif


#ifndef USE_UNICODE_FSTRING
void NormalizeDirPathPrefix(FString &dirPath)
{
  if (dirPath.IsEmpty())
    return;
  if (dirPath.Back() != FCHAR_PATH_SEPARATOR)
    dirPath += FCHAR_PATH_SEPARATOR;
}
#endif

void NormalizeDirPathPrefix(UString &dirPath)
{
  if (dirPath.IsEmpty())
    return;
  if (dirPath.Back() != WCHAR_PATH_SEPARATOR)
    dirPath += WCHAR_PATH_SEPARATOR;
}


#ifdef _WIN32

const wchar_t *kSuperPathPrefix = L"\\\\?\\";
static const wchar_t *kSuperUncPrefix = L"\\\\?\\UNC\\";

#define IS_DEVICE_PATH(s)  ((s)[0] == '\\' && (s)[1] == '\\' && (s)[2] == '.' && (s)[3] == '\\')
#define IS_SUPER_PREFIX(s) ((s)[0] == '\\' && (s)[1] == '\\' && (s)[2] == '?' && (s)[3] == '\\')
#define IS_SUPER_OR_DEVICE_PATH(s) ((s)[0] == '\\' && (s)[1] == '\\' && ((s)[2] == '?' || (s)[2] == '.') && (s)[3] == '\\')
#define IS_LETTER_CHAR(c) ((c) >= 'a' && (c) <= 'z' || (c) >= 'A' && (c) <= 'Z')

#define IS_UNC_WITH_SLASH(s) ( \
  ((s)[0] == 'U' || (s)[0] == 'u') && \
  ((s)[1] == 'N' || (s)[1] == 'n') && \
  ((s)[2] == 'C' || (s)[2] == 'c') && \
   (s)[3] == '\\')

bool IsDevicePath(CFSTR s) throw()
{
  #ifdef UNDER_CE

  s = s;
  return false;
  /*
  // actually we don't know the way to open device file in WinCE.
  unsigned len = MyStringLen(s);
  if (len < 5 || len > 5 || memcmp(s, FTEXT("DSK"), 3 * sizeof(FChar)) != 0)
    return false;
  if (s[4] != ':')
    return false;
  // for reading use SG_REQ sg; if (DeviceIoControl(dsk, IOCTL_DISK_READ));
  */
  
  #else
  
  if (!IS_DEVICE_PATH(s))
    return false;
  unsigned len = MyStringLen(s);
  if (len == 6 && s[5] == ':')
    return true;
  if (len < 18 || len > 22 || memcmp(s + kDevicePathPrefixSize, FTEXT("PhysicalDrive"), 13 * sizeof(FChar)) != 0)
    return false;
  for (unsigned i = 17; i < len; i++)
    if (s[i] < '0' || s[i] > '9')
      return false;
  return true;
  
  #endif
}

bool IsSuperUncPath(CFSTR s) throw() { return (IS_SUPER_PREFIX(s) && IS_UNC_WITH_SLASH(s + kSuperPathPrefixSize)); }

bool IsDrivePath(const wchar_t *s) throw() { return IS_LETTER_CHAR(s[0]) && s[1] == ':' && s[2] == '\\'; }
bool IsSuperPath(const wchar_t *s) throw() { return IS_SUPER_PREFIX(s); }
bool IsSuperOrDevicePath(const wchar_t *s) throw() { return IS_SUPER_OR_DEVICE_PATH(s); }
// bool IsSuperUncPath(const wchar_t *s) { return (IS_SUPER_PREFIX(s) && IS_UNC_WITH_SLASH(s + kSuperPathPrefixSize)); }

#ifndef USE_UNICODE_FSTRING
bool IsDrivePath(CFSTR s) throw() { return IS_LETTER_CHAR(s[0]) && s[1] == ':' && s[2] == '\\'; }
bool IsSuperPath(CFSTR s) throw() { return IS_SUPER_PREFIX(s); }
bool IsSuperOrDevicePath(CFSTR s) throw() { return IS_SUPER_OR_DEVICE_PATH(s); }
#endif // USE_UNICODE_FSTRING

bool IsAbsolutePath(const wchar_t *s) throw()
{
  return s[0] == WCHAR_PATH_SEPARATOR || IsDrivePath(s);
}

static const unsigned kDrivePrefixSize = 3; /* c:\ */

#ifndef USE_UNICODE_FSTRING

static unsigned GetRootPrefixSize_Of_NetworkPath(CFSTR s) throw()
{
  // Network path: we look "server\path\" as root prefix
  int pos = FindCharPosInString(s, '\\');
  if (pos < 0)
    return 0;
  int pos2 = FindCharPosInString(s + pos + 1, '\\');
  if (pos2 < 0)
    return 0;
  return pos + pos2 + 2;
}

static unsigned GetRootPrefixSize_Of_SimplePath(CFSTR s) throw()
{
  if (IsDrivePath(s))
    return kDrivePrefixSize;
  if (s[0] != '\\' || s[1] != '\\')
    return 0;
  unsigned size = GetRootPrefixSize_Of_NetworkPath(s + 2);
  return (size == 0) ? 0 : 2 + size;
}

static unsigned GetRootPrefixSize_Of_SuperPath(CFSTR s) throw()
{
  if (IS_UNC_WITH_SLASH(s + kSuperPathPrefixSize))
  {
    unsigned size = GetRootPrefixSize_Of_NetworkPath(s + kSuperUncPathPrefixSize);
    return (size == 0) ? 0 : kSuperUncPathPrefixSize + size;
  }
  // we support \\?\c:\ paths and volume GUID paths \\?\Volume{GUID}\"
  int pos = FindCharPosInString(s + kSuperPathPrefixSize, FCHAR_PATH_SEPARATOR);
  if (pos < 0)
    return 0;
  return kSuperPathPrefixSize + pos + 1;
}

unsigned GetRootPrefixSize(CFSTR s) throw()
{
  if (IS_DEVICE_PATH(s))
    return kDevicePathPrefixSize;
  if (IsSuperPath(s))
    return GetRootPrefixSize_Of_SuperPath(s);
  return GetRootPrefixSize_Of_SimplePath(s);
}

#endif // USE_UNICODE_FSTRING

static unsigned GetRootPrefixSize_Of_NetworkPath(const wchar_t *s) throw()
{
  // Network path: we look "server\path\" as root prefix
  int pos = FindCharPosInString(s, L'\\');
  if (pos < 0)
    return 0;
  int pos2 = FindCharPosInString(s + pos + 1, L'\\');
  if (pos2 < 0)
    return 0;
  return pos + pos2 + 2;
}

static unsigned GetRootPrefixSize_Of_SimplePath(const wchar_t *s) throw()
{
  if (IsDrivePath(s))
    return kDrivePrefixSize;
  if (s[0] != '\\' || s[1] != '\\')
    return 0;
  unsigned size = GetRootPrefixSize_Of_NetworkPath(s + 2);
  return (size == 0) ? 0 : 2 + size;
}

static unsigned GetRootPrefixSize_Of_SuperPath(const wchar_t *s) throw()
{
  if (IS_UNC_WITH_SLASH(s + kSuperPathPrefixSize))
  {
    unsigned size = GetRootPrefixSize_Of_NetworkPath(s + kSuperUncPathPrefixSize);
    return (size == 0) ? 0 : kSuperUncPathPrefixSize + size;
  }
  // we support \\?\c:\ paths and volume GUID paths \\?\Volume{GUID}\"
  int pos = FindCharPosInString(s + kSuperPathPrefixSize, L'\\');
  if (pos < 0)
    return 0;
  return kSuperPathPrefixSize + pos + 1;
}

unsigned GetRootPrefixSize(const wchar_t *s) throw()
{
  if (IS_DEVICE_PATH(s))
    return kDevicePathPrefixSize;
  if (IsSuperPath(s))
    return GetRootPrefixSize_Of_SuperPath(s);
  return GetRootPrefixSize_Of_SimplePath(s);
}

#else // _WIN32

static const unsigned kDrivePrefixSize = 3; /* c:\ */
#define IS_LETTER_CHAR(c) ((c) >= 'a' && (c) <= 'z' || (c) >= 'A' && (c) <= 'Z')
bool IsDrivePath(const wchar_t *s) throw() { return IS_LETTER_CHAR(s[0]) && s[1] == ':' && s[2] == WCHAR_PATH_SEPARATOR; }

bool IsAbsolutePath(const wchar_t *s) throw() { return s[0] == WCHAR_PATH_SEPARATOR; }

#ifndef USE_UNICODE_FSTRING
unsigned GetRootPrefixSize(CFSTR s) throw() { return s[0] == CHAR_PATH_SEPARATOR ? 1 : 0; }
#endif
unsigned GetRootPrefixSize(const wchar_t *s) throw() {
  if (IsDrivePath(s))
    return kDrivePrefixSize;

 return s[0] == CHAR_PATH_SEPARATOR ? 1 : 0;
}

#endif // _WIN32


#ifndef UNDER_CE

static bool GetCurDir(UString &path)
{
  path.Empty();
  char begin[MAX_PATHNAME_LEN];
  begin[0]='c';
  begin[1]=':';
  char * cret = getcwd(begin+2, MAX_PATHNAME_LEN - 3);
  if (cret)
  {
#ifdef _UNICODE
    path = GetUnicodeString(begin);
#else
    path = begin;
#endif
    return true;
  }
  return false;
}

static bool ResolveDotsFolders(UString &s)
{
  #ifdef _WIN32
  s.Replace(L'/', WCHAR_PATH_SEPARATOR);
  #endif
  for (int i = 0;;)
  {
    wchar_t c = s[i];
    if (c == 0)
      return true;
    if (c == '.' && (i == 0 || s[i - 1] == WCHAR_PATH_SEPARATOR))
    {
      wchar_t c1 = s[i + 1];
      if (c1 == '.')
      {
        wchar_t c2 = s[i + 2];
        if (c2 == WCHAR_PATH_SEPARATOR || c2 == 0)
        {
          if (i == 0)
            return false;
          int k = i - 2;
          for (; k >= 0; k--)
            if (s[k] == WCHAR_PATH_SEPARATOR)
              break;
          unsigned num;
          if (k >= 0)
          {
            num = i + 2 - k;
            i = k;
          }
          else
          {
            num = (c2 == 0 ? (i + 2) : (i + 3));
            i = 0;
          }
          s.Delete(i, num);
          continue;
        }
      }
      else
      {
        if (c1 == WCHAR_PATH_SEPARATOR || c1 == 0)
        {
          unsigned num = 2;
          if (i != 0)
            i--;
          else if (c1 == 0)
            num = 1;
          s.Delete(i, num);
          continue;
        }
      }
    }
    i++;
  }
}

#endif // UNDER_CE

#define LONG_PATH_DOTS_FOLDERS_PARSING


/*
Windows (at least 64-bit XP) can't resolve "." or ".." in paths that start with SuperPrefix \\?\
To solve that problem we check such path:
   - super path contains        "." or ".." - we use kSuperPathType_UseOnlySuper
   - super path doesn't contain "." or ".." - we use kSuperPathType_UseOnlyMain
*/
#ifdef LONG_PATH_DOTS_FOLDERS_PARSING
#ifndef UNDER_CE
static bool AreThereDotsFolders(CFSTR s)
{
  for (unsigned i = 0;; i++)
  {
    FChar c = s[i];
    if (c == 0)
      return false;
    if (c == '.' && (i == 0 || s[i - 1] == CHAR_PATH_SEPARATOR))
    {
      FChar c1 = s[i + 1];
      if (c1 == 0 || c1 == CHAR_PATH_SEPARATOR ||
          (c1 == '.' && (s[i + 2] == 0 || s[i + 2] == CHAR_PATH_SEPARATOR)))
        return true;
    }
  }
}
#endif
#endif // LONG_PATH_DOTS_FOLDERS_PARSING

#ifdef WIN_LONG_PATH

/*
Most of Windows versions have problems, if some file or dir name
contains '.' or ' ' at the end of name (Bad Path).
To solve that problem, we always use Super Path ("\\?\" prefix and full path)
in such cases. Note that "." and ".." are not bad names.

There are 3 cases:
  1) If the path is already Super Path, we use that path
  2) If the path is not Super Path :
     2.1) Bad Path;  we use only Super Path.
     2.2) Good Path; we use Main Path. If it fails, we use Super Path.

 NeedToUseOriginalPath returns:
    kSuperPathType_UseOnlyMain    : Super already
    kSuperPathType_UseOnlySuper    : not Super, Bad Path
    kSuperPathType_UseMainAndSuper : not Super, Good Path
*/

int GetUseSuperPathType(CFSTR s) throw()
{
  if (IsSuperOrDevicePath(s))
  {
    #ifdef LONG_PATH_DOTS_FOLDERS_PARSING
    if ((s)[2] != '.')
      if (AreThereDotsFolders(s + kSuperPathPrefixSize))
        return kSuperPathType_UseOnlySuper;
    #endif
    return kSuperPathType_UseOnlyMain;
  }

  for (unsigned i = 0;; i++)
  {
    FChar c = s[i];
    if (c == 0)
      return kSuperPathType_UseMainAndSuper;
    if (c == '.' || c == ' ')
    {
      FChar c2 = s[i + 1];
      if (c2 == 0 || c2 == CHAR_PATH_SEPARATOR)
      {
        // if it's "." or "..", it's not bad name.
        if (c == '.')
        {
          if (i == 0 || s[i - 1] == CHAR_PATH_SEPARATOR)
            continue;
          if (s[i - 1] == '.')
          {
            if (i - 1 == 0 || s[i - 2] == CHAR_PATH_SEPARATOR)
              continue;
          }
        }
        return kSuperPathType_UseOnlySuper;
      }
    }
  }
}


/*
   returns false in two cases:
     - if GetCurDir was used, and GetCurDir returned error.
     - if we can't resolve ".." name.
   if path is ".", "..", res is empty.
   if it's Super Path already, res is empty.
   for \**** , and if GetCurDir is not drive (c:\), res is empty
   for absolute paths, returns true, res is Super path.
*/


static bool GetSuperPathBase(CFSTR s, UString &res)
{
  res.Empty();
  
  FChar c = s[0];
  if (c == 0)
    return true;
  if (c == '.' && (s[1] == 0 || (s[1] == '.' && s[2] == 0)))
    return true;
  
  if (IsSuperOrDevicePath(s))
  {
    #ifdef LONG_PATH_DOTS_FOLDERS_PARSING
    
    if ((s)[2] == '.')
      return true;

    // we will return true here, so we will try to use these problem paths.

    if (!AreThereDotsFolders(s + kSuperPathPrefixSize))
      return true;
    
    UString temp = fs2us(s);
    unsigned fixedSize = GetRootPrefixSize_Of_SuperPath(temp);
    if (fixedSize == 0)
      return true;

    UString rem = &temp[fixedSize];
    if (!ResolveDotsFolders(rem))
      return true;

    temp.DeleteFrom(fixedSize);
    res += temp;
    res += rem;
    
    #endif

    return true;
  }

  if (c == CHAR_PATH_SEPARATOR)
  {
    if (s[1] == CHAR_PATH_SEPARATOR)
    {
      UString temp = fs2us(s + 2);
      unsigned fixedSize = GetRootPrefixSize_Of_NetworkPath(temp);
      if (fixedSize == 0) // maybe we must ignore that error to allow short network paths?
        return false;
      UString rem = &temp[fixedSize];
      if (!ResolveDotsFolders(rem))
        return false;
      res += kSuperUncPrefix;
      temp.DeleteFrom(fixedSize);
      res += temp;
      res += rem;
      return true;
    }
  }
  else
  {
    if (IsDrivePath(s))
    {
      UString temp = fs2us(s);
      UString rem = &temp[kDrivePrefixSize];
      if (!ResolveDotsFolders(rem))
        return true;
      res += kSuperPathPrefix;
      temp.DeleteFrom(kDrivePrefixSize);
      res += temp;
      res += rem;
      return true;
    }
  }

  UString curDir;
  if (!GetCurDir(curDir))
    return false;
  if (curDir.Back() != WCHAR_PATH_SEPARATOR)
    curDir += WCHAR_PATH_SEPARATOR;

  unsigned fixedSizeStart = 0;
  unsigned fixedSize = 0;
  const wchar_t *superMarker = NULL;
  if (IsSuperPath(curDir))
  {
    fixedSize = GetRootPrefixSize_Of_SuperPath(curDir);
    if (fixedSize == 0)
      return false;
  }
  else
  {
    if (IsDrivePath(curDir))
    {
      superMarker = kSuperPathPrefix;
      fixedSize = kDrivePrefixSize;
    }
    else
    {
      if (curDir[0] != CHAR_PATH_SEPARATOR || curDir[1] != CHAR_PATH_SEPARATOR)
        return false;
      fixedSizeStart = 2;
      fixedSize = GetRootPrefixSize_Of_NetworkPath(&curDir[2]);
      if (fixedSize == 0)
        return false;
      superMarker = kSuperUncPrefix;
    }
  }
  
  UString temp;
  if (c == CHAR_PATH_SEPARATOR)
  {
    temp = fs2us(s + 1);
  }
  else
  {
    temp += &curDir[fixedSizeStart + fixedSize];
    temp += fs2us(s);
  }
  if (!ResolveDotsFolders(temp))
    return false;
  if (superMarker)
    res += superMarker;
  res += curDir.Mid(fixedSizeStart, fixedSize);
  res += temp;
  return true;
}


/*
  In that case if GetSuperPathBase doesn't return new path, we don't need
  to use same path that was used as main path
                        
  GetSuperPathBase  superPath.IsEmpty() onlyIfNew
     false            *                *          GetCurDir Error
     true            false             *          use Super path
     true            true             true        don't use any path, we already used mainPath
     true            true             false       use main path as Super Path, we don't try mainMath
                                                  That case is possible now if GetCurDir returns unknow
                                                  type of path (not drive and not network)

  We can change that code if we want to try mainPath, if GetSuperPathBase returns error,
  and we didn't try mainPath still.
  If we want to work that way, we don't need to use GetSuperPathBase return code.
*/

bool GetSuperPath(CFSTR path, UString &superPath, bool onlyIfNew)
{
  if (GetSuperPathBase(path, superPath))
  {
    if (superPath.IsEmpty())
    {
      // actually the only possible when onlyIfNew == true and superPath is empty
      // is case when

      if (onlyIfNew)
        return false;
      superPath = fs2us(path);
    }
    return true;
  }
  return false;
}

bool GetSuperPaths(CFSTR s1, CFSTR s2, UString &d1, UString &d2, bool onlyIfNew)
{
  if (!GetSuperPathBase(s1, d1) ||
      !GetSuperPathBase(s2, d2))
    return false;
  if (d1.IsEmpty() && d2.IsEmpty() && onlyIfNew)
    return false;
  if (d1.IsEmpty()) d1 = fs2us(s1);
  if (d2.IsEmpty()) d2 = fs2us(s2);
  return true;
}


/*
// returns true, if we need additional use with New Super path.
bool GetSuperPath(CFSTR path, UString &superPath)
{
  if (GetSuperPathBase(path, superPath))
    return !superPath.IsEmpty();
  return false;
}
*/
#endif // WIN_LONG_PATH

bool GetFullPath(CFSTR dirPrefix, CFSTR s, FString &res)
{
  res = s;

  #ifdef UNDER_CE

  if (s[0] != CHAR_PATH_SEPARATOR)
  {
    if (!dirPrefix)
      return false;
    res = dirPrefix;
    res += s;
  }

  #else

  unsigned prefixSize = GetRootPrefixSize(s);
  if (prefixSize != 0)
  {
    if (!AreThereDotsFolders(s + prefixSize))
      return true;
    
    UString rem = fs2us(s + prefixSize);
    if (!ResolveDotsFolders(rem))
      return true; // maybe false;
    res.DeleteFrom(prefixSize);
    res += us2fs(rem);
    return true;
  }

  /*
  FChar c = s[0];
  if (c == 0)
    return true;
  if (c == '.' && (s[1] == 0 || (s[1] == '.' && s[2] == 0)))
    return true;
  if (c == CHAR_PATH_SEPARATOR && s[1] == CHAR_PATH_SEPARATOR)
    return true;
  if (IsDrivePath(s))
    return true;
  */

  UString curDir;
  if (dirPrefix)
    curDir = fs2us(dirPrefix);
  else
  {
    if (!GetCurDir(curDir))
      return false;
  }
  if (!curDir.IsEmpty() && curDir.Back() != WCHAR_PATH_SEPARATOR)
    curDir += WCHAR_PATH_SEPARATOR;

  unsigned fixedSize = 0;

  #ifdef _WIN32

  if (IsSuperPath(curDir))
  {
    fixedSize = GetRootPrefixSize_Of_SuperPath(curDir);
    if (fixedSize == 0)
      return false;
  }
  else
  {
    if (IsDrivePath(curDir))
      fixedSize = kDrivePrefixSize;
    else
    {
      if (curDir[0] != WCHAR_PATH_SEPARATOR || curDir[1] != WCHAR_PATH_SEPARATOR)
        return false;
      fixedSize = GetRootPrefixSize_Of_NetworkPath(&curDir[2]);
      if (fixedSize == 0)
        return false;
      fixedSize += 2;
    }
  }
  #else
    if (IsDrivePath(curDir))
      fixedSize = kDrivePrefixSize;

  #endif // _WIN32
  
  UString temp;
  if (s[0] == CHAR_PATH_SEPARATOR)
  {
    temp = fs2us(s + 1);
  }
  else
  {
    temp += curDir.Ptr(fixedSize);
    temp += fs2us(s);
  }
  if (!ResolveDotsFolders(temp))
    return false;
  curDir.DeleteFrom(fixedSize);
  res = us2fs(curDir);
  res += us2fs(temp);
  
  #endif // UNDER_CE

  return true;
}

bool GetFullPath(CFSTR path, FString &fullPath)
{
  return GetFullPath(NULL, path, fullPath);
}

}}}
