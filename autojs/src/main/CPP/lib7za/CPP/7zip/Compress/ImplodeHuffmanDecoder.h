// ImplodeHuffmanDecoder.h

#ifndef __IMPLODE_HUFFMAN_DECODER_H
#define __IMPLODE_HUFFMAN_DECODER_H

#include "../Common/InBuffer.h"

#include "BitlDecoder.h"

namespace NCompress {
namespace NImplode {
namespace NHuffman {

const unsigned kNumBitsInLongestCode = 16;

typedef NBitl::CDecoder<CInBuffer> CInBit;

class CDecoder
{
  UInt32 m_Limitits[kNumBitsInLongestCode + 2]; // m_Limitits[i] = value limit for symbols with length = i
  UInt32 m_Positions[kNumBitsInLongestCode + 2];   // m_Positions[i] = index in m_Symbols[] of first symbol with length = i
  UInt32 m_NumSymbols; // number of symbols in m_Symbols
  UInt32 *m_Symbols; // symbols: at first with len=1 then 2, ... 15.
public:
  CDecoder(UInt32 numSymbols);
  ~CDecoder();
  
  bool SetCodeLengths(const Byte *codeLengths);
  UInt32 DecodeSymbol(CInBit *inStream);
};

}}}

#endif
