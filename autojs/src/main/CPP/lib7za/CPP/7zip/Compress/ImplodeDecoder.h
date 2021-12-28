// ImplodeDecoder.h

#ifndef __COMPRESS_IMPLODE_DECODER_H
#define __COMPRESS_IMPLODE_DECODER_H

#include "../../Common/MyCom.h"

#include "../ICoder.h"

#include "ImplodeHuffmanDecoder.h"
#include "LzOutWindow.h"

namespace NCompress {
namespace NImplode {
namespace NDecoder {

class CCoder:
  public ICompressCoder,
  public ICompressSetDecoderProperties2,
  public CMyUnknownImp
{
  CLzOutWindow m_OutWindowStream;
  NBitl::CDecoder<CInBuffer> m_InBitStream;
  
  NImplode::NHuffman::CDecoder m_LiteralDecoder;
  NImplode::NHuffman::CDecoder m_LengthDecoder;
  NImplode::NHuffman::CDecoder m_DistanceDecoder;

  bool m_BigDictionaryOn;
  bool m_LiteralsOn;

  int m_NumDistanceLowDirectBits;
  UInt32 m_MinMatchLength;

  bool ReadLevelItems(NImplode::NHuffman::CDecoder &table, Byte *levels, int numLevelItems);
  bool ReadTables();
  void DeCodeLevelTable(Byte *newLevels, int numLevels);
public:
  CCoder();

  MY_UNKNOWN_IMP1(ICompressSetDecoderProperties2)

  // void ReleaseStreams();
  
  HRESULT CodeReal(ISequentialInStream *inStream, ISequentialOutStream *outStream,
      const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);

  STDMETHOD(Code)(ISequentialInStream *inStream, ISequentialOutStream *outStream,
      const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);

  STDMETHOD(SetDecoderProperties2)(const Byte *data, UInt32 size);
};

}}}

#endif
