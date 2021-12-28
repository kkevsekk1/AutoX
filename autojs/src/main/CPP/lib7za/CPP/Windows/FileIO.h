// Windows/FileIO.h

#ifndef __WINDOWS_FILEIO_H
#define __WINDOWS_FILEIO_H

#include <Common/MyString.h>

#ifndef _WIN32

#define FILE_SHARE_READ	1
#define FILE_SHARE_WRITE 2

#define FILE_BEGIN	SEEK_SET
#define FILE_CURRENT	SEEK_CUR
#define FILE_END	SEEK_END
#define INVALID_SET_FILE_POINTER	((DWORD)-1)

#endif

#define _my_IO_REPARSE_TAG_MOUNT_POINT  (0xA0000003L)
#define _my_IO_REPARSE_TAG_SYMLINK      (0xA000000CL)

#define _my_SYMLINK_FLAG_RELATIVE 1

namespace NWindows {
namespace NFile {

struct CReparseAttr
{
  UInt32 Tag;
  UInt32 Flags;
  UString SubsName;
  UString PrintName;

  CReparseAttr(): Tag(0), Flags(0) {}
  bool Parse(const Byte *p, size_t size);

  bool IsMountPoint() const { return Tag == _my_IO_REPARSE_TAG_MOUNT_POINT; } // it's Junction
  bool IsSymLink() const { return Tag == _my_IO_REPARSE_TAG_SYMLINK; }
  bool IsRelative() const { return Flags == _my_SYMLINK_FLAG_RELATIVE; }
  // bool IsVolume() const;

  bool IsOkNamePair() const;
  UString GetPath() const;
};

namespace NIO {


class CFileBase
{
protected:
  int     _fd;
  AString _unix_filename;
  time_t   _lastAccessTime;
  time_t   _lastWriteTime;
#ifdef ENV_HAVE_LSTAT
  int     _size;
  char    _buffer[MAX_PATHNAME_LEN+1];
  int     _offset;
#endif

  bool Create(CFSTR fileName, DWORD desiredAccess,
      DWORD shareMode, DWORD creationDisposition,  DWORD flagsAndAttributes,bool ignoreSymbolicLink=false);

public:
  CFileBase(): _fd(-1) {};
  virtual ~CFileBase();

  virtual bool Close();

  bool GetLength(UINT64 &length) const;

  bool Seek(INT64 distanceToMove, DWORD moveMethod, UINT64 &newPosition);
  bool Seek(UINT64 position, UINT64 &newPosition);
};

class CInFile: public CFileBase
{
public:
  bool Open(CFSTR fileName, DWORD shareMode, DWORD creationDisposition,  DWORD flagsAndAttributes);
  bool OpenShared(CFSTR fileName, bool /* shareForWrite */ ,bool ignoreSymbolicLink=false) {
    return Open(fileName,ignoreSymbolicLink);
  }
  bool Open(CFSTR fileName,bool ignoreSymbolicLink=false);
  bool ReadPart(void *data, UINT32 size, UINT32 &processedSize);
  bool Read(void *data, UINT32 size, UINT32 &processedSize);
};

class COutFile: public CFileBase
{
public:
  bool Open(CFSTR fileName, DWORD shareMode, DWORD creationDisposition, DWORD flagsAndAttributes);
  bool Open(CFSTR fileName, DWORD creationDisposition);
  bool Create(CFSTR fileName, bool createAlways);
  bool CreateAlways(CFSTR fileName, DWORD flagsAndAttributes);

  bool SetTime(const FILETIME *cTime, const FILETIME *aTime, const FILETIME *mTime) throw();
  bool SetMTime(const FILETIME *mTime) throw();
  bool WritePart(const void *data, UInt32 size, UInt32 &processedSize) throw();
  bool Write(const void *data, UInt32 size, UInt32 &processedSize) throw();
  bool SetEndOfFile() throw();
  bool SetLength(UInt64 length) throw();
};

}}}

#endif
