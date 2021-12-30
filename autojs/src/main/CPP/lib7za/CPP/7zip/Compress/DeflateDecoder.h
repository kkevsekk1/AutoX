// DeflateDecoder.h

#ifndef __DEFLATE_DECODER_H
#define __DEFLATE_DECODER_H

#include "../../Common/MyCom.h"

#include "../ICoder.h"

#include "../Common/InBuffer.h"

#include "BitlDecoder.h"
#include "DeflateConst.h"
#include "HuffmanDecoder.h"
#include "LzOutWindow.h"

namespace NCompress {
namespace NDeflate {
namespace NDecoder {

const int kLenIdFinished = -1;
const int kLenIdNeedInit = -2;

class CCoder:
  public ICompressCoder,
  public ICompressGetInStreamProcessedSize,
  #ifndef NO_READ_FROM_CODER
  public ICompressSetInStream,
  public ICompressSetOutStreamSize,
  public ISequentialInStream,
  #endif
  public CMyUnknownImp
{
  CLzOutWindow m_OutWindowStream;
  CMyComPtr<ISequentialInStream> m_InStreamRef;
  NBitl::CDecoder<CInBuffer> m_InBitStream;
  NCompress::NHuffman::CDecoder<kNumHuffmanBits, kFixedMainTableSize> m_MainDecoder;
  NCompress::NHuffman::CDecoder<kNumHuffmanBits, kFixedDistTableSize> m_DistDecoder;
  NCompress::NHuffman::CDecoder7b<kLevelTableSize> m_LevelDecoder;

  UInt32 m_StoredBlockSize;

  UInt32 _numDistLevels;
  bool m_FinalBlock;
  bool m_StoredMode;

  bool _deflateNSIS;
  bool _deflate64Mode;
  bool _keepHistory;
  bool _needFinishInput;
  
  bool _needInitInStream;
  bool _needReadTable;
  Int32 _remainLen;
  UInt32 _rep0;

  UInt32 ReadBits(unsigned numBits);

  bool DecodeLevels(Byte *levels, unsigned numSymbols);
  bool ReadTables();
  
  HRESULT Flush() { return m_OutWindowStream.Flush(); }
  class CCoderReleaser
  {
    CCoder *_coder;
  public:
    bool NeedFlush;
    CCoderReleaser(CCoder *coder): _coder(coder), NeedFlush(true) {}
    ~CCoderReleaser()
    {
      if (NeedFlush)
        _coder->Flush();
    }
  };
  friend class CCoderReleaser;

  HRESULT CodeSpec(UInt32 curSize, bool finishInputStream);
public:
  bool ZlibMode;
  Byte ZlibFooter[4];

  CCoder(bool deflate64Mode, bool deflateNSIS = false);
  virtual ~CCoder() {};

  void Set_KeepHistory(bool keepHistory) { _keepHistory = keepHistory; }
  void Set_NeedFinishInput(bool needFinishInput) { _needFinishInput = needFinishInput; }

  bool IsFinished() const { return _remainLen == kLenIdFinished;; }
  bool IsFinalBlock() const { return m_FinalBlock; }

  HRESULT CodeReal(ISequentialOutStream *outStream,
      const UInt64 *outSize, ICompressProgressInfo *progress);

  #ifndef NO_READ_FROM_CODER
  MY_UNKNOWN_IMP5(
      ICompressCoder,
      ICompressGetInStreamProcessedSize,
      ICompressSetInStream,
      ICompressSetOutStreamSize,
      ISequentialInStream
      )
  #else
  MY_UNKNOWN_IMP2(
      ICompressCoder,
      ICompressGetInStreamProcessedSize)
  #endif

  STDMETHOD(Code)(ISequentialInStream *inStream, ISequentialOutStream *outStream,
      const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);

  STDMETHOD(SetInStream)(ISequentialInStream *inStream);
  STDMETHOD(ReleaseInStream)();
  STDMETHOD(SetOutStreamSize)(const UInt64 *outSize);
  
  #ifndef NO_READ_FROM_CODER
  STDMETHOD(Read)(void *data, UInt32 size, UInt32 *processedSize);
  #endif

  STDMETHOD(CodeResume)(ISequentialOutStream *outStream, const UInt64 *outSize, ICompressProgressInfo *progress);

  HRESULT InitInStream(bool needInit)
  {
    if (!m_InBitStream.Create(1 << 17))
      return E_OUTOFMEMORY;
    if (needInit)
    {
      m_InBitStream.Init();
      _needInitInStream = false;
    }
    return S_OK;
  }

  void AlignToByte() { m_InBitStream.AlignToByte(); }
  Byte ReadAlignedByte();
  UInt32 ReadAligned_UInt16() // aligned for Byte range
  {
    UInt32 v = m_InBitStream.ReadAlignedByte();
    return v | ((UInt32)m_InBitStream.ReadAlignedByte() << 8);
  }
  bool InputEofError() const { return m_InBitStream.ExtraBitsWereRead(); }

  UInt64 GetStreamSize() const { return m_InBitStream.GetStreamSize(); }
  UInt64 GetInputProcessedSize() const { return m_InBitStream.GetProcessedSize(); }

  // IGetInStreamProcessedSize
  STDMETHOD(GetInStreamProcessedSize)(UInt64 *value);
};

class CCOMCoder     : public CCoder { public: CCOMCoder(): CCoder(false) {} };
class CNsisCOMCoder : public CCoder { public: CNsisCOMCoder(): CCoder(false, true) {} };
class CCOMCoder64   : public CCoder { public: CCOMCoder64(): CCoder(true) {} };

}}}

#endif
