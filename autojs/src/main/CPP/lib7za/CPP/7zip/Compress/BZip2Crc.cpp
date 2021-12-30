// BZip2Crc.cpp

#include "StdAfx.h"

#include "BZip2Crc.h"

UInt32 CBZip2Crc::Table[256];

static const UInt32 kBZip2CrcPoly = 0x04c11db7;  /* AUTODIN II, Ethernet, & FDDI */

void CBZip2Crc::InitTable()
{
  for (UInt32 i = 0; i < 256; i++)
  {
    UInt32 r = (i << 24);
    for (int j = 8; j > 0; j--)
      r = (r & 0x80000000) ? ((r << 1) ^ kBZip2CrcPoly) : (r << 1);
    Table[i] = r;
  }
}

class CBZip2CrcTableInit
{
public:
  CBZip2CrcTableInit() { CBZip2Crc::InitTable(); }
} g_BZip2CrcTableInit;
