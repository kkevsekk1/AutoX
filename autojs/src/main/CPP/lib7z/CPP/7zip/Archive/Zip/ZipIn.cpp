// Archive/ZipIn.cpp

#include "StdAfx.h"

// #include <stdio.h>

#include "../../../Common/DynamicBuffer.h"
#include "../../../Common/IntToString.h"
#include "../../../Common/StringToInt.h"

#include "../../../Windows/PropVariant.h"

#include "../../Common/StreamUtils.h"

#include "../IArchive.h"

#include "ZipIn.h"

#define Get16(p) GetUi16(p)
#define Get32(p) GetUi32(p)
#define Get64(p) GetUi64(p)

#define G16(offs, v) v = Get16(p + (offs))
#define G32(offs, v) v = Get32(p + (offs))
#define G64(offs, v) v = Get64(p + (offs))

namespace NArchive {
namespace NZip {

struct CEcd
{
  UInt16 ThisDisk;
  UInt16 CdDisk;
  UInt16 NumEntries_in_ThisDisk;
  UInt16 NumEntries;
  UInt32 Size;
  UInt32 Offset;
  UInt16 CommentSize;
  
  bool IsEmptyArc() const
  {
    return ThisDisk == 0
        && CdDisk == 0
        && NumEntries_in_ThisDisk == 0
        && NumEntries == 0
        && Size == 0
        && Offset == 0 // test it
    ;
  }

  void Parse(const Byte *p); // (p) doesn't include signature
};

void CEcd::Parse(const Byte *p)
{
  // (p) doesn't include signature
  G16(0, ThisDisk);
  G16(2, CdDisk);
  G16(4, NumEntries_in_ThisDisk);
  G16(6, NumEntries);
  G32(8, Size);
  G32(12, Offset);
  G16(16, CommentSize);
}


void CCdInfo::ParseEcd32(const Byte *p)
{
  // (p) includes signature
  p += 4;
  G16(0, ThisDisk);
  G16(2, CdDisk);
  G16(4, NumEntries_in_ThisDisk);
  G16(6, NumEntries);
  G32(8, Size);
  G32(12, Offset);
  G16(16, CommentSize);
}

void CCdInfo::ParseEcd64e(const Byte *p)
{
  // (p) exclude signature
  G16(0, VersionMade);
  G16(2, VersionNeedExtract);
  G32(4, ThisDisk);
  G32(8, CdDisk);

  G64(12, NumEntries_in_ThisDisk);
  G64(20, NumEntries);
  G64(28, Size);
  G64(36, Offset);
}


struct CLocator
{
  UInt32 Ecd64Disk;
  UInt32 NumDisks;
  UInt64 Ecd64Offset;
  
  CLocator(): Ecd64Disk(0), NumDisks(0), Ecd64Offset(0) {}

  void Parse(const Byte *p)
  {
    G32(0, Ecd64Disk);
    G64(4, Ecd64Offset);
    G32(12, NumDisks);
  }
};




void CInArchive::ClearRefs()
{
  StreamRef.Release();
  Stream = NULL;
  StartStream = NULL;
  Callback = NULL;

  Vols.Clear();
}

void CInArchive::Close()
{
  _processedCnt = 0;
  IsArc = false;
  IsArcOpen = false;
  IsMultiVol = false;
  UseDisk_in_SingleVol = false;
  EcdVolIndex = 0;
  HeadersError = false;
  HeadersWarning = false;
  ExtraMinorError = false;
  UnexpectedEnd = false;
  NoCentralDir = false;
  IsZip64 = false;
  MarkerIsFound = false;
  
  ClearRefs();
}


HRESULT CInArchive::Seek(UInt64 offset)
{
  return Stream->Seek(offset, STREAM_SEEK_SET, NULL);
}


static bool CheckDosTime(UInt32 dosTime)
{
  if (dosTime == 0)
    return true;
  unsigned month = (dosTime >> 21) & 0xF;
  unsigned day = (dosTime >> 16) & 0x1F;
  unsigned hour = (dosTime >> 11) & 0x1F;
  unsigned min = (dosTime >> 5) & 0x3F;
  unsigned sec = (dosTime & 0x1F) * 2;
  if (month < 1 || month > 12 || day < 1 || day > 31 || hour > 23 || min > 59 || sec > 59)
    return false;
  return true;
}

API_FUNC_IsArc IsArc_Zip(const Byte *p, size_t size)
{
  if (size < 8)
    return k_IsArc_Res_NEED_MORE;
  if (p[0] != 'P')
    return k_IsArc_Res_NO;

  UInt32 value = Get32(p);

  if (value == NSignature::kNoSpan
     || value == NSignature::kSpan)
  {
    p += 4;
    size -= 4;
  }

  value = Get32(p);

  if (value == NSignature::kEcd)
  {
    if (size < kEcdSize)
      return k_IsArc_Res_NEED_MORE;
    CEcd ecd;
    ecd.Parse(p + 4);
    // if (ecd.cdSize != 0)
    if (!ecd.IsEmptyArc())
      return k_IsArc_Res_NO;
    return k_IsArc_Res_YES; // k_IsArc_Res_YES_2;
  }
  
  if (value != NSignature::kLocalFileHeader)
    return k_IsArc_Res_NO;

  if (size < kLocalHeaderSize)
    return k_IsArc_Res_NEED_MORE;
  
  p += 4;

  {
    const unsigned kPureHeaderSize = kLocalHeaderSize - 4;
    unsigned i;
    for (i = 0; i < kPureHeaderSize && p[i] == 0; i++);
    if (i == kPureHeaderSize)
      return k_IsArc_Res_NEED_MORE;
  }

  /*
  if (p[0] >= 128) // ExtractVersion.Version;
    return k_IsArc_Res_NO;
  */

  // ExtractVersion.Version = p[0];
  // ExtractVersion.HostOS = p[1];
  // Flags = Get16(p + 2);
  // Method = Get16(p + 4);
  /*
  // 9.33: some zip archives contain incorrect value in timestamp. So we don't check it now
  UInt32 dosTime = Get32(p + 6);
  if (!CheckDosTime(dosTime))
    return k_IsArc_Res_NO;
  */
  // Crc = Get32(p + 10);
  // PackSize = Get32(p + 14);
  // Size = Get32(p + 18);
  const unsigned nameSize = Get16(p + 22);
  unsigned extraSize = Get16(p + 24);
  const UInt32 extraOffset = kLocalHeaderSize + (UInt32)nameSize;
  if (extraOffset + extraSize > (1 << 16))
    return k_IsArc_Res_NO;

  p -= 4;

  {
    size_t rem = size - kLocalHeaderSize;
    if (rem > nameSize)
      rem = nameSize;
    const Byte *p2 = p + kLocalHeaderSize;
    for (size_t i = 0; i < rem; i++)
      if (p2[i] == 0)
        if (i != nameSize - 1)
          return k_IsArc_Res_NO;
  }

  if (size < extraOffset)
    return k_IsArc_Res_NEED_MORE;

  if (extraSize > 0)
  {
    p += extraOffset;
    size -= extraOffset;
    while (extraSize != 0)
    {
      if (extraSize < 4)
      {
        // 7-Zip before 9.31 created incorrect WsAES Extra in folder's local headers.
        // so we return k_IsArc_Res_YES to support such archives.
        // return k_IsArc_Res_NO; // do we need to support such extra ?
        return k_IsArc_Res_YES;
      }
      if (size < 4)
        return k_IsArc_Res_NEED_MORE;
      unsigned dataSize = Get16(p + 2);
      size -= 4;
      extraSize -= 4;
      p += 4;
      if (dataSize > extraSize)
        return k_IsArc_Res_NO;
      if (dataSize > size)
        return k_IsArc_Res_NEED_MORE;
      size -= dataSize;
      extraSize -= dataSize;
      p += dataSize;
    }
  }
  
  return k_IsArc_Res_YES;
}

static UInt32 IsArc_Zip_2(const Byte *p, size_t size, bool isFinal)
{
  UInt32 res = IsArc_Zip(p, size);
  if (res == k_IsArc_Res_NEED_MORE && isFinal)
    return k_IsArc_Res_NO;
  return res;
}



HRESULT CInArchive::FindMarker(IInStream *stream, const UInt64 *searchLimit)
{
  ArcInfo.MarkerPos = m_Position;
  ArcInfo.MarkerPos2 = m_Position;

  if (searchLimit && *searchLimit == 0)
  {
    Byte startBuf[kMarkerSize];
    {
      size_t processed = kMarkerSize;
      RINOK(ReadStream(stream, startBuf, &processed));
      m_Position += processed;
      if (processed != kMarkerSize)
        return S_FALSE;
    }

    m_Signature = Get32(startBuf);

    if (m_Signature != NSignature::kEcd &&
        m_Signature != NSignature::kLocalFileHeader)
    {
      if (m_Signature != NSignature::kNoSpan)
      {
        if (m_Signature != NSignature::kSpan)
          return S_FALSE;
        if (m_Position != 4) // we don't support multivol archives with sfx stub
          return S_FALSE;
        ArcInfo.IsSpanMode = true;
      }
      size_t processed = kMarkerSize;
      RINOK(ReadStream(stream, startBuf, &processed));
      m_Position += processed;
      if (processed != kMarkerSize)
        return S_FALSE;
      m_Signature = Get32(startBuf);
      if (m_Signature != NSignature::kEcd &&
          m_Signature != NSignature::kLocalFileHeader)
        return S_FALSE;
      ArcInfo.MarkerPos2 += 4;
    }

    // we use weak test in case of (*searchLimit == 0)
    // since error will be detected later in Open function
    return S_OK; // maybe we need to search backward.
  }

  const size_t kBufSize = (size_t)1 << 18; // must be larger than kCheckSize
  const size_t kCheckSize = (size_t)1 << 16; // must be smaller than kBufSize
  CByteArr buffer(kBufSize);
  
  size_t numBytesInBuffer = 0;
  UInt64 curScanPos = 0;

  for (;;)
  {
    size_t numReadBytes = kBufSize - numBytesInBuffer;
    RINOK(ReadStream(stream, buffer + numBytesInBuffer, &numReadBytes));
    m_Position += numReadBytes;
    numBytesInBuffer += numReadBytes;
    const bool isFinished = (numBytesInBuffer != kBufSize);
    
    size_t limit = numBytesInBuffer;;
    if (isFinished)
    {
      if (limit == 0)
        break;
      limit--;
    }
    else
      limit -= kCheckSize;

    if (searchLimit && curScanPos + limit > *searchLimit)
      limit = (size_t)(*searchLimit - curScanPos + 1);

    if (limit < 1)
      break;

    const Byte *buf = buffer;
    for (size_t pos = 0; pos < limit; pos++)
    {
      if (buf[pos] != 0x50)
        continue;
      if (buf[pos + 1] != 0x4B)
        continue;
      size_t rem = numBytesInBuffer - pos;
      UInt32 res = IsArc_Zip_2(buf + pos, rem, isFinished);
      if (res != k_IsArc_Res_NO)
      {
        if (rem < kMarkerSize)
          return S_FALSE;
        m_Signature = Get32(buf + pos);
        ArcInfo.MarkerPos += curScanPos + pos;
        ArcInfo.MarkerPos2 = ArcInfo.MarkerPos;
        if (m_Signature == NSignature::kNoSpan
            || m_Signature == NSignature::kSpan)
        {
          m_Signature = Get32(buf + pos + 4);
          ArcInfo.MarkerPos2 += 4;
        }
        m_Position = ArcInfo.MarkerPos2 + kMarkerSize;
        return S_OK;
      }
    }

    if (isFinished)
      break;

    curScanPos += limit;
    numBytesInBuffer -= limit;
    memmove(buffer, buffer + limit, numBytesInBuffer);
  }
  
  return S_FALSE;
}


HRESULT CInArchive::IncreaseRealPosition(Int64 addValue, bool &isFinished)
{
  isFinished = false;
  if (!IsMultiVol)
    return Stream->Seek(addValue, STREAM_SEEK_CUR, &m_Position);

  for (;;)
  {
    if (addValue == 0)
      return S_OK;
    if (addValue > 0)
    {
      if (Vols.StreamIndex < 0)
        return S_FALSE;
      if ((unsigned)Vols.StreamIndex >= Vols.Streams.Size())
      {
        isFinished = true;
        return S_OK;
      }
      {
        const CVols::CSubStreamInfo &s = Vols.Streams[Vols.StreamIndex];
        if (!s.Stream)
        {
          isFinished = true;
          return S_OK;
        }
        if (m_Position > s.Size)
          return S_FALSE;
        UInt64 rem = s.Size - m_Position;
        if ((UInt64)addValue <= rem)
          return Stream->Seek(addValue, STREAM_SEEK_CUR, &m_Position);
        RINOK(Stream->Seek(s.Size, STREAM_SEEK_SET, &m_Position));
        addValue -= rem;
        Stream = NULL;
        Vols.StreamIndex++;
        if ((unsigned)Vols.StreamIndex >= Vols.Streams.Size())
        {
          isFinished = true;
          return S_OK;
        }
      }
      const CVols::CSubStreamInfo &s2 = Vols.Streams[Vols.StreamIndex];
      if (!s2.Stream)
      {
        isFinished = true;
        return S_OK;
      }
      Stream = s2.Stream;
      m_Position = 0;
      RINOK(Stream->Seek(0, STREAM_SEEK_SET, &m_Position));
    }
    else
    {
      if (!Stream)
        return S_FALSE;
      {
        if (m_Position >= (UInt64)(-addValue))
          return Stream->Seek(addValue, STREAM_SEEK_CUR, &m_Position);
        addValue += m_Position;
        RINOK(Stream->Seek(0, STREAM_SEEK_SET, &m_Position));
        m_Position = 0;
        Stream = NULL;
        if (--Vols.StreamIndex < 0)
          return S_FALSE;
      }
      const CVols::CSubStreamInfo &s2 = Vols.Streams[Vols.StreamIndex];
      if (!s2.Stream)
        return S_FALSE;
      Stream = s2.Stream;
      m_Position = s2.Size;
      RINOK(Stream->Seek(s2.Size, STREAM_SEEK_SET, &m_Position));
    }
  }
}


class CUnexpectEnd {};


HRESULT CInArchive::ReadBytes(void *data, UInt32 size, UInt32 *processedSize)
{
  size_t realProcessedSize = size;
  HRESULT result = S_OK;
  if (_inBufMode)
  {
    try { realProcessedSize = _inBuffer.ReadBytes((Byte *)data, size); }
    catch (const CInBufferException &e) { return e.ErrorCode; }
  }
  else
    result = ReadStream(Stream, data, &realProcessedSize);
  if (processedSize)
    *processedSize = (UInt32)realProcessedSize;
  m_Position += realProcessedSize;
  return result;
}

void CInArchive::SafeReadBytes(void *data, unsigned size)
{
  size_t processed = size;
  
  HRESULT result = S_OK;

  if (!_inBufMode)
    result = ReadStream(Stream, data, &processed);
  else
  {
    for (;;)
    {
      processed = _inBuffer.ReadBytes((Byte *)data, size);
      if (processed != 0
          || IsMultiVol
          || !CanStartNewVol
          || Vols.StreamIndex < 0
          || (unsigned)Vols.StreamIndex >= Vols.Streams.Size())
        break;
      Vols.StreamIndex++;
      const CVols::CSubStreamInfo &s = Vols.Streams[Vols.StreamIndex];
      if (!s.Stream)
        break;
      // if (Vols.NeedSeek)
      {
        result = s.Stream->Seek(0, STREAM_SEEK_SET, NULL);
        m_Position = 0;
        if (result != S_OK)
          break;
        Vols.NeedSeek = false;
      }
      _inBuffer.SetStream(s.Stream);
      _inBuffer.Init();
    }
    CanStartNewVol = false;
  }

  m_Position += processed;
  _processedCnt += processed;

  if (result != S_OK)
    throw CSystemException(result);
  
  if (processed != size)
    throw CUnexpectEnd();
}

void CInArchive::ReadBuffer(CByteBuffer &buffer, unsigned size)
{
  buffer.Alloc(size);
  if (size > 0)
    SafeReadBytes(buffer, size);
}

Byte CInArchive::ReadByte()
{
  Byte b;
  SafeReadBytes(&b, 1);
  return b;
}

UInt16 CInArchive::ReadUInt16() { Byte buf[2]; SafeReadBytes(buf, 2); return Get16(buf); }
UInt32 CInArchive::ReadUInt32() { Byte buf[4]; SafeReadBytes(buf, 4); return Get32(buf); }
UInt64 CInArchive::ReadUInt64() { Byte buf[8]; SafeReadBytes(buf, 8); return Get64(buf); }

// we use Skip() inside headers only, so no need for stream change in multivol.

void CInArchive::Skip(unsigned num)
{
  if (_inBufMode)
  {
    size_t skip = _inBuffer.Skip(num);
    m_Position += skip;
    _processedCnt += skip;
    if (skip != num)
      throw CUnexpectEnd();
  }
  else
  {
    for (unsigned i = 0; i < num; i++)
      ReadByte();
  }
}

void CInArchive::Skip64(UInt64 num)
{
  for (UInt64 i = 0; i < num; i++)
    ReadByte();
}


void CInArchive::ReadFileName(unsigned size, AString &s)
{
  if (size == 0)
  {
    s.Empty();
    return;
  }
  SafeReadBytes(s.GetBuf(size), size);
  s.ReleaseBuf_CalcLen(size);
}


bool CInArchive::ReadExtra(unsigned extraSize, CExtraBlock &extraBlock,
    UInt64 &unpackSize, UInt64 &packSize, UInt64 &localHeaderOffset, UInt32 &diskStartNumber)
{
  extraBlock.Clear();
  
  UInt32 remain = extraSize;
  
  while (remain >= 4)
  {
    CExtraSubBlock subBlock;
    subBlock.ID = ReadUInt16();
    unsigned dataSize = ReadUInt16();
    remain -= 4;
    if (dataSize > remain) // it's bug
    {
      HeadersWarning = true;
      Skip(remain);
      return false;
    }
    if (subBlock.ID == NFileHeader::NExtraID::kZip64)
    {
      if (unpackSize == 0xFFFFFFFF)
      {
        if (dataSize < 8)
        {
          HeadersWarning = true;
          Skip(remain);
          return false;
        }
        unpackSize = ReadUInt64();
        remain -= 8;
        dataSize -= 8;
      }
      if (packSize == 0xFFFFFFFF)
      {
        if (dataSize < 8)
          break;
        packSize = ReadUInt64();
        remain -= 8;
        dataSize -= 8;
      }
      if (localHeaderOffset == 0xFFFFFFFF)
      {
        if (dataSize < 8)
          break;
        localHeaderOffset = ReadUInt64();
        remain -= 8;
        dataSize -= 8;
      }
      if (diskStartNumber == 0xFFFF)
      {
        if (dataSize < 4)
          break;
        diskStartNumber = ReadUInt32();
        remain -= 4;
        dataSize -= 4;
      }
      Skip(dataSize);
    }
    else
    {
      ReadBuffer(subBlock.Data, dataSize);
      extraBlock.SubBlocks.Add(subBlock);
    }
    remain -= dataSize;
  }

  if (remain != 0)
  {
    ExtraMinorError = true;
    // 7-Zip before 9.31 created incorrect WsAES Extra in folder's local headers.
    // so we don't return false, but just set warning flag
    // return false;
  }
  
  Skip(remain);
  return true;
}


bool CInArchive::ReadLocalItem(CItemEx &item)
{
  item.Disk = 0;
  if (IsMultiVol && Vols.StreamIndex >= 0)
    item.Disk = Vols.StreamIndex;
  const unsigned kPureHeaderSize = kLocalHeaderSize - 4;
  Byte p[kPureHeaderSize];
  SafeReadBytes(p, kPureHeaderSize);
  {
    unsigned i;
    for (i = 0; i < kPureHeaderSize && p[i] == 0; i++);
    if (i == kPureHeaderSize)
      return false;
  }

  item.ExtractVersion.Version = p[0];
  item.ExtractVersion.HostOS = p[1];
  G16(2, item.Flags);
  G16(4, item.Method);
  G32(6, item.Time);
  G32(10, item.Crc);
  G32(14, item.PackSize);
  G32(18, item.Size);
  const unsigned nameSize = Get16(p + 22);
  const unsigned extraSize = Get16(p + 24);
  ReadFileName(nameSize, item.Name);
  item.LocalFullHeaderSize = kLocalHeaderSize + (UInt32)nameSize + extraSize;

  /*
  if (item.IsDir())
    item.Size = 0; // check It
  */

  if (extraSize > 0)
  {
    UInt64 localHeaderOffset = 0;
    UInt32 diskStartNumber = 0;
    if (!ReadExtra(extraSize, item.LocalExtra, item.Size, item.PackSize,
        localHeaderOffset, diskStartNumber))
    {
      /* Most of archives are OK for Extra. But there are some rare cases
         that have error. And if error in first item, it can't open archive.
         So we ignore that error */
      // return false;
    }
  }
  
  if (!CheckDosTime(item.Time))
  {
    HeadersWarning = true;
    // return false;
  }
  
  if (item.Name.Len() != nameSize)
  {
    // we support "bad" archives with null-terminated name.
    if (item.Name.Len() + 1 != nameSize)
      return false;
    HeadersWarning = true;
  }
  
  return item.LocalFullHeaderSize <= ((UInt32)1 << 16);
}


static bool FlagsAreSame(const CItem &i1, const CItem &i2)
{
  if (i1.Method != i2.Method)
    return false;
  if (i1.Flags == i2.Flags)
    return true;
  UInt32 mask = 0xFFFF;
  switch (i1.Method)
  {
    case NFileHeader::NCompressionMethod::kDeflated:
      mask = 0x7FF9;
      break;
    default:
      if (i1.Method <= NFileHeader::NCompressionMethod::kImploded)
        mask = 0x7FFF;
  }

  // we can ignore utf8 flag, if name is ascii
  if ((i1.Flags ^ i2.Flags) & NFileHeader::NFlags::kUtf8)
    if (i1.Name.IsAscii() && i2.Name.IsAscii())
      mask &= ~NFileHeader::NFlags::kUtf8;
  
  return ((i1.Flags & mask) == (i2.Flags & mask));
}


// #ifdef _WIN32
static bool AreEqualPaths_IgnoreSlashes(const char *s1, const char *s2)
{
  for (;;)
  {
    char c1 = *s1++;
    char c2 = *s2++;
    if (c1 == c2)
    {
      if (c1 == 0)
        return true;
    }
    else
    {
      if (c1 == '\\') c1 = '/';
      if (c2 == '\\') c2 = '/';
      if (c1 != c2)
        return false;
    }
  }
}
// #endif


static bool AreItemsEqual(const CItemEx &localItem, const CItemEx &cdItem)
{
  if (!FlagsAreSame(cdItem, localItem))
    return false;
  if (!localItem.HasDescriptor())
  {
    if (cdItem.Crc != localItem.Crc ||
        cdItem.PackSize != localItem.PackSize ||
        cdItem.Size != localItem.Size)
      return false;
  }
  /* pkzip 2.50 creates incorrect archives. It uses
       - WIN encoding for name in local header
       - OEM encoding for name in central header
     We don't support these strange items. */

  /* if (cdItem.Name.Len() != localItem.Name.Len())
    return false;
  */
  if (cdItem.Name != localItem.Name)
  {
    // #ifdef _WIN32
    // some xap files use backslash in central dir items.
    // we can ignore such errors in windows, where all slashes are converted to backslashes
    unsigned hostOs = cdItem.GetHostOS();
    
    if (hostOs == NFileHeader::NHostOS::kFAT ||
        hostOs == NFileHeader::NHostOS::kNTFS)
    {
      if (!AreEqualPaths_IgnoreSlashes(cdItem.Name, localItem.Name))
      {
        // pkzip 2.50 uses DOS encoding in central dir and WIN encoding in local header.
        // so we ignore that error
        if (hostOs != NFileHeader::NHostOS::kFAT
            || cdItem.MadeByVersion.Version != 25)
          return false;
      }
    }
    /*
    else
    #endif
      return false;
    */
  }
  return true;
}


HRESULT CInArchive::ReadLocalItemAfterCdItem(CItemEx &item, bool &isAvail)
{
  isAvail = true;
  if (item.FromLocal)
    return S_OK;
  try
  {
    UInt64 offset = item.LocalHeaderPos;

    if (IsMultiVol)
    {
      if (item.Disk >= Vols.Streams.Size())
      {
        isAvail = false;
        return S_FALSE;
      }
      IInStream *str2 = Vols.Streams[item.Disk].Stream;
      if (!str2)
      {
        isAvail = false;
        return S_FALSE;
      }
      RINOK(str2->Seek(offset, STREAM_SEEK_SET, NULL));
      Stream = str2;
      Vols.StreamIndex = item.Disk;
    }
    else
    {
      if (UseDisk_in_SingleVol && item.Disk != EcdVolIndex)
      {
        isAvail = false;
        return S_FALSE;
      }
      Stream = StreamRef;

      offset += ArcInfo.Base;
      if (ArcInfo.Base < 0 && (Int64)offset < 0)
      {
        isAvail = false;
        return S_FALSE;
      }
      RINOK(Seek(offset));
    }


    CItemEx localItem;
    if (ReadUInt32() != NSignature::kLocalFileHeader)
      return S_FALSE;
    ReadLocalItem(localItem);
    if (!AreItemsEqual(localItem, item))
      return S_FALSE;
    item.LocalFullHeaderSize = localItem.LocalFullHeaderSize;
    item.LocalExtra = localItem.LocalExtra;
    item.FromLocal = true;
  }
  catch(...) { return S_FALSE; }
  return S_OK;
}


HRESULT CInArchive::ReadLocalItemDescriptor(CItemEx &item)
{
  const unsigned kBufSize = (1 << 12);
  Byte buf[kBufSize];
  
  UInt32 numBytesInBuffer = 0;
  UInt32 packedSize = 0;
  
  for (;;)
  {
    UInt32 processedSize;
    RINOK(ReadBytes(buf + numBytesInBuffer, kBufSize - numBytesInBuffer, &processedSize));
    numBytesInBuffer += processedSize;
    if (numBytesInBuffer < kDataDescriptorSize)
      return S_FALSE;
    
    UInt32 i;
    for (i = 0; i <= numBytesInBuffer - kDataDescriptorSize; i++)
    {
      // descriptor signature field is Info-ZIP's extension to pkware Zip specification.
      // New ZIP specification also allows descriptorSignature.
      if (buf[i] != 0x50)
        continue;
      // !!!! It must be fixed for Zip64 archives
      if (Get32(buf + i) == NSignature::kDataDescriptor)
      {
        UInt32 descriptorPackSize = Get32(buf + i + 8);
        if (descriptorPackSize == packedSize + i)
        {
          item.Crc = Get32(buf + i + 4);
          item.PackSize = descriptorPackSize;
          item.Size = Get32(buf + i + 12);
          bool isFinished;
          return IncreaseRealPosition((Int64)(Int32)(0 - (numBytesInBuffer - i - kDataDescriptorSize)), isFinished);
        }
      }
    }
    
    packedSize += i;
    unsigned j;
    for (j = 0; i < numBytesInBuffer; i++, j++)
      buf[j] = buf[i];
    numBytesInBuffer = j;
  }
}


HRESULT CInArchive::ReadLocalItemAfterCdItemFull(CItemEx &item)
{
  if (item.FromLocal)
    return S_OK;
  try
  {
    bool isAvail = true;
    RINOK(ReadLocalItemAfterCdItem(item, isAvail));
    if (item.HasDescriptor())
    {
      // pkzip's version without descriptor is not supported
      RINOK(Seek(ArcInfo.Base + item.GetDataPosition() + item.PackSize));
      if (ReadUInt32() != NSignature::kDataDescriptor)
        return S_FALSE;
      UInt32 crc = ReadUInt32();
      UInt64 packSize, unpackSize;

      /*
      if (IsZip64)
      {
        packSize = ReadUInt64();
        unpackSize = ReadUInt64();
      }
      else
      */
      {
        packSize = ReadUInt32();
        unpackSize = ReadUInt32();
      }

      if (crc != item.Crc || item.PackSize != packSize || item.Size != unpackSize)
        return S_FALSE;
    }
  }
  catch(...) { return S_FALSE; }
  return S_OK;
}
  

HRESULT CInArchive::ReadCdItem(CItemEx &item)
{
  item.FromCentral = true;
  Byte p[kCentralHeaderSize - 4];
  SafeReadBytes(p, kCentralHeaderSize - 4);

  item.MadeByVersion.Version = p[0];
  item.MadeByVersion.HostOS = p[1];
  item.ExtractVersion.Version = p[2];
  item.ExtractVersion.HostOS = p[3];
  G16(4, item.Flags);
  G16(6, item.Method);
  G32(8, item.Time);
  G32(12, item.Crc);
  G32(16, item.PackSize);
  G32(20, item.Size);
  const unsigned nameSize = Get16(p + 24);
  const unsigned extraSize = Get16(p + 26);
  const unsigned commentSize = Get16(p + 28);
  G16(30, item.Disk);
  G16(32, item.InternalAttrib);
  G32(34, item.ExternalAttrib);
  G32(38, item.LocalHeaderPos);
  ReadFileName(nameSize, item.Name);
  
  if (extraSize > 0)
    ReadExtra(extraSize, item.CentralExtra, item.Size, item.PackSize, item.LocalHeaderPos, item.Disk);

  // May be these strings must be deleted
  /*
  if (item.IsDir())
    item.Size = 0;
  */
  
  ReadBuffer(item.Comment, commentSize);
  return S_OK;
}


HRESULT CInArchive::TryEcd64(UInt64 offset, CCdInfo &cdInfo)
{
  if (offset >= ((UInt64)1 << 63))
    return S_FALSE;
  RINOK(Seek(offset));
  Byte buf[kEcd64_FullSize];

  RINOK(ReadStream_FALSE(Stream, buf, kEcd64_FullSize));

  if (Get32(buf) != NSignature::kEcd64)
    return S_FALSE;
  UInt64 mainSize = Get64(buf + 4);
  if (mainSize < kEcd64_MainSize || mainSize > ((UInt64)1 << 32))
    return S_FALSE;
  cdInfo.ParseEcd64e(buf + 12);
  return S_OK;
}


HRESULT CInArchive::FindCd(bool checkOffsetMode)
{
  CCdInfo &cdInfo = Vols.ecd;

  UInt64 endPos;
  
  RINOK(Stream->Seek(0, STREAM_SEEK_END, &endPos));
  
  const UInt32 kBufSizeMax = ((UInt32)1 << 16) + kEcdSize + kEcd64Locator_Size + kEcd64_FullSize;
  const UInt32 bufSize = (endPos < kBufSizeMax) ? (UInt32)endPos : kBufSizeMax;
  if (bufSize < kEcdSize)
    return S_FALSE;
  CByteArr byteBuffer(bufSize);

  const UInt64 startPos = endPos - bufSize;
  RINOK(Stream->Seek(startPos, STREAM_SEEK_SET, &m_Position));
  if (m_Position != startPos)
    return S_FALSE;
  
  RINOK(ReadStream_FALSE(Stream, byteBuffer, bufSize));
  
  for (UInt32 i = bufSize - kEcdSize + 1;;)
  {
    if (i == 0)
      return S_FALSE;
    
    const Byte *buf = byteBuffer;

    for (;;)
    {
      i--;
      if (buf[i] == 0x50)
        break;
      if (i == 0)
        return S_FALSE;
    }
    
    if (Get32(buf + i) != NSignature::kEcd)
      continue;

    cdInfo.ParseEcd32(buf + i);
    
    if (i >= kEcd64Locator_Size)
    {
      const Byte *locatorPtr = buf + i - kEcd64Locator_Size;
      if (Get32(locatorPtr) == NSignature::kEcd64Locator)
      {
        CLocator locator;
        locator.Parse(locatorPtr + 4);
        if ((cdInfo.ThisDisk == locator.NumDisks - 1 || cdInfo.ThisDisk == 0xFFFF)
            && locator.Ecd64Disk < locator.NumDisks)
        {
          if (locator.Ecd64Disk != cdInfo.ThisDisk && cdInfo.ThisDisk != 0xFFFF)
            return E_NOTIMPL;
          
          // Most of the zip64 use fixed size Zip64 ECD
          // we try relative backward reading.

          UInt64 absEcd64 = endPos - bufSize + i - (kEcd64Locator_Size + kEcd64_FullSize);
          if (checkOffsetMode || absEcd64 == locator.Ecd64Offset)
          {
            const Byte *ecd64 = locatorPtr - kEcd64_FullSize;
            if (Get32(ecd64) == NSignature::kEcd64)
            {
              UInt64 mainEcd64Size = Get64(ecd64 + 4);
              if (mainEcd64Size == kEcd64_MainSize)
              {
                cdInfo.ParseEcd64e(ecd64 + 12);
                ArcInfo.Base = absEcd64 - locator.Ecd64Offset;
                // ArcInfo.BaseVolIndex = cdInfo.ThisDisk;
                return S_OK;
              }
            }
          }
          
          // some zip64 use variable size Zip64 ECD.
          // we try to use absolute offset from locator.

          if (absEcd64 != locator.Ecd64Offset)
          {
            if (TryEcd64(locator.Ecd64Offset, cdInfo) == S_OK)
            {
              ArcInfo.Base = 0;
              // ArcInfo.BaseVolIndex = cdInfo.ThisDisk;
              return S_OK;
            }
          }
          
          // for variable Zip64 ECD with for archives with offset != 0.

          if (checkOffsetMode
              && ArcInfo.MarkerPos != 0
              && ArcInfo.MarkerPos + locator.Ecd64Offset != absEcd64)
          {
            if (TryEcd64(ArcInfo.MarkerPos + locator.Ecd64Offset, cdInfo) == S_OK)
            {
              ArcInfo.Base = ArcInfo.MarkerPos;
              // ArcInfo.BaseVolIndex = cdInfo.ThisDisk;
              return S_OK;
            }
          }
        }
      }
    }
    
    // bool isVolMode = (Vols.EndVolIndex != -1);
    // UInt32 searchDisk = (isVolMode ? Vols.EndVolIndex : 0);
    
    if (/* searchDisk == thisDisk && */ cdInfo.CdDisk <= cdInfo.ThisDisk)
    {
      // if (isVolMode)
      {
        if (cdInfo.CdDisk != cdInfo.ThisDisk)
          return S_OK;
      }
      
      UInt64 absEcdPos = endPos - bufSize + i;
      UInt64 cdEnd = cdInfo.Size + cdInfo.Offset;
      ArcInfo.Base = 0;
      // ArcInfo.BaseVolIndex = cdInfo.ThisDisk;
      if (absEcdPos != cdEnd)
      {
        /*
        if (cdInfo.Offset <= 16 && cdInfo.Size != 0)
        {
          // here we support some rare ZIP files with Central directory at the start
          ArcInfo.Base = 0;
        }
        else
        */
        ArcInfo.Base = absEcdPos - cdEnd;
      }
      return S_OK;
    }
  }
}


HRESULT CInArchive::TryReadCd(CObjectVector<CItemEx> &items, const CCdInfo &cdInfo, UInt64 cdOffset, UInt64 cdSize)
{
  items.Clear();

  ISequentialInStream *stream;
  
  if (!IsMultiVol)
  {
    stream = this->StartStream;
    Vols.StreamIndex = -1;
    RINOK(this->StartStream->Seek(cdOffset, STREAM_SEEK_SET, &m_Position));
    if (m_Position != cdOffset)
      return S_FALSE;
  }
  else
  {
    if (cdInfo.CdDisk >= Vols.Streams.Size())
      return S_FALSE;
    IInStream *str2 = Vols.Streams[cdInfo.CdDisk].Stream;
    if (!str2)
      return S_FALSE;
    RINOK(str2->Seek(cdOffset, STREAM_SEEK_SET, NULL));
    stream = str2;
    Vols.NeedSeek = false;
    Vols.StreamIndex = cdInfo.CdDisk;
    m_Position = cdOffset;
  }

  _inBuffer.SetStream(stream);

  _inBuffer.Init();
  _inBufMode = true;

  _processedCnt = 0;

  while (_processedCnt < cdSize)
  {
    CanStartNewVol = true;
    if (ReadUInt32() != NSignature::kCentralFileHeader)
      return S_FALSE;
    {
      CItemEx cdItem;
      RINOK(ReadCdItem(cdItem));
      items.Add(cdItem);
    }
    if (Callback && (items.Size() & 0xFFF) == 0)
    {
      const UInt64 numFiles = items.Size();
      RINOK(Callback->SetCompleted(&numFiles, NULL));
    }
  }

  CanStartNewVol = true;

  return (_processedCnt == cdSize) ? S_OK : S_FALSE;
}


HRESULT CInArchive::ReadCd(CObjectVector<CItemEx> &items, UInt32 &cdDisk, UInt64 &cdOffset, UInt64 &cdSize)
{
  bool checkOffsetMode = true;
  
  if (IsMultiVol)
  {
    if (Vols.EndVolIndex == -1)
      return S_FALSE;
    Stream = Vols.Streams[Vols.EndVolIndex].Stream;
    if (!Vols.StartIsZip)
      checkOffsetMode = false;
  }
  else
    Stream = StartStream;

  if (!Vols.ecd_wasRead)
  {
    RINOK(FindCd(checkOffsetMode));
  }

  CCdInfo &cdInfo = Vols.ecd;
  
  HRESULT res = S_FALSE;
  
  cdSize = cdInfo.Size;
  cdOffset = cdInfo.Offset;
  cdDisk = cdInfo.CdDisk;

  if (Callback)
  {
    RINOK(Callback->SetTotal(&cdInfo.NumEntries, NULL));
  }
  
  const UInt64 base = (IsMultiVol ? 0 : ArcInfo.Base);
  res = TryReadCd(items, cdInfo, base + cdOffset, cdSize);
  
  if (res == S_FALSE && !IsMultiVol && base != ArcInfo.MarkerPos)
  {
    // do we need that additional attempt to read cd?
    res = TryReadCd(items, cdInfo, ArcInfo.MarkerPos + cdOffset, cdSize);
    if (res == S_OK)
      ArcInfo.Base = ArcInfo.MarkerPos;
  }
  
  return res;
}


static int FindItem(const CObjectVector<CItemEx> &items, const CItemEx &item)
{
  unsigned left = 0, right = items.Size();
  for (;;)
  {
    if (left >= right)
      return -1;
    unsigned index = (left + right) / 2;
    const CItemEx &item2 = items[index];
    if (item.Disk < item2.Disk)
      right = index;
    else if (item.Disk > item2.Disk)
      left = index + 1;
    else if (item.LocalHeaderPos == item2.LocalHeaderPos)
      return index;
    else if (item.LocalHeaderPos < item2.LocalHeaderPos)
      right = index;
    else
      left = index + 1;
  }
}

static bool IsStrangeItem(const CItem &item)
{
  return item.Name.Len() > (1 << 14) || item.Method > (1 << 8);
}


HRESULT CInArchive::ReadLocals(CObjectVector<CItemEx> &items)
{
  items.Clear();
  
  while (m_Signature == NSignature::kLocalFileHeader)
  {
    CItemEx item;
    item.LocalHeaderPos = m_Position - 4;
    if (!IsMultiVol)
      item.LocalHeaderPos -= ArcInfo.MarkerPos;

    // we write ralative LocalHeaderPos here. Later we can correct it to real Base.
    
    try
    {
      ReadLocalItem(item);
      item.FromLocal = true;
      bool isFinished = false;
  
      if (item.HasDescriptor())
        ReadLocalItemDescriptor(item);
      else
      {
        /*
        if (IsMultiVol)
        {
          const int kStep = 10000;
          RINOK(IncreaseRealPosition(-kStep, isFinished));
          RINOK(IncreaseRealPosition(item.PackSize + kStep, isFinished));
        }
        else
        */
        RINOK(IncreaseRealPosition(item.PackSize, isFinished));
      }
      
      items.Add(item);
      
      if (isFinished)
        throw CUnexpectEnd();
      
      m_Signature = ReadUInt32();
    }
    catch (CUnexpectEnd &)
    {
      if (items.IsEmpty() || items.Size() == 1 && IsStrangeItem(items[0]))
        return S_FALSE;
      throw;
    }

    if (Callback && (items.Size() & 0xFF) == 0)
    {
      const UInt64 numFiles = items.Size();
      UInt64 numBytes = 0;
      // if (!sMultiVol)
        numBytes = item.LocalHeaderPos;
      RINOK(Callback->SetCompleted(&numFiles, &numBytes));
    }
  }

  if (items.Size() == 1 && m_Signature != NSignature::kCentralFileHeader)
    if (IsStrangeItem(items[0]))
      return S_FALSE;
  
  return S_OK;
}



HRESULT CVols::ParseArcName(IArchiveOpenVolumeCallback *volCallback)
{
  UString name;
  {
    NWindows::NCOM::CPropVariant prop;
    RINOK(volCallback->GetProperty(kpidName, &prop));
    if (prop.vt != VT_BSTR)
      return S_OK;
    name = prop.bstrVal;
  }

  UString base = name;
  int dotPos = name.ReverseFind_Dot();

  if (dotPos < 0)
    return S_OK;

  base.DeleteFrom(dotPos + 1);

  const UString ext = name.Ptr(dotPos + 1);
  StartVolIndex = (Int32)(-1);

  if (ext.IsEmpty())
    return S_OK;
  else
  {
    wchar_t c = ext[0];
    IsUpperCase = (c >= 'A' && c <= 'Z');
    if (ext.IsEqualTo_Ascii_NoCase("zip"))
    {
      BaseName = base;
      StartIsZ = true;
      StartIsZip = true;
      return S_OK;
    }
    else if (ext.IsEqualTo_Ascii_NoCase("exe"))
    {
      StartIsExe = true;
      BaseName = base;
      StartVolIndex = 0;
    }
    else if (ext[0] == 'z' || ext[0] == 'Z')
    {
      if (ext.Len() < 3)
        return S_OK;
      const wchar_t *end = NULL;
      UInt32 volNum = ConvertStringToUInt32(ext.Ptr(1), &end);
      if (*end != 0 || volNum < 1 || volNum > ((UInt32)1 << 30))
        return S_OK;
      StartVolIndex = volNum - 1;
      BaseName = base;
      StartIsZ = true;
    }
    else
      return S_OK;
  }

  UString volName = BaseName;
  volName.AddAscii(IsUpperCase ? "ZIP" : "zip");
  HRESULT result = volCallback->GetStream(volName, &ZipStream);
  if (result == S_FALSE || !ZipStream)
  {
    if (MissingName.IsEmpty())
      MissingName = volName;
    return S_OK;
  }

  return result;
}


HRESULT CInArchive::ReadVols2(IArchiveOpenVolumeCallback *volCallback,
    unsigned start, int lastDisk, int zipDisk, unsigned numMissingVolsMax, unsigned &numMissingVols)
{
  numMissingVols = 0;

  for (unsigned i = start;; i++)
  {
    if (lastDisk >= 0 && i >= (unsigned)lastDisk)
      break;
    
    if (i < Vols.Streams.Size())
      if (Vols.Streams[i].Stream)
        continue;

    CMyComPtr<IInStream> stream;

    if ((int)i == zipDisk)
    {
      stream = Vols.ZipStream;
    }
    else if ((int)i == Vols.StartVolIndex)
    {
      stream = StartStream;
    }
    else
    {
      UString volName = Vols.BaseName;
      {
        volName += (wchar_t)(Vols.IsUpperCase ? 'Z' : 'z');
        {
          char s[32];
          ConvertUInt32ToString(i + 1, s);
          unsigned len = (unsigned)strlen(s);
          while (len < 2)
          {
            volName += (wchar_t)'0';
            len++;
          }
          volName.AddAscii(s);
        }
      }
        
      HRESULT result = volCallback->GetStream(volName, &stream);
      if (result != S_OK && result != S_FALSE)
        return result;
      if (result == S_FALSE || !stream)
      {
        if (Vols.MissingName.IsEmpty())
          Vols.MissingName = volName;
        numMissingVols++;
        if (numMissingVols > numMissingVolsMax)
          return S_OK;
        if (lastDisk == -1 && numMissingVols != 0)
          return S_OK;
        continue;
      }
    }

    UInt64 size;

    UInt64 pos;
    RINOK(stream->Seek(0, STREAM_SEEK_CUR, &pos));
    RINOK(stream->Seek(0, STREAM_SEEK_END, &size));
    RINOK(stream->Seek(pos, STREAM_SEEK_SET, NULL));

    while (i >= Vols.Streams.Size())
      Vols.Streams.AddNew();
    
    CVols::CSubStreamInfo &ss = Vols.Streams[i];
    Vols.NumVols++;
    ss.Stream = stream;
    ss.Size = size;

    if ((int)i == zipDisk)
    {
      Vols.EndVolIndex = Vols.Streams.Size() - 1;
      break;
    }
  }

  return S_OK;
}


HRESULT CInArchive::ReadVols()
{
  CMyComPtr<IArchiveOpenVolumeCallback> volCallback;

  Callback->QueryInterface(IID_IArchiveOpenVolumeCallback, (void **)&volCallback);
  if (!volCallback)
    return S_OK;

  RINOK(Vols.ParseArcName(volCallback));

  int startZIndex = Vols.StartVolIndex;

  if (!Vols.StartIsZ)
  {
    // if (!Vols.StartIsExe)
      return S_OK;
  }

  int zipDisk = -1;
  int cdDisk = -1;

  if (Vols.StartIsZip)
    Vols.ZipStream = StartStream;

  // bool cdOK = false;

  if (Vols.ZipStream)
  {
    Stream = Vols.ZipStream;
    HRESULT res = FindCd(true);
    CCdInfo &ecd = Vols.ecd;
    if (res == S_OK)
    {
      zipDisk = ecd.ThisDisk;
      Vols.ecd_wasRead = true;
      if (ecd.ThisDisk == 0
          || ecd.ThisDisk >= ((UInt32)1 << 30)
          || ecd.ThisDisk < ecd.CdDisk)
        return S_OK;
      cdDisk = ecd.CdDisk;
      if (Vols.StartVolIndex < 0)
        Vols.StartVolIndex = ecd.ThisDisk;
      // Vols.StartVolIndex = ecd.ThisDisk;
      // Vols.EndVolIndex = ecd.ThisDisk;
      unsigned numMissingVols;
      if (cdDisk == zipDisk)
      {
        // cdOK = true;
      }
      else
      {
        RINOK(ReadVols2(volCallback, cdDisk, zipDisk, zipDisk, 0, numMissingVols));
        if (numMissingVols == 0)
        {
          // cdOK = false;
        }
      }
    }
    else if (res != S_FALSE)
      return res;
  }

  if (Vols.Streams.Size() > 0)
    IsMultiVol = true;
  
  if (Vols.StartVolIndex < 0)
    return S_OK;

  unsigned numMissingVols;

  if (cdDisk != 0)
  {
    RINOK(ReadVols2(volCallback, 0, cdDisk < 0 ? -1 : cdDisk, zipDisk, 1 << 10, numMissingVols));
  }

  if (Vols.ZipStream)
  {
    if (Vols.Streams.IsEmpty())
      if (zipDisk > (1 << 10))
        return S_OK;
    RINOK(ReadVols2(volCallback, zipDisk, zipDisk + 1, zipDisk, 0, numMissingVols));
  }

  if (!Vols.Streams.IsEmpty())
  {
    IsMultiVol = true;
    /*
    if (cdDisk)
      IsMultiVol = true;
    */
    if (startZIndex >= 0)
    {
      if (Vols.Streams.Size() >= (unsigned)startZIndex)
      {
        for (unsigned i = 0; i < (unsigned)startZIndex; i++)
          if (!Vols.Streams[i].Stream)
          {
            Vols.StartParsingVol = startZIndex;
            break;
          }
      }
    }
  }

  return S_OK;
}







HRESULT CVols::Read(void *data, UInt32 size, UInt32 *processedSize)
{
  if (processedSize)
    *processedSize = 0;
  if (size == 0)
    return S_OK;

  for (;;)
  {
    if (StreamIndex < 0)
      return S_OK;
    if ((unsigned)StreamIndex >= Streams.Size())
      return S_OK;
    const CVols::CSubStreamInfo &s = Streams[StreamIndex];
    if (!s.Stream)
      return S_FALSE;
    if (NeedSeek)
    {
      RINOK(s.Stream->Seek(0, STREAM_SEEK_SET, NULL));
      NeedSeek = false;
    }
    UInt32 realProcessedSize = 0;
    HRESULT res = s.Stream->Read(data, size, &realProcessedSize);
    if (processedSize)
      *processedSize = realProcessedSize;
    if (res != S_OK)
      return res;
    if (realProcessedSize != 0)
      return res;
    StreamIndex++;
    NeedSeek = true;
  }
}

STDMETHODIMP CVolStream::Read(void *data, UInt32 size, UInt32 *processedSize)
{
  return Vols->Read(data, size, processedSize);
}




#define COPY_ECD_ITEM_16(n) if (!isZip64 || ecd. n != 0xFFFF)     ecd64. n = ecd. n;
#define COPY_ECD_ITEM_32(n) if (!isZip64 || ecd. n != 0xFFFFFFFF) ecd64. n = ecd. n;


HRESULT CInArchive::ReadHeaders2(CObjectVector<CItemEx> &items)
{
  HRESULT res = S_OK;

  bool localsWereRead = false;
  UInt64 cdSize = 0, cdRelatOffset = 0, cdAbsOffset = 0;
  UInt32 cdDisk = 0;

  if (!_inBuffer.Create(1 << 15))
    return E_OUTOFMEMORY;

  if (!MarkerIsFound)
  {
    IsArc = true;
    res = ReadCd(items, cdDisk, cdRelatOffset, cdSize);
    if (res == S_OK)
      m_Signature = ReadUInt32();
  }
  else
  {
 
  // m_Signature must be kLocalFileHeader or kEcd
  // m_Position points to next byte after signature
  RINOK(Stream->Seek(m_Position, STREAM_SEEK_SET, NULL));

  _inBuffer.SetStream(Stream);

  bool needReadCd = true;

  if (m_Signature == NSignature::kEcd)
  {
    // It must be empty archive or backware archive
    // we don't support backware archive still
    
    const unsigned kBufSize = kEcdSize - 4;
    Byte buf[kBufSize];
    SafeReadBytes(buf, kBufSize);
    CEcd ecd;
    ecd.Parse(buf);
    // if (ecd.cdSize != 0)
    // Do we need also to support the case where empty zip archive with PK00 uses cdOffset = 4 ??
    if (!ecd.IsEmptyArc())
      return S_FALSE;

    ArcInfo.Base = ArcInfo.MarkerPos;
    needReadCd = false;
    IsArc = true; // check it: we need more tests?
    RINOK(Stream->Seek(ArcInfo.MarkerPos2 + 4, STREAM_SEEK_SET, &m_Position));
  }

  if (needReadCd)
  {
    CItemEx firstItem;
    // try
    {
      try
      {
        if (!ReadLocalItem(firstItem))
          return S_FALSE;
      }
      catch(CUnexpectEnd &)
      {
        return S_FALSE;
      }

      IsArc = true;
      res = ReadCd(items, cdDisk, cdRelatOffset, cdSize);
      if (res == S_OK)
        m_Signature = ReadUInt32();
    }
    // catch() { res = S_FALSE; }
    if (res != S_FALSE && res != S_OK)
      return res;

    if (res == S_OK && items.Size() == 0)
      res = S_FALSE;

    if (res == S_OK)
    {
      // we can't read local items here to keep _inBufMode state
      if ((Int64)ArcInfo.MarkerPos2 < ArcInfo.Base)
        res = S_FALSE;
      else
      {
        firstItem.LocalHeaderPos = ArcInfo.MarkerPos2 - ArcInfo.Base;
        int index = FindItem(items, firstItem);
        if (index == -1)
          res = S_FALSE;
        else if (!AreItemsEqual(firstItem, items[index]))
          res = S_FALSE;
        else
        {
          ArcInfo.CdWasRead = true;
          ArcInfo.FirstItemRelatOffset = items[0].LocalHeaderPos;
        }
      }
    }
  }
  }



  CObjectVector<CItemEx> cdItems;

  bool needSetBase = false;
  unsigned numCdItems = items.Size();
  
  if (res == S_FALSE)
  {
    // CD doesn't match firstItem,
    // so we clear items and read Locals.
    items.Clear();
    localsWereRead = true;
    _inBufMode = false;
    ArcInfo.Base = ArcInfo.MarkerPos;

    if (IsMultiVol)
    {
      Vols.StreamIndex = Vols.StartParsingVol;
      if (Vols.StartParsingVol >= (int)Vols.Streams.Size())
        return S_FALSE;
      Stream = Vols.Streams[Vols.StartParsingVol].Stream;
      if (!Stream)
        return S_FALSE;
    }

    RINOK(Stream->Seek(ArcInfo.MarkerPos2, STREAM_SEEK_SET, &m_Position));
    m_Signature = ReadUInt32();
    
    RINOK(ReadLocals(items));

    if (m_Signature != NSignature::kCentralFileHeader)
    {
      // if (!UnexpectedEnd)
        m_Position -= 4;
      NoCentralDir = true;
      HeadersError = true;
      return S_OK;
    }
    
    _inBufMode = true;
    _inBuffer.Init();
    
    cdAbsOffset = m_Position - 4;
    cdDisk = Vols.StreamIndex;

    for (;;)
    {
      CItemEx cdItem;
      CanStartNewVol = true;
      
      RINOK(ReadCdItem(cdItem));
      
      cdItems.Add(cdItem);
      if (Callback && (cdItems.Size() & 0xFFF) == 0)
      {
        const UInt64 numFiles = items.Size();
        RINOK(Callback->SetCompleted(&numFiles, NULL));
      }
      CanStartNewVol = true;
      m_Signature = ReadUInt32();
      if (m_Signature != NSignature::kCentralFileHeader)
        break;
    }
    
    cdSize = (m_Position - 4) - cdAbsOffset;
    needSetBase = true;
    numCdItems = cdItems.Size();

    if (!cdItems.IsEmpty())
    {
      ArcInfo.CdWasRead = true;
      ArcInfo.FirstItemRelatOffset = cdItems[0].LocalHeaderPos;
    }
  }

  
  
  CCdInfo ecd64;
  CLocator locator;
  bool isZip64 = false;
  const UInt64 ecd64AbsOffset = m_Position - 4;
  int ecd64Disk = -1;
  
  if (m_Signature == NSignature::kEcd64)
  {
    ecd64Disk = Vols.StreamIndex;

    IsZip64 = isZip64 = true;

    {
      const UInt64 recordSize = ReadUInt64();
      if (recordSize < kEcd64_MainSize)
      {
        HeadersError = true;
        return S_OK;
      }
      
      {
        const unsigned kBufSize = kEcd64_MainSize;
        Byte buf[kBufSize];
        SafeReadBytes(buf, kBufSize);
        ecd64.ParseEcd64e(buf);
      }
      
      Skip64(recordSize - kEcd64_MainSize);
    }


    m_Signature = ReadUInt32();

    if (m_Signature != NSignature::kEcd64Locator)
    {
      HeadersError = true;
      return S_OK;
    }
  
    {
      const unsigned kBufSize = 16;
      Byte buf[kBufSize];
      SafeReadBytes(buf, kBufSize);
      locator.Parse(buf);
    }

    m_Signature = ReadUInt32();
  }
  
  
  if (m_Signature != NSignature::kEcd)
  {
    HeadersError = true;
    return S_OK;
  }

  
  // ---------- ECD ----------

  CEcd ecd;
  {
    const unsigned kBufSize = kEcdSize - 4;
    Byte buf[kBufSize];
    SafeReadBytes(buf, kBufSize);
    ecd.Parse(buf);
  }

  COPY_ECD_ITEM_16(ThisDisk);
  COPY_ECD_ITEM_16(CdDisk);
  COPY_ECD_ITEM_16(NumEntries_in_ThisDisk);
  COPY_ECD_ITEM_16(NumEntries);
  COPY_ECD_ITEM_32(Size);
  COPY_ECD_ITEM_32(Offset);

  if (IsMultiVol)
  {
    if (cdDisk != (int)ecd64.CdDisk)
      HeadersError = true;
  }
  else if (needSetBase)
  {
    if (isZip64)
    {
      if (ecd64Disk == Vols.StartVolIndex)
      {
        ArcInfo.Base = ecd64AbsOffset - locator.Ecd64Offset;
        // cdRelatOffset = ecd64.Offset;
        needSetBase = false;
      }
    }
    else
    {
      if ((int)cdDisk == Vols.StartVolIndex)
      {
        ArcInfo.Base = cdAbsOffset - ecd64.Offset;
        cdRelatOffset = ecd64.Offset;
        needSetBase = false;
      }
    }
  }

  EcdVolIndex = ecd64.ThisDisk;

  if (!IsMultiVol)
  {
    UseDisk_in_SingleVol = true;

    if (localsWereRead)
    {
      if ((UInt64)ArcInfo.Base != ArcInfo.MarkerPos)
      {
        const UInt64 delta = ArcInfo.MarkerPos - ArcInfo.Base;
        FOR_VECTOR (i, items)
          items[i].LocalHeaderPos += delta;
      }
      
      if (EcdVolIndex != 0)
      {
        FOR_VECTOR (i, items)
          items[i].Disk = EcdVolIndex;
      }
    }
  }

  if (isZip64)
  {
    if (ecd64.ThisDisk == 0 && ecd64AbsOffset != ArcInfo.Base + locator.Ecd64Offset
        // || ecd64.NumEntries_in_ThisDisk != numCdItems
        || ecd64.NumEntries != numCdItems
        || ecd64.Size != cdSize
        || (ecd64.Offset != cdRelatOffset && !items.IsEmpty()))
    {
      HeadersError = true;
      return S_OK;
    }
  }

  // ---------- merge Central Directory Items ----------

  if (!cdItems.IsEmpty())
  {
    CObjectVector<CItemEx> items2;

    FOR_VECTOR (i, cdItems)
    {
      const CItemEx &cdItem = cdItems[i];
      int index = FindItem(items, cdItem);
      if (index == -1)
      {
        items2.Add(cdItem);
        HeadersError = true;
        continue;
      }
      CItemEx &item = items[index];
      if (item.Name != cdItem.Name
          // || item.Name.Len() != cdItem.Name.Len()
          || item.PackSize != cdItem.PackSize
          || item.Size != cdItem.Size
          // item.ExtractVersion != cdItem.ExtractVersion
          || !FlagsAreSame(item, cdItem)
          || item.Crc != cdItem.Crc)
      {
        HeadersError = true;
        continue;
      }

      // item.Name = cdItem.Name;
      item.MadeByVersion = cdItem.MadeByVersion;
      item.CentralExtra = cdItem.CentralExtra;
      item.InternalAttrib = cdItem.InternalAttrib;
      item.ExternalAttrib = cdItem.ExternalAttrib;
      item.Comment = cdItem.Comment;
      item.FromCentral = cdItem.FromCentral;
    }

    items += items2;
  }


  if (ecd.NumEntries < ecd.NumEntries_in_ThisDisk)
    HeadersError = true;

  if (ecd.ThisDisk == 0)
  {
    // if (isZip64)
    {
      if (ecd.NumEntries != ecd.NumEntries_in_ThisDisk)
        HeadersError = true;
    }
  }

  if (ecd.NumEntries > items.Size())
    HeadersError = true;

  if (isZip64)
  {
    if (ecd64.NumEntries != items.Size())
      HeadersError = true;
  }
  else
  {
    // old 7-zip could store 32-bit number of CD items to 16-bit field.
    /*
    if ((UInt16)ecd64.NumEntries == (UInt16)items.Size())
      HeadersError = true;
    */
  }

  ReadBuffer(ArcInfo.Comment, ecd.CommentSize);
  _inBufMode = false;
  _inBuffer.Free();

  if ((UInt16)ecd64.NumEntries != (UInt16)numCdItems
      || (UInt32)ecd64.Size != (UInt32)cdSize
      || ((UInt32)ecd64.Offset != (UInt32)cdRelatOffset && !items.IsEmpty()))
  {
    // return S_FALSE;
    HeadersError = true;
  }
  
  // printf("\nOpen OK");
  return S_OK;
}



HRESULT CInArchive::Open(IInStream *stream, const UInt64 *searchLimit,
    IArchiveOpenCallback *callback, CObjectVector<CItemEx> &items)
{
  _inBufMode = false;
  items.Clear();
  
  Close();
  ArcInfo.Clear();

  UInt64 startPos;
  RINOK(stream->Seek(0, STREAM_SEEK_CUR, &startPos));
  RINOK(stream->Seek(0, STREAM_SEEK_END, &ArcInfo.FileEndPos));
  m_Position = ArcInfo.FileEndPos;

  StartStream = stream;
  Callback = callback;
  
  bool volWasRequested = false;

  if (callback
      && (startPos == 0 || !searchLimit || *searchLimit != 0))
  {
    volWasRequested = true;
    RINOK(ReadVols());
  }

  if (IsMultiVol && Vols.StartVolIndex != 0)
  {
    Stream = Vols.Streams[0].Stream;
    if (Stream)
    {
      m_Position = 0;
      RINOK(Stream->Seek(0, STREAM_SEEK_SET, NULL));
      UInt64 limit = 0;
      HRESULT res = FindMarker(Stream, &limit);
      if (res == S_OK)
        MarkerIsFound = true;
      else if (res != S_FALSE)
        return res;
    }
  }
  else
  {
    // printf("\nOpen offset = %u\n", (unsigned)startPos);
    RINOK(stream->Seek(startPos, STREAM_SEEK_SET, NULL));
    m_Position = startPos;
    HRESULT res = FindMarker(stream, searchLimit);
    UInt64 curPos = m_Position;
    if (res == S_OK)
      MarkerIsFound = true;
    else
    {
      // if (res != S_FALSE)
      return res;
    }

    MarkerIsFound = true;
    
    if (ArcInfo.IsSpanMode && !volWasRequested)
    {
      RINOK(ReadVols());
    }
    
    if (IsMultiVol && (unsigned)Vols.StartVolIndex < Vols.Streams.Size())
    {
      Stream = Vols.Streams[Vols.StartVolIndex].Stream;
      if (!Stream)
        IsMultiVol = false;
      else
      {
        RINOK(Stream->Seek(curPos, STREAM_SEEK_SET, NULL));
        m_Position = curPos;
      }
    }
    else
      IsMultiVol = false;

    if (!IsMultiVol)
    {
      RINOK(stream->Seek(curPos, STREAM_SEEK_SET, NULL));
      m_Position = curPos;
      StreamRef = stream;
      Stream = stream;
    }
  }


  {
    HRESULT res;
    try
    {
      res = ReadHeaders2(items);
    }
    catch (const CInBufferException &e) { res = e.ErrorCode; }
    catch (const CUnexpectEnd &)
    {
      if (items.IsEmpty())
        return S_FALSE;
      UnexpectedEnd = true;
      res = S_OK;
    }
    catch (...)
    {
      _inBufMode = false;
      throw;
    }
    
    if (IsMultiVol)
    {
      ArcInfo.FinishPos = ArcInfo.FileEndPos;
      if ((unsigned)Vols.StreamIndex < Vols.Streams.Size())
        if (m_Position < Vols.Streams[Vols.StreamIndex].Size)
          ArcInfo.ThereIsTail = true;
    }
    else
    {
      ArcInfo.FinishPos = m_Position;
      ArcInfo.ThereIsTail = (ArcInfo.FileEndPos > m_Position);
    }

    _inBufMode = false;
    IsArcOpen = true;
    if (!IsMultiVol)
      Vols.Streams.Clear();
    return res;
  }
}


HRESULT CInArchive::GetItemStream(const CItemEx &item, bool seekPackData, CMyComPtr<ISequentialInStream> &stream)
{
  stream.Release();

  UInt64 pos = item.LocalHeaderPos;
  if (seekPackData)
    pos += item.LocalFullHeaderSize;

  if (!IsMultiVol)
  {
    if (UseDisk_in_SingleVol && item.Disk != EcdVolIndex)
      return S_OK;
    pos += ArcInfo.Base;
    RINOK(StreamRef->Seek(pos, STREAM_SEEK_SET, NULL));
    stream = StreamRef;
    return S_OK;
  }

  if (item.Disk >= Vols.Streams.Size())
    return S_OK;
    
  IInStream *str2 = Vols.Streams[item.Disk].Stream;
  if (!str2)
    return S_OK;
  RINOK(str2->Seek(pos, STREAM_SEEK_SET, NULL));
    
  Vols.NeedSeek = false;
  Vols.StreamIndex = item.Disk;
    
  CVolStream *volsStreamSpec = new CVolStream;
  volsStreamSpec->Vols = &Vols;
  stream = volsStreamSpec;
  
  return S_OK;
}

}}
