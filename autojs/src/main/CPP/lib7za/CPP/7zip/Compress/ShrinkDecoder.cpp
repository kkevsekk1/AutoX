// ShrinkDecoder.cpp


#include "StdAfx.h"

#include <stdio.h>

#include "../../../C/Alloc.h"

#include "../Common/InBuffer.h"
#include "../Common/OutBuffer.h"

#include "BitlDecoder.h"
#include "ShrinkDecoder.h"

namespace NCompress {
namespace NShrink {

static const UInt32 kBufferSize = (1 << 18);
static const unsigned kNumMinBits = 9;

HRESULT CDecoder::CodeReal(ISequentialInStream *inStream, ISequentialOutStream *outStream,
    const UInt64 * /* inSize */, const UInt64 * /* outSize */, ICompressProgressInfo *progress)
{
  NBitl::CBaseDecoder<CInBuffer> inBuffer;
  COutBuffer outBuffer;

  if (!inBuffer.Create(kBufferSize))
    return E_OUTOFMEMORY;
  inBuffer.SetStream(inStream);
  inBuffer.Init();

  if (!outBuffer.Create(kBufferSize))
    return E_OUTOFMEMORY;
  outBuffer.SetStream(outStream);
  outBuffer.Init();

  {
    unsigned i;
    for (i = 0; i < 257; i++)
      _parents[i] = (UInt16)i;
    for (; i < kNumItems; i++)
      _parents[i] = kNumItems;
    for (i = 0; i < kNumItems; i++)
      _suffixes[i] = 0;
  }

  UInt64 prevPos = 0;
  unsigned numBits = kNumMinBits;
  unsigned head = 257;
  int lastSym = -1;
  Byte lastChar2 = 0;

  for (;;)
  {
    UInt32 sym = inBuffer.ReadBits(numBits);
    
    if (inBuffer.ExtraBitsWereRead())
      break;
    
    if (sym == 256)
    {
      sym = inBuffer.ReadBits(numBits);
      if (sym == 1)
      {
        if (numBits >= kNumMaxBits)
          return S_FALSE;
        numBits++;
        continue;
      }
      if (sym != 2)
        return S_FALSE;
      {
        unsigned i;
        for (i = 257; i < kNumItems; i++)
          _stack[i] = 0;
        for (i = 257; i < kNumItems; i++)
        {
          unsigned par = _parents[i];
          if (par != kNumItems)
            _stack[par] = 1;
        }
        for (i = 257; i < kNumItems; i++)
          if (_stack[i] == 0)
            _parents[i] = kNumItems;
       
        head = 257;
       
        continue;
      }
    }

    bool needPrev = false;
    if (head < kNumItems && lastSym >= 0)
    {
      while (head < kNumItems && _parents[head] != kNumItems)
        head++;
      if (head < kNumItems)
      {
        if (head == (unsigned)lastSym)
        {
          // we need to fix the code for that case
          // _parents[head] is not allowed to link to itself
          return E_NOTIMPL;
        }
        needPrev = true;
        _parents[head] = (UInt16)lastSym;
        _suffixes[head] = (Byte)lastChar2;
        head++;
      }
    }

    if (_parents[sym] == kNumItems)
      return S_FALSE;

    lastSym = sym;
    unsigned cur = sym;
    unsigned i = 0;
    
    while (cur >= 256)
    {
      _stack[i++] = _suffixes[cur];
      cur = _parents[cur];
    }
    
    _stack[i++] = (Byte)cur;
    lastChar2 = (Byte)cur;

    if (needPrev)
      _suffixes[head - 1] = (Byte)cur;

    do
      outBuffer.WriteByte(_stack[--i]);
    while (i);
    
    if (progress)
    {
      const UInt64 nowPos = outBuffer.GetProcessedSize();
      if (nowPos - prevPos >= (1 << 18))
      {
        prevPos = nowPos;
        const UInt64 packSize = inBuffer.GetProcessedSize();
        RINOK(progress->SetRatioInfo(&packSize, &nowPos));
      }
    }
  }
  
  return outBuffer.Flush();
}

STDMETHODIMP CDecoder ::Code(ISequentialInStream *inStream, ISequentialOutStream *outStream,
    const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress)
{
  try { return CodeReal(inStream, outStream, inSize, outSize, progress); }
  catch(const CInBufferException &e) { return e.ErrorCode; }
  catch(const COutBufferException &e) { return e.ErrorCode; }
  catch(...) { return S_FALSE; }
}

}}
