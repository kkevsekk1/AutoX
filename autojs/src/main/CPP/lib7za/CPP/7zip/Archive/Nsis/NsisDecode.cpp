// NsisDecode.cpp

#include "StdAfx.h"

#include "../../../../C/CpuArch.h"

#include "NsisDecode.h"

#include "../../Common/CreateCoder.h"
#include "../../Common/LimitedStreams.h"
#include "../../Common/MethodId.h"

#include "../../Compress/BcjCoder.h"
#include "../../Compress/BZip2Decoder.h"
#include "../../Compress/DeflateDecoder.h"

#define Get32(p) GetUi32(p)

namespace NArchive {
namespace NNsis {

HRESULT CDecoder::Init(ISequentialInStream *inStream, bool &useFilter)
{
  useFilter = false;

  if (_decoderInStream)
    if (Method != _curMethod)
      Release();
  _curMethod = Method;
  if (!_codecInStream)
  {
    switch (Method)
    {
      // case NMethodType::kCopy: return E_NOTIMPL;
      case NMethodType::kDeflate: _codecInStream = new NCompress::NDeflate::NDecoder::CNsisCOMCoder(); break;
      case NMethodType::kBZip2: _codecInStream = new NCompress::NBZip2::CNsisDecoder(); break;
      case NMethodType::kLZMA:
        _lzmaDecoder = new NCompress::NLzma::CDecoder();
        _codecInStream = _lzmaDecoder;
        break;
      default: return E_NOTIMPL;
    }
  }

  if (FilterFlag)
  {
    Byte flag;
    RINOK(ReadStream_FALSE(inStream, &flag, 1));
    if (flag > 1)
      return E_NOTIMPL;
    useFilter = (flag != 0);
  }
  
  if (!useFilter)
    _decoderInStream = _codecInStream;
  else
  {
    if (!_filterInStream)
    {
      _filter = new CFilterCoder(false);
      _filterInStream = _filter;
      _filter->Filter = new NCompress::NBcj::CCoder(false);
    }
    RINOK(_filter->SetInStream(_codecInStream));
    _decoderInStream = _filterInStream;
  }

  if (Method == NMethodType::kLZMA)
  {
    const unsigned kPropsSize = LZMA_PROPS_SIZE;
    Byte props[kPropsSize];
    RINOK(ReadStream_FALSE(inStream, props, kPropsSize));
    RINOK(_lzmaDecoder->SetDecoderProperties2((const Byte *)props, kPropsSize));
  }

  {
    CMyComPtr<ICompressSetInStream> setInStream;
    _codecInStream.QueryInterface(IID_ICompressSetInStream, &setInStream);
    if (!setInStream)
      return E_NOTIMPL;
    RINOK(setInStream->SetInStream(inStream));
  }

  {
    CMyComPtr<ICompressSetOutStreamSize> setOutStreamSize;
    _codecInStream.QueryInterface(IID_ICompressSetOutStreamSize, &setOutStreamSize);
    if (!setOutStreamSize)
      return E_NOTIMPL;
    RINOK(setOutStreamSize->SetOutStreamSize(NULL));
  }

  if (useFilter)
  {
    RINOK(_filter->SetOutStreamSize(NULL));
  }

  return S_OK;
}

static const UInt32 kMask_IsCompressed = (UInt32)1 << 31;

HRESULT CDecoder::SetToPos(UInt64 pos, ICompressProgressInfo *progress)
{
  if (StreamPos > pos)
    return E_FAIL;
  UInt64 inSizeStart = 0;
  if (_lzmaDecoder)
    inSizeStart = _lzmaDecoder->GetInputProcessedSize();
  UInt64 offset = 0;
  while (StreamPos < pos)
  {
    size_t size = (size_t)MyMin(pos - StreamPos, (UInt64)Buffer.Size());
    RINOK(Read(Buffer, &size));
    if (size == 0)
      return S_FALSE;
    StreamPos += size;
    offset += size;

    UInt64 inSize = 0;
    if (_lzmaDecoder)
      inSize = _lzmaDecoder->GetInputProcessedSize() - inSizeStart;
    RINOK(progress->SetRatioInfo(&inSize, &offset));
  }
  return S_OK;
}

HRESULT CDecoder::Decode(CByteBuffer *outBuf, bool unpackSizeDefined, UInt32 unpackSize,
    ISequentialOutStream *realOutStream, ICompressProgressInfo *progress,
    UInt32 &packSizeRes, UInt32 &unpackSizeRes)
{
  CLimitedSequentialInStream *limitedStreamSpec = NULL;
  CMyComPtr<ISequentialInStream> limitedStream;
  packSizeRes = 0;
  unpackSizeRes = 0;

  if (Solid)
  {
    Byte temp[4];
    size_t processedSize = 4;
    RINOK(Read(temp, &processedSize));
    if (processedSize != 4)
      return S_FALSE;
    StreamPos += processedSize;
    UInt32 size = Get32(temp);
    if (unpackSizeDefined && size != unpackSize)
      return S_FALSE;
    unpackSize = size;
    unpackSizeDefined = true;
  }
  else
  {
    Byte temp[4];
    RINOK(ReadStream_FALSE(InputStream, temp, 4));
    StreamPos += 4;
    UInt32 size = Get32(temp);

    if ((size & kMask_IsCompressed) == 0)
    {
      if (unpackSizeDefined && size != unpackSize)
        return S_FALSE;
      packSizeRes = size;
      if (outBuf)
        outBuf->Alloc(size);

      UInt64 offset = 0;
      
      while (size > 0)
      {
        UInt32 curSize = (UInt32)MyMin((size_t)size, Buffer.Size());
        UInt32 processedSize;
        RINOK(InputStream->Read(Buffer, curSize, &processedSize));
        if (processedSize == 0)
          return S_FALSE;
        if (outBuf)
          memcpy((Byte *)*outBuf + (size_t)offset, Buffer, processedSize);
        offset += processedSize;
        size -= processedSize;
        StreamPos += processedSize;
        unpackSizeRes += processedSize;
        if (realOutStream)
          RINOK(WriteStream(realOutStream, Buffer, processedSize));
        RINOK(progress->SetRatioInfo(&offset, &offset));
      }

      return S_OK;
    }
    
    size &= ~kMask_IsCompressed;
    packSizeRes = size;
    limitedStreamSpec = new CLimitedSequentialInStream;
    limitedStream = limitedStreamSpec;
    limitedStreamSpec->SetStream(InputStream);
    limitedStreamSpec->Init(size);
    {
      bool useFilter;
      RINOK(Init(limitedStream, useFilter));
    }
  }
  
  if (outBuf)
  {
    if (!unpackSizeDefined)
      return S_FALSE;
    outBuf->Alloc(unpackSize);
  }

  UInt64 inSizeStart = 0;
  if (_lzmaDecoder)
    inSizeStart = _lzmaDecoder->GetInputProcessedSize();

  // we don't allow files larger than 4 GB;
  if (!unpackSizeDefined)
    unpackSize = 0xFFFFFFFF;
  UInt32 offset = 0;

  for (;;)
  {
    size_t rem = unpackSize - offset;
    if (rem == 0)
      break;
    size_t size = Buffer.Size();
    if (size > rem)
      size = rem;
    RINOK(Read(Buffer, &size));
    if (size == 0)
    {
      if (unpackSizeDefined)
        return S_FALSE;
      break;
    }
    if (outBuf)
      memcpy((Byte *)*outBuf + (size_t)offset, Buffer, size);
    StreamPos += size;
    offset += (UInt32)size;

    UInt64 inSize = 0; // it can be improved: we need inSize for Deflate and BZip2 too.
    if (_lzmaDecoder)
      inSize = _lzmaDecoder->GetInputProcessedSize() - inSizeStart;
    if (Solid)
      packSizeRes = (UInt32)inSize;
    unpackSizeRes += (UInt32)size;
    
    UInt64 outSize = offset;
    RINOK(progress->SetRatioInfo(&inSize, &outSize));
    if (realOutStream)
      RINOK(WriteStream(realOutStream, Buffer, size));
  }
  return S_OK;
}

}}
