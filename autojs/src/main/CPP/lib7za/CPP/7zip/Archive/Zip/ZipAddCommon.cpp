// ZipAddCommon.cpp

#include "StdAfx.h"

#include "../../../../C/7zCrc.h"
#include "../../../../C/Alloc.h"

#include "../../../Windows/PropVariant.h"

#include "../../ICoder.h"
#include "../../IPassword.h"
#include "../../MyVersion.h"

#include "../../Common/CreateCoder.h"
#include "../../Common/StreamObjects.h"
#include "../../Common/StreamUtils.h"

#include "../../Compress/LzmaEncoder.h"
#include "../../Compress/PpmdZip.h"

#include "../Common/InStreamWithCRC.h"

#include "ZipAddCommon.h"
#include "ZipHeader.h"

namespace NArchive {
namespace NZip {

static const CMethodId kMethodId_ZipBase   = 0x040100;
static const CMethodId kMethodId_BZip2     = 0x040202;

static const UInt32 kLzmaPropsSize = 5;
static const UInt32 kLzmaHeaderSize = 4 + kLzmaPropsSize;

class CLzmaEncoder:
  public ICompressCoder,
  public ICompressSetCoderProperties,
  public CMyUnknownImp
{
  NCompress::NLzma::CEncoder *EncoderSpec;
  CMyComPtr<ICompressCoder> Encoder;
  Byte Header[kLzmaHeaderSize];
public:
  STDMETHOD(Code)(ISequentialInStream *inStream, ISequentialOutStream *outStream,
      const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);
  STDMETHOD(SetCoderProperties)(const PROPID *propIDs, const PROPVARIANT *props, UInt32 numProps);

  MY_UNKNOWN_IMP1(ICompressSetCoderProperties)
};

STDMETHODIMP CLzmaEncoder::SetCoderProperties(const PROPID *propIDs, const PROPVARIANT *props, UInt32 numProps)
{
  if (!Encoder)
  {
    EncoderSpec = new NCompress::NLzma::CEncoder;
    Encoder = EncoderSpec;
  }
  CBufPtrSeqOutStream *outStreamSpec = new CBufPtrSeqOutStream;
  CMyComPtr<ISequentialOutStream> outStream(outStreamSpec);
  outStreamSpec->Init(Header + 4, kLzmaPropsSize);
  RINOK(EncoderSpec->SetCoderProperties(propIDs, props, numProps));
  RINOK(EncoderSpec->WriteCoderProperties(outStream));
  if (outStreamSpec->GetPos() != kLzmaPropsSize)
    return E_FAIL;
  Header[0] = MY_VER_MAJOR;
  Header[1] = MY_VER_MINOR;
  Header[2] = kLzmaPropsSize;
  Header[3] = 0;
  return S_OK;
}

STDMETHODIMP CLzmaEncoder::Code(ISequentialInStream *inStream, ISequentialOutStream *outStream,
    const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress)
{
  RINOK(WriteStream(outStream, Header, kLzmaHeaderSize));
  return Encoder->Code(inStream, outStream, inSize, outSize, progress);
}


CAddCommon::CAddCommon(const CCompressionMethodMode &options):
    _options(options),
    _copyCoderSpec(NULL),
    _cryptoStreamSpec(NULL),
    _buf(NULL)
    {}

CAddCommon::~CAddCommon()
{
  MidFree(_buf);
}

static const UInt32 kBufSize = ((UInt32)1 << 16);

HRESULT CAddCommon::CalcStreamCRC(ISequentialInStream *inStream, UInt32 &resultCRC)
{
  if (!_buf)
  {
    _buf = (Byte *)MidAlloc(kBufSize);
    if (!_buf)
      return E_OUTOFMEMORY;
  }

  UInt32 crc = CRC_INIT_VAL;
  for (;;)
  {
    UInt32 processed;
    RINOK(inStream->Read(_buf, kBufSize, &processed));
    if (processed == 0)
    {
      resultCRC = CRC_GET_DIGEST(crc);
      return S_OK;
    }
    crc = CrcUpdate(crc, _buf, (size_t)processed);
  }
}

HRESULT CAddCommon::Compress(
    DECL_EXTERNAL_CODECS_LOC_VARS
    ISequentialInStream *inStream, IOutStream *outStream,
    UInt32 /* fileTime */,
    ICompressProgressInfo *progress, CCompressingResult &opRes)
{
  if (!inStream)
  {
    // We can create empty stream here. But it was already implemented in caller code in 9.33+
    return E_INVALIDARG;
  }

  // CSequentialInStreamWithCRC *inSecCrcStreamSpec = NULL;
  CInStreamWithCRC *inCrcStreamSpec = NULL;
  CMyComPtr<ISequentialInStream> inCrcStream;
  {
    CMyComPtr<IInStream> inStream2;
    
    inStream->QueryInterface(IID_IInStream, (void **)&inStream2);

    if (inStream2)
    {
      inCrcStreamSpec = new CInStreamWithCRC;
      inCrcStream = inCrcStreamSpec;
      inCrcStreamSpec->SetStream(inStream2);
      inCrcStreamSpec->Init();
    }
    else
    {
      // we don't support stdin, since stream from stdin can require 64-bit size header
      return E_NOTIMPL;
      /*
      inSecCrcStreamSpec = new CSequentialInStreamWithCRC;
      inCrcStream = inSecCrcStreamSpec;
      inSecCrcStreamSpec->SetStream(inStream);
      inSecCrcStreamSpec->Init();
      */
    }
  }

  unsigned numTestMethods = _options.MethodSequence.Size();
  
  if (numTestMethods > 1 && !inCrcStreamSpec)
    numTestMethods = 1;

  UInt32 crc = 0;
  bool crc_IsCalculated = false;
  
  Byte method = 0;
  CFilterCoder::C_OutStream_Releaser outStreamReleaser;
  opRes.ExtractVersion = NFileHeader::NCompressionMethod::kExtractVersion_Default;
  opRes.FileTimeWasUsed = false;
  
  for (unsigned i = 0; i < numTestMethods; i++)
  {
    opRes.ExtractVersion = NFileHeader::NCompressionMethod::kExtractVersion_Default;
    if (inCrcStreamSpec)
      RINOK(inCrcStreamSpec->Seek(0, STREAM_SEEK_SET, NULL));
    RINOK(outStream->SetSize(0));
    RINOK(outStream->Seek(0, STREAM_SEEK_SET, NULL));
    
    if (_options.PasswordIsDefined)
    {
      opRes.ExtractVersion = NFileHeader::NCompressionMethod::kExtractVersion_ZipCrypto;

      if (!_cryptoStream)
      {
        _cryptoStreamSpec = new CFilterCoder(true);
        _cryptoStream = _cryptoStreamSpec;
      }
      
      if (_options.IsAesMode)
      {
        opRes.ExtractVersion = NFileHeader::NCompressionMethod::kExtractVersion_Aes;
        if (!_cryptoStreamSpec->Filter)
        {
          _cryptoStreamSpec->Filter = _filterAesSpec = new NCrypto::NWzAes::CEncoder;
          _filterAesSpec->SetKeyMode(_options.AesKeyMode);
          RINOK(_filterAesSpec->CryptoSetPassword((const Byte *)(const char *)_options.Password, _options.Password.Len()));
        }
        RINOK(_filterAesSpec->WriteHeader(outStream));
      }
      else
      {
        if (!_cryptoStreamSpec->Filter)
        {
          _cryptoStreamSpec->Filter = _filterSpec = new NCrypto::NZip::CEncoder;
          _filterSpec->CryptoSetPassword((const Byte *)(const char *)_options.Password, _options.Password.Len());
        }
        
        UInt32 check;
        
        // if (inCrcStreamSpec)
        {
          if (!crc_IsCalculated)
          {
            RINOK(CalcStreamCRC(inStream, crc));
            crc_IsCalculated = true;
            RINOK(inCrcStreamSpec->Seek(0, STREAM_SEEK_SET, NULL));
          }
          check = (crc >> 16);
        }
        /*
        else
        {
          opRes.FileTimeWasUsed = true;
          check = (fileTime & 0xFFFF);
        }
        */
        
        RINOK(_filterSpec->WriteHeader_Check16(outStream, (UInt16)check));
      }
      
      RINOK(_cryptoStreamSpec->SetOutStream(outStream));
      RINOK(_cryptoStreamSpec->InitEncoder());
      outStreamReleaser.FilterCoder = _cryptoStreamSpec;
    }

    method = _options.MethodSequence[i];
    
    switch (method)
    {
      case NFileHeader::NCompressionMethod::kStored:
      {
        if (_copyCoderSpec == NULL)
        {
          _copyCoderSpec = new NCompress::CCopyCoder;
          _copyCoder = _copyCoderSpec;
        }
        CMyComPtr<ISequentialOutStream> outStreamNew;
        if (_options.PasswordIsDefined)
          outStreamNew = _cryptoStream;
        else
          outStreamNew = outStream;
        RINOK(_copyCoder->Code(inCrcStream, outStreamNew, NULL, NULL, progress));
        break;
      }
      
      default:
      {
        if (!_compressEncoder)
        {
          if (method == NFileHeader::NCompressionMethod::kLZMA)
          {
            _compressExtractVersion = NFileHeader::NCompressionMethod::kExtractVersion_LZMA;
            CLzmaEncoder *_lzmaEncoder = new CLzmaEncoder();
            _compressEncoder = _lzmaEncoder;
          }
          else if (method == NFileHeader::NCompressionMethod::kPPMd)
          {
            _compressExtractVersion = NFileHeader::NCompressionMethod::kExtractVersion_PPMd;
            NCompress::NPpmdZip::CEncoder *encoder = new NCompress::NPpmdZip::CEncoder();
            _compressEncoder = encoder;
          }
          else
          {
          CMethodId methodId;
          switch (method)
          {
            case NFileHeader::NCompressionMethod::kBZip2:
              methodId = kMethodId_BZip2;
              _compressExtractVersion = NFileHeader::NCompressionMethod::kExtractVersion_BZip2;
              break;
            default:
              _compressExtractVersion = ((method == NFileHeader::NCompressionMethod::kDeflated64) ?
                  NFileHeader::NCompressionMethod::kExtractVersion_Deflate64 :
                  NFileHeader::NCompressionMethod::kExtractVersion_Deflate);
              methodId = kMethodId_ZipBase + method;
              break;
          }
          RINOK(CreateCoder(
              EXTERNAL_CODECS_LOC_VARS
              methodId, true, _compressEncoder));
          if (!_compressEncoder)
            return E_NOTIMPL;

          if (method == NFileHeader::NCompressionMethod::kDeflated ||
              method == NFileHeader::NCompressionMethod::kDeflated64)
          {
          }
          else if (method == NFileHeader::NCompressionMethod::kBZip2)
          {
          }
          }
          {
            CMyComPtr<ICompressSetCoderProperties> setCoderProps;
            _compressEncoder.QueryInterface(IID_ICompressSetCoderProperties, &setCoderProps);
            if (setCoderProps)
            {
              RINOK(_options.MethodInfo.SetCoderProps(setCoderProps,
                  _options._dataSizeReduceDefined ? &_options._dataSizeReduce : NULL));
            }
          }
        }
        CMyComPtr<ISequentialOutStream> outStreamNew;
        if (_options.PasswordIsDefined)
          outStreamNew = _cryptoStream;
        else
          outStreamNew = outStream;
        if (_compressExtractVersion > opRes.ExtractVersion)
          opRes.ExtractVersion = _compressExtractVersion;
        RINOK(_compressEncoder->Code(inCrcStream, outStreamNew, NULL, NULL, progress));
        break;
      }
    }

    if (_options.PasswordIsDefined)
    {
      RINOK(_cryptoStreamSpec->OutStreamFinish());
      
      if (_options.IsAesMode)
      {
        RINOK(_filterAesSpec->WriteFooter(outStream));
      }
    }
    
    RINOK(outStream->Seek(0, STREAM_SEEK_CUR, &opRes.PackSize));

    // if (inCrcStreamSpec)
    {
      opRes.CRC = inCrcStreamSpec->GetCRC();
      opRes.UnpackSize = inCrcStreamSpec->GetSize();
    }
    /*
    else
    {
      opRes.CRC = inSecCrcStreamSpec->GetCRC();
      opRes.UnpackSize = inSecCrcStreamSpec->GetSize();
    }
    */

    if (_options.PasswordIsDefined)
    {
      if (opRes.PackSize < opRes.UnpackSize +
          (_options.IsAesMode ? _filterAesSpec->GetAddPackSize() : NCrypto::NZip::kHeaderSize))
        break;
    }
    else if (opRes.PackSize < opRes.UnpackSize)
      break;
  }


  opRes.Method = method;
  return S_OK;
}

}}
