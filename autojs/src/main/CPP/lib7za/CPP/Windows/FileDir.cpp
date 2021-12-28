// Windows/FileDir.cpp

#include "StdAfx.h"

#ifndef _UNICODE
#include "../Common/StringConvert.h"
#endif

#include "FileDir.h"
#include "FileFind.h"
#include "FileName.h"

using namespace NWindows;
using namespace NFile;
using namespace NName;

#include "../Common/StringConvert.h"
#include "../Common/IntToString.h"

#define NEED_NAME_WINDOWS_TO_UNIX
#include "myPrivate.h"
#include "Windows/Synchronization.h"

#include <unistd.h> // rmdir
#include <errno.h>

#include <sys/stat.h> // mkdir
#include <sys/types.h>
#include <fcntl.h>

#include <utime.h>

// #define TRACEN(u) u;
#define TRACEN(u)  /* */

int g_filedir = 1;

static NWindows::NSynchronization::CCriticalSection g_CountCriticalSection;

class Umask
{
  public:
  mode_t  current_umask;
  mode_t  mask;
  Umask() {
    current_umask = umask (0);  /* get and set the umask */
    umask(current_umask);   /* restore the umask */
    mask = 0777 & (~current_umask);
  }
};

static Umask gbl_umask;
extern BOOLEAN WINAPI RtlTimeToSecondsSince1970( const LARGE_INTEGER *Time, DWORD *Seconds );


#ifdef _UNICODE
AString nameWindowToUnix2(LPCWSTR name) // FIXME : optimization ?
{
   AString astr = UnicodeStringToMultiByte(name);
   return AString(nameWindowToUnix((const char *)astr));
}
#endif

DWORD WINAPI GetFullPathNameW( LPCTSTR name, DWORD len, LPTSTR buffer, LPTSTR *lastpart ) { // FIXME
  if (name == 0) return 0;

  DWORD name_len = lstrlen(name);

  if (name[0] == '/') {
    DWORD ret = name_len+2;
    if (ret >= len) {
      TRACEN((printf("GetFullPathNameA(%ls,%d,)=0000 (case 0)\n",name, (int)len)))
      return 0;
    }
    lstrcpy(buffer,L"c:");
    lstrcat(buffer,name);

    *lastpart=buffer;
    TCHAR *ptr=buffer;
    while (*ptr) {
      if (*ptr == '/')
        *lastpart=ptr+1;
      ptr++;
    }
    TRACEN((printf("GetFullPathNameA(%ls,%d,%ls,%ls)=%d\n",name, (int)len,buffer, *lastpart,(int)ret)))
    return ret;
  }
  if (isascii(name[0]) && (name[1] == ':')) { // FIXME isascii
    DWORD ret = name_len;
    if (ret >= len) {
      TRACEN((printf("GetFullPathNameA(%ls,%d,)=0000 (case 1)\n",name, (int)len)))
      return 0;
    }
    lstrcpy(buffer,name);

    *lastpart=buffer;
    TCHAR *ptr=buffer;
    while (*ptr) {
      if (*ptr == '/')
        *lastpart=ptr+1;
      ptr++;
    }
    TRACEN((printf("GetFullPathNameA(%ls,%d,%ls,%ls)=%d\n",name, (int)len,buffer, *lastpart,(int)ret)))
    return ret;
  }

  // name is a relative pathname.
  //
  if (len < 2) {
    TRACEN((printf("GetFullPathNameA(%ls,%d,)=0000 (case 2)\n",name, (int)len)))
    return 0;
  }

  DWORD ret = 0;
  char begin[MAX_PATHNAME_LEN];
  /* DWORD begin_len = GetCurrentDirectoryA(MAX_PATHNAME_LEN,begin); */
  DWORD begin_len = 0;
  begin[0]='c';
  begin[1]=':';
  char * cret = getcwd(begin+2, MAX_PATHNAME_LEN - 3);
  if (cret) {
    begin_len = strlen(begin);
  }

  if (begin_len >= 1) {
    //    strlen(begin) + strlen("/") + strlen(name)
    ret = begin_len     +    1        + name_len;

    if (ret >= len) {
      TRACEN((printf("GetFullPathNameA(%ls,%d,)=0000 (case 4)\n",name, (int)len)))
      return 0;
    }
    UString wbegin = GetUnicodeString(begin);
    lstrcpy(buffer,wbegin);
    lstrcat(buffer,L"/");
    lstrcat(buffer,name);

    *lastpart=buffer + begin_len + 1;
    TCHAR *ptr=buffer;
    while (*ptr) {
      if (*ptr == '/')
        *lastpart=ptr+1;
      ptr++;
    }
    TRACEN((printf("GetFullPathNameA(%ls,%d,%ls,%ls)=%d\n",name, (int)len,buffer, *lastpart,(int)ret)))
  } else {
    ret = 0;
    TRACEN((printf("GetFullPathNameA(%ls,%d,)=0000 (case 5)\n",name, (int)len)))
  }
  return ret;
}

static int copy_fd(int fin,int fout)
{
  char buffer[16384];
  ssize_t ret_in;
  ssize_t ret_out;

  do {
    ret_out = -1;
    do {
      ret_in = read(fin, buffer,sizeof(buffer));
    } while (ret_in < 0 && (errno == EINTR));
    if (ret_in >= 1) {
      do {
        ret_out = write (fout, buffer, ret_in);
      } while (ret_out < 0 && (errno == EINTR));
    } else if (ret_in == 0) {
      ret_out = 0;
    }
  } while (ret_out >= 1);
  return ret_out;
}

static BOOL CopyFile(const char *src,const char *dst)
{
  int ret = -1;

#ifdef O_BINARY
  int   flags = O_BINARY;
#else
  int   flags = 0;
#endif

#ifdef O_LARGEFILE
  flags |= O_LARGEFILE;
#endif

  // printf("##DBG CopyFile(%s,%s)\n",src,dst);
  int fout = open(dst,O_CREAT | O_WRONLY | O_EXCL | flags, 0600);
  if (fout != -1)
  {
    int fin = open(src,O_RDONLY | flags , 0600);
    if (fin != -1)
    {
      ret = copy_fd(fin,fout);
      if (ret == 0) ret = close(fin);
      else                close(fin);
    }
    if (ret == 0) ret = close(fout);
    else                close(fout);
  }
  if (ret == 0) return TRUE;
  return FALSE;
}


#ifndef _UNICODE
extern bool g_IsNT;
#endif

namespace NWindows {
namespace NFile {

// SetCurrentDirectory doesn't support \\?\ prefix

#ifdef WIN_LONG_PATH
bool GetLongPathBase(CFSTR fileName, UString &res);
bool GetLongPath(CFSTR fileName, UString &res);
#endif

namespace NDir {


#ifdef _WIN32

#ifndef UNDER_CE

bool MyGetWindowsDirectory(FString &path)
{
  UINT needLength;
  #ifndef _UNICODE
  if (!g_IsNT)
  {
    TCHAR s[MAX_PATH + 2];
    s[0] = 0;
    needLength = ::GetWindowsDirectory(s, MAX_PATH + 1);
    path = fas2fs(s);
  }
  else
  #endif
  {
    WCHAR s[MAX_PATH + 2];
    s[0] = 0;
    needLength = ::GetWindowsDirectoryW(s, MAX_PATH + 1);
    path = us2fs(s);
  }
  return (needLength > 0 && needLength <= MAX_PATH);
}


bool MyGetSystemDirectory(FString &path)
{
  UINT needLength;
  #ifndef _UNICODE
  if (!g_IsNT)
  {
    TCHAR s[MAX_PATH + 2];
    s[0] = 0;
    needLength = ::GetSystemDirectory(s, MAX_PATH + 1);
    path = fas2fs(s);
  }
  else
  #endif
  {
    WCHAR s[MAX_PATH + 2];
    s[0] = 0;
    needLength = ::GetSystemDirectoryW(s, MAX_PATH + 1);
    path = us2fs(s);
  }
  return (needLength > 0 && needLength <= MAX_PATH);
}
#endif
#endif // _WIN32

bool SetDirTime(CFSTR fileName, const FILETIME * /* cTime */ , const FILETIME *aTime, const FILETIME *mTime)
{
  AString  cfilename = UnicodeStringToMultiByte(fileName);
  const char * unix_filename = nameWindowToUnix((const char *)cfilename);

  struct utimbuf buf;

  struct stat    oldbuf;
  int ret = stat(unix_filename,&oldbuf);
  if (ret == 0) {
    buf.actime  = oldbuf.st_atime;
    buf.modtime = oldbuf.st_mtime;
  } else {
    time_t current_time = time(0);
    buf.actime  = current_time;
    buf.modtime = current_time;
  }

  if (aTime)
  {
    LARGE_INTEGER  ltime;
    DWORD dw;
    ltime.QuadPart = aTime->dwHighDateTime;
    ltime.QuadPart = (ltime.QuadPart << 32) | aTime->dwLowDateTime;
    RtlTimeToSecondsSince1970( &ltime, &dw );
    buf.actime = dw;
  }

  if (mTime)
  {
    LARGE_INTEGER  ltime;
    DWORD dw;
    ltime.QuadPart = mTime->dwHighDateTime;
    ltime.QuadPart = (ltime.QuadPart << 32) | mTime->dwLowDateTime;
    RtlTimeToSecondsSince1970( &ltime, &dw );
    buf.modtime = dw;
  }

  /* ret = */ utime(unix_filename, &buf);

  return true;
}

#ifdef WIN_LONG_PATH
bool GetLongPaths(CFSTR s1, CFSTR s2, UString &d1, UString &d2)
{
  if (!GetLongPathBase(s1, d1) ||
      !GetLongPathBase(s2, d2))
    return false;
  if (d1.IsEmpty() && d2.IsEmpty())
    return false;
  if (d1.IsEmpty()) d1 = fs2us(s1);
  if (d2.IsEmpty()) d2 = fs2us(s2);
  return true;
}
#endif

static int convert_to_symlink(const char * name) {
  TRACEN(printf("LINK(%s)\n",name))
  FILE *file = fopen(name,"rb");
  if (file) {
    char buf[MAX_PATHNAME_LEN+1];
    char * ret = fgets(buf,sizeof(buf)-1,file);
    fclose(file);
    if (ret) {
      int ir = unlink(name);
      if (ir == 0) {
        ir = symlink(buf,name);
        TRACEN(printf("TO(%s)\n",buf))
      }
      return ir;
    }
  }
  return -1;
}

bool SetFileAttrib(CFSTR fileName, DWORD fileAttributes,CObjectVector<CDelayedSymLink> *delayedSymLinks)
{
  if (!fileName) {
    SetLastError(ERROR_PATH_NOT_FOUND);
    TRACEN((printf("SetFileAttrib(NULL,%d) : false-1\n",fileAttributes)))
    return false;
  }
#ifdef _UNICODE
  AString name = nameWindowToUnix2(fileName);
#else
  const char * name = nameWindowToUnix(fileName);
#endif
  struct stat stat_info;
#ifdef ENV_HAVE_LSTAT
  if (global_use_lstat) {
    if(lstat(name,&stat_info)!=0) {
      TRACEN((printf("SetFileAttrib(%s,%d) : false-2-1\n",(const char *)name,fileAttributes)))
      return false;
    }
  } else
#endif
  {
    if(stat(name,&stat_info)!=0) {
      TRACEN((printf("SetFileAttrib(%s,%d) : false-2-2\n",(const char *)name,fileAttributes)))
      return false;
    }
  }

  if (fileAttributes & FILE_ATTRIBUTE_UNIX_EXTENSION) {
     stat_info.st_mode = fileAttributes >> 16;
#ifdef ENV_HAVE_LSTAT
     if (S_ISLNK(stat_info.st_mode)) {
         if (delayedSymLinks) {
           delayedSymLinks->Add(CDelayedSymLink(name));
         } else if ( convert_to_symlink(name) != 0) {
          TRACEN((printf("SetFileAttrib(%s,%d) : false-3\n",(const char *)name,fileAttributes)))
          return false;
        }
     } else
#endif
     if (S_ISREG(stat_info.st_mode)) {
       TRACEN((printf("##DBG chmod-2(%s,%o)\n",(const char *)name,(unsigned)stat_info.st_mode & gbl_umask.mask)))
       chmod(name,stat_info.st_mode & gbl_umask.mask);
     } else if (S_ISDIR(stat_info.st_mode)) {
       // user/7za must be able to create files in this directory
       stat_info.st_mode |= (S_IRUSR | S_IWUSR | S_IXUSR);
       TRACEN((printf("##DBG chmod-3(%s,%o)\n",(const char *)name,(unsigned)stat_info.st_mode & gbl_umask.mask)))
       chmod(name,stat_info.st_mode & gbl_umask.mask);
     }
#ifdef ENV_HAVE_LSTAT
  } else if (!S_ISLNK(stat_info.st_mode)) {
    // do not use chmod on a link
#else
  } else {
#endif

    /* Only Windows Attributes */
    if( S_ISDIR(stat_info.st_mode)) {
       /* Remark : FILE_ATTRIBUTE_READONLY ignored for directory. */
       TRACEN((printf("##DBG chmod-4(%s,%o)\n",(const char *)name,(unsigned)stat_info.st_mode & gbl_umask.mask)))
       chmod(name,stat_info.st_mode & gbl_umask.mask);
    } else {
       if (fileAttributes & FILE_ATTRIBUTE_READONLY) stat_info.st_mode &= ~0222; /* octal!, clear write permission bits */
       TRACEN((printf("##DBG chmod-5(%s,%o)\n",(const char *)name,(unsigned)stat_info.st_mode & gbl_umask.mask)))
       chmod(name,stat_info.st_mode & gbl_umask.mask);
    }
  }
  TRACEN((printf("SetFileAttrib(%s,%d) : true\n",(const char *)name,fileAttributes)))

  return true;
}

bool RemoveDir(CFSTR path)
{
  if (!path || !*path) {
    SetLastError(ERROR_PATH_NOT_FOUND);
    return FALSE;
  }
  AString name = nameWindowToUnix2(path);
  TRACEN((printf("RemoveDirectoryA(%s)\n",(const char *)name)))

  if (rmdir( (const char *)name ) != 0) {
    return FALSE;
  }
  return TRUE;
}

bool MyMoveFile(CFSTR existFileName, CFSTR newFileName)
{
#ifdef _UNICODE
  AString src = nameWindowToUnix2(existFileName);
  AString dst = nameWindowToUnix2(newFileName);
#else
  const char * src = nameWindowToUnix(existFileName);
  const char * dst = nameWindowToUnix(newFileName);
#endif

  TRACEN((printf("MyMoveFile(%s,%s)\n",(const char *)src,(const char *)dst)))

  int ret = rename(src,dst);
  if (ret != 0)
  {
    if (errno == EXDEV) // FIXED : bug #1112167 (Temporary directory must be on same partition as target)
    {
      BOOL bret = CopyFile(src,dst);
      if (bret == FALSE) return false;

      struct stat info_file;
      ret = stat(src,&info_file);
      if (ret == 0) {
    TRACEN((printf("##DBG chmod-1(%s,%o)\n",(const char *)dst,(unsigned)info_file.st_mode & gbl_umask.mask)))
        ret = chmod(dst,info_file.st_mode & gbl_umask.mask);
      }
      if (ret == 0) {
         ret = unlink(src);
      }
      if (ret == 0) return true;
    }
    return false;
  }
  return true;
}

bool CreateDir(CFSTR path)
{
  if (!path || !*path) {
    SetLastError(ERROR_PATH_NOT_FOUND);
    return false;
  }

#ifdef _UNICODE
  AString name = nameWindowToUnix2(path);
#else
  const char * name = nameWindowToUnix(path);
#endif
  bool bret = false;
  if (mkdir( name, 0700 ) == 0) bret = true;

  TRACEN((printf("CreateDir(%s)=%d\n",(const char *)name,(int)bret)))
  return bret;
}

bool CreateComplexDir(CFSTR _aPathName)
{
  AString name = nameWindowToUnix2(_aPathName);
  TRACEN((printf("CreateComplexDir(%s)\n",(const char *)name)))


  FString pathName = _aPathName;
  int pos = pathName.ReverseFind(FCHAR_PATH_SEPARATOR);
  if (pos > 0 && pos == pathName.Len() - 1)
  {
    if (pathName.Len() == 3 && pathName[1] == L':')
      return true; // Disk folder;
    pathName.Delete(pos);
  }
  FString pathName2 = pathName;
  pos = pathName.Len();
  TRACEN((printf("CreateComplexDir(%s) pathName2=%ls\n",(const char *)name,(CFSTR)pathName2)))
  for (;;)
  {
    if (CreateDir(pathName))
      break;
    TRACEN((printf("CreateComplexDir(%s) GetLastError=%d (ERROR_ALREADY_EXISTS=%d)\n",(const char *)name,::GetLastError(), ERROR_ALREADY_EXISTS)))
    if (::GetLastError() == ERROR_ALREADY_EXISTS)
    {
#ifdef _WIN32 // FIXED for supporting symbolic link instead of a directory
      NFind::CFileInfo fileInfo;
      if (!fileInfo.Find(pathName)) // For network folders
        return true;
      if (!fileInfo.IsDir())
        return false;
#endif
      break;
    }
    pos = pathName.ReverseFind(FCHAR_PATH_SEPARATOR);
    if (pos < 0 || pos == 0)
      return false;
    if (pathName[pos - 1] == L':')
      return false;
    pathName = pathName.Left(pos);
  }
  pathName = pathName2;
  while (pos < pathName.Len())
  {
    pos = pathName.Find(FCHAR_PATH_SEPARATOR, pos + 1);
    if (pos < 0)
      pos = pathName.Len();
    if (!CreateDir(pathName.Left(pos)))
      return false;
  }
  return true;
}

bool DeleteFileAlways(CFSTR name)
{
  if (!name || !*name) {
    SetLastError(ERROR_PATH_NOT_FOUND);
    return false;
  }
#ifdef _UNICODE
   AString unixname = nameWindowToUnix2(name);
#else
   const char * unixname = nameWindowToUnix(name);
#endif
   bool bret = false;
   if (remove(unixname) == 0) bret = true;
   TRACEN((printf("DeleteFileAlways(%s)=%d\n",(const char *)unixname,(int)bret)))
   return bret;
}

bool RemoveDirWithSubItems(const FString &path)
{
  bool needRemoveSubItems = true;
  {
    NFind::CFileInfo fi;
    if (!fi.Find(path))
      return false;
    if (!fi.IsDir())
    {
      ::SetLastError(ERROR_DIRECTORY);
      return false;
    }
    if (fi.HasReparsePoint())
      needRemoveSubItems = false;
  }

  if (needRemoveSubItems)
  {
    FString s = path;
    s += FCHAR_PATH_SEPARATOR;
    unsigned prefixSize = s.Len();
    s += FCHAR_ANY_MASK;
    NFind::CEnumerator enumerator(s);
    NFind::CFileInfo fi;
    while (enumerator.Next(fi))
    {
      s.DeleteFrom(prefixSize);
      s += fi.Name;
      if (fi.IsDir())
      {
        if (!RemoveDirWithSubItems(s))
          return false;
      }
      else if (!DeleteFileAlways(s))
        return false;
    }
  }

  if (!SetFileAttrib(path, 0))
    return false;
  return RemoveDir(path);
}


bool RemoveDirectoryWithSubItems(const FString &path); // FIXME
static bool RemoveDirectorySubItems2(const FString pathPrefix, const NFind::CFileInfo &fileInfo)
{
  if (fileInfo.IsDir())
    return RemoveDirectoryWithSubItems(pathPrefix + fileInfo.Name);
  return DeleteFileAlways(pathPrefix + fileInfo.Name);
}
bool RemoveDirectoryWithSubItems(const FString &path)
{
  NFind::CFileInfo fileInfo;
  FString pathPrefix = path + FCHAR_PATH_SEPARATOR;
  {
    NFind::CEnumerator enumerator(pathPrefix + FCHAR_ANY_MASK);
    while (enumerator.Next(fileInfo))
      if (!RemoveDirectorySubItems2(pathPrefix, fileInfo))
        return false;
  }
  if (!SetFileAttrib(path, 0))
    return false;
  return RemoveDir(path);
}

#ifdef UNDER_CE

bool MyGetFullPathName(CFSTR fileName, FString &resFullPath)
{
  resFullPath = fileName;
  return true;
}

#else

bool MyGetFullPathName(CFSTR path, FString &resFullPath)
{
  return GetFullPath(path, resFullPath);
}

bool SetCurrentDir(CFSTR path)
{
   AString apath = UnicodeStringToMultiByte(path);

   return chdir((const char*)apath) == 0;
}

bool GetCurrentDir(FString &path)
{
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

#endif

bool GetFullPathAndSplit(CFSTR path, FString &resDirPrefix, FString &resFileName)
{
  bool res = MyGetFullPathName(path, resDirPrefix);
  if (!res)
    resDirPrefix = path;
  int pos = resDirPrefix.ReverseFind(FCHAR_PATH_SEPARATOR);
  resFileName = resDirPrefix.Ptr(pos + 1);
  resDirPrefix.DeleteFrom(pos + 1);
  return res;
}

bool GetOnlyDirPrefix(CFSTR path, FString &resDirPrefix)
{
  FString resFileName;
  return GetFullPathAndSplit(path, resDirPrefix, resFileName);
}

bool MyGetTempPath(FString &path)
{
  path = L"c:/tmp/"; // final '/' is needed
  return true;
}

static bool CreateTempFile(CFSTR prefix, bool addRandom, FString &path, NIO::COutFile *outFile)
{
#ifdef _WIN32
  UInt32 d = (GetTickCount() << 12) ^ (GetCurrentThreadId() << 14) ^ GetCurrentProcessId();
#else
  static UInt32 memo_count = 0;
  UInt32 count;

  g_CountCriticalSection.Enter();
  count = memo_count++;
  g_CountCriticalSection.Leave();
  UINT number = (UINT)getpid();

  UInt32 d = (GetTickCount() << 12) ^ (count << 14) ^ number;
#endif
  for (unsigned i = 0; i < 100; i++)
  {
    path = prefix;
    if (addRandom)
    {
      FChar s[16];
      UInt32 value = d;
      unsigned k;
      for (k = 0; k < 8; k++)
      {
        unsigned t = value & 0xF;
        value >>= 4;
        s[k] = (char)((t < 10) ? ('0' + t) : ('A' + (t - 10)));
      }
      s[k] = '\0';
      if (outFile)
        path += FChar('.');
      path += s;
      UInt32 step = GetTickCount() + 2;
      if (step == 0)
        step = 1;
      d += step;
    }
    addRandom = true;
    if (outFile)
      path += FTEXT(".tmp");
    if (NFind::DoesFileOrDirExist(path))
    {
      SetLastError(ERROR_ALREADY_EXISTS);
      continue;
    }
    if (outFile)
    {
      if (outFile->Create(path, false))
        return true;
    }
    else
    {
      if (CreateDir(path))
        return true;
    }
    DWORD error = GetLastError();
    if (error != ERROR_FILE_EXISTS &&
        error != ERROR_ALREADY_EXISTS)
      break;
  }
  path.Empty();
  return false;
}

bool CTempFile::Create(CFSTR prefix, NIO::COutFile *outFile)
{
  if (!Remove())
    return false;
  if (!CreateTempFile(prefix, false, _path, outFile))
    return false;
  _mustBeDeleted = true;
  return true;
}

bool CTempFile::CreateRandomInTempFolder(CFSTR namePrefix, NIO::COutFile *outFile)
{
  if (!Remove())
    return false;
  FString tempPath;
  if (!MyGetTempPath(tempPath))
    return false;
  if (!CreateTempFile(tempPath + namePrefix, true, _path, outFile))
    return false;
  _mustBeDeleted = true;
  return true;
}

bool CTempFile::Remove()
{
  if (!_mustBeDeleted)
    return true;
  _mustBeDeleted = !DeleteFileAlways(_path);
  return !_mustBeDeleted;
}

bool CTempFile::MoveTo(CFSTR name, bool deleteDestBefore)
{
  if (deleteDestBefore)
    if (NFind::DoesFileExist(name))
      if (!DeleteFileAlways(name))
        return false;
  DisableDeleting();
  return MyMoveFile(_path, name);
}

bool CTempDir::Create(CFSTR prefix)
{
  if (!Remove())
    return false;
  FString tempPath;
  if (!MyGetTempPath(tempPath))
    return false;
  if (!CreateTempFile(tempPath + prefix, true, _path, NULL))
    return false;
  _mustBeDeleted = true;
  return true;
}

bool CTempDir::Remove()
{
  if (!_mustBeDeleted)
    return true;
  _mustBeDeleted = !RemoveDirectoryWithSubItems(_path);
  return !_mustBeDeleted;
}

#ifdef ENV_UNIX

CDelayedSymLink::CDelayedSymLink(const char * source)
  : _source(source)
{
  struct stat st;

  if (lstat(_source, &st) == 0) {
    _dev = st.st_dev;
    _ino = st.st_ino;
  } else {
    _dev = 0;
  }
}

bool CDelayedSymLink::Create()
{
  struct stat st;

  if (_dev == 0) {
    errno = EPERM;
    return false;
  }
  if (lstat(_source, &st) != 0)
    return false;
  if (_dev != st.st_dev || _ino != st.st_ino) {
    // Placeholder file has been overwritten or moved by another
    // symbolic link creation
    errno = EPERM;
    return false;
  }

  return convert_to_symlink(_source) == 0;
}

#endif // ENV_UNIX

}}}

#ifndef _SFX

namespace NWindows {
namespace NDLL {

FString GetModuleDirPrefix()
{
  FString s;

  const char *p7zip_home_dir = getenv("P7ZIP_HOME_DIR");
  if (p7zip_home_dir) {
    return MultiByteToUnicodeString(p7zip_home_dir,CP_ACP);
  }

  return FTEXT(".") FSTRING_PATH_SEPARATOR;
}

}}

#endif
