// ShrinkDecoder.h

#ifndef __COMPRESS_SHRINK_DECODER_H
#define __COMPRESS_SHRINK_DECODER_H

#include "../../Common/MyCom.h"

#include "../ICoder.h"

namespace NCompress {
namespace NShrink {

const unsigned kNumMaxBits = 13;
const unsigned kNumItems = 1 << kNumMaxBits;

class CDecoder :
  public ICompressCoder,
  public CMyUnknownImp
{
  UInt16 _parents[kNumItems];
  Byte _suffixes[kNumItems];
  Byte _stack[kNumItems];

public:
  MY_UNKNOWN_IMP

  HRESULT CodeReal(ISequentialInStream *inStream, ISequentialOutStream *outStream,
      const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);
  
  STDMETHOD(Code)(ISequentialInStream *inStream, ISequentialOutStream *outStream,
      const UInt64 *inSize, const UInt64 *outSize, ICompressProgressInfo *progress);
};

}}

#endif
