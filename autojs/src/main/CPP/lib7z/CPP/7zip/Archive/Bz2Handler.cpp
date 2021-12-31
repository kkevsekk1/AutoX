// Bz2Handler.cpp

#include "StdAfx.h"

#include "../../Common/ComTry.h"

#include "../Common/ProgressUtils.h"
#include "../Common/RegisterArc.h"
#include "../Common/StreamUtils.h"

#include "../Compress/BZip2Decoder.h"
#include "../Compress/BZip2Encoder.h"
#include "../Compress/CopyCoder.h"

#include "Common/DummyOutStream.h"
#include "Common/HandlerOut.h"

using namespace NWindows;

namespace NArchive {
namespace NBz2 {

class CHandler:
  public IInArchive,
  public IArchiveOpenSeq,
  public IOutArchive,
  public ISetProperties,
  public CMyUnknownImp
{
  CMyComPtr<IInStream> _stream;
  CMyComPtr<ISequentialInStream> _seqStream;
  
  bool _isArc;
  bool _needSeekToStart;
  bool _dataAfterEnd;
  bool _needMoreInput;

  bool _packSize_Defined;
  bool _unpackSize_Defined;
  bool _numStreams_Defined;
  bool _numBlocks_Defined;

  UInt64 _packSize;
  UInt64 _unpackSize;
  UInt64 _numStreams;
  UInt64 _numBlocks;

  CSingleMethodProps _props;

public:
  MY_UNKNOWN_IMP4(
      IInArchive,
      IArchiveOpenSeq,
      IOutArchive,
      ISetProperties)
  INTERFACE_IInArchive(;)
  INTERFACE_IOutArchive(;)
  STDMETHOD(OpenSeq)(ISequentialInStream *stream);
  STDMETHOD(SetProperties)(const wchar_t * const *names, const PROPVARIANT *values, UInt32 numProps);

  CHandler() { }
};

static const Byte kProps[] =
{
  kpidSize,
  kpidPackSize
};

static const Byte kArcProps[] =
{
  kpidNumStreams,
  kpidNumBlocks
};

IMP_IInArchive_Props
IMP_IInArchive_ArcProps

STDMETHODIMP CHandler::GetArchiveProperty(PROPID propID, PROPVARIANT *value)
{
  NCOM::CPropVariant prop;
  switch (propID)
  {
    case kpidPhySize: if (_packSize_Defined) prop = _packSize; break;
    case kpidUnpackSize: if (_unpackSize_Defined) prop = _unpackSize; break;
    case kpidNumStreams: if (_numStreams_Defined) prop = _numStreams; break;
    case kpidNumBlocks: if (_numBlocks_Defined) prop = _numBlocks; break;
    case kpidErrorFlags:
    {
      UInt32 v = 0;
      if (!_isArc) v |= kpv_ErrorFlags_IsNotArc;;
      if (_needMoreInput) v |= kpv_ErrorFlags_UnexpectedEnd;
      if (_dataAfterEnd) v |= kpv_ErrorFlags_DataAfterEnd;
      prop = v;
    }
  }
  prop.Detach(value);
  return S_OK;
}

STDMETHODIMP CHandler::GetNumberOfItems(UInt32 *numItems)
{
  *numItems = 1;
  return S_OK;
}

STDMETHODIMP CHandler::GetProperty(UInt32 /* index */, PROPID propID, PROPVARIANT *value)
{
  NCOM::CPropVariant prop;
  switch (propID)
  {
    case kpidPackSize: if (_packSize_Defined) prop = _packSize; break;
    case kpidSize: if (_unpackSize_Defined) prop = _unpackSize; break;
  }
  prop.Detach(value);
  return S_OK;
}

static const unsigned kSignatureCheckSize = 10;

API_FUNC_static_IsArc IsArc_BZip2(const Byte *p, size_t size)
{
  if (size < kSignatureCheckSize)
    return k_IsArc_Res_NEED_MORE;
  if (p[0] != 'B' || p[1] != 'Z' || p[2] != 'h' || p[3] < '1' || p[3] > '9')
    return k_IsArc_Res_NO;
  p += 4;
  if (NCompress::NBZip2::IsBlockSig(p))
    return k_IsArc_Res_YES;
  if (NCompress::NBZip2::IsEndSig(p))
    return k_IsArc_Res_YES;
  return k_IsArc_Res_NO;
}
}

STDMETHODIMP CHandler::Open(IInStream *stream, const UInt64 *, IArchiveOpenCallback *)
{
  COM_TRY_BEGIN
  Close();
  {
    Byte buf[kSignatureCheckSize];
    RINOK(ReadStream_FALSE(stream, buf, kSignatureCheckSize));
    if (IsArc_BZip2(buf, kSignatureCheckSize) == k_IsArc_Res_NO)
      return S_FALSE;
    _isArc = true;
    _stream = stream;
    _seqStream = stream;
    _needSeekToStart = true;
  }
  return S_OK;
  COM_TRY_END
}


STDMETHODIMP CHandler::OpenSeq(ISequentialInStream *stream)
{
  Close();
  _isArc = true;
  _seqStream = stream;
  return S_OK;
}

STDMETHODIMP CHandler::Close()
{
  _isArc = false;
  _needSeekToStart = false;
  _dataAfterEnd = false;
  _needMoreInput = false;

  _packSize_Defined = false;
  _unpackSize_Defined = false;
  _numStreams_Defined = false;
  _numBlocks_Defined = false;

  _packSize = 0;

  _seqStream.Release();
  _stream.Release();
  return S_OK;
}

STDMETHODIMP CHandler::Extract(const UInt32 *indices, UInt32 numItems,
    Int32 testMode, IArchiveExtractCallback *extractCallback)
{
  COM_TRY_BEGIN
  if (numItems == 0)
    return S_OK;
  if (numItems != (UInt32)(Int32)-1 && (numItems != 1 || indices[0] != 0))
    return E_INVALIDARG;

  if (_packSize_Defined)
    extractCallback->SetTotal(_packSize);

  // RINOK(extractCallback->SetCompleted(&packSize));

  CMyComPtr<ISequentialOutStream> realOutStream;
  Int32 askMode = testMode ?
      NExtract::NAskMode::kTest :
      NExtract::NAskMode::kExtract;
  RINOK(extractCallback->GetStream(0, &realOutStream, askMode));
  if (!testMode && !realOutStream)
    return S_OK;

  extractCallback->PrepareOperation(askMode);


  if (_needSeekToStart)
  {
    if (!_stream)
      return E_FAIL;
    RINOK(_stream->Seek(0, STREAM_SEEK_SET, NULL));
  }
  else
    _needSeekToStart = true;

  Int32 opRes;

  try
  {

  NCompress::NBZip2::CDecoder *decoderSpec = new NCompress::NBZip2::CDecoder;
  CMyComPtr<ICompressCoder> decoder = decoderSpec;
  decoderSpec->SetInStream(_seqStream);

  #ifndef _7ZIP_ST
  RINOK(decoderSpec->SetNumberOfThreads(_props._numThreads));
  #endif

  CDummyOutStream *outStreamSpec = new CDummyOutStream;
  CMyComPtr<ISequentialOutStream> outStream(outStreamSpec);
  outStreamSpec->SetStream(realOutStream);
  outStreamSpec->Init();
  
  realOutStream.Release();

  CLocalProgress *lps = new CLocalProgress;
  CMyComPtr<ICompressProgressInfo> progress = lps;
  lps->Init(extractCallback, true);

  UInt64 packSize = 0;
  UInt64 unpackedSize = 0;
  UInt64 numStreams = 0;

  decoderSpec->InitNumBlocks();
  
  HRESULT result = S_OK;

  for (;;)
  {
    lps->InSize = packSize;
    lps->OutSize = unpackedSize;

    RINOK(lps->SetCur());

    result = decoderSpec->CodeResume(outStream, progress);

    if (result != S_FALSE && result != S_OK)
      return result;

    if (decoderSpec->IsBz)
      numStreams++;
    else if (numStreams == 0)
    {
      _isArc = false;
      result = S_FALSE;
      break;
    }

    unpackedSize = outStreamSpec->GetSize();
    UInt64 streamSize = decoderSpec->GetStreamSize();

    if (streamSize == packSize)
    {
      // no new bytes in input stream, So it's good end of archive.
      result = S_OK;
      break;
    }
    
    if (!decoderSpec->IsBz)
    {
      _dataAfterEnd = true;
      result = S_FALSE;
      break;
    }

    if (decoderSpec->Base.BitDecoder.ExtraBitsWereRead())
    {
      _needMoreInput = true;
      packSize = streamSize;
      result = S_FALSE;
      break;
    }

    packSize = decoderSpec->GetInputProcessedSize();

    if (packSize > streamSize)
      return E_FAIL;

    if (result != S_OK)
      break;
  }

  if (numStreams != 0)
  {
    _packSize = packSize;
    _unpackSize = unpackedSize;
    _numStreams = numStreams;
    _numBlocks = decoderSpec->GetNumBlocks();

    _packSize_Defined = true;
    _unpackSize_Defined = true;
    _numStreams_Defined = true;
    _numBlocks_Defined = true;
  }
  
  decoderSpec->ReleaseInStream();
  outStream.Release();

  if (!_isArc)
    opRes = NExtract::NOperationResult::kIsNotArc;
  else if (_needMoreInput)
    opRes = NExtract::NOperationResult::kUnexpectedEnd;
  else if (decoderSpec->CrcError)
    opRes = NExtract::NOperationResult::kCRCError;
  else if (_dataAfterEnd)
    opRes = NExtract::NOperationResult::kDataAfterEnd;
  else if (result == S_FALSE)
    opRes = NExtract::NOperationResult::kDataError;
  else if (result == S_OK)
    opRes = NExtract::NOperationResult::kOK;
  else
    return result;

  }
  catch(const CInBufferException &e)  { return e.ErrorCode; }

  return extractCallback->SetOperationResult(opRes);

  COM_TRY_END
}

static HRESULT UpdateArchive(
    UInt64 unpackSize,
    ISequentialOutStream *outStream,
    const CProps &props,
    IArchiveUpdateCallback *updateCallback)
{
  RINOK(updateCallback->SetTotal(unpackSize));
  CMyComPtr<ISequentialInStream> fileInStream;
  RINOK(updateCallback->GetStream(0, &fileInStream));
  CLocalProgress *localProgressSpec = new CLocalProgress;
  CMyComPtr<ICompressProgressInfo> localProgress = localProgressSpec;
  localProgressSpec->Init(updateCallback, true);
  NCompress::NBZip2::CEncoder *encoderSpec = new NCompress::NBZip2::CEncoder;
  CMyComPtr<ICompressCoder> encoder = encoderSpec;
  RINOK(props.SetCoderProps(encoderSpec, NULL));
  RINOK(encoder->Code(fileInStream, outStream, NULL, NULL, localProgress));
  return updateCallback->SetOperationResult(NArchive::NUpdate::NOperationResult::kOK);
}

STDMETHODIMP CHandler::GetFileTimeType(UInt32 *type)
{
  *type = NFileTimeType::kUnix;
  return S_OK;
}

STDMETHODIMP CHandler::UpdateItems(ISequentialOutStream *outStream, UInt32 numItems,
    IArchiveUpdateCallback *updateCallback)
{
  COM_TRY_BEGIN

  if (numItems != 1)
    return E_INVALIDARG;

  Int32 newData, newProps;
  UInt32 indexInArchive;
  if (!updateCallback)
    return E_FAIL;
  RINOK(updateCallback->GetUpdateItemInfo(0, &newData, &newProps, &indexInArchive));
 
  if (IntToBool(newProps))
  {
    {
      NCOM::CPropVariant prop;
      RINOK(updateCallback->GetProperty(0, kpidIsDir, &prop));
      if (prop.vt != VT_EMPTY)
        if (prop.vt != VT_BOOL || prop.boolVal != VARIANT_FALSE)
          return E_INVALIDARG;
    }
  }
  
  if (IntToBool(newData))
  {
    UInt64 size;
    {
      NCOM::CPropVariant prop;
      RINOK(updateCallback->GetProperty(0, kpidSize, &prop));
      if (prop.vt != VT_UI8)
        return E_INVALIDARG;
      size = prop.uhVal.QuadPart;
    }
    return UpdateArchive(size, outStream, _props, updateCallback);
  }

  if (indexInArchive != 0)
    return E_INVALIDARG;

  CLocalProgress *lps = new CLocalProgress;
  CMyComPtr<ICompressProgressInfo> progress = lps;
  lps->Init(updateCallback, true);

  CMyComPtr<IArchiveUpdateCallbackFile> opCallback;
  updateCallback->QueryInterface(IID_IArchiveUpdateCallbackFile, (void **)&opCallback);
  if (opCallback)
  {
    RINOK(opCallback->ReportOperation(
        NEventIndexType::kInArcIndex, 0,
        NUpdateNotifyOp::kReplicate))
  }

  if (_stream)
    RINOK(_stream->Seek(0, STREAM_SEEK_SET, NULL));

  return NCompress::CopyStream(_stream, outStream, progress);

  COM_TRY_END
}

STDMETHODIMP CHandler::SetProperties(const wchar_t * const *names, const PROPVARIANT *values, UInt32 numProps)
{
  return _props.SetProperties(names, values, numProps);
}

static const Byte k_Signature[] = { 'B', 'Z', 'h' };

REGISTER_ARC_IO(
  "bzip2", "bz2 bzip2 tbz2 tbz", "* * .tar .tar", 2,
  k_Signature,
  0,
  NArcInfoFlags::kKeepName,
  IsArc_BZip2)

}}
