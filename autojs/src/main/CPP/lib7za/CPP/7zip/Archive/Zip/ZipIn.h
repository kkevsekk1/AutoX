// Archive/ZipIn.h

#ifndef __ZIP_IN_H
#define __ZIP_IN_H

#include "../../../Common/MyCom.h"

#include "../../IStream.h"

#include "../../Common/InBuffer.h"

#include "ZipHeader.h"
#include "ZipItem.h"

API_FUNC_IsArc IsArc_Zip(const Byte *p, size_t size);

namespace NArchive {
namespace NZip {
  
class CItemEx: public CItem
{
public:
  UInt32 LocalFullHeaderSize; // including Name and Extra
  
  UInt64 GetLocalFullSize() const
    { return LocalFullHeaderSize + PackSize + (HasDescriptor() ? kDataDescriptorSize : 0); }
  UInt64 GetDataPosition() const
    { return LocalHeaderPos + LocalFullHeaderSize; }
};


struct CInArchiveInfo
{
  Int64 Base; /* Base offset of start of archive in stream.
                 Offsets in headers must be calculated from that Base.
                 Base is equal to MarkerPos for normal ZIPs.
                 Base can point to PE stub for some ZIP SFXs.
                 if CentralDir was read,
                   Base can be negative, if start of data is not available,
                 if CentralDirs was not read,
                   Base = ArcInfo.MarkerPos; */

  /* The following *Pos variables contain absolute offsets in Stream */

  UInt64 MarkerPos;  /* Pos of first signature, it can point to kSpan/kNoSpan signature
                        = MarkerPos2      in most archives
                        = MarkerPos2 - 4  if there is kSpan/kNoSpan signature */
  UInt64 MarkerPos2; // Pos of first local item signature in stream
  UInt64 FinishPos;  // Finish pos of archive data in starting volume
  UInt64 FileEndPos; // Finish pos of stream

  UInt64 FirstItemRelatOffset; /* Relative offset of first local (read from cd) (relative to Base).
                                  = 0 in most archives
                                  = size of stub for some SFXs */
  bool CdWasRead;
  bool IsSpanMode;
  bool ThereIsTail;

  // UInt32 BaseVolIndex;

  CByteBuffer Comment;


  CInArchiveInfo():
      Base(0),
      MarkerPos(0),
      MarkerPos2(0),
      FinishPos(0),
      FileEndPos(0),
      FirstItemRelatOffset(0),
      CdWasRead(false),
      IsSpanMode(false),
      ThereIsTail(false)
      // BaseVolIndex(0)
      {}
  
  void Clear()
  {
    // BaseVolIndex = 0;
    Base = 0;
    MarkerPos = 0;
    MarkerPos2 = 0;
    FinishPos = 0;
    FileEndPos = 0;
    ThereIsTail = false;

    FirstItemRelatOffset = 0;

    CdWasRead = false;
    IsSpanMode = false;

    Comment.Free();
  }
};


struct CCdInfo
{
  // 64
  UInt16 VersionMade;
  UInt16 VersionNeedExtract;

  // old zip
  UInt32 ThisDisk;
  UInt32 CdDisk;
  UInt64 NumEntries_in_ThisDisk;
  UInt64 NumEntries;
  UInt64 Size;
  UInt64 Offset;

  UInt16 CommentSize;

  CCdInfo() { memset(this, 0, sizeof(*this)); }

  void ParseEcd32(const Byte *p);   // (p) includes signature
  void ParseEcd64e(const Byte *p);  // (p) exclude signature
};


class CVols
{
public:

  struct CSubStreamInfo
  {
    CMyComPtr<IInStream> Stream;
    UInt64 Size;

    CSubStreamInfo(): Size(0) {}
  };
  
  CObjectVector<CSubStreamInfo> Streams;
  int StreamIndex;
  bool NeedSeek;

  CMyComPtr<IInStream> ZipStream;

  bool StartIsExe;  // is .exe
  bool StartIsZ;    // is .zip or .zNN
  bool StartIsZip;  // is .zip
  bool IsUpperCase;
  Int32 StartVolIndex; // = (NN - 1), if StartStream is .zNN

  Int32 StartParsingVol; // if we need local parsing, we must use that stream
  unsigned NumVols;

  int EndVolIndex; // index of last volume (ecd volume),
                   // -1, if is not multivol

  UString BaseName; // including '.'

  UString MissingName;

  CCdInfo ecd;
  bool ecd_wasRead;

  void Clear()
  {
    StreamIndex = -1;
    NeedSeek = false;


    StartIsExe = false;
    StartIsZ = false;
    StartIsZip = false;
    IsUpperCase = false;

    StartVolIndex = -1;
    StartParsingVol = 0;
    NumVols = 0;
    EndVolIndex = -1;

    BaseName.Empty();
    MissingName.Empty();

    ecd_wasRead = false;

    Streams.Clear();
    ZipStream.Release();
  }

  HRESULT ParseArcName(IArchiveOpenVolumeCallback *volCallback);

  HRESULT Read(void *data, UInt32 size, UInt32 *processedSize);
  
  UInt64 GetTotalSize() const
  {
    UInt64 total = 0;
    FOR_VECTOR (i, Streams)
      total += Streams[i].Size;
    return total;
  }
};


class CVolStream:
  public ISequentialInStream,
  public CMyUnknownImp
{
public:
  CVols *Vols;
  
  MY_UNKNOWN_IMP1(ISequentialInStream)

  STDMETHOD(Read)(void *data, UInt32 size, UInt32 *processedSize);
};


class CInArchive
{
  CInBuffer _inBuffer;
  bool _inBufMode;
  UInt32 m_Signature;
  UInt64 m_Position;

  UInt64 _processedCnt;
  
  bool CanStartNewVol;

  CMyComPtr<IInStream> StreamRef;
  IInStream *Stream;
  IInStream *StartStream;

  bool IsArcOpen;

  HRESULT ReadVols2(IArchiveOpenVolumeCallback *volCallback,
      unsigned start, int lastDisk, int zipDisk, unsigned numMissingVolsMax, unsigned &numMissingVols);
  HRESULT ReadVols();

  HRESULT Seek(UInt64 offset);
  HRESULT FindMarker(IInStream *stream, const UInt64 *searchLimit);
  HRESULT IncreaseRealPosition(Int64 addValue, bool &isFinished);

  HRESULT ReadBytes(void *data, UInt32 size, UInt32 *processedSize);
  void SafeReadBytes(void *data, unsigned size);
  void ReadBuffer(CByteBuffer &buffer, unsigned size);
  Byte ReadByte();
  UInt16 ReadUInt16();
  UInt32 ReadUInt32();
  UInt64 ReadUInt64();
  void Skip(unsigned num);
  void Skip64(UInt64 num);
  void ReadFileName(unsigned nameSize, AString &dest);

  bool ReadExtra(unsigned extraSize, CExtraBlock &extraBlock,
      UInt64 &unpackSize, UInt64 &packSize, UInt64 &localHeaderOffset, UInt32 &diskStartNumber);
  bool ReadLocalItem(CItemEx &item);
  HRESULT ReadLocalItemDescriptor(CItemEx &item);
  HRESULT ReadCdItem(CItemEx &item);
  HRESULT TryEcd64(UInt64 offset, CCdInfo &cdInfo);
  HRESULT FindCd(bool checkOffsetMode);
  HRESULT TryReadCd(CObjectVector<CItemEx> &items, const CCdInfo &cdInfo, UInt64 cdOffset, UInt64 cdSize);
  HRESULT ReadCd(CObjectVector<CItemEx> &items, UInt32 &cdDisk, UInt64 &cdOffset, UInt64 &cdSize);
  HRESULT ReadLocals(CObjectVector<CItemEx> &localItems);

  HRESULT ReadHeaders2(CObjectVector<CItemEx> &items);

  HRESULT GetVolStream(unsigned vol, UInt64 pos, CMyComPtr<ISequentialInStream> &stream);
public:
  CInArchiveInfo ArcInfo;
  
  bool IsArc;
  bool IsZip64;
  bool HeadersError;
  bool HeadersWarning;
  bool ExtraMinorError;
  bool UnexpectedEnd;
  bool NoCentralDir;

  bool MarkerIsFound;

  bool IsMultiVol;
  bool UseDisk_in_SingleVol;
  UInt32 EcdVolIndex;

  CVols Vols;
 
  IArchiveOpenCallback *Callback;

  CInArchive(): Stream(NULL), Callback(NULL), IsArcOpen(false) {}

  UInt64 GetPhySize() const
  {
    if (IsMultiVol)
      return ArcInfo.FinishPos;
    else
      return ArcInfo.FinishPos - ArcInfo.Base;
  }

  UInt64 GetOffset() const
  {
    if (IsMultiVol)
      return 0;
    else
      return ArcInfo.Base;
  }

  
  void ClearRefs();
  void Close();
  HRESULT Open(IInStream *stream, const UInt64 *searchLimit, IArchiveOpenCallback *callback, CObjectVector<CItemEx> &items);
  HRESULT ReadHeaders(CObjectVector<CItemEx> &items);

  bool IsOpen() const { return IsArcOpen; }
  
  bool AreThereErrors() const
  {
    return HeadersError
        || UnexpectedEnd
        || !Vols.MissingName.IsEmpty();
  }

  bool IsLocalOffsetOK(const CItemEx &item) const
  {
    if (item.FromLocal)
      return true;
    return (Int64)GetOffset() + (Int64)item.LocalHeaderPos >= 0;
  }

  UInt64 GetEmbeddedStubSize() const
  {
    if (ArcInfo.CdWasRead)
      return ArcInfo.FirstItemRelatOffset;
    if (IsMultiVol)
      return 0;
    return ArcInfo.MarkerPos2 - ArcInfo.Base;
  }


  HRESULT ReadLocalItemAfterCdItem(CItemEx &item, bool &isAvail);
  HRESULT ReadLocalItemAfterCdItemFull(CItemEx &item);

  HRESULT GetItemStream(const CItemEx &item, bool seekPackData, CMyComPtr<ISequentialInStream> &stream);

  IInStream *GetBaseStream() { return StreamRef; }

  bool CanUpdate() const
  {
    if (AreThereErrors()
       || IsMultiVol
       || ArcInfo.Base < 0
       || (Int64)ArcInfo.MarkerPos2 < ArcInfo.Base
       || ArcInfo.ThereIsTail
       || GetEmbeddedStubSize() != 0)
      return false;
   
    // 7-zip probably can update archives with embedded stubs.
    // we just disable that feature for more safety.

    return true;
  }
};
  
}}
  
#endif
