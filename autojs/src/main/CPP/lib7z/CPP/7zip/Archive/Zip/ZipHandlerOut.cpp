// ZipHandlerOut.cpp

#include "StdAfx.h"

#include "../../../Common/ComTry.h"
#include "../../../Common/StringConvert.h"
#include "../../../Common/StringToInt.h"

#include "../../../Windows/PropVariant.h"
#include "../../../Windows/TimeUtils.h"

#include "../../IPassword.h"

#include "../../Common/OutBuffer.h"

#include "../../Crypto/WzAes.h"

#include "../Common/ItemNameUtils.h"
#include "../Common/ParseProperties.h"

#include "ZipHandler.h"
#include "ZipUpdate.h"

using namespace NWindows;
using namespace NCOM;
using namespace NTime;

namespace NArchive {
namespace NZip {

STDMETHODIMP CHandler::GetFileTimeType(UInt32 *timeType)
{
  *timeType = NFileTimeType::kDOS;
  return S_OK;
}

static bool IsSimpleAsciiString(const wchar_t *s)
{
  for (;;)
  {
    wchar_t c = *s++;
    if (c == 0)
      return true;
    if (c < 0x20 || c > 0x7F)
      return false;
  }
}

#define COM_TRY_BEGIN2 try {
#define COM_TRY_END2 } \
catch(const CSystemException &e) { return e.ErrorCode; } \
catch(...) { return E_OUTOFMEMORY; }

static HRESULT GetTime(IArchiveUpdateCallback *callback, int index, PROPID propID, FILETIME &filetime)
{
  filetime.dwHighDateTime = filetime.dwLowDateTime = 0;
  NCOM::CPropVariant prop;
  RINOK(callback->GetProperty(index, propID, &prop));
  if (prop.vt == VT_FILETIME)
    filetime = prop.filetime;
  else if (prop.vt != VT_EMPTY)
    return E_INVALIDARG;
  return S_OK;
}

STDMETHODIMP CHandler::UpdateItems(ISequentialOutStream *outStream, UInt32 numItems,
    IArchiveUpdateCallback *callback)
{
  COM_TRY_BEGIN2
  
  if (m_Archive.IsOpen())
  {
    if (!m_Archive.CanUpdate())
      return E_NOTIMPL;
  }

  CObjectVector<CUpdateItem> updateItems;
  bool thereAreAesUpdates = false;
  UInt64 largestSize = 0;
  bool largestSizeDefined = false;

  for (UInt32 i = 0; i < numItems; i++)
  {
    CUpdateItem ui;
    Int32 newData;
    Int32 newProps;
    UInt32 indexInArchive;
    if (!callback)
      return E_FAIL;
    RINOK(callback->GetUpdateItemInfo(i, &newData, &newProps, &indexInArchive));
    ui.NewProps = IntToBool(newProps);
    ui.NewData = IntToBool(newData);
    ui.IndexInArc = indexInArchive;
    ui.IndexInClient = i;
    bool existInArchive = (indexInArchive != (UInt32)(Int32)-1);
    if (existInArchive && newData)
      if (m_Items[indexInArchive].IsAesEncrypted())
        thereAreAesUpdates = true;

    if (IntToBool(newProps))
    {
      UString name;
      {
        NCOM::CPropVariant prop;
        RINOK(callback->GetProperty(i, kpidAttrib, &prop));
        if (prop.vt == VT_EMPTY)
          ui.Attrib = 0;
        else if (prop.vt != VT_UI4)
          return E_INVALIDARG;
        else
          ui.Attrib = prop.ulVal;
      }

      {
        NCOM::CPropVariant prop;
        RINOK(callback->GetProperty(i, kpidPath, &prop));
        if (prop.vt == VT_EMPTY)
          name.Empty();
        else if (prop.vt != VT_BSTR)
          return E_INVALIDARG;
        else
          name = prop.bstrVal;
      }
      {
        NCOM::CPropVariant prop;
        RINOK(callback->GetProperty(i, kpidIsDir, &prop));
        if (prop.vt == VT_EMPTY)
          ui.IsDir = false;
        else if (prop.vt != VT_BOOL)
          return E_INVALIDARG;
        else
          ui.IsDir = (prop.boolVal != VARIANT_FALSE);
      }

      {
        CPropVariant prop;
        RINOK(callback->GetProperty(i, kpidTimeType, &prop));
        if (prop.vt == VT_UI4)
          ui.NtfsTimeIsDefined = (prop.ulVal == NFileTimeType::kWindows);
        else
          ui.NtfsTimeIsDefined = m_WriteNtfsTimeExtra;
      }
      RINOK(GetTime(callback, i, kpidMTime, ui.Ntfs_MTime));
      RINOK(GetTime(callback, i, kpidATime, ui.Ntfs_ATime));
      RINOK(GetTime(callback, i, kpidCTime, ui.Ntfs_CTime));

      {
        FILETIME localFileTime = { 0, 0 };
        if (ui.Ntfs_MTime.dwHighDateTime != 0 ||
            ui.Ntfs_MTime.dwLowDateTime != 0)
          if (!FileTimeToLocalFileTime(&ui.Ntfs_MTime, &localFileTime))
            return E_INVALIDARG;
        FileTimeToDosTime(localFileTime, ui.Time);
      }

      name = NItemName::MakeLegalName(name);
      bool needSlash = ui.IsDir;
      const wchar_t kSlash = L'/';
      if (!name.IsEmpty())
      {
        if (name.Back() == kSlash)
        {
          if (!ui.IsDir)
            return E_INVALIDARG;
          needSlash = false;
        }
      }
      if (needSlash)
        name += kSlash;

      UINT codePage = _forceCodePage ? _specifiedCodePage : CP_OEMCP;

      bool tryUtf8 = true;
      if ((m_ForceLocal || !m_ForceUtf8) && codePage != CP_UTF8)
      {
#ifdef _WIN32
        bool defaultCharWasUsed;
        ui.Name = UnicodeStringToMultiByte(name, codePage, '_', defaultCharWasUsed);
        tryUtf8 = (!m_ForceLocal && (defaultCharWasUsed ||
          MultiByteToUnicodeString(ui.Name, codePage) != name));
#else
	// FIXME
        ui.Name = UnicodeStringToMultiByte(name, CP_OEMCP);
        tryUtf8 = (!m_ForceLocal);
#endif
      }

      if (tryUtf8)
      {
        ui.IsUtf8 = !name.IsAscii();
        ConvertUnicodeToUTF8(name, ui.Name);
      }

      if (ui.Name.Len() >= (1 << 16))
        return E_INVALIDARG;

      ui.IndexInClient = i;
      /*
      if (existInArchive)
      {
        const CItemEx &itemInfo = m_Items[indexInArchive];
        // ui.Commented = itemInfo.IsCommented();
        ui.Commented = false;
        if (ui.Commented)
        {
          ui.CommentRange.Position = itemInfo.GetCommentPosition();
          ui.CommentRange.Size  = itemInfo.CommentSize;
        }
      }
      else
        ui.Commented = false;
      */
    }
    if (IntToBool(newData))
    {
      UInt64 size = 0;
      if (!ui.IsDir)
      {
        NCOM::CPropVariant prop;
        RINOK(callback->GetProperty(i, kpidSize, &prop));
        if (prop.vt != VT_UI8)
          return E_INVALIDARG;
        size = prop.uhVal.QuadPart;
        if (largestSize < size)
          largestSize = size;
        largestSizeDefined = true;
      }
      ui.Size = size;

      // ui.Size -= ui.Size / 2;
    }
    updateItems.Add(ui);
  }

  CMyComPtr<ICryptoGetTextPassword2> getTextPassword;
  {
    CMyComPtr<IArchiveUpdateCallback> udateCallBack2(callback);
    udateCallBack2.QueryInterface(IID_ICryptoGetTextPassword2, &getTextPassword);
  }
  CCompressionMethodMode options;
  (CBaseProps &)options = _props;
  options._dataSizeReduce = largestSize;
  options._dataSizeReduceDefined = largestSizeDefined;

  options.PasswordIsDefined = false;
  options.Password.Empty();
  if (getTextPassword)
  {
    CMyComBSTR password;
    Int32 passwordIsDefined;
    RINOK(getTextPassword->CryptoGetTextPassword2(&passwordIsDefined, &password));
    options.PasswordIsDefined = IntToBool(passwordIsDefined);
    if (options.PasswordIsDefined)
    {
      if (!m_ForceAesMode)
        options.IsAesMode = thereAreAesUpdates;

      if (!IsSimpleAsciiString(password))
        return E_INVALIDARG;
      if (password)
        options.Password = UnicodeStringToMultiByte((LPCOLESTR)password, CP_OEMCP);
      if (options.IsAesMode)
      {
        if (options.Password.Len() > NCrypto::NWzAes::kPasswordSizeMax)
          return E_INVALIDARG;
      }
    }
  }

  Byte mainMethod;
  if (m_MainMethod < 0)
    mainMethod = (Byte)(((_props.Level == 0) ?
        NFileHeader::NCompressionMethod::kStored :
        NFileHeader::NCompressionMethod::kDeflated));
  else
    mainMethod = (Byte)m_MainMethod;
  options.MethodSequence.Add(mainMethod);
  if (mainMethod != NFileHeader::NCompressionMethod::kStored)
    options.MethodSequence.Add(NFileHeader::NCompressionMethod::kStored);

  return Update(
      EXTERNAL_CODECS_VARS
      m_Items, updateItems, outStream,
      m_Archive.IsOpen() ? &m_Archive : NULL, _removeSfxBlock,
      &options, callback);
 
  COM_TRY_END2
}

struct CMethodIndexToName
{
  unsigned Method;
  const char *Name;
};

static const CMethodIndexToName k_SupportedMethods[] =
{
  { NFileHeader::NCompressionMethod::kStored, "copy" },
  { NFileHeader::NCompressionMethod::kDeflated, "deflate" },
  { NFileHeader::NCompressionMethod::kDeflated64, "deflate64" },
  { NFileHeader::NCompressionMethod::kBZip2, "bzip2" },
  { NFileHeader::NCompressionMethod::kLZMA, "lzma" },
  { NFileHeader::NCompressionMethod::kPPMd, "ppmd" }
};

STDMETHODIMP CHandler::SetProperties(const wchar_t * const *names, const PROPVARIANT *values, UInt32 numProps)
{
  InitMethodProps();
  #ifndef _7ZIP_ST
  const UInt32 numProcessors = _props.NumThreads;
  #endif
  
  for (UInt32 i = 0; i < numProps; i++)
  {
    UString name = names[i];
    name.MakeLower_Ascii();
    if (name.IsEmpty())
      return E_INVALIDARG;

    const PROPVARIANT &prop = values[i];

    if (name[0] == L'x')
    {
      UInt32 level = 9;
      RINOK(ParsePropToUInt32(name.Ptr(1), prop, level));
      _props.Level = level;
      _props.MethodInfo.AddProp_Level(level);
    }
    else if (name == L"m")
    {
      if (prop.vt == VT_BSTR)
      {
        UString m = prop.bstrVal, m2;
        m.MakeLower_Ascii();
        int colonPos = m.Find(L':');
        if (colonPos >= 0)
        {
          m2 = m.Ptr(colonPos + 1);
          m.DeleteFrom(colonPos);
        }
        unsigned k;
        for (k = 0; k < ARRAY_SIZE(k_SupportedMethods); k++)
        {
          const CMethodIndexToName &pair = k_SupportedMethods[k];
          if (m.IsEqualTo(pair.Name))
          {
            if (!m2.IsEmpty())
            {
              RINOK(_props.MethodInfo.ParseParamsFromString(m2));
            }
            m_MainMethod = pair.Method;
            break;
          }
        }
        if (k == ARRAY_SIZE(k_SupportedMethods))
          return E_INVALIDARG;
      }
      else if (prop.vt == VT_UI4)
      {
        unsigned k;
        for (k = 0; k < ARRAY_SIZE(k_SupportedMethods); k++)
        {
          unsigned method = k_SupportedMethods[k].Method;
          if (prop.ulVal == method)
          {
            m_MainMethod = method;
            break;
          }
        }
        if (k == ARRAY_SIZE(k_SupportedMethods))
          return E_INVALIDARG;
      }
      else
        return E_INVALIDARG;
    }
    else if (name.IsPrefixedBy(L"em"))
    {
      if (prop.vt != VT_BSTR)
        return E_INVALIDARG;
      {
        UString m = prop.bstrVal;
        m.MakeLower_Ascii();
        if (m.IsPrefixedBy(L"aes"))
        {
          m.DeleteFrontal(3);
          if (m == L"128")
            _props.AesKeyMode = 1;
          else if (m == L"192")
            _props.AesKeyMode = 2;
          else if (m == L"256" || m.IsEmpty())
            _props.AesKeyMode = 3;
          else
            return E_INVALIDARG;
          _props.IsAesMode = true;
          m_ForceAesMode = true;
        }
        else if (m == L"zipcrypto")
        {
          _props.IsAesMode = false;
          m_ForceAesMode = true;
        }
        else
          return E_INVALIDARG;
      }
    }
    else if (name.IsPrefixedBy(L"mt"))
    {
      #ifndef _7ZIP_ST
      RINOK(ParseMtProp(name.Ptr(2), prop, numProcessors, _props.NumThreads));
      _props.NumThreadsWasChanged = true;
      #endif
    }
    else if (name.IsEqualTo("tc"))
    {
      RINOK(PROPVARIANT_to_bool(prop, m_WriteNtfsTimeExtra));
    }
    else if (name.IsEqualTo("cl"))
    {
      RINOK(PROPVARIANT_to_bool(prop, m_ForceLocal));
      if (m_ForceLocal)
        m_ForceUtf8 = false;
    }
    else if (name.IsEqualTo("cu"))
    {
      RINOK(PROPVARIANT_to_bool(prop, m_ForceUtf8));
      if (m_ForceUtf8)
        m_ForceLocal = false;
    }
    else if (name.IsEqualTo("cp"))
    {
      UInt32 cp = CP_OEMCP;
      RINOK(ParsePropToUInt32(L"", prop, cp));
      _forceCodePage = true;
      _specifiedCodePage = cp;
    }
    else if (name.IsEqualTo("rsfx"))
    {
      RINOK(PROPVARIANT_to_bool(prop, _removeSfxBlock));
    }
    else
    {
      RINOK(_props.MethodInfo.ParseParamsFromPROPVARIANT(name, prop));
    }
  }
  return S_OK;
}

}}
