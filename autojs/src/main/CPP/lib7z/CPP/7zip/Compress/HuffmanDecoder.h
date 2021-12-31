// Compress/HuffmanDecoder.h

#ifndef __COMPRESS_HUFFMAN_DECODER_H
#define __COMPRESS_HUFFMAN_DECODER_H

#include "../../Common/MyTypes.h"

namespace NCompress {
namespace NHuffman {

template <unsigned kNumBitsMax, UInt32 m_NumSymbols, unsigned kNumTableBits = 9>
class CDecoder
{
  UInt32 _limits[kNumBitsMax + 2];
  UInt32 _poses[kNumBitsMax + 1];
  UInt16 _lens[1 << kNumTableBits];
  UInt16 _symbols[m_NumSymbols];
public:

  bool Build(const Byte *lens) throw()
  {
    UInt32 lenCounts[kNumBitsMax + 1];
    UInt32 tmpPoses[kNumBitsMax + 1];
    
    unsigned i;
    for (i = 0; i <= kNumBitsMax; i++)
      lenCounts[i] = 0;
    
    UInt32 sym;
    
    for (sym = 0; sym < m_NumSymbols; sym++)
      lenCounts[lens[sym]]++;
    
    lenCounts[0] = 0;
    _poses[0] = 0;
    _limits[0] = 0;
    UInt32 startPos = 0;
    const UInt32 kMaxValue = (UInt32)1 << kNumBitsMax;
    
    for (i = 1; i <= kNumBitsMax; i++)
    {
      startPos += lenCounts[i] << (kNumBitsMax - i);
      if (startPos > kMaxValue)
        return false;
      _limits[i] = startPos;
      _poses[i] = _poses[i - 1] + lenCounts[i - 1];
      tmpPoses[i] = _poses[i];
    }

    _limits[kNumBitsMax + 1] = kMaxValue;
    
    for (sym = 0; sym < m_NumSymbols; sym++)
    {
      unsigned len = lens[sym];
      if (len == 0)
        continue;

      unsigned offset = tmpPoses[len];
      _symbols[offset] = (UInt16)sym;
      tmpPoses[len] = offset + 1;

      if (len <= kNumTableBits)
      {
        offset -= _poses[len];
        UInt32 num = (UInt32)1 << (kNumTableBits - len);
        UInt16 val = (UInt16)((sym << 4) | len);
        UInt16 *dest = _lens + (_limits[len - 1] >> (kNumBitsMax - kNumTableBits)) + (offset << (kNumTableBits - len));
        for (UInt32 k = 0; k < num; k++)
          dest[k] = val;
      }
    }
    
    return true;
  }

  bool BuildFull(const Byte *lens, UInt32 numSymbols = m_NumSymbols) throw()
  {
    UInt32 lenCounts[kNumBitsMax + 1];
    UInt32 tmpPoses[kNumBitsMax + 1];
    
    unsigned i;
    for (i = 0; i <= kNumBitsMax; i++)
      lenCounts[i] = 0;
    
    UInt32 sym;
    
    for (sym = 0; sym < numSymbols; sym++)
      lenCounts[lens[sym]]++;
    
    lenCounts[0] = 0;
    _poses[0] = 0;
    _limits[0] = 0;
    UInt32 startPos = 0;
    const UInt32 kMaxValue = (UInt32)1 << kNumBitsMax;
    
    for (i = 1; i <= kNumBitsMax; i++)
    {
      startPos += lenCounts[i] << (kNumBitsMax - i);
      if (startPos > kMaxValue)
        return false;
      _limits[i] = startPos;
      _poses[i] = _poses[i - 1] + lenCounts[i - 1];
      tmpPoses[i] = _poses[i];
    }

    _limits[kNumBitsMax + 1] = kMaxValue;
    
    for (sym = 0; sym < numSymbols; sym++)
    {
      unsigned len = lens[sym];
      if (len == 0)
        continue;

      unsigned offset = tmpPoses[len];
      _symbols[offset] = (UInt16)sym;
      tmpPoses[len] = offset + 1;

      if (len <= kNumTableBits)
      {
        offset -= _poses[len];
        UInt32 num = (UInt32)1 << (kNumTableBits - len);
        UInt16 val = (UInt16)((sym << 4) | len);
        UInt16 *dest = _lens + (_limits[len - 1] >> (kNumBitsMax - kNumTableBits)) + (offset << (kNumTableBits - len));
        for (UInt32 k = 0; k < num; k++)
          dest[k] = val;
      }
    }
    
    return startPos == kMaxValue;
  }

  template <class TBitDecoder>
  UInt32 Decode(TBitDecoder *bitStream) const throw()
  {
    UInt32 val = bitStream->GetValue(kNumBitsMax);
    
    if (val < _limits[kNumTableBits])
    {
      UInt32 pair = _lens[val >> (kNumBitsMax - kNumTableBits)];
      bitStream->MovePos((unsigned)(pair & 0xF));
      return pair >> 4;
    }

    unsigned numBits;
    for (numBits = kNumTableBits + 1; val >= _limits[numBits]; numBits++);
    
    if (numBits > kNumBitsMax)
      return 0xFFFFFFFF;

    bitStream->MovePos(numBits);
    UInt32 index = _poses[numBits] + ((val - _limits[numBits - 1]) >> (kNumBitsMax - numBits));
    return _symbols[index];
  }

  template <class TBitDecoder>
  UInt32 DecodeFull(TBitDecoder *bitStream) const throw()
  {
    UInt32 val = bitStream->GetValue(kNumBitsMax);
    
    if (val < _limits[kNumTableBits])
    {
      UInt32 pair = _lens[val >> (kNumBitsMax - kNumTableBits)];
      bitStream->MovePos((unsigned)(pair & 0xF));
      return pair >> 4;
    }

    unsigned numBits;
    for (numBits = kNumTableBits + 1; val >= _limits[numBits]; numBits++);
    
    bitStream->MovePos(numBits);
    UInt32 index = _poses[numBits] + ((val - _limits[numBits - 1]) >> (kNumBitsMax - numBits));
    return _symbols[index];
  }
};



template <UInt32 m_NumSymbols>
class CDecoder7b
{
  Byte _lens[1 << 7];
public:

  bool Build(const Byte *lens) throw()
  {
    const unsigned kNumBitsMax = 7;
    
    UInt32 lenCounts[kNumBitsMax + 1];
    UInt32 tmpPoses[kNumBitsMax + 1];
    UInt32 _poses[kNumBitsMax + 1];
    UInt32 _limits[kNumBitsMax + 1];
      
    unsigned i;
    for (i = 0; i <= kNumBitsMax; i++)
      lenCounts[i] = 0;
    
    UInt32 sym;
    
    for (sym = 0; sym < m_NumSymbols; sym++)
      lenCounts[lens[sym]]++;
    
    lenCounts[0] = 0;
    _poses[0] = 0;
    _limits[0] = 0;
    UInt32 startPos = 0;
    const UInt32 kMaxValue = (UInt32)1 << kNumBitsMax;
    
    for (i = 1; i <= kNumBitsMax; i++)
    {
      startPos += lenCounts[i] << (kNumBitsMax - i);
      if (startPos > kMaxValue)
        return false;
      _limits[i] = startPos;
      _poses[i] = _poses[i - 1] + lenCounts[i - 1];
      tmpPoses[i] = _poses[i];
    }

    for (sym = 0; sym < m_NumSymbols; sym++)
    {
      unsigned len = lens[sym];
      if (len == 0)
        continue;

      unsigned offset = tmpPoses[len];
      tmpPoses[len] = offset + 1;

      {
        offset -= _poses[len];
        UInt32 num = (UInt32)1 << (kNumBitsMax - len);
        Byte val = (Byte)((sym << 3) | len);
        Byte *dest = _lens + (_limits[len - 1]) + (offset << (kNumBitsMax - len));
        for (UInt32 k = 0; k < num; k++)
          dest[k] = val;
      }
    }

    {
      UInt32 limit = _limits[kNumBitsMax];
      UInt32 num = ((UInt32)1 << kNumBitsMax) - limit;
      Byte *dest = _lens + limit;
      for (UInt32 k = 0; k < num; k++)
        dest[k] = (Byte)(0x1F << 3);
    }
    
    return true;
  }

  template <class TBitDecoder>
  UInt32 Decode(TBitDecoder *bitStream) const throw()
  {
    UInt32 val = bitStream->GetValue(7);
    UInt32 pair = _lens[val];
    bitStream->MovePos((unsigned)(pair & 0x7));
    return pair >> 3;
  }
};

}}

#endif
