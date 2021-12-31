// Common/StdInStream.cpp

#include "StdAfx.h"

#include <tchar.h>

#include "StdInStream.h"
#include "StringConvert.h"
#include "UTFConvert.h"

static const char kNewLineChar = '\n';

static const char *kEOFMessage = "Unexpected end of input stream";
static const char *kReadErrorMessage  ="Error reading input stream";
static const char *kIllegalCharMessage = "Illegal character in input stream";

// static LPCTSTR kFileOpenMode = TEXT("r");

// extern int g_CodePage;

#ifdef _UNICODE
#define NEED_NAME_WINDOWS_TO_UNIX
#include "myPrivate.h"
#endif

CStdInStream g_StdIn(stdin);

bool CStdInStream::Open(LPCTSTR fileName) throw()
{
  Close();
//  _stream = _tfopen(fileName, kFileOpenMode);
#ifdef _UNICODE
  AString aStr = UnicodeStringToMultiByte(fileName, CP_ACP); // FIXME
  const char * name = nameWindowToUnix(aStr);
#else
  const char * name = nameWindowToUnix(fileName);
#endif
  _stream = fopen(name, "r");
  _streamIsOpen = (_stream != 0);
  return _streamIsOpen;
}

bool CStdInStream::Close() throw()
{
  if (!_streamIsOpen)
    return true;
  _streamIsOpen = (fclose(_stream) != 0);
  return !_streamIsOpen;
}

AString CStdInStream::ScanStringUntilNewLine(bool allowEOF)
{
  AString s;
  for (;;)
  {
    int intChar = GetChar();
    if (intChar == EOF)
    {
      if (allowEOF)
        break;
      throw kEOFMessage;
    }
    char c = (char)intChar;
    if (c == 0)
      throw kIllegalCharMessage;
    if (c == kNewLineChar)
      break;
    s += c;
  }
  return s;
}

#ifdef _WIN32
UString CStdInStream::ScanUStringUntilNewLine()
{
  AString s = ScanStringUntilNewLine(true);
  int codePage = g_CodePage;
  if (codePage == -1)
    codePage = CP_OEMCP;
  UString dest;
  if (codePage == CP_UTF8)
    ConvertUTF8ToUnicode(s, dest);
  else
    dest = MultiByteToUnicodeString(s, (UINT)codePage);
  return dest;
}
#else

#ifndef ENV_HAVE_GETPASS
UString CStdInStream::ScanUStringUntilNewLine()
{
  AString s = ScanStringUntilNewLine(true);
  UString dest = MultiByteToUnicodeString(s, (UINT)-1);
  return dest;
}
#endif

#endif

void CStdInStream::ReadToString(AString &resultString)
{
  resultString.Empty();
  int c;
  while ((c = GetChar()) != EOF)
    resultString += (char)c;
}

bool CStdInStream::Eof() throw()
{
  return (feof(_stream) != 0);
}

int CStdInStream::GetChar()
{
  int c = fgetc(_stream); // getc() doesn't work in BeOS?
  if (c == EOF && !Eof())
    throw kReadErrorMessage;
  return c;
}
