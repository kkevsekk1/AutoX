// ZipOut.h

#ifndef __ZIP_OUT_H
#define __ZIP_OUT_H

#include "../../../Common/MyCom.h"

#include "../../IStream.h"
#include "../../Common/OutBuffer.h"

#include "ZipItem.h"

namespace NArchive {
namespace NZip {

// can throw CSystemException and COutBufferException

class CItemOut: public CItem
{
public:
  FILETIME Ntfs_MTime;
  FILETIME Ntfs_ATime;
  FILETIME Ntfs_CTime;
  bool NtfsTimeIsDefined;

  // It's possible that NtfsTime is not defined, but there is NtfsTime in Extra.

  CItemOut(): NtfsTimeIsDefined(false) {}
};

class COutArchive
{
  CMyComPtr<IOutStream> m_Stream;
  COutBuffer m_OutBuffer;

  UInt64 m_Base; // Base of arc (offset in output Stream)
  UInt64 m_CurPos; // Curent position in archive (relative from m_Base)

  UInt32 m_LocalFileHeaderSize;
  UInt32 m_ExtraSize;
  bool m_IsZip64;

  void SeekToRelatPos(UInt64 offset);

  void WriteBytes(const void *buffer, UInt32 size);
  void Write8(Byte b);
  void Write16(UInt16 val);
  void Write32(UInt32 val);
  void Write64(UInt64 val);
  void WriteNtfsTime(const FILETIME &ft)
  {
    Write32(ft.dwLowDateTime);
    Write32(ft.dwHighDateTime);
  }

  void WriteExtra(const CExtraBlock &extra);
  void WriteCommonItemInfo(const CLocalItem &item, bool isZip64);
  void WriteCentralHeader(const CItemOut &item);

  void PrepareWriteCompressedDataZip64(unsigned fileNameLen, bool isZip64, bool aesEncryption);

public:
  HRESULT Create(IOutStream *outStream);
  
  void MoveCurPos(UInt64 distanceToMove);
  UInt64 GetCurPos() const { return m_CurPos; }

  void SeekToCurPos();

  void PrepareWriteCompressedData(unsigned fileNameLen, UInt64 unPackSize, bool aesEncryption);
  void PrepareWriteCompressedData2(unsigned fileNameLen, UInt64 unPackSize, UInt64 packSize, bool aesEncryption);
  void WriteLocalHeader(const CLocalItem &item);

  void WriteLocalHeader_And_SeekToNextFile(const CLocalItem &item)
  {
    WriteLocalHeader(item);
    SeekToCurPos();
  }

  void WriteCentralDir(const CObjectVector<CItemOut> &items, const CByteBuffer *comment);

  void CreateStreamForCompressing(IOutStream **outStream);
  void CreateStreamForCopying(ISequentialOutStream **outStream);
};

}}

#endif
