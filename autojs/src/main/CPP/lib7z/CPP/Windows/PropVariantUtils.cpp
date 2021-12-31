// PropVariantUtils.cpp

#include "StdAfx.h"

#include "../Common/IntToString.h"

#include "PropVariantUtils.h"

using namespace NWindows;

static AString GetHex(UInt32 v)
{
  char sz[16];
  sz[0] = '0';
  sz[1] = 'x';
  ConvertUInt32ToHex(v, sz + 2);
  return sz;
}

AString TypePairToString(const CUInt32PCharPair *pairs, unsigned num, UInt32 value)
{
  AString s;
  for (unsigned i = 0; i < num; i++)
  {
    const CUInt32PCharPair &p = pairs[i];
    if (p.Value == value)
      s = p.Name;
  }
  if (s.IsEmpty())
    s = GetHex(value);
  return s;
}

void PairToProp(const CUInt32PCharPair *pairs, unsigned num, UInt32 value, NCOM::CPropVariant &prop)
{
  prop = TypePairToString(pairs, num, value);
}


AString TypeToString(const char * const table[], unsigned num, UInt32 value)
{
  if (value < num)
    return table[value];
  return GetHex(value);
}

void TypeToProp(const char * const table[], unsigned num, UInt32 value, NCOM::CPropVariant &prop)
{
  prop = TypeToString(table, num, value);
}


AString FlagsToString(const char * const *names, unsigned num, UInt32 flags)
{
  AString s;
  for (unsigned i = 0; i < num; i++)
  {
    UInt32 flag = (UInt32)1 << i;
    if ((flags & flag) != 0)
    {
      const char *name = names[i];
      if (name != 0 && name[0] != 0)
      {
        if (!s.IsEmpty())
          s += ' ';
        s += name;
        flags &= ~flag;
      }
    }
  }
  if (flags != 0)
  {
    if (!s.IsEmpty())
      s += ' ';
    s += GetHex(flags);
  }
  return s;
}

AString FlagsToString(const CUInt32PCharPair *pairs, unsigned num, UInt32 flags)
{
  AString s;
  for (unsigned i = 0; i < num; i++)
  {
    const CUInt32PCharPair &p = pairs[i];
    UInt32 flag = (UInt32)1 << (unsigned)p.Value;
    if ((flags & flag) != 0)
    {
      if (p.Name[0] != 0)
      {
        if (!s.IsEmpty())
          s += ' ';
        s += p.Name;
      }
    }
    flags &= ~flag;
  }
  if (flags != 0)
  {
    if (!s.IsEmpty())
      s += ' ';
    s += GetHex(flags);
  }
  return s;
}

void FlagsToProp(const CUInt32PCharPair *pairs, unsigned num, UInt32 flags, NCOM::CPropVariant &prop)
{
  prop = FlagsToString(pairs, num, flags);
}


AString Flags64ToString(const CUInt32PCharPair *pairs, unsigned num, UInt64 flags)
{
  AString s;
  for (unsigned i = 0; i < num; i++)
  {
    const CUInt32PCharPair &p = pairs[i];
    UInt64 flag = (UInt64)1 << (unsigned)p.Value;
    if ((flags & flag) != 0)
    {
      if (p.Name[0] != 0)
      {
        if (!s.IsEmpty())
          s += ' ';
        s += p.Name;
      }
    }
    flags &= ~flag;
  }
  if (flags != 0)
  {
    if (!s.IsEmpty())
      s += ' ';
    {
      char sz[32];
      sz[0] = '0';
      sz[1] = 'x';
      ConvertUInt64ToHex(flags, sz + 2);
      s += sz;
    }
  }
  return s;
}

void Flags64ToProp(const CUInt32PCharPair *pairs, unsigned num, UInt64 flags, NCOM::CPropVariant &prop)
{
  prop = Flags64ToString(pairs, num, flags);
}
