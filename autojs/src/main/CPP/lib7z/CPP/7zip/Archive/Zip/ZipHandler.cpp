// ZipHandler.cpp

#include "StdAfx.h"

#include "../../../Common/ComTry.h"
#include "../../../Common/IntToString.h"
#include "../../../Common/StringConvert.h"

#include "../../../Windows/PropVariant.h"
#include "../../../Windows/TimeUtils.h"

#include "../../IPassword.h"

#include "../../Common/FilterCoder.h"
#include "../../Common/LimitedStreams.h"
#include "../../Common/ProgressUtils.h"
#include "../../Common/StreamObjects.h"
#include "../../Common/StreamUtils.h"

#include "../../Compress/CopyCoder.h"
#include "../../Compress/LzmaDecoder.h"
#include "../../Compress/ImplodeDecoder.h"
#include "../../Compress/PpmdZip.h"
#include "../../Compress/ShrinkDecoder.h"

#include "../../Crypto/WzAes.h"
#include "../../Crypto/ZipCrypto.h"
#include "../../Crypto/ZipStrong.h"

#include "../Common/ItemNameUtils.h"
#include "../Common/OutStreamWithCRC.h"

#include "../XzHandler.h"

#include "ZipHandler.h"

using namespace NWindows;

namespace NArchive {
namespace NZip {

static const CMethodId kMethodId_ZipBase = 0x040100;
static const CMethodId kMethodId_BZip2 = 0x040202;

static const char * const kHostOS[] =
{
    "FAT"
  , "AMIGA"
  , "VMS"
  , "Unix"
  , "VM/CMS"
  , "Atari"
  , "HPFS"
  , "Macintosh"
  , "Z-System"
  , "CP/M"
  , "TOPS-20"
  , "NTFS"
  , "SMS/QDOS"
  , "Acorn"
  , "VFAT"
  , "MVS"
  , "BeOS"
  , "Tandem"
  , "OS/400"
  , "OS/X"
};

static const char * const kMethods[] =
{
    "Store"
  , "Shrink"
  , "Reduced1"
  , "Reduced2"
  , "Reduced3"
  , "Reduced4"
  , "Implode"
  , "Tokenizing"
  , "Deflate"
  , "Deflate64"
  , "PKImploding"
};

static const char *kMethod_AES = "AES";
static const char *kMethod_ZipCrypto = "ZipCrypto";
static const char *kMethod_StrongCrypto = "StrongCrypto";

struct CIdToNamePair
{
  unsigned Id;
  const char *Name;
};

static const CIdToNamePair k_MethodIdNamePairs[] =
{
  { NFileHeader::NCompressionMethod::kBZip2, "BZip2" },
  { NFileHeader::NCompressionMethod::kLZMA, "LZMA" },
  { NFileHeader::NCompressionMethod::kXz, "xz" },
  { NFileHeader::NCompressionMethod::kJpeg, "Jpeg" },
  { NFileHeader::NCompressionMethod::kWavPack, "WavPack" },
  { NFileHeader::NCompressionMethod::kPPMd, "PPMd" }
};

static const CIdToNamePair k_StrongCryptoPairs[] =
{
  { NStrongCrypto_AlgId::kDES, "DES" },
  { NStrongCrypto_AlgId::kRC2old, "RC2a" },
  { NStrongCrypto_AlgId::k3DES168, "3DES-168" },
  { NStrongCrypto_AlgId::k3DES112, "3DES-112" },
  { NStrongCrypto_AlgId::kAES128, "pkAES-128" },
  { NStrongCrypto_AlgId::kAES192, "pkAES-192" },
  { NStrongCrypto_AlgId::kAES256, "pkAES-256" },
  { NStrongCrypto_AlgId::kRC2, "RC2" },
  { NStrongCrypto_AlgId::kBlowfish, "Blowfish" },
  { NStrongCrypto_AlgId::kTwofish, "Twofish" },
  { NStrongCrypto_AlgId::kRC4, "RC4" }
};

const char *FindNameForId(const CIdToNamePair *pairs, unsigned num, unsigned id)
{
  for (unsigned i = 0; i < num; i++)
  {
    const CIdToNamePair &pair = pairs[i];
    if (id == pair.Id)
      return pair.Name;
  }
  return NULL;
}

static const Byte kProps[] =
{
  kpidPath,
  kpidIsDir,
  kpidSize,
  kpidPackSize,
  kpidMTime,
  kpidCTime,
  kpidATime,
  kpidAttrib,
  // kpidPosixAttrib,
  kpidEncrypted,
  kpidComment,
  kpidCRC,
  kpidMethod,
  kpidHostOS,
  kpidUnpackVer,
  kpidVolumeIndex
};

static const Byte kArcProps[] =
{
  kpidEmbeddedStubSize,
  kpidBit64,
  kpidComment,
  kpidTotalPhySize,
  kpidIsVolume,
  kpidVolumeIndex,
  kpidNumVolumes
};

CHandler::CHandler()
{
  InitMethodProps();
}

static AString BytesToString(const CByteBuffer &data)
{
  AString s;
  s.SetFrom_CalcLen((const char *)(const Byte *)data, (unsigned)data.Size());
  return s;
}

IMP_IInArchive_Props
IMP_IInArchive_ArcProps

STDMETHODIMP CHandler::GetArchiveProperty(PROPID propID, PROPVARIANT *value)
{
  COM_TRY_BEGIN
  NWindows::NCOM::CPropVariant prop;
  switch (propID)
  {
    case kpidBit64:  if (m_Archive.IsZip64) prop = m_Archive.IsZip64; break;
    case kpidComment:  if (m_Archive.ArcInfo.Comment.Size() != 0) prop = MultiByteToUnicodeString(BytesToString(m_Archive.ArcInfo.Comment), CP_ACP); break;

    case kpidPhySize:  prop = m_Archive.GetPhySize(); break;
    case kpidOffset:  prop = m_Archive.GetOffset(); break;

    case kpidEmbeddedStubSize:
    {
      UInt64 stubSize = m_Archive.GetEmbeddedStubSize();
      if (stubSize != 0)
        prop = stubSize;
      break;
    }

    case kpidTotalPhySize: if (m_Archive.IsMultiVol) prop = m_Archive.Vols.GetTotalSize(); break;
    case kpidVolumeIndex: if (m_Archive.IsMultiVol) prop = (UInt32)m_Archive.Vols.StartVolIndex; break;
    case kpidIsVolume: if (m_Archive.IsMultiVol) prop = true; break;
    case kpidNumVolumes: if (m_Archive.IsMultiVol) prop = (UInt32)m_Archive.Vols.Streams.Size(); break;

    case kpidWarningFlags:
    {
      UInt32 v = 0;
      // if (m_Archive.ExtraMinorError) v |= kpv_ErrorFlags_HeadersError;
      if (m_Archive.HeadersWarning) v |= kpv_ErrorFlags_HeadersError;
      if (v != 0)
        prop = v;
      break;
    }

    case kpidError:
    {
      if (!m_Archive.Vols.MissingName.IsEmpty())
      {
        UString s;
        s.SetFromAscii("Missing volume : ");
        s += m_Archive.Vols.MissingName;
        prop = s;
      }
      break;
    }

    case kpidErrorFlags:
    {
      UInt32 v = 0;
      if (!m_Archive.IsArc) v |= kpv_ErrorFlags_IsNotArc;
      if (m_Archive.HeadersError) v |= kpv_ErrorFlags_HeadersError;
      if (m_Archive.UnexpectedEnd) v |= kpv_ErrorFlags_UnexpectedEnd;
      if (m_Archive.ArcInfo.Base < 0)
      {
        /* We try to support case when we have sfx-zip with embedded stub,
           but the stream has access only to zip part.
           In that case we ignore UnavailableStart error.
           maybe we must show warning in that case. */
        UInt64 stubSize = m_Archive.GetEmbeddedStubSize();
        if (stubSize < (UInt64)-m_Archive.ArcInfo.Base)
          v |= kpv_ErrorFlags_UnavailableStart;
      }
      if (m_Archive.NoCentralDir) v |= kpv_ErrorFlags_UnconfirmedStart;
      prop = v;
      break;
    }

    case kpidReadOnly:
    {
      if (m_Archive.IsOpen())
        if (!m_Archive.CanUpdate())
          prop = true;
      break;
    }
  }
  prop.Detach(value);
  COM_TRY_END
  return S_OK;
}

STDMETHODIMP CHandler::GetNumberOfItems(UInt32 *numItems)
{
  *numItems = m_Items.Size();
  return S_OK;
}

STDMETHODIMP CHandler::GetProperty(UInt32 index, PROPID propID, PROPVARIANT *value)
{
  COM_TRY_BEGIN
  NWindows::NCOM::CPropVariant prop;
  const CItemEx &item = m_Items[index];
  const CExtraBlock &extra = item.GetMainExtra();
  
  switch (propID)
  {
    case kpidPath:
    {
      UString res;
      item.GetUnicodeString(res, item.Name, false, _forceCodePage, _specifiedCodePage);
      NItemName::ConvertToOSName2(res);
      prop = res;
      break;
    }
    
    case kpidIsDir:  prop = item.IsDir(); break;
    case kpidSize:  prop = item.Size; break;
    case kpidPackSize:  prop = item.PackSize; break;
    
    case kpidTimeType:
    {
      FILETIME ft;
      UInt32 unixTime;
      UInt32 type;
      if (extra.GetNtfsTime(NFileHeader::NNtfsExtra::kMTime, ft))
        type = NFileTimeType::kWindows;
      else if (extra.GetUnixTime(true, NFileHeader::NUnixTime::kMTime, unixTime))
        type = NFileTimeType::kUnix;
      else
        type = NFileTimeType::kDOS;
      prop = type;
      break;
    }
    
    case kpidCTime:
    {
      FILETIME ft;
      if (extra.GetNtfsTime(NFileHeader::NNtfsExtra::kCTime, ft))
        prop = ft;
      break;
    }
    
    case kpidATime:
    {
      FILETIME ft;
      if (extra.GetNtfsTime(NFileHeader::NNtfsExtra::kATime, ft))
        prop = ft;
      break;
    }
    
    case kpidMTime:
    {
      FILETIME utc;
      bool defined = true;
      if (!extra.GetNtfsTime(NFileHeader::NNtfsExtra::kMTime, utc))
      {
        UInt32 unixTime = 0;
        if (extra.GetUnixTime(true, NFileHeader::NUnixTime::kMTime, unixTime))
          NTime::UnixTimeToFileTime(unixTime, utc);
        else
        {
          FILETIME localFileTime;
          if (item.Time == 0)
            defined = false;
          else if (!NTime::DosTimeToFileTime(item.Time, localFileTime) ||
              !LocalFileTimeToFileTime(&localFileTime, &utc))
            utc.dwHighDateTime = utc.dwLowDateTime = 0;
        }
      }
      if (defined)
        prop = utc;
      break;
    }
    
    case kpidAttrib:  prop = item.GetWinAttrib(); break;
    
    case kpidPosixAttrib:
    {
      UInt32 attrib;
      if (item.GetPosixAttrib(attrib))
        prop = attrib;
      break;
    }
    
    case kpidEncrypted:  prop = item.IsEncrypted(); break;
    
    case kpidComment:
    {
      if (item.Comment.Size() != 0)
      {
        UString res;
        item.GetUnicodeString(res, BytesToString(item.Comment), true, _forceCodePage, _specifiedCodePage);
        prop = res;
      }
      break;
    }
    
    case kpidCRC:  if (item.IsThereCrc()) prop = item.Crc; break;
    
    case kpidMethod:
    {
      unsigned id = item.Method;
      AString m;
      
      if (item.IsEncrypted())
      {
        if (id == NFileHeader::NCompressionMethod::kWzAES)
        {
          m += kMethod_AES;
          CWzAesExtra aesField;
          if (extra.GetWzAes(aesField))
          {
            char s[16];
            s[0] = '-';
            ConvertUInt32ToString(((unsigned)aesField.Strength + 1) * 64 , s + 1);
            m += s;
            id = aesField.Method;
          }
        }
        else if (item.IsStrongEncrypted())
        {
          CStrongCryptoExtra f;
          f.AlgId = 0;
          if (extra.GetStrongCrypto(f))
          {
            const char *s = FindNameForId(k_StrongCryptoPairs, ARRAY_SIZE(k_StrongCryptoPairs), f.AlgId);
            if (s)
              m += s;
            else
            {
              m += kMethod_StrongCrypto;
              char temp[16];
              temp[0] = ':';
              ConvertUInt32ToString(f.AlgId, temp + 1);
              m += temp;
            }
            if (f.CertificateIsUsed())
              m += "-Cert";
          }
          else
            m += kMethod_StrongCrypto;
        }
        else
          m += kMethod_ZipCrypto;
        m += ' ';
      }
      
      {
        char temp[16];
        const char *s = NULL;
        if (id < ARRAY_SIZE(kMethods))
          s = kMethods[id];
        else
        {
          s = FindNameForId(k_MethodIdNamePairs, ARRAY_SIZE(k_MethodIdNamePairs), id);
          if (!s)
          {
            ConvertUInt32ToString(id, temp);
            s = temp;
          }
        }
        m += s;
        if (id == NFileHeader::NCompressionMethod::kLZMA && item.IsLzmaEOS())
          m += ":EOS";
      }
      
      prop = m;
      break;
    }

    case kpidHostOS:
    {
      Byte hostOS = item.GetHostOS();
      char temp[16];
      const char *s = NULL;
      if (hostOS < ARRAY_SIZE(kHostOS))
        s = kHostOS[hostOS];
      else
      {
        ConvertUInt32ToString(hostOS, temp);
        s = temp;
      }
      prop = s;
      break;
    }
    
    case kpidUnpackVer:
      prop = (UInt32)item.ExtractVersion.Version;
      break;

    case kpidVolumeIndex:
      prop = item.Disk;
      break;
  }
  
  prop.Detach(value);
  return S_OK;
  COM_TRY_END
}


STDMETHODIMP CHandler::Open(IInStream *inStream,
    const UInt64 *maxCheckStartPosition, IArchiveOpenCallback *callback)
{
  COM_TRY_BEGIN
  try
  {
    Close();
    HRESULT res = m_Archive.Open(inStream, maxCheckStartPosition, callback, m_Items);
    if (res != S_OK)
    {
      m_Items.Clear();
      m_Archive.ClearRefs();
    }
    return res;
  }
  catch(...) { Close(); throw; }
  COM_TRY_END
}

STDMETHODIMP CHandler::Close()
{
  m_Items.Clear();
  m_Archive.Close();
  return S_OK;
}


class CLzmaDecoder:
  public ICompressCoder,
  public CMyUnknownImp
{
  NCompress::NLzma::CDecoder *DecoderSpec;
  CMyComPtr<ICompressCoder> Decoder;
public:
  CLzmaDecoder();
  STDMETHOD(Code)(ISequentialInStream *inStream, ISequentialOutStream *outStream,
      const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);

  MY_UNKNOWN_IMP
};

CLzmaDecoder::CLzmaDecoder()
{
  DecoderSpec = new NCompress::NLzma::CDecoder;
  Decoder = DecoderSpec;
}

HRESULT CLzmaDecoder::Code(ISequentialInStream *inStream, ISequentialOutStream *outStream,
    const UInt64 * /* inSize */, const UInt64 *outSize, ICompressProgressInfo *progress)
{
  Byte buf[9];
  RINOK(ReadStream_FALSE(inStream, buf, 9));
  if (buf[2] != 5 || buf[3] != 0)
    return E_NOTIMPL;
  RINOK(DecoderSpec->SetDecoderProperties2(buf + 4, 5));
  return Decoder->Code(inStream, outStream, NULL, outSize, progress);
}


class CXzDecoder:
  public ICompressCoder,
  public CMyUnknownImp
{
  NArchive::NXz::CDecoder _decoder;
public:

  STDMETHOD(Code)(ISequentialInStream *inStream, ISequentialOutStream *outStream,
      const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);

  MY_UNKNOWN_IMP
};

HRESULT CXzDecoder::Code(ISequentialInStream *inStream, ISequentialOutStream *outStream,
    const UInt64 * /* inSize */, const UInt64 * /* outSize */, ICompressProgressInfo *progress)
{
  RINOK(_decoder.Decode(inStream, outStream, progress));
  Int32 opRes = _decoder.Get_Extract_OperationResult();
  if (opRes == NExtract::NOperationResult::kUnsupportedMethod)
    return E_NOTIMPL;
  if (opRes != NExtract::NOperationResult::kOK)
    return S_FALSE;
  return S_OK;
}


struct CMethodItem
{
  unsigned ZipMethod;
  CMyComPtr<ICompressCoder> Coder;
};



class CZipDecoder
{
  NCrypto::NZip::CDecoder *_zipCryptoDecoderSpec;
  NCrypto::NZipStrong::CDecoder *_pkAesDecoderSpec;
  NCrypto::NWzAes::CDecoder *_wzAesDecoderSpec;

  CMyComPtr<ICompressFilter> _zipCryptoDecoder;
  CMyComPtr<ICompressFilter> _pkAesDecoder;
  CMyComPtr<ICompressFilter> _wzAesDecoder;

  CFilterCoder *filterStreamSpec;
  CMyComPtr<ISequentialInStream> filterStream;
  CMyComPtr<ICryptoGetTextPassword> getTextPassword;
  CObjectVector<CMethodItem> methodItems;

public:
  CZipDecoder():
      _zipCryptoDecoderSpec(0),
      _pkAesDecoderSpec(0),
      _wzAesDecoderSpec(0),
      filterStreamSpec(0) {}

  HRESULT Decode(
    DECL_EXTERNAL_CODECS_LOC_VARS
    CInArchive &archive, const CItemEx &item,
    ISequentialOutStream *realOutStream,
    IArchiveExtractCallback *extractCallback,
    ICompressProgressInfo *compressProgress,
    #ifndef _7ZIP_ST
    UInt32 numThreads,
    #endif
    Int32 &res);
};


static HRESULT SkipStreamData(ISequentialInStream *stream, UInt64 size)
{
  const size_t kBufSize = 1 << 12;
  Byte buf[kBufSize];
  for (;;)
  {
    if (size == 0)
      return S_OK;
    size_t curSize = kBufSize;
    if (curSize > size)
      curSize = (size_t)size;
    RINOK(ReadStream_FALSE(stream, buf, curSize));
    size -= curSize;
  }
}


HRESULT CZipDecoder::Decode(
    DECL_EXTERNAL_CODECS_LOC_VARS
    CInArchive &archive, const CItemEx &item,
    ISequentialOutStream *realOutStream,
    IArchiveExtractCallback *extractCallback,
    ICompressProgressInfo *compressProgress,
    #ifndef _7ZIP_ST
    UInt32 numThreads,
    #endif
    Int32 &res)
{
  res = NExtract::NOperationResult::kDataError;
  CFilterCoder::C_InStream_Releaser inStreamReleaser;

  bool needCRC = true;
  bool wzAesMode = false;
  bool pkAesMode = false;
  unsigned id = item.Method;

  if (item.IsEncrypted())
  {
    if (item.IsStrongEncrypted())
    {
      CStrongCryptoExtra f;
      if (item.CentralExtra.GetStrongCrypto(f))
      {
        pkAesMode = true;
      }
      if (!pkAesMode)
      {
        res = NExtract::NOperationResult::kUnsupportedMethod;
        return S_OK;
      }
    }
    if (!pkAesMode && id == NFileHeader::NCompressionMethod::kWzAES)
    {
      CWzAesExtra aesField;
      if (item.GetMainExtra().GetWzAes(aesField))
      {
        wzAesMode = true;
        needCRC = aesField.NeedCrc();
      }
    }
  }
    
  COutStreamWithCRC *outStreamSpec = new COutStreamWithCRC;
  CMyComPtr<ISequentialOutStream> outStream = outStreamSpec;
  outStreamSpec->SetStream(realOutStream);
  outStreamSpec->Init(needCRC);
  
  CMyComPtr<ISequentialInStream> packStream;

  CLimitedSequentialInStream *limitedStreamSpec = new CLimitedSequentialInStream;
  CMyComPtr<ISequentialInStream> inStream(limitedStreamSpec);

  {
    UInt64 packSize = item.PackSize;
    if (wzAesMode)
    {
      if (packSize < NCrypto::NWzAes::kMacSize)
        return S_OK;
      packSize -= NCrypto::NWzAes::kMacSize;
    }
    RINOK(archive.GetItemStream(item, true, packStream));
    if (!packStream)
    {
      res = NExtract::NOperationResult::kUnavailable;
      return S_OK;
    }
    limitedStreamSpec->SetStream(packStream);
    limitedStreamSpec->Init(packSize);
  }
  
  CMyComPtr<ICompressFilter> cryptoFilter;
  
  if (item.IsEncrypted())
  {
    if (wzAesMode)
    {
      CWzAesExtra aesField;
      if (!item.GetMainExtra().GetWzAes(aesField))
        return S_OK;
      id = aesField.Method;
      if (!_wzAesDecoder)
      {
        _wzAesDecoderSpec = new NCrypto::NWzAes::CDecoder;
        _wzAesDecoder = _wzAesDecoderSpec;
      }
      cryptoFilter = _wzAesDecoder;
      if (!_wzAesDecoderSpec->SetKeyMode(aesField.Strength))
      {
        res = NExtract::NOperationResult::kUnsupportedMethod;
        return S_OK;
      }
    }
    else if (pkAesMode)
    {
      if (!_pkAesDecoder)
      {
        _pkAesDecoderSpec = new NCrypto::NZipStrong::CDecoder;
        _pkAesDecoder = _pkAesDecoderSpec;
      }
      cryptoFilter = _pkAesDecoder;
    }
    else
    {
      if (!_zipCryptoDecoder)
      {
        _zipCryptoDecoderSpec = new NCrypto::NZip::CDecoder;
        _zipCryptoDecoder = _zipCryptoDecoderSpec;
      }
      cryptoFilter = _zipCryptoDecoder;
    }
    
    CMyComPtr<ICryptoSetPassword> cryptoSetPassword;
    RINOK(cryptoFilter.QueryInterface(IID_ICryptoSetPassword, &cryptoSetPassword));
    
    if (!getTextPassword)
      extractCallback->QueryInterface(IID_ICryptoGetTextPassword, (void **)&getTextPassword);
    
    if (getTextPassword)
    {
      CMyComBSTR password;
      RINOK(getTextPassword->CryptoGetTextPassword(&password));
      AString charPassword;
      if (password)
      {
        if (wzAesMode || pkAesMode)
        {
          charPassword = UnicodeStringToMultiByte((const wchar_t *)password, CP_ACP);
          /*
          for (unsigned i = 0;; i++)
          {
            wchar_t c = password[i];
            if (c == 0)
              break;
            if (c >= 0x80)
            {
              res = NExtract::NOperationResult::kDataError;
              return S_OK;
            }
            charPassword += (char)c;
          }
          */
        }
        else
        {
          /* pkzip25 / WinZip / Windows probably use ANSI for some files
             We use OEM for compatibility with previous versions of 7-Zip? */
          charPassword = UnicodeStringToMultiByte((const wchar_t *)password, CP_OEMCP);
        }
      }
      HRESULT result = cryptoSetPassword->CryptoSetPassword(
        (const Byte *)(const char *)charPassword, charPassword.Len());
      if (result != S_OK)
        return S_OK;
    }
    else
    {
      RINOK(cryptoSetPassword->CryptoSetPassword(0, 0));
    }
  }
  
  unsigned m;
  for (m = 0; m < methodItems.Size(); m++)
    if (methodItems[m].ZipMethod == id)
      break;

  if (m == methodItems.Size())
  {
    CMethodItem mi;
    mi.ZipMethod = id;
    if (id == NFileHeader::NCompressionMethod::kStored)
      mi.Coder = new NCompress::CCopyCoder;
    else if (id == NFileHeader::NCompressionMethod::kShrunk)
      mi.Coder = new NCompress::NShrink::CDecoder;
    else if (id == NFileHeader::NCompressionMethod::kImploded)
      mi.Coder = new NCompress::NImplode::NDecoder::CCoder;
    else if (id == NFileHeader::NCompressionMethod::kLZMA)
      mi.Coder = new CLzmaDecoder;
    else if (id == NFileHeader::NCompressionMethod::kXz)
      mi.Coder = new CXzDecoder;
    else if (id == NFileHeader::NCompressionMethod::kPPMd)
      mi.Coder = new NCompress::NPpmdZip::CDecoder(true);
    else
    {
      CMethodId szMethodID;
      if (id == NFileHeader::NCompressionMethod::kBZip2)
        szMethodID = kMethodId_BZip2;
      else
      {
        if (id > 0xFF)
        {
          res = NExtract::NOperationResult::kUnsupportedMethod;
          return S_OK;
        }
        szMethodID = kMethodId_ZipBase + (Byte)id;
      }

      RINOK(CreateCoder(EXTERNAL_CODECS_LOC_VARS szMethodID, false, mi.Coder));

      if (mi.Coder == 0)
      {
        res = NExtract::NOperationResult::kUnsupportedMethod;
        return S_OK;
      }
    }
    m = methodItems.Add(mi);
  }

  ICompressCoder *coder = methodItems[m].Coder;
  
  {
    CMyComPtr<ICompressSetDecoderProperties2> setDecoderProperties;
    coder->QueryInterface(IID_ICompressSetDecoderProperties2, (void **)&setDecoderProperties);
    if (setDecoderProperties)
    {
      Byte properties = (Byte)item.Flags;
      RINOK(setDecoderProperties->SetDecoderProperties2(&properties, 1));
    }
  }
  
  #ifndef _7ZIP_ST
  {
    CMyComPtr<ICompressSetCoderMt> setCoderMt;
    coder->QueryInterface(IID_ICompressSetCoderMt, (void **)&setCoderMt);
    if (setCoderMt)
    {
      RINOK(setCoderMt->SetNumberOfThreads(numThreads));
    }
  }
  #endif
  
  {
    HRESULT result = S_OK;
    CMyComPtr<ISequentialInStream> inStreamNew;
    if (item.IsEncrypted())
    {
      if (!filterStream)
      {
        filterStreamSpec = new CFilterCoder(false);
        filterStream = filterStreamSpec;
      }
     
      filterStreamSpec->Filter = cryptoFilter;
      
      if (wzAesMode)
      {
        result = _wzAesDecoderSpec->ReadHeader(inStream);
        if (result == S_OK)
        {
          if (!_wzAesDecoderSpec->Init_and_CheckPassword())
          {
            res = NExtract::NOperationResult::kWrongPassword;
            return S_OK;
          }
        }
      }
      else if (pkAesMode)
      {
        result =_pkAesDecoderSpec->ReadHeader(inStream, item.Crc, item.Size);
        if (result == S_OK)
        {
          bool passwOK;
          result = _pkAesDecoderSpec->Init_and_CheckPassword(passwOK);
          if (result == S_OK && !passwOK)
          {
            res = NExtract::NOperationResult::kWrongPassword;
            return S_OK;
          }
        }
      }
      else
      {
        result = _zipCryptoDecoderSpec->ReadHeader(inStream);
        if (result == S_OK)
        {
          _zipCryptoDecoderSpec->Init_BeforeDecode();
          
          /* Info-ZIP modification to ZipCrypto format:
               if bit 3 of the general purpose bit flag is set,
               it uses high byte of 16-bit File Time.
             Info-ZIP code probably writes 2 bytes of File Time.
             We check only 1 byte. */

          // UInt32 v1 = GetUi16(_zipCryptoDecoderSpec->_header + NCrypto::NZip::kHeaderSize - 2);
          // UInt32 v2 = (item.HasDescriptor() ? (item.Time & 0xFFFF) : (item.Crc >> 16));

          Byte v1 = _zipCryptoDecoderSpec->_header[NCrypto::NZip::kHeaderSize - 1];
          Byte v2 = (Byte)(item.HasDescriptor() ? (item.Time >> 8) : (item.Crc >> 24));

          if (v1 != v2)
          {
            res = NExtract::NOperationResult::kWrongPassword;
            return S_OK;
          }
        }
      }

      if (result == S_OK)
      {
        inStreamReleaser.FilterCoder = filterStreamSpec;
        RINOK(filterStreamSpec->SetInStream(inStream));
        
        /* IFilter::Init() does nothing in all zip crypto filters.
           So we can call any Initialize function in CFilterCoder. */

        RINOK(filterStreamSpec->Init_NoSubFilterInit());
        // RINOK(filterStreamSpec->SetOutStreamSize(NULL));
      
        inStreamNew = filterStream;
      }
    }
    else
      inStreamNew = inStream;

    if (result == S_OK)
      result = coder->Code(inStreamNew, outStream, NULL, &item.Size, compressProgress);
    
    if (result == S_FALSE)
      return S_OK;
    
    if (result == E_NOTIMPL)
    {
      res = NExtract::NOperationResult::kUnsupportedMethod;
      return S_OK;
    }

    RINOK(result);
  }

  bool crcOK = true;
  bool authOk = true;
  if (needCRC)
    crcOK = (outStreamSpec->GetCRC() == item.Crc);
  
  if (wzAesMode)
  {
    const UInt64 rem = limitedStreamSpec->GetRem();
    if (rem != 0)
      if (SkipStreamData(inStream, rem) != S_OK)
        authOk = false;

    limitedStreamSpec->Init(NCrypto::NWzAes::kMacSize);
    if (_wzAesDecoderSpec->CheckMac(inStream, authOk) != S_OK)
      authOk = false;
  }
  
  res = ((crcOK && authOk) ?
    NExtract::NOperationResult::kOK :
    NExtract::NOperationResult::kCRCError);
  return S_OK;
}


STDMETHODIMP CHandler::Extract(const UInt32 *indices, UInt32 numItems,
    Int32 testMode, IArchiveExtractCallback *extractCallback)
{
  COM_TRY_BEGIN
  CZipDecoder myDecoder;
  UInt64 totalUnPacked = 0, totalPacked = 0;
  bool allFilesMode = (numItems == (UInt32)(Int32)-1);
  if (allFilesMode)
    numItems = m_Items.Size();
  if (numItems == 0)
    return S_OK;
  UInt32 i;
  for (i = 0; i < numItems; i++)
  {
    const CItemEx &item = m_Items[allFilesMode ? i : indices[i]];
    totalUnPacked += item.Size;
    totalPacked += item.PackSize;
  }
  RINOK(extractCallback->SetTotal(totalUnPacked));

  UInt64 currentTotalUnPacked = 0, currentTotalPacked = 0;
  UInt64 currentItemUnPacked, currentItemPacked;
  
  CLocalProgress *lps = new CLocalProgress;
  CMyComPtr<ICompressProgressInfo> progress = lps;
  lps->Init(extractCallback, false);

  for (i = 0; i < numItems; i++,
      currentTotalUnPacked += currentItemUnPacked,
      currentTotalPacked += currentItemPacked)
  {
    currentItemUnPacked = 0;
    currentItemPacked = 0;

    lps->InSize = currentTotalPacked;
    lps->OutSize = currentTotalUnPacked;
    RINOK(lps->SetCur());

    CMyComPtr<ISequentialOutStream> realOutStream;
    Int32 askMode = testMode ?
        NExtract::NAskMode::kTest :
        NExtract::NAskMode::kExtract;
    UInt32 index = allFilesMode ? i : indices[i];

    CItemEx item = m_Items[index];
    bool isLocalOffsetOK = m_Archive.IsLocalOffsetOK(item);
    bool skip = !isLocalOffsetOK && !item.IsDir();
    if (skip)
      askMode = NExtract::NAskMode::kSkip;

    currentItemUnPacked = item.Size;
    currentItemPacked = item.PackSize;

    RINOK(extractCallback->GetStream(index, &realOutStream, askMode));

    if (!isLocalOffsetOK)
    {
      RINOK(extractCallback->PrepareOperation(askMode));
      realOutStream.Release();
      RINOK(extractCallback->SetOperationResult(NExtract::NOperationResult::kUnavailable));
      continue;
    }
    
    if (!item.FromLocal)
    {
      bool isAvail = true;
      HRESULT res = m_Archive.ReadLocalItemAfterCdItem(item, isAvail);
      if (res == S_FALSE)
      {
        if (item.IsDir() || realOutStream || testMode)
        {
          RINOK(extractCallback->PrepareOperation(askMode));
          realOutStream.Release();
          RINOK(extractCallback->SetOperationResult(
              isAvail ?
                NExtract::NOperationResult::kHeadersError :
                NExtract::NOperationResult::kUnavailable));
        }
        continue;
      }
      RINOK(res);
    }

    if (item.IsDir())
    {
      // if (!testMode)
      {
        RINOK(extractCallback->PrepareOperation(askMode));
        realOutStream.Release();
        RINOK(extractCallback->SetOperationResult(NExtract::NOperationResult::kOK));
      }
      continue;
    }

    if (!testMode && !realOutStream)
      continue;

    RINOK(extractCallback->PrepareOperation(askMode));

    Int32 res;
    HRESULT hres = myDecoder.Decode(
        EXTERNAL_CODECS_VARS
        m_Archive, item, realOutStream, extractCallback,
        progress,
        #ifndef _7ZIP_ST
        _props.NumThreads,
        #endif
        res);
    RINOK(hres);
    realOutStream.Release();
    
    RINOK(extractCallback->SetOperationResult(res))
  }
  
  lps->InSize = currentTotalPacked;
  lps->OutSize = currentTotalUnPacked;
  return lps->SetCur();
  COM_TRY_END
}

IMPL_ISetCompressCodecsInfo

}}
