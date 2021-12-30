// ImplodeHuffmanDecoder.cpp

#include "StdAfx.h"

#include "ImplodeHuffmanDecoder.h"

namespace NCompress {
namespace NImplode {
namespace NHuffman {

CDecoder::CDecoder(UInt32 numSymbols):
  m_NumSymbols(numSymbols)
{
  m_Symbols = new UInt32[m_NumSymbols];
}

CDecoder::~CDecoder()
{
  delete []m_Symbols;
}

bool CDecoder::SetCodeLengths(const Byte *codeLengths)
{
  // unsigned lenCounts[kNumBitsInLongestCode + 1], tmpPositions[kNumBitsInLongestCode + 1];
  unsigned lenCounts[kNumBitsInLongestCode + 2], tmpPositions[kNumBitsInLongestCode + 1];
  unsigned i;
  for (i = 0; i <= kNumBitsInLongestCode; i++)
    lenCounts[i] = 0;
  UInt32 symbolIndex;
  for (symbolIndex = 0; symbolIndex < m_NumSymbols; symbolIndex++)
    lenCounts[codeLengths[symbolIndex]]++;
  // lenCounts[0] = 0;
  
  // tmpPositions[0] = m_Positions[0] = m_Limitits[0] = 0;
  m_Limitits[kNumBitsInLongestCode + 1] = 0;
  m_Positions[kNumBitsInLongestCode + 1] = 0;
  lenCounts[kNumBitsInLongestCode + 1] = 0;


  UInt32 startPos = 0;
  static const UInt32 kMaxValue = (1 << kNumBitsInLongestCode);

  for (i = kNumBitsInLongestCode; i > 0; i--)
  {
    startPos += lenCounts[i] << (kNumBitsInLongestCode - i);
    if (startPos > kMaxValue)
      return false;
    m_Limitits[i] = startPos;
    m_Positions[i] = m_Positions[i + 1] + lenCounts[i + 1];
    tmpPositions[i] = m_Positions[i] + lenCounts[i];

  }

  // if _ZIP_MODE do not throw exception for trees containing only one node
  // #ifndef _ZIP_MODE
  if (startPos != kMaxValue)
    return false;
  // #endif

  for (symbolIndex = 0; symbolIndex < m_NumSymbols; symbolIndex++)
    if (codeLengths[symbolIndex] != 0)
      m_Symbols[--tmpPositions[codeLengths[symbolIndex]]] = symbolIndex;
  return true;
}

UInt32 CDecoder::DecodeSymbol(CInBit *inStream)
{
  UInt32 numBits = 0;
  UInt32 value = inStream->GetValue(kNumBitsInLongestCode);
  unsigned i;
  for (i = kNumBitsInLongestCode; i > 0; i--)
  {
    if (value < m_Limitits[i])
    {
      numBits = i;
      break;
    }
  }
  if (i == 0)
    return 0xFFFFFFFF;
  inStream->MovePos(numBits);
  UInt32 index = m_Positions[numBits] +
      ((value - m_Limitits[numBits + 1]) >> (kNumBitsInLongestCode - numBits));
  if (index >= m_NumSymbols)
    return 0xFFFFFFFF;
  return m_Symbols[index];
}

}}}
