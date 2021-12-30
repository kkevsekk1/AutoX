// Archive/IsoItem.h

#ifndef __ARCHIVE_ISO_ITEM_H
#define __ARCHIVE_ISO_ITEM_H

#include "../../../Common/MyString.h"
#include "../../../Common/MyBuffer.h"

#include "../../../Windows/TimeUtils.h"

#include "IsoHeader.h"

namespace NArchive {
namespace NIso {

struct CRecordingDateTime
{
  Byte Year;
  Byte Month;
  Byte Day;
  Byte Hour;
  Byte Minute;
  Byte Second;
  signed char GmtOffset; // min intervals from -48 (West) to +52 (East) recorded.
  
  bool GetFileTime(FILETIME &ft) const
  {
    UInt64 value;
    bool res = NWindows::NTime::GetSecondsSince1601(Year + 1900, Month, Day, Hour, Minute, Second, value);
    if (res)
    {
      value -= (Int64)((Int32)GmtOffset * 15 * 60);
      value *= 10000000;
    }
    ft.dwLowDateTime = (DWORD)value;
    ft.dwHighDateTime = (DWORD)(value >> 32);
    return res;
  }
};

struct CDirRecord
{
  UInt32 ExtentLocation;
  UInt32 Size;
  CRecordingDateTime DateTime;
  Byte FileFlags;
  Byte FileUnitSize;
  Byte InterleaveGapSize;
  Byte ExtendedAttributeRecordLen;
  UInt16 VolSequenceNumber;
  CByteBuffer FileId;
  CByteBuffer SystemUse;

  bool AreMultiPartEqualWith(const CDirRecord &a) const
  {
    return FileId == a.FileId
        && (FileFlags & (~NFileFlags::kNonFinalExtent)) ==
        (a.FileFlags & (~NFileFlags::kNonFinalExtent));
  }

  bool IsDir() const { return (FileFlags & NFileFlags::kDirectory) != 0; }
  bool IsNonFinalExtent() const { return (FileFlags & NFileFlags::kNonFinalExtent) != 0; }

  bool IsSystemItem() const
  {
    if (FileId.Size() != 1)
      return false;
    Byte b = *(const Byte *)FileId;
    return (b == 0 || b == 1);
  }

  const Byte* FindSuspName(unsigned skipSize, unsigned &lenRes) const
  {
    lenRes = 0;
    if (SystemUse.Size() < skipSize)
      return 0;
    const Byte *p = (const Byte *)SystemUse + skipSize;
    unsigned rem = (unsigned)(SystemUse.Size() - skipSize);
    while (rem >= 5)
    {
      unsigned len = p[2];
      if (len < 3 || len > rem)
        return 0;
      if (p[0] == 'N' && p[1] == 'M' && p[3] == 1)
      {
        if (len < 5)
          return 0; // Check it
        lenRes = len - 5;
        return p + 5;
      }
      p += len;
      rem -= len;
    }
    return 0;
  }

  const Byte* GetNameCur(bool checkSusp, int skipSize, unsigned &nameLenRes) const
  {
    const Byte *res = NULL;
    unsigned len = 0;
    if (checkSusp)
      res = FindSuspName(skipSize, len);
    if (!res)
    {
      res = (const Byte *)FileId;
      len = (unsigned)FileId.Size();
    }
    unsigned i;
    for (i = 0; i < len; i++)
      if (res[i] == 0)
        break;
    nameLenRes = i;
    return res;
  }


  bool CheckSusp(const Byte *p, unsigned &startPos) const
  {
    if (p[0] == 'S' &&
        p[1] == 'P' &&
        p[2] == 0x7 &&
        p[3] == 0x1 &&
        p[4] == 0xBE &&
        p[5] == 0xEF)
    {
      startPos = p[6];
      return true;
    }
    return false;
  }

  bool CheckSusp(unsigned &startPos) const
  {
    const Byte *p = (const Byte *)SystemUse;
    unsigned len = (int)SystemUse.Size();
    const unsigned kMinLen = 7;
    if (len < kMinLen)
      return false;
    if (CheckSusp(p, startPos))
      return true;
    const unsigned kOffset2 = 14;
    if (len < kOffset2 + kMinLen)
      return false;
    return CheckSusp(p + kOffset2, startPos);
  }
};

}}

#endif
