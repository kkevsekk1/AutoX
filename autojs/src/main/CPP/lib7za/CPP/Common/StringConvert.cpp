// Common/StringConvert.cpp

#include "StdAfx.h"

#include "StringConvert.h"
extern "C"
{
int global_use_utf16_conversion = 0;
}

#ifdef LOCALE_IS_UTF8

#ifdef __APPLE_CC__
#define UInt32  macUIn32
#include <CoreFoundation/CoreFoundation.h>
#undef UInt32

UString MultiByteToUnicodeString(const AString &srcString, UINT codePage)
{
  if (!srcString.IsEmpty())
  {
    UString resultString;
    const char * path = &srcString[0];

// FIXME    size_t n = strlen(path);

    CFStringRef cfpath = CFStringCreateWithCString(NULL,path,kCFStringEncodingUTF8);

    if (cfpath)
    {

       CFMutableStringRef cfpath2 = CFStringCreateMutableCopy(NULL,0,cfpath);
       CFRelease(cfpath);
       CFStringNormalize(cfpath2,kCFStringNormalizationFormC);
    
       size_t n = CFStringGetLength(cfpath2);
       for(size_t i =   0 ; i< n ;i++) {
         UniChar uc = CFStringGetCharacterAtIndex(cfpath2,i);
         resultString += (wchar_t)uc; // FIXME
       }

       CFRelease(cfpath2);  

       return resultString;
    }
  }

  UString resultString;
  for (int i = 0; i < srcString.Len(); i++)
    resultString += wchar_t(srcString[i] & 255);

  return resultString;
}

AString UnicodeStringToMultiByte(const UString &srcString, UINT codePage)
{
  if (!srcString.IsEmpty())
  {
    const wchar_t * wcs = &srcString[0];
    char utf8[4096];
    UniChar unipath[4096];

    size_t n = wcslen(wcs);

    for(size_t i =   0 ; i<= n ;i++) {
      unipath[i] = wcs[i];
    }

    CFStringRef cfpath = CFStringCreateWithCharacters(NULL,unipath,n);

    CFMutableStringRef cfpath2 = CFStringCreateMutableCopy(NULL,0,cfpath);
    CFRelease(cfpath);
    CFStringNormalize(cfpath2,kCFStringNormalizationFormD);
    
    CFStringGetCString(cfpath2,(char *)utf8,4096,kCFStringEncodingUTF8);

    CFRelease(cfpath2);  

    return AString(utf8);
  }

  AString resultString;
  for (int i = 0; i < srcString.Len(); i++)
  {
    if (srcString[i] >= 256) resultString += '?';
    else                     resultString += char(srcString[i]);
  }
  return resultString;
}

#else /* __APPLE_CC__ */


#include "UTFConvert.h"

UString MultiByteToUnicodeString(const AString &srcString, UINT codePage)
{
  if ((global_use_utf16_conversion) && (!srcString.IsEmpty()))
  {
    UString resultString;
    bool bret = ConvertUTF8ToUnicode(srcString,resultString);
    if (bret) return resultString;
  }

  UString resultString;
  for (int i = 0; i < srcString.Len(); i++)
    resultString += wchar_t(srcString[i] & 255);

  return resultString;
}

AString UnicodeStringToMultiByte(const UString &srcString, UINT codePage)
{
  if ((global_use_utf16_conversion) && (!srcString.IsEmpty()))
  {
    AString resultString;
    ConvertUnicodeToUTF8(srcString,resultString);
    return resultString;
  }

  AString resultString;
  for (int i = 0; i < srcString.Len(); i++)
  {
    if (srcString[i] >= 256) resultString += '?';
    else                     resultString += char(srcString[i]);
  }
  return resultString;
}

#endif /* __APPLE_CC__ */

#else /* LOCALE_IS_UTF8 */

UString MultiByteToUnicodeString(const AString &srcString, UINT /* codePage */ )
{
#ifdef ENV_HAVE_MBSTOWCS
  if ((global_use_utf16_conversion) && (!srcString.IsEmpty()))
  {
    UString resultString;
    int numChars = mbstowcs(resultString.GetBuf(srcString.Len()),srcString,srcString.Len()+1);
    if (numChars >= 0) {
        resultString.ReleaseBuf_SetEnd(numChars);

#if WCHAR_MAX > 0xffff
      for (int i = numChars; i >= 0; i--) {
        if (resultString[i] > 0xffff) {
          wchar_t c = resultString[i] - 0x10000;
          resultString.Delete(i);
          wchar_t texts[]= { ((c >> 10) & 0x3ff) + 0xd800,  (c & 0x3ff) + 0xdc00 , 0 };
          resultString.Insert(i, texts);
          numChars++;
        }
      }
#endif

      return resultString;
    }
  }
#endif

  UString resultString;
  for (int i = 0; i < srcString.Len(); i++)
    resultString += wchar_t(srcString[i] & 255);

  return resultString;
}

AString UnicodeStringToMultiByte(const UString &src, UINT /* codePage */ )
{
#ifdef ENV_HAVE_WCSTOMBS
#if WCHAR_MAX > 0xffff
  UString srcString(src);
  for (int i = 0; i < srcString.Len(); i++) {
    if ((0xd800 <= srcString[i] && srcString[i] <= 0xdbff) && ((i + 1) < srcString.Len()) &&
        (0xdc00 <= srcString[i + 1] && srcString[i + 1] < 0xE000)) {
      wchar_t c = (((srcString[i] - 0xd800) << 10) | (srcString[i + 1] - 0xdc00)) + 0x10000;
      srcString.Delete(i, 2);
      srcString.Insert(i, c);
    }
  }
#else
  const UString &srcString = src;
#endif

  if ((global_use_utf16_conversion) && (!srcString.IsEmpty()))
  {
    AString resultString;
    int numRequiredBytes = srcString.Len() * 6+1;
    int numChars = wcstombs(resultString.GetBuf(numRequiredBytes),srcString,numRequiredBytes);
    if (numChars >= 0) {
      resultString.ReleaseBuf_SetEnd(numChars);
      return resultString;
    }
  }
#else
  const UString &srcString = src;
#endif

  AString resultString;
  for (int i = 0; i < srcString.Len(); i++)
  {
    if (srcString[i] >= 256) resultString += '?';
    else                     resultString += char(srcString[i]);
  }
  return resultString;
}

#endif /* LOCALE_IS_UTF8 */


void MultiByteToUnicodeString2(UString &dest, const AString &srcString, UINT codePage)
{
  dest = MultiByteToUnicodeString(srcString,codePage);
}

void UnicodeStringToMultiByte2(AString &dest, const UString &srcString, UINT codePage)
{
  dest = UnicodeStringToMultiByte(srcString,codePage);
}

