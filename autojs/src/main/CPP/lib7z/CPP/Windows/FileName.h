// Windows/FileName.h

#ifndef __WINDOWS_FILENAME_H
#define __WINDOWS_FILENAME_H

#include "../../C/7zTypes.h"

#include "../Common/MyString.h"

namespace NWindows {
namespace NFile {
namespace NName {

int FindSepar(const wchar_t *s) throw();
#ifndef USE_UNICODE_FSTRING
int FindSepar(const FChar *s) throw();
#endif

const TCHAR kDirDelimiter = CHAR_PATH_SEPARATOR;
const TCHAR kAnyStringWildcard = '*';

void NormalizeDirPathPrefix(CSysString &dirPath); // ensures that it ended with '\\'
#ifndef _UNICODE
void NormalizeDirPathPrefix(UString &dirPath); // ensures that it ended with '\\'
#endif

bool IsAbsolutePath(const wchar_t *s) throw();
unsigned GetRootPrefixSize(const wchar_t *s) throw();

bool GetFullPath(CFSTR dirPrefix, CFSTR path, FString &fullPath);
bool GetFullPath(CFSTR path, FString &fullPath);

}}}

#endif
