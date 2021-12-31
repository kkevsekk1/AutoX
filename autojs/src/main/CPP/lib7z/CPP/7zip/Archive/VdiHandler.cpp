// VdiHandler.cpp

#include "StdAfx.h"

// #include <stdio.h>

#include "../../../C/CpuArch.h"

#include "../../Common/ComTry.h"
#include "../../Common/IntToString.h"
#include "../../Common/MyBuffer.h"

#include "../../Windows/PropVariant.h"

#include "../Common/RegisterArc.h"
#include "../Common/StreamUtils.h"

#include "HandlerCont.h"

#define Get32(p) GetUi32(p)
#define Get64(p) GetUi64(p)

using namespace NWindows;

namespace NArchive {
namespace NVdi {

#define SIGNATURE { 0x7F, 0x10, 0xDA, 0xBE }
  
static const Byte k_Signature[] = SIGNATURE;

static const unsigned k_ClusterBits = 20;
static const UInt32 k_ClusterSize = (UInt32)1 << k_ClusterBits;
static const UInt32 k_UnusedCluster = 0xFFFFFFFF;

// static const UInt32 kDiskType_Dynamic = 1;
// static const UInt32 kDiskType_Static = 2;

static const char * const kDiskTypes[] =
{
    "0"
  , "Dynamic"
  , "Static"
};

class CHandler: public CHandlerImg
{
  UInt32 _dataOffset;
  CByteBuffer _table;
  UInt64 _phySize;
  UInt32 _imageType;
  bool _isArc;
  bool _unsupported;

  HRESULT Seek(UInt64 offset)
  {
    _posInArc = offset;
    return Stream->Seek(offset, STREAM_SEEK_SET, NULL);
  }

  HRESULT InitAndSeek()
  {
    _virtPos = 0;
    return Seek(0);
  }

  HRESULT Open2(IInStream *stream, IArchiveOpenCallback *openCallback);

public:
  INTERFACE_IInArchive_Img(;)

  STDMETHOD(GetStream)(UInt32 index, ISequentialInStream **stream);
  STDMETHOD(Read)(void *data, UInt32 size, UInt32 *processedSize);
};


STDMETHODIMP CHandler::Read(void *data, UInt32 size, UInt32 *processedSize)
{
  if (processedSize)
    *processedSize = 0;
  if (_virtPos >= _size)
    return S_OK;
  {
    UInt64 rem = _size - _virtPos;
    if (size > rem)
      size = (UInt32)rem;
    if (size == 0)
      return S_OK;
  }
 
  {
    UInt64 cluster = _virtPos >> k_ClusterBits;
    UInt32 lowBits = (UInt32)_virtPos & (k_ClusterSize - 1);
    {
      UInt32 rem = k_ClusterSize - lowBits;
      if (size > rem)
        size = rem;
    }

    cluster <<= 2;
    if (cluster < _table.Size())
    {
      const Byte *p = (const Byte *)_table + (size_t)cluster;
      UInt32 v = Get32(p);
      if (v != k_UnusedCluster)
      {
        UInt64 offset = _dataOffset + ((UInt64)v << k_ClusterBits);
        offset += lowBits;
        if (offset != _posInArc)
        {
          RINOK(Seek(offset));
        }
        HRESULT res = Stream->Read(data, size, &size);
        _posInArc += size;
        _virtPos += size;
        if (processedSize)
          *processedSize = size;
        return res;
      }
    }
    
    memset(data, 0, size);
    _virtPos += size;
    if (processedSize)
      *processedSize = size;
    return S_OK;
  }
}


static const Byte kProps[] =
{
  kpidSize,
  kpidPackSize
};

static const Byte kArcProps[] =
{
  kpidHeadersSize,
  kpidMethod
};

IMP_IInArchive_Props
IMP_IInArchive_ArcProps

STDMETHODIMP CHandler::GetArchiveProperty(PROPID propID, PROPVARIANT *value)
{
  COM_TRY_BEGIN
  NCOM::CPropVariant prop;

  switch (propID)
  {
    case kpidMainSubfile: prop = (UInt32)0; break;
    case kpidPhySize: if (_phySize != 0) prop = _phySize; break;
    case kpidHeadersSize: prop = _dataOffset; break;
    
    case kpidMethod:
    {
      char s[16];
      const char *ptr;
      if (_imageType < ARRAY_SIZE(kDiskTypes))
        ptr = kDiskTypes[_imageType];
      else
      {
        ConvertUInt32ToString(_imageType, s);
        ptr = s;
      }
      prop = ptr;
      break;
    }

    case kpidErrorFlags:
    {
      UInt32 v = 0;
      if (!_isArc) v |= kpv_ErrorFlags_IsNotArc;;
      if (_unsupported) v |= kpv_ErrorFlags_UnsupportedMethod;
      // if (_headerError) v |= kpv_ErrorFlags_HeadersError;
      if (!Stream && v == 0 && _isArc)
        v = kpv_ErrorFlags_HeadersError;
      if (v != 0)
        prop = v;
      break;
    }
  }
  
  prop.Detach(value);
  return S_OK;
  COM_TRY_END
}


STDMETHODIMP CHandler::GetProperty(UInt32 /* index */, PROPID propID, PROPVARIANT *value)
{
  COM_TRY_BEGIN
  NCOM::CPropVariant prop;

  switch (propID)
  {
    case kpidSize: prop = _size; break;
    case kpidPackSize: prop = _phySize - _dataOffset; break;
    case kpidExtension: prop = (_imgExt ? _imgExt : "img"); break;
  }

  prop.Detach(value);
  return S_OK;
  COM_TRY_END
}


static bool IsEmptyGuid(const Byte *data)
{
  for (unsigned i = 0; i < 16; i++)
    if (data[i] != 0)
      return false;
  return true;
}


HRESULT CHandler::Open2(IInStream *stream, IArchiveOpenCallback * /* openCallback */)
{
  const unsigned kHeaderSize = 512;
  Byte buf[kHeaderSize];
  RINOK(ReadStream_FALSE(stream, buf, kHeaderSize));

  if (memcmp(buf + 0x40, k_Signature, sizeof(k_Signature)) != 0)
    return S_FALSE;

  UInt32 version = Get32(buf + 0x44);
  if (version >= 0x20000)
    return S_FALSE;
  
  UInt32 headerSize = Get32(buf + 0x48);
  if (headerSize < 0x140 || headerSize > 0x1B8)
    return S_FALSE;

  _imageType = Get32(buf + 0x4C);
  _dataOffset = Get32(buf + 0x158);

  UInt32 tableOffset = Get32(buf + 0x154);
  if (tableOffset < 0x200)
    return S_FALSE;
  
  UInt32 sectorSize = Get32(buf + 0x168);
  if (sectorSize != 0x200)
    return S_FALSE;

  _size = Get64(buf + 0x170);
  _isArc = true;

  if (_imageType > 2)
  {
    _unsupported = true;
    return S_FALSE;
  }

  if (_dataOffset < tableOffset)
    return S_FALSE;

  UInt32 blockSize = Get32(buf + 0x178);
  if (blockSize != ((UInt32)1 << k_ClusterBits))
  {
    _unsupported = true;
    return S_FALSE;
  }

  UInt32 totalBlocks = Get32(buf + 0x180);

  {
    UInt64 size2 = (UInt64)totalBlocks << k_ClusterBits;
    if (size2 < _size)
    {
      _unsupported = true;
      return S_FALSE;
    }
    /*
    if (size2 > _size)
      _size = size2;
    */
  }

  if (headerSize >= 0x180)
  {
    if (!IsEmptyGuid(buf + 0x1A8) ||
        !IsEmptyGuid(buf + 0x1B8))
    {
      _unsupported = true;
      return S_FALSE;
    }
  }

  UInt32 numAllocatedBlocks = Get32(buf + 0x184);

  {
    UInt32 tableReserved = _dataOffset - tableOffset;
    if ((tableReserved >> 2) < totalBlocks)
      return S_FALSE;
  }

  _phySize = _dataOffset + ((UInt64)numAllocatedBlocks << k_ClusterBits);

  size_t numBytes = (size_t)totalBlocks * 4;
  if ((numBytes >> 2) != totalBlocks)
  {
    _unsupported = true;
    return S_FALSE;
  }

  _table.Alloc(numBytes);
  RINOK(stream->Seek(tableOffset, STREAM_SEEK_SET, NULL));
  RINOK(ReadStream_FALSE(stream, _table, numBytes));
    
  const Byte *data = _table;
  for (UInt32 i = 0; i < totalBlocks; i++)
  {
    UInt32 v = Get32(data + (size_t)i * 4);
    if (v == k_UnusedCluster)
      continue;
    if (v >= numAllocatedBlocks)
      return S_FALSE;
  }
  
  Stream = stream;
  return S_OK;
}


STDMETHODIMP CHandler::Close()
{
  _table.Free();
  _phySize = 0;
  _size = 0;
  _isArc = false;
  _unsupported = false;

  _imgExt = NULL;
  Stream.Release();
  return S_OK;
}


STDMETHODIMP CHandler::GetStream(UInt32 /* index */, ISequentialInStream **stream)
{
  COM_TRY_BEGIN
  *stream = NULL;
  if (_unsupported)
    return S_FALSE;
  CMyComPtr<ISequentialInStream> streamTemp = this;
  RINOK(InitAndSeek());
  *stream = streamTemp.Detach();
  return S_OK;
  COM_TRY_END
}


REGISTER_ARC_I(
  "VDI", "vdi", NULL, 0xC9,
  k_Signature,
  0x40,
  0,
  NULL)

}}
