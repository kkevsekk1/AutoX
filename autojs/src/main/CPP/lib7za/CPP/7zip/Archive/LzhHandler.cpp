// LzhHandler.cpp

#include "StdAfx.h"

#include "../../../C/CpuArch.h"

#include "../../Common/ComTry.h"
#include "../../Common/MyBuffer.h"
#include "../../Common/StringConvert.h"

#include "../../Windows/PropVariant.h"
#include "../../Windows/TimeUtils.h"

#include "../ICoder.h"

#include "../Common/LimitedStreams.h"
#include "../Common/ProgressUtils.h"
#include "../Common/RegisterArc.h"
#include "../Common/StreamUtils.h"

#include "../Compress/CopyCoder.h"
#include "../Compress/LzhDecoder.h"

#include "IArchive.h"

#include "Common/ItemNameUtils.h"

using namespace NWindows;
using namespace NTime;

#define Get16(p) GetUi16(p)
#define Get32(p) GetUi32(p)

namespace NArchive {
namespace NLzh{

const int kMethodIdSize = 5;

const Byte kExtIdFileName = 0x01;
const Byte kExtIdDirName  = 0x02;
const Byte kExtIdUnixTime = 0x54;

struct CExtension
{
  Byte Type;
  CByteBuffer Data;
  
  AString GetString() const
  {
    AString s;
    for (size_t i = 0; i < Data.Size(); i++)
    {
      char c = (char)Data[i];
      if (c == 0)
        break;
      s += c;
    }
    return s;
  }
};

const UInt32 kBasicPartSize = 22;

API_FUNC_static_IsArc IsArc_Lzh(const Byte *p, size_t size)
{
  if (size < 2 + kBasicPartSize)
    return k_IsArc_Res_NEED_MORE;
  if (p[2] != '-' || p[3] != 'l'  || p[4] != 'h' || p[6] != '-')
    return k_IsArc_Res_NO;
  Byte n = p[5];
  if (n != 'd')
    if (n < '0' || n > '7')
      return k_IsArc_Res_NO;
  return k_IsArc_Res_YES;
}
}

struct CItem
{
  AString Name;
  Byte Method[kMethodIdSize];
  Byte Attributes;
  Byte Level;
  Byte OsId;
  UInt32 PackSize;
  UInt32 Size;
  UInt32 ModifiedTime;
  UInt16 CRC;
  CObjectVector<CExtension> Extensions;

  bool IsValidMethod() const  { return (Method[0] == '-' && Method[1] == 'l' && Method[4] == '-'); }
  bool IsLhMethod() const  {return (IsValidMethod() && Method[2] == 'h'); }
  bool IsDir() const {return (IsLhMethod() && Method[3] == 'd'); }

  bool IsCopyMethod() const
  {
    return (IsLhMethod() && Method[3] == '0') ||
      (IsValidMethod() && Method[2] == 'z' && Method[3] == '4');
  }
  
  bool IsLh1GroupMethod() const
  {
    if (!IsLhMethod())
      return false;
    switch (Method[3])
    {
      case '1':
        return true;
    }
    return false;
  }
  
  bool IsLh4GroupMethod() const
  {
    if (!IsLhMethod())
      return false;
    switch (Method[3])
    {
      case '4':
      case '5':
      case '6':
      case '7':
        return true;
    }
    return false;
  }
  
  int GetNumDictBits() const
  {
    if (!IsLhMethod())
      return 0;
    switch (Method[3])
    {
      case '1': return 12;
      case '2': return 13;
      case '3': return 13;
      case '4': return 12;
      case '5': return 13;
      case '6': return 15;
      case '7': return 16;
    }
    return 0;
  }

  int FindExt(Byte type) const
  {
    FOR_VECTOR (i, Extensions)
      if (Extensions[i].Type == type)
        return i;
    return -1;
  }
  bool GetUnixTime(UInt32 &value) const
  {
    value = 0;
    int index = FindExt(kExtIdUnixTime);
    if (index < 0)
    {
      if (Level == 2)
      {
        value = ModifiedTime;
        return true;
      }
      return false;
    }
    const Byte *data = (const Byte *)(Extensions[index].Data);
    value = GetUi32(data);
    return true;
  }

  AString GetDirName() const
  {
    int index = FindExt(kExtIdDirName);
    if (index < 0)
      return AString();
    return Extensions[index].GetString();
  }

  AString GetFileName() const
  {
    int index = FindExt(kExtIdFileName);
    if (index < 0)
      return Name;
    return Extensions[index].GetString();
  }

  AString GetName() const
  {
    AString dirName = GetDirName();
    const char kDirSeparator = CHAR_PATH_SEPARATOR; // '\\';
    // check kDirSeparator in Linux
    dirName.Replace((char)(unsigned char)0xFF, kDirSeparator);
    if (!dirName.IsEmpty() && dirName.Back() != kDirSeparator)
      dirName += kDirSeparator;
    return dirName + GetFileName();
  }
};

static const Byte *ReadUInt16(const Byte *p, UInt16 &v)
{
  v = Get16(p);
  return p + 2;
}

static const Byte *ReadString(const Byte *p, size_t size, AString &s)
{
  s.Empty();
  for (size_t i = 0; i < size; i++)
  {
    char c = p[i];
    if (c == 0)
      break;
    s += c;
  }
  return p + size;
}

static Byte CalcSum(const Byte *data, size_t size)
{
  Byte sum = 0;
  for (size_t i = 0; i < size; i++)
    sum = (Byte)(sum + data[i]);
  return sum;
}

static HRESULT GetNextItem(ISequentialInStream *stream, bool &filled, CItem &item)
{
  filled = false;

  size_t processedSize = 2;
  Byte startHeader[2];
  RINOK(ReadStream(stream, startHeader, &processedSize))
  if (processedSize == 0)
    return S_OK;
  if (processedSize == 1)
    return (startHeader[0] == 0) ? S_OK: S_FALSE;
  if (startHeader[0] == 0 && startHeader[1] == 0)
    return S_OK;

  Byte header[256];
  processedSize = kBasicPartSize;
  RINOK(ReadStream(stream, header, &processedSize));
  if (processedSize != kBasicPartSize)
    return (startHeader[0] == 0) ? S_OK: S_FALSE;

  const Byte *p = header;
  memcpy(item.Method, p, kMethodIdSize);
  if (!item.IsValidMethod())
    return S_OK;
  p += kMethodIdSize;
  item.PackSize = Get32(p);
  item.Size = Get32(p + 4);
  item.ModifiedTime = Get32(p + 8);
  item.Attributes = p[12];
  item.Level = p[13];
  p += 14;
  if (item.Level > 2)
    return S_FALSE;
  UInt32 headerSize;
  if (item.Level < 2)
  {
    headerSize = startHeader[0];
    if (headerSize < kBasicPartSize)
      return S_FALSE;
    RINOK(ReadStream_FALSE(stream, header + kBasicPartSize, headerSize - kBasicPartSize));
    if (startHeader[1] != CalcSum(header, headerSize))
      return S_FALSE;
    size_t nameLength = *p++;
    if ((p - header) + nameLength + 2 > headerSize)
      return S_FALSE;
    p = ReadString(p, nameLength, item.Name);
  }
  else
    headerSize = startHeader[0] | ((UInt32)startHeader[1] << 8);
  p = ReadUInt16(p, item.CRC);
  if (item.Level != 0)
  {
    if (item.Level == 2)
    {
      RINOK(ReadStream_FALSE(stream, header + kBasicPartSize, 2));
    }
    if ((size_t)(p - header) + 3 > headerSize)
      return S_FALSE;
    item.OsId = *p++;
    UInt16 nextSize;
    p = ReadUInt16(p, nextSize);
    while (nextSize != 0)
    {
      if (nextSize < 3)
        return S_FALSE;
      if (item.Level == 1)
      {
        if (item.PackSize < nextSize)
          return S_FALSE;
        item.PackSize -= nextSize;
      }
      if (item.Extensions.Size() >= (1 << 8))
        return S_FALSE;
      CExtension ext;
      RINOK(ReadStream_FALSE(stream, &ext.Type, 1))
      nextSize -= 3;
      ext.Data.Alloc(nextSize);
      RINOK(ReadStream_FALSE(stream, (Byte *)ext.Data, nextSize))
      item.Extensions.Add(ext);
      Byte hdr2[2];
      RINOK(ReadStream_FALSE(stream, hdr2, 2));
      ReadUInt16(hdr2, nextSize);
    }
  }
  filled = true;
  return S_OK;
}

struct COsPair
{
  Byte Id;
  const char *Name;
};

static const COsPair g_OsPairs[] =
{
  {   0, "MS-DOS" },
  { 'M', "MS-DOS" },
  { '2', "OS/2" },
  { '9', "OS9" },
  { 'K', "OS/68K" },
  { '3', "OS/386" },
  { 'H', "HUMAN" },
  { 'U', "UNIX" },
  { 'C', "CP/M" },
  { 'F', "FLEX" },
  { 'm', "Mac" },
  { 'R', "Runser" },
  { 'T', "TownsOS" },
  { 'X', "XOSK" },
  { 'w', "Windows 95" },
  { 'W', "Windows NT" },
  { 'J', "Java VM" }
};

static const char *kUnknownOS = "Unknown";

static const char *GetOS(Byte osId)
{
  for (unsigned i = 0; i < ARRAY_SIZE(g_OsPairs); i++)
    if (g_OsPairs[i].Id == osId)
      return g_OsPairs[i].Name;
  return kUnknownOS;
}

static const Byte kProps[] =
{
  kpidPath,
  kpidIsDir,
  kpidSize,
  kpidPackSize,
  kpidMTime,
  // kpidAttrib,
  kpidCRC,
  kpidMethod,
  kpidHostOS
};

class CCRC
{
  UInt16 _value;
public:
  static UInt16 Table[256];
  static void InitTable();
  
  CCRC(): _value(0) {}
  void Init() { _value = 0; }
  void Update(const void *data, size_t size);
  UInt16 GetDigest() const { return _value; }
};

static const UInt16 kCRCPoly = 0xA001;

UInt16 CCRC::Table[256];

void CCRC::InitTable()
{
  for (UInt32 i = 0; i < 256; i++)
  {
    UInt32 r = i;
    for (int j = 0; j < 8; j++)
      if (r & 1)
        r = (r >> 1) ^ kCRCPoly;
      else
        r >>= 1;
    CCRC::Table[i] = (UInt16)r;
  }
}

class CCRCTableInit
{
public:
  CCRCTableInit() { CCRC::InitTable(); }
} g_CRCTableInit;

void CCRC::Update(const void *data, size_t size)
{
  UInt16 v = _value;
  const Byte *p = (const Byte *)data;
  for (; size > 0; size--, p++)
    v = (UInt16)(Table[((Byte)(v)) ^ *p] ^ (v >> 8));
  _value = v;
}


class COutStreamWithCRC:
  public ISequentialOutStream,
  public CMyUnknownImp
{
public:
  MY_UNKNOWN_IMP

  STDMETHOD(Write)(const void *data, UInt32 size, UInt32 *processedSize);
private:
  CCRC _crc;
  CMyComPtr<ISequentialOutStream> _stream;
public:
  void Init(ISequentialOutStream *stream)
  {
    _stream = stream;
    _crc.Init();
  }
  void ReleaseStream() { _stream.Release(); }
  UInt32 GetCRC() const { return _crc.GetDigest(); }
  void InitCRC() { _crc.Init(); }
};

STDMETHODIMP COutStreamWithCRC::Write(const void *data, UInt32 size, UInt32 *processedSize)
{
  UInt32 realProcessedSize;
  HRESULT result;
  if (!_stream)
  {
    realProcessedSize = size;
    result = S_OK;
  }
  else
    result = _stream->Write(data, size, &realProcessedSize);
  _crc.Update(data, realProcessedSize);
  if (processedSize != NULL)
    *processedSize = realProcessedSize;
  return result;
}

struct CItemEx: public CItem
{
  UInt64 DataPosition;
};

class CHandler:
  public IInArchive,
  public CMyUnknownImp
{
  CObjectVector<CItemEx> _items;
  CMyComPtr<IInStream> _stream;
  UInt64 _phySize;
  UInt32 _errorFlags;
  bool _isArc;
public:
  MY_UNKNOWN_IMP1(IInArchive)
  INTERFACE_IInArchive(;)
  CHandler();
};

IMP_IInArchive_Props
IMP_IInArchive_ArcProps_NO_Table

CHandler::CHandler() {}

STDMETHODIMP CHandler::GetNumberOfItems(UInt32 *numItems)
{
  *numItems = _items.Size();
  return S_OK;
}

STDMETHODIMP CHandler::GetArchiveProperty(PROPID propID, PROPVARIANT *value)
{
  NCOM::CPropVariant prop;
  switch (propID)
  {
    case kpidPhySize: prop = _phySize; break;
   
    case kpidErrorFlags:
      UInt32 v = _errorFlags;
      if (!_isArc) v |= kpv_ErrorFlags_IsNotArc;
      prop = v;
      break;
  }
  prop.Detach(value);
  return S_OK;
}

STDMETHODIMP CHandler::GetProperty(UInt32 index, PROPID propID, PROPVARIANT *value)
{
  COM_TRY_BEGIN
  NCOM::CPropVariant prop;
  const CItemEx &item = _items[index];
  switch (propID)
  {
    case kpidPath:
    {
      UString s = NItemName::WinNameToOSName(MultiByteToUnicodeString(item.GetName(), CP_OEMCP));
      if (!s.IsEmpty())
      {
        if (s.Back() == WCHAR_PATH_SEPARATOR)
          s.DeleteBack();
        prop = s;
      }
      break;
    }
    case kpidIsDir:  prop = item.IsDir(); break;
    case kpidSize:   prop = item.Size; break;
    case kpidPackSize:  prop = item.PackSize; break;
    case kpidCRC:  prop = (UInt32)item.CRC; break;
    case kpidHostOS:  prop = GetOS(item.OsId); break;
    case kpidMTime:
    {
      FILETIME utc;
      UInt32 unixTime;
      if (item.GetUnixTime(unixTime))
        NTime::UnixTimeToFileTime(unixTime, utc);
      else
      {
        FILETIME localFileTime;
        if (DosTimeToFileTime(item.ModifiedTime, localFileTime))
        {
          if (!LocalFileTimeToFileTime(&localFileTime, &utc))
            utc.dwHighDateTime = utc.dwLowDateTime = 0;
        }
        else
          utc.dwHighDateTime = utc.dwLowDateTime = 0;
      }
      prop = utc;
      break;
    }
    // case kpidAttrib:  prop = (UInt32)item.Attributes; break;
    case kpidMethod:
    {
      char method2[kMethodIdSize + 1];
      method2[kMethodIdSize] = 0;
      memcpy(method2, item.Method, kMethodIdSize);
      prop = method2;
      break;
    }
  }
  prop.Detach(value);
  return S_OK;
  COM_TRY_END
}

STDMETHODIMP CHandler::Open(IInStream *stream,
    const UInt64 * /* maxCheckStartPosition */, IArchiveOpenCallback *callback)
{
  COM_TRY_BEGIN
  Close();
  try
  {
    _items.Clear();

    UInt64 endPos = 0;
    bool needSetTotal = true;

    RINOK(stream->Seek(0, STREAM_SEEK_END, &endPos));
    RINOK(stream->Seek(0, STREAM_SEEK_SET, NULL));

    for (;;)
    {
      CItemEx item;
      bool filled;
      HRESULT result = GetNextItem(stream, filled, item);
      RINOK(stream->Seek(0, STREAM_SEEK_CUR, &item.DataPosition));
      if (result == S_FALSE)
      {
        _errorFlags = kpv_ErrorFlags_HeadersError;
        break;
      }

      if (result != S_OK)
        return S_FALSE;
      _phySize = item.DataPosition;
      if (!filled)
        break;
      _items.Add(item);

      _isArc = true;

      UInt64 newPostion;
      RINOK(stream->Seek(item.PackSize, STREAM_SEEK_CUR, &newPostion));
      if (newPostion > endPos)
      {
        _phySize = endPos;
        _errorFlags = kpv_ErrorFlags_UnexpectedEnd;
        break;
      }
      _phySize = newPostion;
      if (callback)
      {
        if (needSetTotal)
        {
          RINOK(callback->SetTotal(NULL, &endPos));
          needSetTotal = false;
        }
        if (_items.Size() % 100 == 0)
        {
          UInt64 numFiles = _items.Size();
          UInt64 numBytes = item.DataPosition;
          RINOK(callback->SetCompleted(&numFiles, &numBytes));
        }
      }
    }
    if (_items.IsEmpty())
      return S_FALSE;

    _stream = stream;
  }
  catch(...)
  {
    return S_FALSE;
  }
  COM_TRY_END
  return S_OK;
}

STDMETHODIMP CHandler::Close()
{
  _isArc = false;
  _phySize = 0;
  _errorFlags = 0;
  _items.Clear();
  _stream.Release();
  return S_OK;
}

STDMETHODIMP CHandler::Extract(const UInt32 *indices, UInt32 numItems,
    Int32 testModeSpec, IArchiveExtractCallback *extractCallback)
{
  COM_TRY_BEGIN
  bool testMode = (testModeSpec != 0);
  UInt64 totalUnPacked = 0, totalPacked = 0;
  bool allFilesMode = (numItems == (UInt32)(Int32)-1);
  if (allFilesMode)
    numItems = _items.Size();
  if (numItems == 0)
    return S_OK;
  UInt32 i;
  for (i = 0; i < numItems; i++)
  {
    const CItemEx &item = _items[allFilesMode ? i : indices[i]];
    totalUnPacked += item.Size;
    totalPacked += item.PackSize;
  }
  RINOK(extractCallback->SetTotal(totalUnPacked));

  UInt64 currentTotalUnPacked = 0, currentTotalPacked = 0;
  UInt64 currentItemUnPacked, currentItemPacked;
  
  NCompress::NLzh::NDecoder::CCoder *lzhDecoderSpec = 0;
  CMyComPtr<ICompressCoder> lzhDecoder;
  // CMyComPtr<ICompressCoder> lzh1Decoder;
  // CMyComPtr<ICompressCoder> arj2Decoder;

  NCompress::CCopyCoder *copyCoderSpec = new NCompress::CCopyCoder();
  CMyComPtr<ICompressCoder> copyCoder = copyCoderSpec;

  CLocalProgress *lps = new CLocalProgress;
  CMyComPtr<ICompressProgressInfo> progress = lps;
  lps->Init(extractCallback, false);

  CLimitedSequentialInStream *streamSpec = new CLimitedSequentialInStream;
  CMyComPtr<ISequentialInStream> inStream(streamSpec);
  streamSpec->SetStream(_stream);

  for (i = 0; i < numItems; i++, currentTotalUnPacked += currentItemUnPacked,
      currentTotalPacked += currentItemPacked)
  {
    currentItemUnPacked = 0;
    currentItemPacked = 0;

    lps->InSize = currentTotalPacked;
    lps->OutSize = currentTotalUnPacked;
    RINOK(lps->SetCur());

    CMyComPtr<ISequentialOutStream> realOutStream;
    Int32 askMode;
    askMode = testMode ? NExtract::NAskMode::kTest :
        NExtract::NAskMode::kExtract;
    Int32 index = allFilesMode ? i : indices[i];
    const CItemEx &item = _items[index];
    RINOK(extractCallback->GetStream(index, &realOutStream, askMode));

    if (item.IsDir())
    {
      // if (!testMode)
      {
        RINOK(extractCallback->PrepareOperation(askMode));
        RINOK(extractCallback->SetOperationResult(NExtract::NOperationResult::kOK));
      }
      continue;
    }

    if (!testMode && !realOutStream)
      continue;

    RINOK(extractCallback->PrepareOperation(askMode));
    currentItemUnPacked = item.Size;
    currentItemPacked = item.PackSize;

    {
      COutStreamWithCRC *outStreamSpec = new COutStreamWithCRC;
      CMyComPtr<ISequentialOutStream> outStream(outStreamSpec);
      outStreamSpec->Init(realOutStream);
      realOutStream.Release();
      
      UInt64 pos;
      _stream->Seek(item.DataPosition, STREAM_SEEK_SET, &pos);

      streamSpec->Init(item.PackSize);

      HRESULT result = S_OK;
      Int32 opRes = NExtract::NOperationResult::kOK;

      if (item.IsCopyMethod())
      {
        result = copyCoder->Code(inStream, outStream, NULL, NULL, progress);
        if (result == S_OK && copyCoderSpec->TotalSize != item.PackSize)
          result = S_FALSE;
      }
      else if (item.IsLh4GroupMethod())
      {
        if (!lzhDecoder)
        {
          lzhDecoderSpec = new NCompress::NLzh::NDecoder::CCoder;
          lzhDecoder = lzhDecoderSpec;
        }
        lzhDecoderSpec->FinishMode = true;
        lzhDecoderSpec->SetDictSize(1 << item.GetNumDictBits());
        result = lzhDecoder->Code(inStream, outStream, NULL, &currentItemUnPacked, progress);
        if (result == S_OK && lzhDecoderSpec->GetInputProcessedSize() != item.PackSize)
          result = S_FALSE;
      }
      /*
      else if (item.IsLh1GroupMethod())
      {
        if (!lzh1Decoder)
        {
          lzh1DecoderSpec = new NCompress::NLzh1::NDecoder::CCoder;
          lzh1Decoder = lzh1DecoderSpec;
        }
        lzh1DecoderSpec->SetDictionary(item.GetNumDictBits());
        result = lzh1Decoder->Code(inStream, outStream, NULL, &currentItemUnPacked, progress);
      }
      */
      else
        opRes = NExtract::NOperationResult::kUnsupportedMethod;

      if (opRes == NExtract::NOperationResult::kOK)
      {
        if (result == S_FALSE)
          opRes = NExtract::NOperationResult::kDataError;
        else
        {
          RINOK(result);
          if (outStreamSpec->GetCRC() != item.CRC)
            opRes = NExtract::NOperationResult::kCRCError;
        }
      }
      outStream.Release();
      RINOK(extractCallback->SetOperationResult(opRes));
    }
  }
  return S_OK;
  COM_TRY_END
}

static const Byte k_Signature[] = { '-', 'l', 'h' };

REGISTER_ARC_I(
  "Lzh", "lzh lha", 0, 6,
  k_Signature,
  2,
  0,
  IsArc_Lzh)

}}
