// Crypto/ZipStrong.cpp

#include "StdAfx.h"

#include "../../../C/7zCrc.h"
#include "../../../C/CpuArch.h"

#include "../Common/StreamUtils.h"

#include "Sha1Cls.h"
#include "ZipStrong.h"

namespace NCrypto {
namespace NZipStrong {

static const UInt16 kAES128 = 0x660E;

// DeriveKey* function is similar to CryptDeriveKey() from Windows.
// But MSDN tells that we need such scheme only if
// "the required key length is longer than the hash value"
// but ZipStrong uses it always.

static void DeriveKey2(const Byte *digest, Byte c, Byte *dest)
{
  Byte buf[64];
  memset(buf, c, 64);
  for (unsigned i = 0; i < NSha1::kDigestSize; i++)
    buf[i] ^= digest[i];
  NSha1::CContext sha;
  sha.Init();
  sha.Update(buf, 64);
  sha.Final(dest);
}
 
static void DeriveKey(NSha1::CContext &sha, Byte *key)
{
  Byte digest[NSha1::kDigestSize];
  sha.Final(digest);
  Byte temp[NSha1::kDigestSize * 2];
  DeriveKey2(digest, 0x36, temp);
  DeriveKey2(digest, 0x5C, temp + NSha1::kDigestSize);
  memcpy(key, temp, 32);
}

void CKeyInfo::SetPassword(const Byte *data, UInt32 size)
{
  NSha1::CContext sha;
  sha.Init();
  sha.Update(data, size);
  DeriveKey(sha, MasterKey);
}

STDMETHODIMP CBaseCoder::CryptoSetPassword(const Byte *data, UInt32 size)
{
  _key.SetPassword(data, size);
  return S_OK;
}

STDMETHODIMP CBaseCoder::Init()
{
  return S_OK;
}

HRESULT CDecoder::ReadHeader(ISequentialInStream *inStream, UInt32 crc, UInt64 unpackSize)
{
  Byte temp[4];
  RINOK(ReadStream_FALSE(inStream, temp, 2));
  _ivSize = GetUi16(temp);
  if (_ivSize == 0)
  {
    memset(_iv, 0, 16);
    SetUi32(_iv + 0, crc);
    SetUi64(_iv + 4, unpackSize);
    _ivSize = 12;
  }
  else if (_ivSize == 16)
  {
    RINOK(ReadStream_FALSE(inStream, _iv, _ivSize));
  }
  else
    return E_NOTIMPL;
  RINOK(ReadStream_FALSE(inStream, temp, 4));
  _remSize = GetUi32(temp);
  const UInt32 kAlign = 16;
  if (_remSize < 16 || _remSize > (1 << 18))
    return E_NOTIMPL;
  if (_remSize + kAlign > _buf.Size())
  {
    _buf.Alloc(_remSize + kAlign);
    _bufAligned = (Byte *)((ptrdiff_t)((Byte *)_buf + kAlign - 1) & ~(ptrdiff_t)(kAlign - 1));
  }
  return ReadStream_FALSE(inStream, _bufAligned, _remSize);
}

HRESULT CDecoder::Init_and_CheckPassword(bool &passwOK)
{
  passwOK = false;
  if (_remSize < 16)
    return E_NOTIMPL;
  Byte *p = _bufAligned;
  UInt16 format = GetUi16(p);
  if (format != 3)
    return E_NOTIMPL;
  UInt16 algId = GetUi16(p + 2);
  if (algId < kAES128)
    return E_NOTIMPL;
  algId -= kAES128;
  if (algId > 2)
    return E_NOTIMPL;
  UInt16 bitLen = GetUi16(p + 4);
  UInt16 flags = GetUi16(p + 6);
  if (algId * 64 + 128 != bitLen)
    return E_NOTIMPL;
  _key.KeySize = 16 + algId * 8;
  bool cert = ((flags & 2) != 0);

  if ((flags & 0x4000) != 0)
  {
    // Use 3DES
    return E_NOTIMPL;
  }

  if (cert)
  {
    return E_NOTIMPL;
  }
  else
  {
    if ((flags & 1) == 0)
      return E_NOTIMPL;
  }

  UInt32 rdSize = GetUi16(p + 8);

  if (rdSize + 16 > _remSize)
    return E_NOTIMPL;

  /*
  if (cert)
  {
    // how to filter rd, if ((rdSize & 0xF) != 0) ?
    if ((rdSize & 0x7) != 0)
      return E_NOTIMPL;
  }
  else
  */
  {
    if ((rdSize & 0xF) != 0)
      return E_NOTIMPL;
  }

  memmove(p, p + 10, rdSize);
  const Byte *p2 = p + rdSize + 10;
  UInt32 reserved = GetUi32(p2);
  p2 += 4;
  
  /*
  if (cert)
  {
    UInt32 numRecipients = reserved;

    if (numRecipients == 0)
      return E_NOTIMPL;

    {
      UInt32 hashAlg = GetUi16(p2);
      hashAlg = hashAlg;
      UInt32 hashSize = GetUi16(p2 + 2);
      hashSize = hashSize;
      p2 += 4;

      reserved = reserved;
      // return E_NOTIMPL;

      for (unsigned r = 0; r < numRecipients; r++)
      {
        UInt32 specSize = GetUi16(p2);
        p2 += 2;
        p2 += specSize;
      }
    }
  }
  else
  */
  {
    if (reserved != 0)
      return E_NOTIMPL;
  }

  UInt32 validSize = GetUi16(p2);
  p2 += 2;
  const size_t validOffset = p2 - p;
  if ((validSize & 0xF) != 0 || validOffset + validSize != _remSize)
    return E_NOTIMPL;

  {
    RINOK(SetKey(_key.MasterKey, _key.KeySize));
    RINOK(SetInitVector(_iv, 16));
    RINOK(Init());
    Filter(p, rdSize);
  }

  Byte fileKey[32];
  NSha1::CContext sha;
  sha.Init();
  sha.Update(_iv, _ivSize);
  sha.Update(p, rdSize - 16); // we don't use last 16 bytes (PAD bytes)
  DeriveKey(sha, fileKey);
  
  RINOK(SetKey(fileKey, _key.KeySize));
  RINOK(SetInitVector(_iv, 16));
  Init();

  memmove(p, p + validOffset, validSize);
  Filter(p, validSize);

  if (validSize < 4)
    return E_NOTIMPL;
  validSize -= 4;
  if (GetUi32(p + validSize) != CrcCalc(p, validSize))
    return S_OK;
  passwOK = true;
  return S_OK;
}

}}
