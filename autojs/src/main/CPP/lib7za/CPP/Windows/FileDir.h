// Windows/FileDir.h

#ifndef __WINDOWS_FILE_DIR_H
#define __WINDOWS_FILE_DIR_H

#include "../Common/MyString.h"
#include "../Common/MyVector.h"

#include "FileIO.h"

#ifdef ENV_UNIX
#include <sys/stat.h> // ino_t
#endif

namespace NWindows {
namespace NFile {
namespace NDir {

class CDelayedSymLink;

bool GetWindowsDir(FString &path);
bool GetSystemDir(FString &path);

bool SetDirTime(CFSTR path, const FILETIME *cTime, const FILETIME *aTime, const FILETIME *mTime);
bool SetFileAttrib(CFSTR path, DWORD attrib, CObjectVector<CDelayedSymLink> *delayedSymLinks = 0);
bool MyMoveFile(CFSTR existFileName, CFSTR newFileName);

#ifndef UNDER_CE
bool MyCreateHardLink(CFSTR newFileName, CFSTR existFileName);
#endif

bool RemoveDir(CFSTR path);
bool CreateDir(CFSTR path);

/* CreateComplexDir returns true, if directory can contain files after the call (two cases):
    1) the directory already exists (network shares and drive paths are supported)
    2) the directory was created
  path can be WITH or WITHOUT trailing path separator. */

bool CreateComplexDir(CFSTR path);

bool DeleteFileAlways(CFSTR name);
bool RemoveDirWithSubItems(const FString &path);

bool MyGetFullPathName(CFSTR path, FString &resFullPath);
bool GetFullPathAndSplit(CFSTR path, FString &resDirPrefix, FString &resFileName);
bool GetOnlyDirPrefix(CFSTR path, FString &resDirPrefix);

#ifndef UNDER_CE

bool SetCurrentDir(CFSTR path);
bool GetCurrentDir(FString &resultPath);

#endif

bool MyGetTempPath(FString &resultPath);

class CTempFile
{
  bool _mustBeDeleted;
  FString _path;
  void DisableDeleting() { _mustBeDeleted = false; }
public:
  CTempFile(): _mustBeDeleted(false) {}
  ~CTempFile() { Remove(); }
  const FString &GetPath() const { return _path; }
  bool Create(CFSTR pathPrefix, NIO::COutFile *outFile); // pathPrefix is not folder prefix
  bool CreateRandomInTempFolder(CFSTR namePrefix, NIO::COutFile *outFile);
  bool Remove();
  bool MoveTo(CFSTR name, bool deleteDestBefore);
};

class CTempDir
{
  bool _mustBeDeleted;
  FString _path;
public:
  CTempDir(): _mustBeDeleted(false) {}
  ~CTempDir() { Remove();  }
  const FString &GetPath() const { return _path; }
  void DisableDeleting() { _mustBeDeleted = false; }
  bool Create(CFSTR namePrefix) ;
  bool Remove();
};

// Symbolic links must be created last so that they can't be used to
// create or overwrite files above the extraction directory.
class CDelayedSymLink
{
#ifdef ENV_UNIX
  // Where the symlink should be created.  The target is specified in
  // the placeholder file.
  AString _source;

  // Device and inode of the placeholder file.  Before creating the
  // symlink, we must check that these haven't been changed by creation
  // of another symlink.
  dev_t _dev;
  ino_t _ino;

public:
  explicit CDelayedSymLink(const char * source);
  bool Create();
#else // !ENV_UNIX
public:
  CDelayedSymLink(const char * source) {}
  bool Create() { return true; }
#endif // ENV_UNIX
};


#if !defined(UNDER_CE)
class CCurrentDirRestorer
{
  FString _path;
public:
  bool NeedRestore;

  CCurrentDirRestorer(): NeedRestore(true)
  {
    GetCurrentDir(_path);
  }
  ~CCurrentDirRestorer()
  {
    if (!NeedRestore)
      return;
    FString s;
    if (GetCurrentDir(s))
      if (s != _path)
        SetCurrentDir(_path);
  }
};
#endif

}}}

#endif
