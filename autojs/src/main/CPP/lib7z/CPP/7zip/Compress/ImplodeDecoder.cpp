// ImplodeDecoder.cpp

#include "StdAfx.h"

#include "../../Common/Defs.h"

#include "ImplodeDecoder.h"

namespace NCompress {
namespace NImplode {
namespace NDecoder {

class CException
{
public:
  enum ECauseType
  {
    kData
  } m_Cause;
  CException(ECauseType cause): m_Cause(cause) {}
};

static const int kNumDistanceLowDirectBitsForBigDict = 7;
static const int kNumDistanceLowDirectBitsForSmallDict = 6;

static const int kNumBitsInByte = 8;

// static const int kLevelStructuresNumberFieldSize = kNumBitsInByte;
static const int kLevelStructuresNumberAdditionalValue = 1;

static const int kNumLevelStructureLevelBits = 4;
static const int kLevelStructureLevelAdditionalValue = 1;

static const int kNumLevelStructureRepNumberBits = 4;
static const int kLevelStructureRepNumberAdditionalValue = 1;


static const int kLiteralTableSize = (1 << kNumBitsInByte);
static const int kDistanceTableSize = 64;
static const int kLengthTableSize = 64;

static const UInt32 kHistorySize =
    (1 << MyMax(kNumDistanceLowDirectBitsForBigDict,
                kNumDistanceLowDirectBitsForSmallDict)) *
    kDistanceTableSize; // = 8 KB;

static const int kNumAdditionalLengthBits = 8;

static const UInt32 kMatchMinLenWhenLiteralsOn = 3;
static const UInt32 kMatchMinLenWhenLiteralsOff = 2;

static const UInt32 kMatchMinLenMax = MyMax(kMatchMinLenWhenLiteralsOn,
    kMatchMinLenWhenLiteralsOff);  // 3

// static const UInt32 kMatchMaxLenMax = kMatchMinLenMax + (kLengthTableSize - 1) + (1 << kNumAdditionalLengthBits) - 1;  // or 2

enum
{
  kMatchId = 0,
  kLiteralId = 1
};


CCoder::CCoder():
  m_LiteralDecoder(kLiteralTableSize),
  m_LengthDecoder(kLengthTableSize),
  m_DistanceDecoder(kDistanceTableSize)
{
}

/*
void CCoder::ReleaseStreams()
{
  m_OutWindowStream.ReleaseStream();
  m_InBitStream.ReleaseStream();
}
*/

bool CCoder::ReadLevelItems(NImplode::NHuffman::CDecoder &decoder,
    Byte *levels, int numLevelItems)
{
  int numCodedStructures = m_InBitStream.ReadBits(kNumBitsInByte) +
      kLevelStructuresNumberAdditionalValue;
  int currentIndex = 0;
  for (int i = 0; i < numCodedStructures; i++)
  {
    int level = m_InBitStream.ReadBits(kNumLevelStructureLevelBits) +
      kLevelStructureLevelAdditionalValue;
    int rep = m_InBitStream.ReadBits(kNumLevelStructureRepNumberBits) +
      kLevelStructureRepNumberAdditionalValue;
    if (currentIndex + rep > numLevelItems)
      throw CException(CException::kData);
    for (int j = 0; j < rep; j++)
      levels[currentIndex++] = (Byte)level;
  }
  if (currentIndex != numLevelItems)
    return false;
  return decoder.SetCodeLengths(levels);
}


bool CCoder::ReadTables(void)
{
  if (m_LiteralsOn)
  {
    Byte literalLevels[kLiteralTableSize];
    if (!ReadLevelItems(m_LiteralDecoder, literalLevels, kLiteralTableSize))
      return false;
  }

  Byte lengthLevels[kLengthTableSize];
  if (!ReadLevelItems(m_LengthDecoder, lengthLevels, kLengthTableSize))
    return false;

  Byte distanceLevels[kDistanceTableSize];
  return ReadLevelItems(m_DistanceDecoder, distanceLevels, kDistanceTableSize);
}

/*
class CCoderReleaser
{
  CCoder *m_Coder;
public:
  CCoderReleaser(CCoder *coder): m_Coder(coder) {}
  ~CCoderReleaser() { m_Coder->ReleaseStreams(); }
};
*/

HRESULT CCoder::CodeReal(ISequentialInStream *inStream, ISequentialOutStream *outStream,
    const UInt64 * /* inSize */, const UInt64 *outSize, ICompressProgressInfo *progress)
{
  if (!m_InBitStream.Create(1 << 20))
    return E_OUTOFMEMORY;
  if (!m_OutWindowStream.Create(kHistorySize))
    return E_OUTOFMEMORY;
  if (outSize == NULL)
    return E_INVALIDARG;
  UInt64 pos = 0, unPackSize = *outSize;

  m_OutWindowStream.SetStream(outStream);
  m_OutWindowStream.Init(false);
  m_InBitStream.SetStream(inStream);
  m_InBitStream.Init();
  // CCoderReleaser coderReleaser(this);

  if (!ReadTables())
    return S_FALSE;
  
  while (pos < unPackSize)
  {
    if (progress != NULL && pos % (1 << 16) == 0)
    {
      UInt64 packSize = m_InBitStream.GetProcessedSize();
      RINOK(progress->SetRatioInfo(&packSize, &pos));
    }
    if (m_InBitStream.ReadBits(1) == kMatchId) // match
    {
      UInt32 lowDistBits = m_InBitStream.ReadBits(m_NumDistanceLowDirectBits);
      UInt32 distance = m_DistanceDecoder.DecodeSymbol(&m_InBitStream);
      if (distance >= kDistanceTableSize)
        return S_FALSE;
      distance = (distance << m_NumDistanceLowDirectBits) + lowDistBits;
      UInt32 lengthSymbol = m_LengthDecoder.DecodeSymbol(&m_InBitStream);
      if (lengthSymbol >= kLengthTableSize)
        return S_FALSE;
      UInt32 length = lengthSymbol + m_MinMatchLength;
      if (lengthSymbol == kLengthTableSize - 1) // special symbol = 63
        length += m_InBitStream.ReadBits(kNumAdditionalLengthBits);
      while (distance >= pos && length > 0)
      {
        m_OutWindowStream.PutByte(0);
        pos++;
        length--;
      }
      if (length > 0)
        m_OutWindowStream.CopyBlock(distance, length);
      pos += length;
    }
    else
    {
      Byte b;
      if (m_LiteralsOn)
      {
        UInt32 temp = m_LiteralDecoder.DecodeSymbol(&m_InBitStream);
        if (temp >= kLiteralTableSize)
          return S_FALSE;
        b = (Byte)temp;
      }
      else
        b = (Byte)m_InBitStream.ReadBits(kNumBitsInByte);
      m_OutWindowStream.PutByte(b);
      pos++;
    }
  }
  if (pos > unPackSize)
    return S_FALSE;
  return m_OutWindowStream.Flush();
}

STDMETHODIMP CCoder::Code(ISequentialInStream *inStream, ISequentialOutStream *outStream,
    const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress)
{
  try { return CodeReal(inStream, outStream, inSize, outSize, progress);  }
  catch(const CLzOutWindowException &e) { return e.ErrorCode; }
  catch(...) { return S_FALSE; }
}

STDMETHODIMP CCoder::SetDecoderProperties2(const Byte *data, UInt32 size)
{
  if (size < 1)
    return E_INVALIDARG;
  Byte flag = data[0];
  m_BigDictionaryOn = ((flag & 2) != 0);
  m_NumDistanceLowDirectBits = m_BigDictionaryOn ?
      kNumDistanceLowDirectBitsForBigDict:
      kNumDistanceLowDirectBitsForSmallDict;
  m_LiteralsOn = ((flag & 4) != 0);
  m_MinMatchLength = m_LiteralsOn ?
      kMatchMinLenWhenLiteralsOn :
      kMatchMinLenWhenLiteralsOff;
  return S_OK;
}

}}}
