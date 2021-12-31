// Compress/BZip2Decoder.h

#ifndef __COMPRESS_BZIP2_DECODER_H
#define __COMPRESS_BZIP2_DECODER_H

#include "../../Common/MyCom.h"

#ifndef _7ZIP_ST
#include "../../Windows/Synchronization.h"
#include "../../Windows/Thread.h"
#endif

#include "../ICoder.h"

#include "../Common/InBuffer.h"
#include "../Common/OutBuffer.h"

#include "BitmDecoder.h"
#include "BZip2Const.h"
#include "BZip2Crc.h"
#include "HuffmanDecoder.h"

namespace NCompress {
namespace NBZip2 {

bool IsEndSig(const Byte *p) throw();
bool IsBlockSig(const Byte *p) throw();

typedef NCompress::NHuffman::CDecoder<kMaxHuffmanLen, kMaxAlphaSize> CHuffmanDecoder;

class CDecoder;

struct CState
{
  UInt32 *Counters;

  #ifndef _7ZIP_ST

  CDecoder *Decoder;
  NWindows::CThread Thread;
  bool m_OptimizeNumTables;

  NWindows::NSynchronization::CAutoResetEvent StreamWasFinishedEvent;
  NWindows::NSynchronization::CAutoResetEvent WaitingWasStartedEvent;

  // it's not member of this thread. We just need one event per thread
  NWindows::NSynchronization::CAutoResetEvent CanWriteEvent;

  Byte MtPad[1 << 8]; // It's pad for Multi-Threading. Must be >= Cache_Line_Size.

  HRESULT Create();
  void FinishStream();
  void ThreadFunc();

  #endif

  CState(): Counters(0) {}
  ~CState() { Free(); }
  bool Alloc();
  void Free();
};

struct CBlockProps
{
  UInt32 blockSize;
  UInt32 origPtr;
  bool randMode;
  
  CBlockProps(): blockSize(0), origPtr(0), randMode(false) {}
};

struct CBase
{
  CMyComPtr<ISequentialInStream> InStreamRef;
  NBitm::CDecoder<CInBuffer> BitDecoder;

private:
  Byte m_Selectors[kNumSelectorsMax];
  CHuffmanDecoder m_HuffmanDecoders[kNumTablesMax];

public:
  UInt64 NumBlocks;

  CBase(): NumBlocks(0) {}
  UInt32 ReadBits(unsigned numBits);
  unsigned ReadBit();
  void InitNumBlocks() { NumBlocks = 0; }

  /*
    ReadBlock() props->randMode:
      in: need read randMode bit,
      out: randMode status
  */
  HRESULT ReadBlock(UInt32 *charCounters, UInt32 blockSizeMax, CBlockProps *props);
};

class CDecoder :
  public ICompressCoder,
  #ifndef _7ZIP_ST
  public ICompressSetCoderMt,
  #endif
  public CMyUnknownImp
{
public:
  COutBuffer m_OutStream;
  Byte MtPad[1 << 8]; // It's pad for Multi-Threading. Must be >= Cache_Line_Size.

  CBase Base;

  UInt64 _inStart;

private:

  bool _needInStreamInit;

  Byte ReadByte();

  HRESULT DecodeFile(ICompressProgressInfo *progress);
  HRESULT CodeReal(ISequentialInStream *inStream, ISequentialOutStream *outStream, ICompressProgressInfo *progress);
  
  class CDecoderFlusher
  {
    CDecoder *_decoder;
  public:
    bool NeedFlush;
    CDecoderFlusher(CDecoder *decoder): _decoder(decoder), NeedFlush(true) {}
    ~CDecoderFlusher()
    {
      if (NeedFlush)
        _decoder->Flush();
    }
  };

public:
  CBZip2CombinedCrc CombinedCrc;
  ICompressProgressInfo *Progress;

  #ifndef _7ZIP_ST
  CState *m_States;
  UInt32 m_NumThreadsPrev;

  NWindows::NSynchronization::CManualResetEvent CanProcessEvent;
  NWindows::NSynchronization::CCriticalSection CS;
  UInt32 NumThreads;
  bool MtMode;
  UInt32 NextBlockIndex;
  bool CloseThreads;
  bool StreamWasFinished1;
  bool StreamWasFinished2;
  NWindows::NSynchronization::CManualResetEvent CanStartWaitingEvent;

  HRESULT Result1;
  HRESULT Result2;

  UInt32 BlockSizeMax;

  ~CDecoder();
  HRESULT Create();
  void Free();

  #else
  CState m_States[1];
  #endif

  bool IsBz;
  bool BzWasFinished; // bzip stream was finished with end signature
  bool CrcError; // it can CRC error of block or CRC error of whole stream.

  CDecoder();

  HRESULT SetRatioProgress(UInt64 packSize);
  HRESULT ReadSignature(UInt32 &crc);

  HRESULT Flush() { return m_OutStream.Flush(); }

  MY_QUERYINTERFACE_BEGIN2(ICompressCoder)
  #ifndef _7ZIP_ST
  MY_QUERYINTERFACE_ENTRY(ICompressSetCoderMt)
  #endif

  MY_QUERYINTERFACE_END
  MY_ADDREF_RELEASE

  
  STDMETHOD(Code)(ISequentialInStream *inStream, ISequentialOutStream *outStream,
      const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);

  STDMETHOD(SetInStream)(ISequentialInStream *inStream);
  STDMETHOD(ReleaseInStream)();

  HRESULT CodeResume(ISequentialOutStream *outStream, ICompressProgressInfo *progress);

  UInt64 GetStreamSize() const { return Base.BitDecoder.GetStreamSize(); }
  UInt64 GetInputProcessedSize() const { return Base.BitDecoder.GetProcessedSize(); }

  void InitNumBlocks() { Base.InitNumBlocks(); }
  UInt64 GetNumBlocks() const { return Base.NumBlocks; }
  
  #ifndef _7ZIP_ST
  STDMETHOD(SetNumberOfThreads)(UInt32 numThreads);
  #endif
};


class CNsisDecoder :
  public ISequentialInStream,
  public ICompressSetInStream,
  public ICompressSetOutStreamSize,
  public CMyUnknownImp
{
  CBase Base;

  CState m_State;
  
  int _nsisState;
  UInt32 _tPos;
  unsigned _prevByte;
  unsigned _repRem;
  unsigned _numReps;
  UInt32 _blockSize;

public:

  MY_QUERYINTERFACE_BEGIN2(ISequentialInStream)
  MY_QUERYINTERFACE_ENTRY(ICompressSetInStream)
  MY_QUERYINTERFACE_ENTRY(ICompressSetOutStreamSize)
  MY_QUERYINTERFACE_END
  MY_ADDREF_RELEASE

  STDMETHOD(Read)(void *data, UInt32 size, UInt32 *processedSize);
  STDMETHOD(SetInStream)(ISequentialInStream *inStream);
  STDMETHOD(ReleaseInStream)();
  STDMETHOD(SetOutStreamSize)(const UInt64 *outSize);
};

}}

#endif
