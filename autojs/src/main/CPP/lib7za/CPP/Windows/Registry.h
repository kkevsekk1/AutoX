// Windows/Registry.h

#ifndef __WINDOWS_REGISTRY_H
#define __WINDOWS_REGISTRY_H

#include "../Common/MyBuffer.h"
#include "../Common/MyString.h"
#include "../Common/MyTypes.h"

#ifndef _WIN32
class HKEY_Impl;

typedef HKEY_Impl * HKEY;

#define HKEY_CURRENT_USER       ((HKEY) 0x80000001)

typedef DWORD REGSAM;
#define ERROR_SUCCESS (0)
#define KEY_READ	(0x1234) // FIXME
#define KEY_ALL_ACCESS  (~0)     // FIXME

#endif

namespace NWindows {
namespace NRegistry {

LONG SetValue(HKEY parentKey, LPCTSTR keyName, LPCTSTR valueName, LPCTSTR value);

class CKey
{
  HKEY _object;
public:
  CKey(): _object(NULL) {}
  ~CKey() { Close(); }

  operator HKEY() const { return _object; }
#if 0
  void Attach(HKEY key) { _object = key; }
  HKEY Detach()
  {
    HKEY key = _object;
    _object = NULL;
    return key;
  }

  LONG Create(HKEY parentKey, LPCTSTR keyName,
      LPTSTR keyClass = REG_NONE, DWORD options = REG_OPTION_NON_VOLATILE,
      REGSAM accessMask = KEY_ALL_ACCESS,
      LPSECURITY_ATTRIBUTES securityAttributes = NULL,
      LPDWORD disposition = NULL);
#endif // #if 0
  LONG Create(HKEY parentKey, LPCTSTR keyName) throw();
  LONG Open(HKEY parentKey, LPCTSTR keyName, REGSAM accessMask = KEY_ALL_ACCESS) throw();

  LONG Close() throw();

  LONG DeleteSubKey(LPCTSTR subKeyName) throw();
  LONG RecurseDeleteKey(LPCTSTR subKeyName) throw();

  LONG DeleteValue(LPCTSTR name) throw();
  #ifndef _UNICODE
  LONG DeleteValue(LPCWSTR name);
  #endif

  LONG SetValue(LPCTSTR valueName, UInt32 value) throw();
  LONG SetValue(LPCTSTR valueName, bool value) throw();
  LONG SetValue(LPCTSTR valueName, LPCTSTR value) throw();
  // LONG SetValue(LPCTSTR valueName, const CSysString &value);
  #ifndef _UNICODE
  LONG SetValue(LPCWSTR name, LPCWSTR value);
  // LONG SetValue(LPCWSTR name, const UString &value);
  #endif

  LONG SetValue(LPCTSTR name, const void *value, UInt32 size) throw();

  LONG SetValue_Strings(LPCTSTR valueName, const UStringVector &strings) throw();
  LONG GetValue_Strings(LPCTSTR valueName, UStringVector &strings);

  LONG SetKeyValue(LPCTSTR keyName, LPCTSTR valueName, LPCTSTR value) throw();

  LONG QueryValue(LPCTSTR name, UInt32 &value) throw();
  LONG QueryValue(LPCTSTR name, bool &value) throw();
  LONG QueryValue(LPCTSTR name, LPTSTR value, UInt32 &dataSize) throw();
  LONG QueryValue(LPCTSTR name, CSysString &value);

  LONG GetValue_IfOk(LPCTSTR name, UInt32 &value) throw();
  LONG GetValue_IfOk(LPCTSTR name, bool &value) throw();

  #ifndef _UNICODE
  LONG QueryValue(LPCWSTR name, LPWSTR value, UInt32 &dataSize);
  LONG QueryValue(LPCWSTR name, UString &value);
  #endif

  LONG QueryValue(LPCTSTR name, void *value, UInt32 &dataSize) throw();
  LONG QueryValue(LPCTSTR name, CByteBuffer &value, UInt32 &dataSize);

  LONG EnumKeys(CSysStringVector &keyNames);
};

}}

#endif
