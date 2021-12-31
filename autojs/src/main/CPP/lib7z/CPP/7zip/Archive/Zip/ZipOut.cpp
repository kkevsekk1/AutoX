// ZipOut.cpp

#include "StdAfx.h"

#include "../../Common/OffsetStream.h"

#include "ZipOut.h"

namespace NArchive {
namespace NZip {

HRESULT COutArchive::Create(IOutStream *outStream)
{
  m_CurPos = 0;
  if (!m_OutBuffer.Create(1 << 16))
    return E_OUTOFMEMORY;
  m_Stream = outStream;
  m_OutBuffer.SetStream(outStream);
  m_OutBuffer.Init();

  return m_Stream->Seek(0, STREAM_SEEK_CUR, &m_Base);
}

void COutArchive::MoveCurPos(UInt64 distanceToMove)
{
  m_CurPos += distanceToMove; // test overflow
}

void COutArchive::SeekToRelatPos(UInt64 offset)
{
  HRESULT res = m_Stream->Seek(m_Base + offset, STREAM_SEEK_SET, NULL);
  if (res != S_OK)
    throw CSystemException(res);
}

void COutArchive::PrepareWriteCompressedDataZip64(unsigned fileNameLen, bool isZip64, bool aesEncryption)
{
  m_IsZip64 = isZip64;
  m_ExtraSize = isZip64 ? (4 + 8 + 8) : 0;
  if (aesEncryption)
    m_ExtraSize += 4 + k_WzAesExtra_Size;
  m_LocalFileHeaderSize = kLocalHeaderSize + fileNameLen + m_ExtraSize;
}

void COutArchive::PrepareWriteCompressedData(unsigned fileNameLen, UInt64 unPackSize, bool aesEncryption)
{
  // We use Zip64, if unPackSize size is larger than 0xF8000000 to support
  // cases when compressed size can be about 3% larger than uncompressed size

  PrepareWriteCompressedDataZip64(fileNameLen, unPackSize >= (UInt32)0xF8000000, aesEncryption);
}

#define DOES_NEED_ZIP64(v) (v >= (UInt32)0xFFFFFFFF)

void COutArchive::PrepareWriteCompressedData2(unsigned fileNameLen, UInt64 unPackSize, UInt64 packSize, bool aesEncryption)
{
  bool isZip64 =
      DOES_NEED_ZIP64(unPackSize) ||
      DOES_NEED_ZIP64(packSize);
  PrepareWriteCompressedDataZip64(fileNameLen, isZip64, aesEncryption);
}

void COutArchive::WriteBytes(const void *buffer, UInt32 size)
{
  m_OutBuffer.WriteBytes(buffer, size);
  m_CurPos += size;
}

void COutArchive::Write8(Byte b)
{
  m_OutBuffer.WriteByte(b);
  m_CurPos++;
}

void COutArchive::Write16(UInt16 val)
{
  for (int i = 0; i < 2; i++)
  {
    Write8((Byte)val);
    val >>= 8;
  }
}

void COutArchive::Write32(UInt32 val)
{
  for (int i = 0; i < 4; i++)
  {
    Write8((Byte)val);
    val >>= 8;
  }
}

void COutArchive::Write64(UInt64 val)
{
  for (int i = 0; i < 8; i++)
  {
    Write8((Byte)val);
    val >>= 8;
  }
}

void COutArchive::WriteExtra(const CExtraBlock &extra)
{
  if (extra.SubBlocks.Size() != 0)
  {
    FOR_VECTOR (i, extra.SubBlocks)
    {
      const CExtraSubBlock &subBlock = extra.SubBlocks[i];
      Write16(subBlock.ID);
      Write16((UInt16)subBlock.Data.Size());
      WriteBytes(subBlock.Data, (UInt32)subBlock.Data.Size());
    }
  }
}

void COutArchive::WriteCommonItemInfo(const CLocalItem &item, bool isZip64)
{
  {
    Byte ver = item.ExtractVersion.Version;
    if (isZip64 && ver < NFileHeader::NCompressionMethod::kExtractVersion_Zip64)
      ver = NFileHeader::NCompressionMethod::kExtractVersion_Zip64;
    Write8(ver);
  }
  Write8(item.ExtractVersion.HostOS);
  Write16(item.Flags);
  Write16(item.Method);
  Write32(item.Time);
  Write32(item.Crc);
}

#define WRITE_32_VAL_SPEC(__v, __isZip64) Write32((__isZip64) ? 0xFFFFFFFF : (UInt32)(__v));

void COutArchive::WriteLocalHeader(const CLocalItem &item)
{
  SeekToCurPos();
  
  bool isZip64 = m_IsZip64 ||
      DOES_NEED_ZIP64(item.PackSize) ||
      DOES_NEED_ZIP64(item.Size);
  
  Write32(NSignature::kLocalFileHeader);
  WriteCommonItemInfo(item, isZip64);

  WRITE_32_VAL_SPEC(item.PackSize, isZip64);
  WRITE_32_VAL_SPEC(item.Size, isZip64);

  Write16((UInt16)item.Name.Len());
  {
    UInt16 localExtraSize = (UInt16)((isZip64 ? (4 + 8 + 8): 0) + item.LocalExtra.GetSize());
    if (localExtraSize != m_ExtraSize)
      throw CSystemException(E_FAIL);
  }
  Write16((UInt16)m_ExtraSize);
  WriteBytes((const char *)item.Name, item.Name.Len());

  if (isZip64)
  {
    Write16(NFileHeader::NExtraID::kZip64);
    Write16(8 + 8);
    Write64(item.Size);
    Write64(item.PackSize);
  }

  WriteExtra(item.LocalExtra);

  // Why don't we write NTFS timestamps to local header?
  // Probably we want to reduce size of archive?

  m_OutBuffer.FlushWithCheck();
  MoveCurPos(item.PackSize);
}

void COutArchive::WriteCentralHeader(const CItemOut &item)
{
  bool isUnPack64 = DOES_NEED_ZIP64(item.Size);
  bool isPack64 = DOES_NEED_ZIP64(item.PackSize);
  bool isPosition64 = DOES_NEED_ZIP64(item.LocalHeaderPos);
  bool isZip64 = isPack64 || isUnPack64 || isPosition64;
  
  Write32(NSignature::kCentralFileHeader);
  Write8(item.MadeByVersion.Version);
  Write8(item.MadeByVersion.HostOS);
  
  WriteCommonItemInfo(item, isZip64);

  WRITE_32_VAL_SPEC(item.PackSize, isPack64);
  WRITE_32_VAL_SPEC(item.Size, isUnPack64);

  Write16((UInt16)item.Name.Len());
  
  UInt16 zip64ExtraSize = (UInt16)((isUnPack64 ? 8: 0) + (isPack64 ? 8: 0) + (isPosition64 ? 8: 0));
  const UInt16 kNtfsExtraSize = 4 + 2 + 2 + (3 * 8);
  const UInt16 centralExtraSize = (UInt16)(
      (isZip64 ? 4 + zip64ExtraSize : 0) +
      (item.NtfsTimeIsDefined ? 4 + kNtfsExtraSize : 0) +
      item.CentralExtra.GetSize());

  Write16(centralExtraSize); // test it;
  Write16((UInt16)item.Comment.Size());
  Write16(0); // DiskNumberStart;
  Write16(item.InternalAttrib);
  Write32(item.ExternalAttrib);
  WRITE_32_VAL_SPEC(item.LocalHeaderPos, isPosition64);
  WriteBytes((const char *)item.Name, item.Name.Len());
  
  if (isZip64)
  {
    Write16(NFileHeader::NExtraID::kZip64);
    Write16(zip64ExtraSize);
    if (isUnPack64)
      Write64(item.Size);
    if (isPack64)
      Write64(item.PackSize);
    if (isPosition64)
      Write64(item.LocalHeaderPos);
  }
  
  if (item.NtfsTimeIsDefined)
  {
    Write16(NFileHeader::NExtraID::kNTFS);
    Write16(kNtfsExtraSize);
    Write32(0); // reserved
    Write16(NFileHeader::NNtfsExtra::kTagTime);
    Write16(8 * 3);
    WriteNtfsTime(item.Ntfs_MTime);
    WriteNtfsTime(item.Ntfs_ATime);
    WriteNtfsTime(item.Ntfs_CTime);
  }
  
  WriteExtra(item.CentralExtra);
  if (item.Comment.Size() > 0)
    WriteBytes(item.Comment, (UInt32)item.Comment.Size());
}

void COutArchive::WriteCentralDir(const CObjectVector<CItemOut> &items, const CByteBuffer *comment)
{
  SeekToCurPos();
  
  UInt64 cdOffset = GetCurPos();
  FOR_VECTOR (i, items)
    WriteCentralHeader(items[i]);
  UInt64 cd64EndOffset = GetCurPos();
  UInt64 cdSize = cd64EndOffset - cdOffset;
  bool cdOffset64 = DOES_NEED_ZIP64(cdOffset);
  bool cdSize64 = DOES_NEED_ZIP64(cdSize);
  bool items64 = items.Size() >= 0xFFFF;
  bool isZip64 = (cdOffset64 || cdSize64 || items64);
  
  // isZip64 = true; // to test Zip64

  if (isZip64)
  {
    Write32(NSignature::kEcd64);
    Write64(kEcd64_MainSize);
    Write16(45); // made by version
    Write16(45); // extract version
    Write32(0); // ThisDiskNumber = 0;
    Write32(0); // StartCentralDirectoryDiskNumber;;
    Write64((UInt64)items.Size());
    Write64((UInt64)items.Size());
    Write64((UInt64)cdSize);
    Write64((UInt64)cdOffset);

    Write32(NSignature::kEcd64Locator);
    Write32(0); // number of the disk with the start of the zip64 end of central directory
    Write64(cd64EndOffset);
    Write32(1); // total number of disks
  }
  
  Write32(NSignature::kEcd);
  Write16(0); // ThisDiskNumber = 0;
  Write16(0); // StartCentralDirectoryDiskNumber;
  Write16((UInt16)(items64 ? 0xFFFF: items.Size()));
  Write16((UInt16)(items64 ? 0xFFFF: items.Size()));
  
  WRITE_32_VAL_SPEC(cdSize, cdSize64);
  WRITE_32_VAL_SPEC(cdOffset, cdOffset64);

  UInt32 commentSize = (UInt32)(comment ? comment->Size() : 0);
  Write16((UInt16)commentSize);
  if (commentSize > 0)
    WriteBytes((const Byte *)*comment, commentSize);
  m_OutBuffer.FlushWithCheck();
}

void COutArchive::CreateStreamForCompressing(IOutStream **outStream)
{
  COffsetOutStream *streamSpec = new COffsetOutStream;
  CMyComPtr<IOutStream> tempStream(streamSpec);
  streamSpec->Init(m_Stream, m_Base + m_CurPos + m_LocalFileHeaderSize);
  *outStream = tempStream.Detach();
}

/*
void COutArchive::SeekToPackedDataPosition()
{
  SeekTo(m_BasePosition + m_LocalFileHeaderSize);
}
*/

void COutArchive::SeekToCurPos()
{
  SeekToRelatPos(m_CurPos);
}

void COutArchive::CreateStreamForCopying(ISequentialOutStream **outStream)
{
  CMyComPtr<ISequentialOutStream> tempStream(m_Stream);
  *outStream = tempStream.Detach();
}

}}
