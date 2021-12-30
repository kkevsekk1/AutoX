// MyWindows.cpp

#include "StdAfx.h"

#ifndef _WIN32

#include "MyWindows.h"
#include "MyTypes.h"
#include <stdlib.h> /* FIXED <malloc.h> */

static inline void *AllocateForBSTR(size_t cb) { return ::malloc(cb); }
static inline void FreeForBSTR(void *pv) { ::free(pv);}

/* FIXME
static UINT MyStringLen(const wchar_t *s)
{ 
  UINT i;
  for (i = 0; s[i] != '\0'; i++);
  return i;
}
*/

BSTR SysAllocStringByteLen(LPCSTR psz, UINT len)
{
  // FIXED int realLen = len + sizeof(UINT) + 3;
  const int LEN_ADDON = sizeof(wchar_t) - 1;
  int realLen = len + sizeof(UINT) + sizeof(wchar_t) + LEN_ADDON;
  void *p = AllocateForBSTR(realLen);
  if (p == 0)
    return 0;
  *(UINT *)p = len;
  // "void *" instead of "BSTR" to avoid unaligned copy of "wchar_t" because of optimizer on Solaris
  void * bstr = (void *)((UINT *)p + 1);
  if (psz) memmove(bstr, psz, len); // psz does not always have "wchar_t" alignment.
  void *pb = (void *)(((Byte *)bstr) + len);
  memset(pb,0,sizeof(wchar_t) + LEN_ADDON);
  return (BSTR)bstr;
}

BSTR WINAPI SysAllocStringLen(const OLECHAR *sz, unsigned int numChars) // FIXME - code
{
  UINT len = (numChars + 1) * sizeof(OLECHAR);
  void *p = AllocateForBSTR(len + sizeof(UINT));
  if (p == 0)
    return 0;
  memset(p,0,len + sizeof(UINT));
  *(UINT *)p = numChars * sizeof(OLECHAR); // FIXED
  void * bstr = (void *)((UINT *)p + 1);
  if (sz) memmove(bstr, sz, numChars  * sizeof(OLECHAR)); // sz does not always have "wchar_t" alignment.

  return (BSTR)bstr;
}


BSTR SysAllocString(const OLECHAR *sz)
{
  if (sz == 0)
    return 0;
  UINT strLen = MyStringLen(sz);
  UINT len = (strLen + 1) * sizeof(OLECHAR);
  void *p = AllocateForBSTR(len + sizeof(UINT));
  if (p == 0)
    return 0;
  *(UINT *)p = strLen * sizeof(OLECHAR); // FIXED
  void * bstr = (void *)((UINT *)p + 1);
  memmove(bstr, sz, len); // sz does not always have "wchar_t" alignment.
  return (BSTR)bstr;
}

void SysFreeString(BSTR bstr)
{
  if (bstr != 0)
    FreeForBSTR((UINT *)bstr - 1);
}

UINT SysStringByteLen(BSTR bstr)
{
  if (bstr == 0)
    return 0;
  return *((UINT *)bstr - 1);

}

UINT SysStringLen(BSTR bstr)
{
  return SysStringByteLen(bstr) / sizeof(OLECHAR);
}

HRESULT VariantClear(VARIANTARG *prop)
{
  if (prop->vt == VT_BSTR)
    SysFreeString(prop->bstrVal);
  prop->vt = VT_EMPTY;
  return S_OK;
}

HRESULT VariantCopy(VARIANTARG *dest, VARIANTARG *src)
{
  HRESULT res = ::VariantClear(dest);
  if (res != S_OK)
    return res;
  if (src->vt == VT_BSTR)
  {
    dest->bstrVal = SysAllocStringByteLen((LPCSTR)src->bstrVal, 
        SysStringByteLen(src->bstrVal));
    if (dest->bstrVal == 0)
      return E_OUTOFMEMORY;
    dest->vt = VT_BSTR;
  }
  else
    *dest = *src;
  return S_OK;
}

LONG CompareFileTime(const FILETIME* ft1, const FILETIME* ft2)
{
  if(ft1->dwHighDateTime < ft2->dwHighDateTime)
    return -1;
  if(ft1->dwHighDateTime > ft2->dwHighDateTime)
    return 1;
  if(ft1->dwLowDateTime < ft2->dwLowDateTime)
    return -1;
  if(ft1->dwLowDateTime > ft2->dwLowDateTime)
    return 1;
  return 0;
}

#endif
