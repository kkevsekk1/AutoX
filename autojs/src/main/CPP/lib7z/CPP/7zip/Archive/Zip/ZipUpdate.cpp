// ZipUpdate.cpp

#include "StdAfx.h"

#include "../../../../C/Alloc.h"

#include "../../../Common/AutoPtr.h"
#include "../../../Common/Defs.h"
#include "../../../Common/StringConvert.h"

#include "../../../Windows/TimeUtils.h"
#include "../../../Windows/Thread.h"

#include "../../Common/CreateCoder.h"
#include "../../Common/LimitedStreams.h"
#include "../../Common/OutMemStream.h"
#include "../../Common/ProgressUtils.h"
#ifndef _7ZIP_ST
#include "../../Common/ProgressMt.h"
#endif
#include "../../Common/StreamUtils.h"

#include "../../Compress/CopyCoder.h"

#include "ZipAddCommon.h"
#include "ZipOut.h"
#include "ZipUpdate.h"

using namespace NWindows;
using namespace NSynchronization;

namespace NArchive {
namespace NZip {

static const Byte kHostOS =
  #ifdef _WIN32
  NFileHeader::NHostOS::kFAT;
  #else
  NFileHeader::NHostOS::kUnix;
  #endif

static const Byte kMadeByHostOS = kHostOS;
static const Byte kExtractHostOS = kHostOS;

static const Byte kMethodForDirectory = NFileHeader::NCompressionMethod::kStored;

static HRESULT CopyBlockToArchive(ISequentialInStream *inStream, UInt64 size,
    COutArchive &outArchive, ICompressProgressInfo *progress)
{
  CMyComPtr<ISequentialOutStream> outStream;
  outArchive.CreateStreamForCopying(&outStream);
  return NCompress::CopyStream_ExactSize(inStream, outStream, size, progress);
}

static void SetFileHeader(
    COutArchive &archive,
    const CCompressionMethodMode &options,
    const CUpdateItem &ui,
    // bool isSeqMode,
    CItemOut &item)
{
  item.Size = ui.Size;
  bool isDir;

  item.ClearFlags();

  if (ui.NewProps)
  {
    isDir = ui.IsDir;
    item.Name = ui.Name;
    item.SetUtf8(ui.IsUtf8);
    item.ExternalAttrib = ui.Attrib;
    item.Time = ui.Time;
    item.Ntfs_MTime = ui.Ntfs_MTime;
    item.Ntfs_ATime = ui.Ntfs_ATime;
    item.Ntfs_CTime = ui.Ntfs_CTime;
    item.NtfsTimeIsDefined = ui.NtfsTimeIsDefined;
  }
  else
    isDir = item.IsDir();

  item.LocalHeaderPos = archive.GetCurPos();
  item.MadeByVersion.HostOS = kMadeByHostOS;
  item.MadeByVersion.Version = NFileHeader::NCompressionMethod::kMadeByProgramVersion;
  
  item.ExtractVersion.HostOS = kExtractHostOS;

  item.InternalAttrib = 0; // test it
  item.SetEncrypted(!isDir && options.PasswordIsDefined);
  // item.SetDescriptorMode(isSeqMode);

  if (isDir)
  {
    item.ExtractVersion.Version = NFileHeader::NCompressionMethod::kExtractVersion_Dir;
    item.Method = kMethodForDirectory;
    item.PackSize = 0;
    item.Size = 0;
    item.Crc = 0;
  }
}


static void SetItemInfoFromCompressingResult(const CCompressingResult &compressingResult,
    bool isAesMode, Byte aesKeyMode, CItem &item)
{
  item.ExtractVersion.Version = compressingResult.ExtractVersion;
  item.Method = compressingResult.Method;
  item.Crc = compressingResult.CRC;
  item.Size = compressingResult.UnpackSize;
  item.PackSize = compressingResult.PackSize;

  item.LocalExtra.Clear();
  item.CentralExtra.Clear();

  if (isAesMode)
  {
    CWzAesExtra wzAesField;
    wzAesField.Strength = aesKeyMode;
    wzAesField.Method = compressingResult.Method;
    item.Method = NFileHeader::NCompressionMethod::kWzAES;
    item.Crc = 0;
    CExtraSubBlock sb;
    wzAesField.SetSubBlock(sb);
    item.LocalExtra.SubBlocks.Add(sb);
    item.CentralExtra.SubBlocks.Add(sb);
  }
}


#ifndef _7ZIP_ST

static THREAD_FUNC_DECL CoderThread(void *threadCoderInfo);

struct CThreadInfo
{
  DECL_EXTERNAL_CODECS_LOC_VARS2;

  NWindows::CThread Thread;
  NWindows::NSynchronization::CAutoResetEvent CompressEvent;
  NWindows::NSynchronization::CAutoResetEventWFMO CompressionCompletedEvent;
  bool ExitThread;

  CMtCompressProgress *ProgressSpec;
  CMyComPtr<ICompressProgressInfo> Progress;

  COutMemStream *OutStreamSpec;
  CMyComPtr<IOutStream> OutStream;
  CMyComPtr<ISequentialInStream> InStream;

  CAddCommon Coder;
  HRESULT Result;
  CCompressingResult CompressingResult;

  bool IsFree;
  UInt32 UpdateIndex;
  UInt32 FileTime;

  CThreadInfo(const CCompressionMethodMode &options):
      ExitThread(false),
      ProgressSpec(0),
      OutStreamSpec(0),
      Coder(options),
      FileTime(0)
  {}
  
  HRESULT CreateEvents(CSynchro *sync)
  {
    RINOK(CompressEvent.CreateIfNotCreated());
    return CompressionCompletedEvent.CreateIfNotCreated(sync);
  }
  HRes CreateThread() { return Thread.Create(CoderThread, this); }

  void WaitAndCode();
  void StopWaitClose()
  {
    ExitThread = true;
    if (OutStreamSpec != 0)
      OutStreamSpec->StopWriting(E_ABORT);
    if (CompressEvent.IsCreated())
      CompressEvent.Set();
    Thread.Wait();
    Thread.Close();
  }
};

void CThreadInfo::WaitAndCode()
{
  for (;;)
  {
    CompressEvent.Lock();
    if (ExitThread)
      return;
    
    Result = Coder.Compress(
        EXTERNAL_CODECS_LOC_VARS
        InStream, OutStream, FileTime, Progress, CompressingResult);
    
    if (Result == S_OK && Progress)
      Result = Progress->SetRatioInfo(&CompressingResult.UnpackSize, &CompressingResult.PackSize);
    CompressionCompletedEvent.Set();
  }
}

static THREAD_FUNC_DECL CoderThread(void *threadCoderInfo)
{
  ((CThreadInfo *)threadCoderInfo)->WaitAndCode();
  return 0;
}

class CThreads
{
public:
  CObjectVector<CThreadInfo> Threads;
  ~CThreads()
  {
    FOR_VECTOR (i, Threads)
      Threads[i].StopWaitClose();
  }
};

struct CMemBlocks2: public CMemLockBlocks
{
  CCompressingResult CompressingResult;
  bool Defined;
  bool Skip;
  CMemBlocks2(): Defined(false), Skip(false) {}
};

class CMemRefs
{
public:
  CMemBlockManagerMt *Manager;
  CObjectVector<CMemBlocks2> Refs;
  CMemRefs(CMemBlockManagerMt *manager): Manager(manager) {} ;
  ~CMemRefs()
  {
    FOR_VECTOR (i, Refs)
      Refs[i].FreeOpt(Manager);
  }
};

class CMtProgressMixer2:
  public ICompressProgressInfo,
  public CMyUnknownImp
{
  UInt64 ProgressOffset;
  UInt64 InSizes[2];
  UInt64 OutSizes[2];
  CMyComPtr<IProgress> Progress;
  CMyComPtr<ICompressProgressInfo> RatioProgress;
  bool _inSizeIsMain;
public:
  NWindows::NSynchronization::CCriticalSection CriticalSection;
  MY_UNKNOWN_IMP
  void Create(IProgress *progress, bool inSizeIsMain);
  void SetProgressOffset(UInt64 progressOffset);
  HRESULT SetRatioInfo(unsigned index, const UInt64 *inSize, const UInt64 *outSize);
  STDMETHOD(SetRatioInfo)(const UInt64 *inSize, const UInt64 *outSize);
};

void CMtProgressMixer2::Create(IProgress *progress, bool inSizeIsMain)
{
  Progress = progress;
  Progress.QueryInterface(IID_ICompressProgressInfo, &RatioProgress);
  _inSizeIsMain = inSizeIsMain;
  ProgressOffset = InSizes[0] = InSizes[1] = OutSizes[0] = OutSizes[1] = 0;
}

void CMtProgressMixer2::SetProgressOffset(UInt64 progressOffset)
{
  CriticalSection.Enter();
  InSizes[1] = OutSizes[1] = 0;
  ProgressOffset = progressOffset;
  CriticalSection.Leave();
}

HRESULT CMtProgressMixer2::SetRatioInfo(unsigned index, const UInt64 *inSize, const UInt64 *outSize)
{
  NWindows::NSynchronization::CCriticalSectionLock lock(CriticalSection);
  if (index == 0 && RatioProgress)
  {
    RINOK(RatioProgress->SetRatioInfo(inSize, outSize));
  }
  if (inSize)
    InSizes[index] = *inSize;
  if (outSize)
    OutSizes[index] = *outSize;
  UInt64 v = ProgressOffset + (_inSizeIsMain  ?
      (InSizes[0] + InSizes[1]) :
      (OutSizes[0] + OutSizes[1]));
  return Progress->SetCompleted(&v);
}

STDMETHODIMP CMtProgressMixer2::SetRatioInfo(const UInt64 *inSize, const UInt64 *outSize)
{
  return SetRatioInfo(0, inSize, outSize);
}

class CMtProgressMixer:
  public ICompressProgressInfo,
  public CMyUnknownImp
{
public:
  CMtProgressMixer2 *Mixer2;
  CMyComPtr<ICompressProgressInfo> RatioProgress;
  void Create(IProgress *progress, bool inSizeIsMain);
  MY_UNKNOWN_IMP
  STDMETHOD(SetRatioInfo)(const UInt64 *inSize, const UInt64 *outSize);
};

void CMtProgressMixer::Create(IProgress *progress, bool inSizeIsMain)
{
  Mixer2 = new CMtProgressMixer2;
  RatioProgress = Mixer2;
  Mixer2->Create(progress, inSizeIsMain);
}

STDMETHODIMP CMtProgressMixer::SetRatioInfo(const UInt64 *inSize, const UInt64 *outSize)
{
  return Mixer2->SetRatioInfo(1, inSize, outSize);
}


#endif


static HRESULT UpdateItemOldData(
    COutArchive &archive,
    CInArchive *inArchive,
    const CItemEx &itemEx,
    const CUpdateItem &ui,
    CItemOut &item,
    /* bool izZip64, */
    ICompressProgressInfo *progress,
    IArchiveUpdateCallbackFile *opCallback,
    UInt64 &complexity)
{
  if (opCallback)
  {
    RINOK(opCallback->ReportOperation(
        NEventIndexType::kInArcIndex, (UInt32)ui.IndexInArc,
        NUpdateNotifyOp::kReplicate))
  }

  if (ui.NewProps)
  {
    if (item.HasDescriptor())
      return E_NOTIMPL;
    
    // use old name size.
    
    CMyComPtr<ISequentialInStream> packStream;
    RINOK(inArchive->GetItemStream(itemEx, true, packStream));
    if (!packStream)
      return E_NOTIMPL;

    // we keep ExternalAttrib and some another properties from old archive
    // item.ExternalAttrib = ui.Attrib;

    item.Name = ui.Name;
    item.SetUtf8(ui.IsUtf8);
    item.Time = ui.Time;
    item.Ntfs_MTime = ui.Ntfs_MTime;
    item.Ntfs_ATime = ui.Ntfs_ATime;
    item.Ntfs_CTime = ui.Ntfs_CTime;
    item.NtfsTimeIsDefined = ui.NtfsTimeIsDefined;

    item.CentralExtra.RemoveUnknownSubBlocks();
    item.LocalExtra.RemoveUnknownSubBlocks();
    item.LocalHeaderPos = archive.GetCurPos();

    archive.PrepareWriteCompressedData2(item.Name.Len(), item.Size, item.PackSize, item.LocalExtra.HasWzAes());
    archive.WriteLocalHeader(item);

    RINOK(CopyBlockToArchive(packStream, itemEx.PackSize, archive, progress));

    complexity += itemEx.PackSize;
  }
  else
  {
    CMyComPtr<ISequentialInStream> packStream;
    RINOK(inArchive->GetItemStream(itemEx, false, packStream));
    if (!packStream)
      return E_NOTIMPL;
    
    // set new header position
    item.LocalHeaderPos = archive.GetCurPos();
    
    const UInt64 rangeSize = itemEx.GetLocalFullSize();
    
    RINOK(CopyBlockToArchive(packStream, rangeSize, archive, progress));

    complexity += rangeSize;
    archive.MoveCurPos(rangeSize);
  }

  return S_OK;
}


static void WriteDirHeader(COutArchive &archive, const CCompressionMethodMode *options,
    const CUpdateItem &ui, CItemOut &item)
{
  SetFileHeader(archive, *options, ui, item);
  archive.PrepareWriteCompressedData(item.Name.Len(), ui.Size,
      // options->IsRealAesMode()
      false // fixed 9.31
      );
  archive.WriteLocalHeader_And_SeekToNextFile(item);
}


static inline bool IsZero_FILETIME(const FILETIME &ft)
{
  return (ft.dwHighDateTime == 0 && ft.dwLowDateTime == 0);
}

static void UpdatePropsFromStream(CUpdateItem &item, ISequentialInStream *fileInStream,
    IArchiveUpdateCallback *updateCallback, UInt64 &totalComplexity)
{
  CMyComPtr<IStreamGetProps> getProps;
  fileInStream->QueryInterface(IID_IStreamGetProps, (void **)&getProps);
  if (!getProps)
    return;

  FILETIME cTime, aTime, mTime;
  UInt64 size;
  // UInt32 attrib;
  if (getProps->GetProps(&size, &cTime, &aTime, &mTime, NULL) != S_OK)
    return;
  
  if (size != item.Size && size != (UInt64)(Int64)-1)
  {
    Int64 newComplexity = totalComplexity + ((Int64)size - (Int64)item.Size);
    if (newComplexity > 0)
    {
      totalComplexity = newComplexity;
      updateCallback->SetTotal(totalComplexity);
    }
    item.Size = size;
  }
  
  if (!IsZero_FILETIME(mTime))
  {
    item.Ntfs_MTime = mTime;
    FILETIME loc = { 0, 0 };
    if (FileTimeToLocalFileTime(&mTime, &loc))
    {
      item.Time = 0;
      NTime::FileTimeToDosTime(loc, item.Time);
    }
  }

  if (!IsZero_FILETIME(cTime)) item.Ntfs_CTime = cTime;
  if (!IsZero_FILETIME(aTime)) item.Ntfs_ATime = aTime;

  // item.Attrib = attrib;
}


static HRESULT Update2St(
    DECL_EXTERNAL_CODECS_LOC_VARS
    COutArchive &archive,
    CInArchive *inArchive,
    const CObjectVector<CItemEx> &inputItems,
    CObjectVector<CUpdateItem> &updateItems,
    const CCompressionMethodMode *options,
    const CByteBuffer *comment,
    IArchiveUpdateCallback *updateCallback,
    UInt64 &totalComplexity,
    IArchiveUpdateCallbackFile *opCallback)
{
  CLocalProgress *lps = new CLocalProgress;
  CMyComPtr<ICompressProgressInfo> progress = lps;
  lps->Init(updateCallback, true);

  CAddCommon compressor(*options);
  
  CObjectVector<CItemOut> items;
  UInt64 unpackSizeTotal = 0, packSizeTotal = 0;

  FOR_VECTOR (itemIndex, updateItems)
  {
    lps->InSize = unpackSizeTotal;
    lps->OutSize = packSizeTotal;
    RINOK(lps->SetCur());
    CUpdateItem &ui = updateItems[itemIndex];
    CItemEx itemEx;
    CItemOut item;

    if (!ui.NewProps || !ui.NewData)
    {
      itemEx = inputItems[ui.IndexInArc];
      if (inArchive->ReadLocalItemAfterCdItemFull(itemEx) != S_OK)
        return E_NOTIMPL;
      (CItem &)item = itemEx;
    }

    if (ui.NewData)
    {
      bool isDir = ((ui.NewProps) ? ui.IsDir : item.IsDir());
      if (isDir)
      {
        WriteDirHeader(archive, options, ui, item);
      }
      else
      {
        CMyComPtr<ISequentialInStream> fileInStream;
        HRESULT res = updateCallback->GetStream(ui.IndexInClient, &fileInStream);
        if (res == S_FALSE)
        {
          lps->ProgressOffset += ui.Size;
          RINOK(updateCallback->SetOperationResult(NArchive::NUpdate::NOperationResult::kOK));
          continue;
        }
        RINOK(res);
        if (!fileInStream)
          return E_INVALIDARG;

        // bool isSeqMode = false;
        /*
        {
          CMyComPtr<IInStream> inStream2;
          fileInStream->QueryInterface(IID_IInStream, (void **)&inStream2);
          isSeqMode = (inStream2 == NULL);
        }
        */

        UpdatePropsFromStream(ui, fileInStream, updateCallback, totalComplexity);
        SetFileHeader(archive, *options, ui, item);

        // file Size can be 64-bit !!!
        archive.PrepareWriteCompressedData(item.Name.Len(), ui.Size, options->IsRealAesMode());
        CCompressingResult compressingResult;
        CMyComPtr<IOutStream> outStream;
        archive.CreateStreamForCompressing(&outStream);
        
        RINOK(compressor.Compress(
            EXTERNAL_CODECS_LOC_VARS
            fileInStream, outStream,
            ui.Time,
            progress, compressingResult));
        
        if (compressingResult.FileTimeWasUsed)
        {
          /*
          if (!item.HasDescriptor())
            return E_FAIL;
          */
          item.SetDescriptorMode(true);
        }

        SetItemInfoFromCompressingResult(compressingResult, options->IsRealAesMode(), options->AesKeyMode, item);
        archive.WriteLocalHeader_And_SeekToNextFile(item);
        RINOK(updateCallback->SetOperationResult(NArchive::NUpdate::NOperationResult::kOK));
        unpackSizeTotal += item.Size;
        packSizeTotal += item.PackSize;
      }
    }
    else
    {
      UInt64 complexity = 0;
      lps->SendRatio = false;
      RINOK(UpdateItemOldData(archive, inArchive, itemEx, ui, item, progress, opCallback, complexity));
      lps->SendRatio = true;
      lps->ProgressOffset += complexity;
    }
  
    items.Add(item);
    lps->ProgressOffset += kLocalHeaderSize;
  }

  lps->InSize = unpackSizeTotal;
  lps->OutSize = packSizeTotal;
  RINOK(lps->SetCur());
  archive.WriteCentralDir(items, comment);
  return S_OK;
}


static HRESULT Update2(
    DECL_EXTERNAL_CODECS_LOC_VARS
    COutArchive &archive,
    CInArchive *inArchive,
    const CObjectVector<CItemEx> &inputItems,
    CObjectVector<CUpdateItem> &updateItems,
    const CCompressionMethodMode *options,
    const CByteBuffer *comment,
    IArchiveUpdateCallback *updateCallback)
{
  CMyComPtr<IArchiveUpdateCallbackFile> opCallback;
  updateCallback->QueryInterface(IID_IArchiveUpdateCallbackFile, (void **)&opCallback);

  UInt64 complexity = 0;
  UInt64 numFilesToCompress = 0;
  UInt64 numBytesToCompress = 0;
 
  unsigned i;
  
  for (i = 0; i < updateItems.Size(); i++)
  {
    const CUpdateItem &ui = updateItems[i];
    if (ui.NewData)
    {
      complexity += ui.Size;
      numBytesToCompress += ui.Size;
      numFilesToCompress++;
      /*
      if (ui.Commented)
        complexity += ui.CommentRange.Size;
      */
    }
    else
    {
      CItemEx inputItem = inputItems[ui.IndexInArc];
      if (inArchive->ReadLocalItemAfterCdItemFull(inputItem) != S_OK)
        return E_NOTIMPL;
      complexity += inputItem.GetLocalFullSize();
      // complexity += inputItem.GetCentralExtraPlusCommentSize();
    }
    complexity += kLocalHeaderSize;
    complexity += kCentralHeaderSize;
  }

  if (comment)
    complexity += comment->Size();
  complexity++; // end of central
  updateCallback->SetTotal(complexity);

  UInt64 totalComplexity = complexity;

  CAddCommon compressor(*options);
  
  complexity = 0;
  
  CCompressionMethodMode options2;
  if (options != 0)
    options2 = *options;

  #ifndef _7ZIP_ST

  UInt32 numThreads = options->NumThreads;
  const UInt32 kNumMaxThreads = 64;
  if (numThreads > kNumMaxThreads)
    numThreads = kNumMaxThreads;
  if (numThreads > MAXIMUM_WAIT_OBJECTS) // is 64 in Windows (is it 64 in all versions?)
    numThreads = MAXIMUM_WAIT_OBJECTS;
  if (numThreads < 1)
    numThreads = 1;

  
  const size_t kMemPerThread = (1 << 25);
  const size_t kBlockSize = 1 << 16;

  bool mtMode = ((options != 0) && (numThreads > 1));

  if (numFilesToCompress <= 1)
    mtMode = false;

  Byte method = options->MethodSequence.Front();

  if (!mtMode)
  {
    if (options2.MethodInfo.FindProp(NCoderPropID::kNumThreads) < 0)
    {
      // fixed for 9.31. bzip2 default is just one thread.
      if (options2.NumThreadsWasChanged || method == NFileHeader::NCompressionMethod::kBZip2)
        options2.MethodInfo.AddProp_NumThreads(numThreads);
    }
  }
  else
  {
    if (method == NFileHeader::NCompressionMethod::kStored && !options->PasswordIsDefined)
      numThreads = 1;
    if (method == NFileHeader::NCompressionMethod::kBZip2)
    {
      bool fixedNumber;
      UInt32 numBZip2Threads = options2.MethodInfo.Get_BZip2_NumThreads(fixedNumber);
      if (!fixedNumber)
      {
        UInt64 averageSize = numBytesToCompress / numFilesToCompress;
        UInt32 blockSize = options2.MethodInfo.Get_BZip2_BlockSize();
        UInt64 averageNumberOfBlocks = averageSize / blockSize + 1;
        numBZip2Threads = 32;
        if (averageNumberOfBlocks < numBZip2Threads)
          numBZip2Threads = (UInt32)averageNumberOfBlocks;
        options2.MethodInfo.AddProp_NumThreads(numBZip2Threads);
      }
      numThreads /= numBZip2Threads;
    }
    if (method == NFileHeader::NCompressionMethod::kLZMA)
    {
      bool fixedNumber;
      // we suppose that default LZMA is 2 thread. So we don't change it
      UInt32 numLZMAThreads = options2.MethodInfo.Get_Lzma_NumThreads(fixedNumber);
      numThreads /= numLZMAThreads;
    }
    if (numThreads > numFilesToCompress)
      numThreads = (UInt32)numFilesToCompress;
    if (numThreads <= 1)
      mtMode = false;
  }

  if (!mtMode)
  #endif
    return Update2St(
        EXTERNAL_CODECS_LOC_VARS
        archive, inArchive,
        inputItems, updateItems, &options2, comment, updateCallback, totalComplexity, opCallback);


  #ifndef _7ZIP_ST

  // Warning : before memManager, threads and compressingCompletedEvents
  // in order to have a "good" order for the destructor
  NWindows::NSynchronization::CSynchro synchroForCompressingCompletedEvents;
  synchroForCompressingCompletedEvents.Create();
  NWindows::NSynchronization::CSynchro synchroForOutStreamSpec;
  synchroForOutStreamSpec.Create();

  CObjectVector<CItemOut> items;

  CMtProgressMixer *mtProgressMixerSpec = new CMtProgressMixer;
  CMyComPtr<ICompressProgressInfo> progress = mtProgressMixerSpec;
  mtProgressMixerSpec->Create(updateCallback, true);

  CMtCompressProgressMixer mtCompressProgressMixer;
  mtCompressProgressMixer.Init(numThreads, mtProgressMixerSpec->RatioProgress);

  CMemBlockManagerMt memManager(kBlockSize);
  CMemRefs refs(&memManager);

  CThreads threads;
  CRecordVector<HANDLE> compressingCompletedEvents;
  CUIntVector threadIndices;  // list threads in order of updateItems

  {
    RINOK(memManager.AllocateSpaceAlways(&synchroForOutStreamSpec,(size_t)numThreads * (kMemPerThread / kBlockSize)));
    for (i = 0; i < updateItems.Size(); i++)
      refs.Refs.Add(CMemBlocks2());

    for (i = 0; i < numThreads; i++)
      threads.Threads.Add(CThreadInfo(options2));

    for (i = 0; i < numThreads; i++)
    {
      CThreadInfo &threadInfo = threads.Threads[i];
      #ifdef EXTERNAL_CODECS
      threadInfo.__externalCodecs = __externalCodecs;
      #endif
      RINOK(threadInfo.CreateEvents(&synchroForCompressingCompletedEvents));
      threadInfo.OutStreamSpec = new COutMemStream(&memManager);
      RINOK(threadInfo.OutStreamSpec->CreateEvents(&synchroForOutStreamSpec));
      threadInfo.OutStream = threadInfo.OutStreamSpec;
      threadInfo.IsFree = true;
      threadInfo.ProgressSpec = new CMtCompressProgress();
      threadInfo.Progress = threadInfo.ProgressSpec;
      threadInfo.ProgressSpec->Init(&mtCompressProgressMixer, (int)i);
      threadInfo.FileTime = 0; // fix it !
      RINOK(threadInfo.CreateThread());
    }
  }

  unsigned mtItemIndex = 0;
  unsigned itemIndex = 0;
  int lastRealStreamItemIndex = -1;

  while (itemIndex < updateItems.Size())
  {
    if (threadIndices.Size() < numThreads && mtItemIndex < updateItems.Size())
    {
      CUpdateItem &ui = updateItems[mtItemIndex++];
      if (!ui.NewData)
        continue;
      CItemEx itemEx;
      CItemOut item;
      
      if (ui.NewProps)
      {
        if (ui.IsDir)
          continue;
      }
      else
      {
        itemEx = inputItems[ui.IndexInArc];
        if (inArchive->ReadLocalItemAfterCdItemFull(itemEx) != S_OK)
          return E_NOTIMPL;
        (CItem &)item = itemEx;
        if (item.IsDir())
          continue;
      }
      
      CMyComPtr<ISequentialInStream> fileInStream;
      
      {
        NWindows::NSynchronization::CCriticalSectionLock lock(mtProgressMixerSpec->Mixer2->CriticalSection);
        HRESULT res = updateCallback->GetStream(ui.IndexInClient, &fileInStream);
        if (res == S_FALSE)
        {
          complexity += ui.Size;
          complexity += kLocalHeaderSize;
          mtProgressMixerSpec->Mixer2->SetProgressOffset(complexity);
          RINOK(updateCallback->SetOperationResult(NArchive::NUpdate::NOperationResult::kOK));
          refs.Refs[mtItemIndex - 1].Skip = true;
          continue;
        }
        RINOK(res);
        if (!fileInStream)
          return E_INVALIDARG;
        UpdatePropsFromStream(ui, fileInStream, updateCallback, totalComplexity);
        RINOK(updateCallback->SetOperationResult(NArchive::NUpdate::NOperationResult::kOK));
      }

      for (UInt32 k = 0; k < numThreads; k++)
      {
        CThreadInfo &threadInfo = threads.Threads[k];
        if (threadInfo.IsFree)
        {
          threadInfo.IsFree = false;
          threadInfo.InStream = fileInStream;

          // !!!!! we must release ref before sending event
          // BUG was here in v4.43 and v4.44. It could change ref counter in two threads in same time
          fileInStream.Release();

          threadInfo.OutStreamSpec->Init();
          threadInfo.ProgressSpec->Reinit();
          threadInfo.CompressEvent.Set();
          threadInfo.UpdateIndex = mtItemIndex - 1;

          compressingCompletedEvents.Add(threadInfo.CompressionCompletedEvent);
          threadIndices.Add(k);
          break;
        }
      }
 
      continue;
    }
    
    if (refs.Refs[itemIndex].Skip)
    {
      itemIndex++;
      continue;
    }

    const CUpdateItem &ui = updateItems[itemIndex];

    CItemEx itemEx;
    CItemOut item;
    
    if (!ui.NewProps || !ui.NewData)
    {
      itemEx = inputItems[ui.IndexInArc];
      if (inArchive->ReadLocalItemAfterCdItemFull(itemEx) != S_OK)
        return E_NOTIMPL;
      (CItem &)item = itemEx;
    }

    if (ui.NewData)
    {
      bool isDir = ((ui.NewProps) ? ui.IsDir : item.IsDir());
      
      if (isDir)
      {
        WriteDirHeader(archive, options, ui, item);
      }
      else
      {
        if (lastRealStreamItemIndex < (int)itemIndex)
        {
          lastRealStreamItemIndex = itemIndex;
          SetFileHeader(archive, *options, ui, item);
          // file Size can be 64-bit !!!
          archive.PrepareWriteCompressedData(item.Name.Len(), ui.Size, options->IsRealAesMode());
        }

        CMemBlocks2 &memRef = refs.Refs[itemIndex];
        
        if (memRef.Defined)
        {
          CMyComPtr<IOutStream> outStream;
          archive.CreateStreamForCompressing(&outStream);
          memRef.WriteToStream(memManager.GetBlockSize(), outStream);
          SetFileHeader(archive, *options, ui, item);
          // the BUG was fixed in 9.26:
          // SetItemInfoFromCompressingResult must be after SetFileHeader
          // to write correct Size.
          SetItemInfoFromCompressingResult(memRef.CompressingResult,
              options->IsRealAesMode(), options->AesKeyMode, item);
          archive.WriteLocalHeader_And_SeekToNextFile(item);
          // RINOK(updateCallback->SetOperationResult(NArchive::NUpdate::NOperationResult::kOK));
          memRef.FreeOpt(&memManager);
        }
        else
        {
          {
            CThreadInfo &thread = threads.Threads[threadIndices.Front()];
            if (!thread.OutStreamSpec->WasUnlockEventSent())
            {
              CMyComPtr<IOutStream> outStream;
              archive.CreateStreamForCompressing(&outStream);
              thread.OutStreamSpec->SetOutStream(outStream);
              thread.OutStreamSpec->SetRealStreamMode();
            }
          }

          DWORD result = ::WaitForMultipleObjects(compressingCompletedEvents.Size(),
              &compressingCompletedEvents.Front(), FALSE, INFINITE);
#ifdef _WIN32 // FIXME
          if (result == WAIT_FAILED)
          {
            DWORD lastError = GetLastError();
            return lastError != 0 ? lastError : E_FAIL;
          }
#endif
          unsigned t = (unsigned)(result - WAIT_OBJECT_0);
          if (t >= compressingCompletedEvents.Size())
            return E_FAIL;

          CThreadInfo &threadInfo = threads.Threads[threadIndices[t]];
          threadInfo.InStream.Release();
          threadInfo.IsFree = true;
          RINOK(threadInfo.Result);
          threadIndices.Delete(t);
          compressingCompletedEvents.Delete(t);
          
          if (t == 0)
          {
            RINOK(threadInfo.OutStreamSpec->WriteToRealStream());
            threadInfo.OutStreamSpec->ReleaseOutStream();
            SetFileHeader(archive, *options, ui, item);
            SetItemInfoFromCompressingResult(threadInfo.CompressingResult,
                options->IsRealAesMode(), options->AesKeyMode, item);
            archive.WriteLocalHeader_And_SeekToNextFile(item);
          }
          else
          {
            CMemBlocks2 &memRef2 = refs.Refs[threadInfo.UpdateIndex];
            threadInfo.OutStreamSpec->DetachData(memRef2);
            memRef2.CompressingResult = threadInfo.CompressingResult;
            memRef2.Defined = true;
            continue;
          }
        }
      }
    }
    else
    {
      RINOK(UpdateItemOldData(archive, inArchive, itemEx, ui, item, progress, opCallback, complexity));
    }
 
    items.Add(item);
    complexity += kLocalHeaderSize;
    mtProgressMixerSpec->Mixer2->SetProgressOffset(complexity);
    itemIndex++;
  }
  
  RINOK(mtCompressProgressMixer.SetRatioInfo(0, NULL, NULL));
  archive.WriteCentralDir(items, comment);
  return S_OK;
  #endif
}


static const size_t kCacheBlockSize = (1 << 20);
static const size_t kCacheSize = (kCacheBlockSize << 2);
static const size_t kCacheMask = (kCacheSize - 1);

class CCacheOutStream:
  public IOutStream,
  public CMyUnknownImp
{
  CMyComPtr<IOutStream> _stream;
  Byte *_cache;
  UInt64 _virtPos;
  UInt64 _virtSize;
  UInt64 _phyPos;
  UInt64 _phySize; // <= _virtSize
  UInt64 _cachedPos; // (_cachedPos + _cachedSize) <= _virtSize
  size_t _cachedSize;

  HRESULT MyWrite(size_t size);
  HRESULT MyWriteBlock()
  {
    return MyWrite(kCacheBlockSize - ((size_t)_cachedPos & (kCacheBlockSize - 1)));
  }
  HRESULT FlushCache();
public:
  CCacheOutStream(): _cache(0) {}
  ~CCacheOutStream();
  bool Allocate();
  HRESULT Init(IOutStream *stream);
  
  MY_UNKNOWN_IMP

  STDMETHOD(Write)(const void *data, UInt32 size, UInt32 *processedSize);
  STDMETHOD(Seek)(Int64 offset, UInt32 seekOrigin, UInt64 *newPosition);
  STDMETHOD(SetSize)(UInt64 newSize);
};

bool CCacheOutStream::Allocate()
{
  if (!_cache)
    _cache = (Byte *)::MidAlloc(kCacheSize);
  return (_cache != NULL);
}

HRESULT CCacheOutStream::Init(IOutStream *stream)
{
  _virtPos = _phyPos = 0;
  _stream = stream;
  RINOK(_stream->Seek(0, STREAM_SEEK_CUR, &_virtPos));
  RINOK(_stream->Seek(0, STREAM_SEEK_END, &_virtSize));
  RINOK(_stream->Seek(_virtPos, STREAM_SEEK_SET, &_virtPos));
  _phyPos = _virtPos;
  _phySize = _virtSize;
  _cachedPos = 0;
  _cachedSize = 0;
  return S_OK;
}

HRESULT CCacheOutStream::MyWrite(size_t size)
{
  while (size != 0 && _cachedSize != 0)
  {
    if (_phyPos != _cachedPos)
    {
      RINOK(_stream->Seek(_cachedPos, STREAM_SEEK_SET, &_phyPos));
    }
    size_t pos = (size_t)_cachedPos & kCacheMask;
    size_t curSize = MyMin(kCacheSize - pos, _cachedSize);
    curSize = MyMin(curSize, size);
    RINOK(WriteStream(_stream, _cache + pos, curSize));
    _phyPos += curSize;
    if (_phySize < _phyPos)
      _phySize = _phyPos;
    _cachedPos += curSize;
    _cachedSize -= curSize;
    size -= curSize;
  }
  return S_OK;
}

HRESULT CCacheOutStream::FlushCache()
{
  return MyWrite(_cachedSize);
}

CCacheOutStream::~CCacheOutStream()
{
  FlushCache();
  if (_virtSize != _phySize)
    _stream->SetSize(_virtSize);
  if (_virtPos != _phyPos)
    _stream->Seek(_virtPos, STREAM_SEEK_SET, NULL);
  ::MidFree(_cache);
}

STDMETHODIMP CCacheOutStream::Write(const void *data, UInt32 size, UInt32 *processedSize)
{
  if (processedSize)
    *processedSize = 0;
  if (size == 0)
    return S_OK;

  UInt64 zerosStart = _virtPos;
  if (_cachedSize != 0)
  {
    if (_virtPos < _cachedPos)
    {
      RINOK(FlushCache());
    }
    else
    {
      UInt64 cachedEnd = _cachedPos + _cachedSize;
      if (cachedEnd < _virtPos)
      {
        if (cachedEnd < _phySize)
        {
          RINOK(FlushCache());
        }
        else
          zerosStart = cachedEnd;
      }
    }
  }

  if (_cachedSize == 0 && _phySize < _virtPos)
    _cachedPos = zerosStart = _phySize;

  if (zerosStart != _virtPos)
  {
    // write zeros to [cachedEnd ... _virtPos)
    
    for (;;)
    {
      UInt64 cachedEnd = _cachedPos + _cachedSize;
      size_t endPos = (size_t)cachedEnd & kCacheMask;
      size_t curSize = kCacheSize - endPos;
      if (curSize > _virtPos - cachedEnd)
        curSize = (size_t)(_virtPos - cachedEnd);
      if (curSize == 0)
        break;
      while (curSize > (kCacheSize - _cachedSize))
      {
        RINOK(MyWriteBlock());
      }
      memset(_cache + endPos, 0, curSize);
      _cachedSize += curSize;
    }
  }

  if (_cachedSize == 0)
    _cachedPos = _virtPos;

  size_t pos = (size_t)_virtPos & kCacheMask;
  size = (UInt32)MyMin((size_t)size, kCacheSize - pos);
  UInt64 cachedEnd = _cachedPos + _cachedSize;
  if (_virtPos != cachedEnd) // _virtPos < cachedEnd
    size = (UInt32)MyMin((size_t)size, (size_t)(cachedEnd - _virtPos));
  else
  {
    // _virtPos == cachedEnd
    if (_cachedSize == kCacheSize)
    {
      RINOK(MyWriteBlock());
    }
    size_t startPos = (size_t)_cachedPos & kCacheMask;
    if (startPos > pos)
      size = (UInt32)MyMin((size_t)size, (size_t)(startPos - pos));
    _cachedSize += size;
  }
  memcpy(_cache + pos, data, size);
  if (processedSize)
    *processedSize = size;
  _virtPos += size;
  if (_virtSize < _virtPos)
    _virtSize = _virtPos;
  return S_OK;
}

STDMETHODIMP CCacheOutStream::Seek(Int64 offset, UInt32 seekOrigin, UInt64 *newPosition)
{
  switch (seekOrigin)
  {
    case STREAM_SEEK_SET: break;
    case STREAM_SEEK_CUR: offset += _virtPos; break;
    case STREAM_SEEK_END: offset += _virtSize; break;
    default: return STG_E_INVALIDFUNCTION;
  }
  if (offset < 0)
    return HRESULT_WIN32_ERROR_NEGATIVE_SEEK;
  _virtPos = offset;
  if (newPosition)
    *newPosition = offset;
  return S_OK;
}

STDMETHODIMP CCacheOutStream::SetSize(UInt64 newSize)
{
  _virtSize = newSize;
  if (newSize < _phySize)
  {
    RINOK(_stream->SetSize(newSize));
    _phySize = newSize;
  }
  if (newSize <= _cachedPos)
  {
    _cachedSize = 0;
    _cachedPos = newSize;
  }
  if (newSize < _cachedPos + _cachedSize)
    _cachedSize = (size_t)(newSize - _cachedPos);
  return S_OK;
}


HRESULT Update(
    DECL_EXTERNAL_CODECS_LOC_VARS
    const CObjectVector<CItemEx> &inputItems,
    CObjectVector<CUpdateItem> &updateItems,
    ISequentialOutStream *seqOutStream,
    CInArchive *inArchive, bool removeSfx,
    CCompressionMethodMode *compressionMethodMode,
    IArchiveUpdateCallback *updateCallback)
{
  if (inArchive)
  {
    if (!inArchive->CanUpdate())
      return E_NOTIMPL;
  }


  CMyComPtr<IOutStream> outStream;
  {
    CMyComPtr<IOutStream> outStreamReal;
    seqOutStream->QueryInterface(IID_IOutStream, (void **)&outStreamReal);
    if (!outStreamReal)
      return E_NOTIMPL;

    if (inArchive)
    {
      if (!inArchive->IsMultiVol && inArchive->ArcInfo.Base > 0 && !removeSfx)
      {
        IInStream *baseStream = inArchive->GetBaseStream();
        RINOK(baseStream->Seek(0, STREAM_SEEK_SET, NULL));
        RINOK(NCompress::CopyStream_ExactSize(baseStream, outStreamReal, inArchive->ArcInfo.Base, NULL));
      }
    }

    CCacheOutStream *cacheStream = new CCacheOutStream();
    outStream = cacheStream;
    if (!cacheStream->Allocate())
      return E_OUTOFMEMORY;
    RINOK(cacheStream->Init(outStreamReal));
  }

  COutArchive outArchive;
  RINOK(outArchive.Create(outStream));

  if (inArchive)
  {
    if (!inArchive->IsMultiVol && (Int64)inArchive->ArcInfo.MarkerPos2 > inArchive->ArcInfo.Base)
    {
      IInStream *baseStream = inArchive->GetBaseStream();
      RINOK(baseStream->Seek(inArchive->ArcInfo.Base, STREAM_SEEK_SET, NULL));
      UInt64 embStubSize = inArchive->ArcInfo.MarkerPos2 - inArchive->ArcInfo.Base;
      RINOK(NCompress::CopyStream_ExactSize(baseStream, outStream, embStubSize, NULL));
      outArchive.MoveCurPos(embStubSize);
    }
  }

  return Update2(
      EXTERNAL_CODECS_LOC_VARS
      outArchive, inArchive,
      inputItems, updateItems,
      compressionMethodMode,
      inArchive ? &inArchive->ArcInfo.Comment : NULL,
      updateCallback);
}

}}
