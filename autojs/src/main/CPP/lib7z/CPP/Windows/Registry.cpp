// Windows/Registry.cpp

#include "StdAfx.h"

#ifndef _UNICODE
#include "Common/StringConvert.h"
#endif
#include "Windows/Registry.h"

#include <wx/config.h>

class HKEY_Impl
{
  public:
	wxString path;
	HKEY_Impl(wxString a) : path(a) {}
};

namespace NWindows {
namespace NRegistry {

#define ERROR_SET_VALUE (E_INVALIDARG) // FIXME
#define ERROR_GET_VALUE (E_INVALIDARG) // FIXME
#define PROGRAM_NAME L"p7zip"

static wxConfig * g_config = 0;
static int        g_config_ref = 0;

static void configAddRef() {
	if (g_config == 0) {
		g_config = new wxConfig(PROGRAM_NAME);
		g_config->Flush(true);
		wxConfigBase::Set(g_config);
	}
	g_config_ref++;
}

static void configSubRef() {
	if (g_config_ref >= 1)
	{
		g_config_ref--;
		if (g_config_ref == 0) {
			delete g_config;
			g_config = 0;
			wxConfigBase::Set(NULL);
		} else {
			g_config->Flush(true);
		}
	}
}

LONG CKey::Close() throw()
{
	if (_object) 
	{
		configSubRef();
		delete _object;
	}
	_object = 0;
	return ERROR_SUCCESS;
}

LONG CKey::Create(HKEY parentKey, LPCTSTR keyName) throw()
{
	Close();

	configAddRef();

	wxString path;

	if (parentKey == HKEY_CURRENT_USER) {
		path=L"/" + wxString(keyName);
	} else {
		path = parentKey->path + L"/" + wxString(keyName);
	}
	_object = new HKEY_Impl(path);
	return ERROR_SUCCESS;
}
LONG CKey::Open(HKEY parentKey, LPCTSTR keyName, REGSAM accessMask) throw()
{
	Close();

	configAddRef();

	wxString path;

	if (parentKey == HKEY_CURRENT_USER) {
		path=L"/" + wxString(keyName);
	} else {
		path = parentKey->path + L"/" + wxString(keyName);
	}
	_object = new HKEY_Impl(path);
	return ERROR_SUCCESS;
}

LONG CKey::RecurseDeleteKey(LPCTSTR subKeyName) throw()
{
	g_config->SetPath(_object->path);
	bool ret = g_config->DeleteGroup(subKeyName);
	if (ret) return ERROR_SUCCESS;
	return ERROR_GET_VALUE;
}

LONG CKey::DeleteValue(LPCTSTR name) throw()
{
	g_config->SetPath(_object->path);
	bool ret = g_config->DeleteEntry(name);
	if (ret) return ERROR_SUCCESS;
	return ERROR_GET_VALUE;
}

LONG CKey::QueryValue(LPCTSTR name, UInt32 &value) throw()
{
	g_config->SetPath(_object->path);
	long val;
	bool ret = g_config->Read(name,&val);
	if (ret) {
		value = (UInt32)val;
		return ERROR_SUCCESS;
	}
	return ERROR_GET_VALUE;
}

LONG CKey::QueryValue(LPCTSTR name, bool &value) throw()
{
	g_config->SetPath(_object->path);
	bool ret = g_config->Read(name,&value);
	if (ret) return ERROR_SUCCESS;
	return ERROR_GET_VALUE;
}

LONG CKey::QueryValue(LPCTSTR name, CSysString &value)
{
	g_config->SetPath(_object->path);
	wxString val;
	bool ret = g_config->Read(name,&val);
	if (ret) {
		value = val;
		return ERROR_SUCCESS;
	}
	return ERROR_GET_VALUE;
}

LONG CKey::GetValue_IfOk(LPCTSTR name, UInt32 &value) throw()
{
  UInt32 newVal;
  LONG res = QueryValue(name, newVal);
  if (res == ERROR_SUCCESS)
    value = newVal;
  return res;
}

LONG CKey::GetValue_IfOk(LPCTSTR name, bool &value) throw()
{
  bool newVal;
  LONG res = QueryValue(name, newVal);
  if (res == ERROR_SUCCESS)
    value = newVal;
  return res;
}

LONG CKey::SetValue(LPCTSTR valueName, UInt32 value) throw()
{
	g_config->SetPath(_object->path);
	bool ret = g_config->Write(valueName,(long)value);
	if (ret == true) return ERROR_SUCCESS;
	return ERROR_SET_VALUE;
}

LONG CKey::SetValue(LPCTSTR valueName, bool value) throw()
{
	g_config->SetPath(_object->path);
	bool ret = g_config->Write(valueName,value);
	if (ret == true) return ERROR_SUCCESS;
	return ERROR_SET_VALUE;
}

LONG CKey::SetValue(LPCTSTR valueName, LPCTSTR value) throw()
{
	g_config->SetPath(_object->path);
	bool ret = g_config->Write(valueName,value);
	if (ret == true) return ERROR_SUCCESS;
	return ERROR_SET_VALUE;
}

LONG CKey::SetValue(LPCTSTR name, const void *value, UInt32 size) throw()
{
	static char hexa[] = "0123456789ABCDEF";
	/* FIXME
	MYASSERT(value != NULL);
	MYASSERT(_object != NULL);
	return RegSetValueEx(_object, name, NULL, REG_BINARY, (const BYTE *)value, size);
	*/
	BYTE *buf = (BYTE *)value;
	wxString str;
	for(UInt32 i=0;i<size;i++)
	{
		str += 	hexa[ (buf[i]>>4) & 0x0f];
		str += 	hexa[ buf[i] & 0x0f];
	}
	return SetValue(name,(LPCTSTR)str);
}

LONG CKey::EnumKeys(CSysStringVector &keyNames)
{
	g_config->SetPath(_object->path);
	keyNames.Clear();
	// enumeration variables
	wxString str;
	long dummy;
	bool bCont = g_config->GetFirstEntry(str, dummy);
	while ( bCont ) {
		keyNames.Add((const TCHAR *)str);
		bCont = g_config->GetNextEntry(str, dummy);
	}

	// now all groups...
	bCont = g_config->GetFirstGroup(str, dummy);
	while ( bCont ) {
		keyNames.Add((const TCHAR *)str);
		bCont = g_config->GetNextGroup(str, dummy);
  	}
	return ERROR_SUCCESS;
}

LONG CKey::QueryValue(LPCTSTR name, void *value, UInt32 &dataSize) throw()
{
	g_config->SetPath(_object->path);
	wxString str;
	bool ret = g_config->Read(name,&str);
	if (ret == false) return ERROR_GET_VALUE;

	size_t l =  str.Len() / 2;
	if (l > dataSize) l = dataSize;
	else              dataSize=l;

	BYTE *buf = (BYTE *)value;
	for(UInt32 i=0;i<dataSize;i++)
	{
		char cval[3];
		cval[0] = (char)str[2*i];
		cval[1] = (char)str[2*i+1];
		cval[2] = 0;
		unsigned uval = 0;
		sscanf(cval,"%x",&uval);
		buf[i]=(BYTE)uval;
	}

	return ERROR_SUCCESS;
}


LONG CKey::QueryValue(LPCTSTR name, CByteBuffer &value, UInt32 &dataSize)
{
	g_config->SetPath(_object->path);
	wxString str;
	bool ret = g_config->Read(name,&str);
	if (ret == false) return ERROR_GET_VALUE;

	dataSize =  str.Len() / 2;
	value.Alloc(dataSize);
	return QueryValue(name, (BYTE *)value, dataSize);
}


LONG CKey::SetValue_Strings(LPCTSTR valueName, const UStringVector &strings) throw()
{
  UInt32 numChars = 0;
  int i;
  for (i = 0; i < strings.Size(); i++)
    numChars += strings[i].Len() + 1;
  CBuffer<wchar_t> buffer(numChars);
  int pos = 0;
  for (i = 0; i < strings.Size(); i++)
  {
    const UString &s = strings[i];
    MyStringCopy((wchar_t *)buffer + pos, (const wchar_t *)s);
    pos += s.Len() + 1;
  }
  return SetValue(valueName, buffer, numChars * sizeof(wchar_t));
}

LONG CKey::GetValue_Strings(LPCTSTR valueName, UStringVector &strings)
{
  strings.Clear();
  CByteBuffer buffer;
  UInt32 dataSize;
  LONG res = QueryValue(valueName, buffer, dataSize);
  if (res != ERROR_SUCCESS)
    return res;
  if (dataSize % sizeof(wchar_t) != 0)
    return E_FAIL;
  const wchar_t *data = (const wchar_t *)(const Byte  *)buffer;
  int numChars = dataSize / sizeof(wchar_t);
  UString s;
  for (int i = 0; i < numChars; i++)
  {
    wchar_t c = data[i];
    if (c == 0)
    {
      strings.Add(s);
      s.Empty();
    }
    else
      s += c;
  }
  return res;
}


}
}

