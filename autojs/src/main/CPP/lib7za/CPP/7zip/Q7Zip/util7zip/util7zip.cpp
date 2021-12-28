

#include "StdAfx.h"

// #include "../../../../C/Alloc.h"

#include <stdio.h>
#include <time.h>
#include <locale.h>

#include "Common/MyWindows.h"
#include "Common/MyTypes.h"
typedef          long LONG_PTR;  // FIXME 64 bits ?
typedef LONG_PTR LRESULT;
// typedef          long DWORD_PTR; // FIXME 64 bits ?
#include "Common/MyCom.h"
//#include "Windows/DLL.h"
#include "Windows/FileDir.h"
#include "Windows/FileFind.h"
// #include "Windows/Synchronization.h"
#include "Windows/PropVariant.h"
#include "Common/Wildcard.h"
#include "Windows/FileName.h"

#include "FSDrives.h"
#include "RootFolder.h"

#include "../../PropID.h"

#include "IFolder.h"


// #include "Common/MyInitGuid.h"


using namespace NWindows;
using namespace NFile;
using namespace NFind;

struct CTempFileInfo
{
  UInt32 FileIndex;  // index of file in folder
  UString RelPath;   // Relative path of file from Folder
  FString FolderPath;
  FString FilePath;
  NWindows::NFile::NFind::CFileInfo FileInfo;
  bool NeedDelete;

  CTempFileInfo(): FileIndex((UInt32)(Int32)-1), NeedDelete(false) {}
  void DeleteDirAndFile() const
  {
    if (NeedDelete)
    {
      NWindows::NFile::NDir::DeleteFileAlways(FilePath);
      NWindows::NFile::NDir::RemoveDir(FolderPath);
    }
  }
  bool WasChanged(const NWindows::NFile::NFind::CFileInfo &newFileInfo) const
  {
    return newFileInfo.Size != FileInfo.Size ||
        CompareFileTime(&newFileInfo.MTime, &FileInfo.MTime) != 0;
  }
};

struct CFolderLink: public CTempFileInfo
{
  // NWindows::NDLL::CLibrary Library;
  CMyComPtr<IFolderFolder> ParentFolder;
  bool UsePassword;
  UString Password;
  bool IsVirtual;

  UString VirtualPath;
  CFolderLink(): UsePassword(false), IsVirtual(false) {}

  bool WasChanged(const NWindows::NFile::NFind::CFileInfo &newFileInfo) const
  {
    return IsVirtual || CTempFileInfo::WasChanged(newFileInfo);
  }

};

UString GetFolderPath(IFolderFolder *folder)
{
  NWindows::NCOM::CPropVariant prop;
  if (folder->GetFolderProperty(kpidPath, &prop) == S_OK)
    if (prop.vt == VT_BSTR)
      return (wchar_t *)prop.bstrVal;
  return UString();
}

class Utils7zip
{


    //
    CObjectVector<CFolderLink> _parentFolders;
//    NWindows::NDLL::CLibrary _library;
    CMyComPtr<IFolderFolder> _folder;

public:
    Utils7zip();

    void CloseOpenFolders();
    HRESULT BindToPath(const UString &fullPath, const UString &arcFormat, bool &archiveIsOpened, bool &encrypted);

    HRESULT OpenParentArchiveFolder();

    void SetToRootFolder();

    void LoadFullPath();

    int LoadItems();

    int GetNumberOfItems();

    UString GetItemName(int ind);

    UString _currentFolderPrefix;
};

Utils7zip::Utils7zip()
{

}


void Utils7zip::SetToRootFolder()
{
  _folder.Release();
  // _library.Free();
  CRootFolder *rootFolderSpec = new CRootFolder;
  _folder = rootFolderSpec;
  rootFolderSpec->Init();
}

void Utils7zip::CloseOpenFolders()
{
  while (_parentFolders.Size() > 0)
  {
    _folder.Release();
    // FIXME _library.Free();
    _folder = _parentFolders.Back().ParentFolder;
    // FIXME _library.Attach(_parentFolders.Back().Library.Detach());
    if (_parentFolders.Size() > 1)
      OpenParentArchiveFolder();
    _parentFolders.DeleteBack();
  }
  // _flatMode = _flatModeForDisk;
  _folder.Release();
  // _library.Free();
}


HRESULT Utils7zip::BindToPath(const UString &fullPath, const UString &arcFormat, bool &archiveIsOpened, bool &encrypted)
{
  archiveIsOpened = false;
  encrypted = false;
  // CDisableTimerProcessing disableTimerProcessing1(*this);

  printf("CPanel::BindToPath(%ls)\n",(const wchar_t *)fullPath);

  if (_parentFolders.Size() > 0)
  {
    const UString &virtPath = _parentFolders.Back().VirtualPath;
    if (fullPath.Left(virtPath.Len()) == virtPath)
    {
      for (;;)
      {
        CMyComPtr<IFolderFolder> newFolder;
        HRESULT res = _folder->BindToParentFolder(&newFolder);
        if (!newFolder || res != S_OK)
          break;
        _folder = newFolder;
      }
      UStringVector parts;
      SplitPathToParts(fullPath.Ptr(virtPath.Len()), parts);
      for (int i = 0; i < parts.Size(); i++)
      {
        const UString &s = parts[i];
        if ((i == 0 || i == parts.Size() - 1) && s.IsEmpty())
          continue;
        CMyComPtr<IFolderFolder> newFolder;
        HRESULT res = _folder->BindToFolder(s, &newFolder);
        if (!newFolder || res != S_OK)
          break;
        _folder = newFolder;
      }
      return S_OK;
    }
  }

  CloseOpenFolders();
  UString sysPath = fullPath;
  CFileInfo fileInfo;
  UStringVector reducedParts;
  while (!sysPath.IsEmpty())
  {
    if (fileInfo.Find(us2fs(sysPath)))
      break;
    int pos = sysPath.ReverseFind(WCHAR_PATH_SEPARATOR);
    if (pos < 0)
      sysPath.Empty();
    else
    {
      if (reducedParts.Size() > 0 || pos < sysPath.Len() - 1)
        reducedParts.Add(sysPath.Ptr(pos + 1));
      sysPath = sysPath.Left(pos);
    }
  }
  SetToRootFolder();
  CMyComPtr<IFolderFolder> newFolder;
  if (sysPath.IsEmpty())
  {
    if (_folder->BindToFolder(fullPath, &newFolder) == S_OK)
      _folder = newFolder;
  }
  else if (fileInfo.IsDir())
  {
    NName::NormalizeDirPathPrefix(sysPath);
    if (_folder->BindToFolder(sysPath, &newFolder) == S_OK)
      _folder = newFolder;
  }
  else
  {
    FString dirPrefix, fileName;
    NDir::GetFullPathAndSplit(us2fs(sysPath), dirPrefix, fileName);
    if (_folder->BindToFolder(fs2us(dirPrefix), &newFolder) == S_OK)
    {
      _folder = newFolder;
      LoadFullPath();
      {
        HRESULT res = S_FALSE; // FIXME OpenItemAsArchive(fs2us(fileName), arcFormat, encrypted);
        if (res != S_FALSE)
        {
          RINOK(res);
        }
        /*
        if (res == E_ABORT)
          return res;
        */
        if (res == S_OK)
        {
          archiveIsOpened = true;
          for (int i = reducedParts.Size() - 1; i >= 0; i--)
          {
            CMyComPtr<IFolderFolder> newFolder;
            _folder->BindToFolder(reducedParts[i], &newFolder);
            if (!newFolder)
              break;
            _folder = newFolder;
          }
        }
      }
    }
  }
  return S_OK;
}

void Utils7zip::LoadFullPath()
{
  _currentFolderPrefix.Empty();
  for (int i = 0; i < _parentFolders.Size(); i++)
  {
    const CFolderLink &folderLink = _parentFolders[i];
    _currentFolderPrefix += GetFolderPath(folderLink.ParentFolder);
    _currentFolderPrefix += folderLink.RelPath;
    _currentFolderPrefix += WCHAR_PATH_SEPARATOR;
  }
  if (_folder)
    _currentFolderPrefix += GetFolderPath(_folder);
}

HRESULT Utils7zip::OpenParentArchiveFolder()
{
  // CDisableTimerProcessing disableTimerProcessing1(*this);
  if (_parentFolders.Size() < 2)
    return S_OK;
  const CFolderLink &folderLinkPrev = _parentFolders[_parentFolders.Size() - 2];
  const CFolderLink &folderLink = _parentFolders.Back();
  NFind::CFileInfo newFileInfo;
  if (newFileInfo.Find(folderLink.FilePath))
  {
/* FIXME
    if (folderLink.WasChanged(newFileInfo))
    {
      UString message = MyFormatNew(IDS_WANT_UPDATE_MODIFIED_FILE, 0x03020280, folderLink.RelPath);
      if (::MessageBoxW(HWND(*this), message, L"7-Zip", MB_OKCANCEL | MB_ICONQUESTION) == IDOK)
      {
        if (OnOpenItemChanged(folderLink.FileIndex, folderLink.FilePath,
            folderLinkPrev.UsePassword, folderLinkPrev.Password) != S_OK)
        {
          ::MessageBoxW(HWND(*this), MyFormatNew(IDS_CANNOT_UPDATE_FILE,
              0x03020281, fs2us(folderLink.FilePath)), L"7-Zip", MB_OK | MB_ICONSTOP);
          return S_OK;
        }
      }
    }
*/
  }
  folderLink.DeleteDirAndFile();
  return S_OK;
}

int Utils7zip::LoadItems()
{
    return _folder->LoadItems();
}

int Utils7zip::GetNumberOfItems()
{
    UInt32 numItems;
    _folder->GetNumberOfItems(&numItems);

    return numItems;
}

UString Utils7zip::GetItemName(int itemIndex)
{
    NCOM::CPropVariant prop;
    if (_folder->GetProperty(itemIndex, kpidName, &prop) != S_OK)
      throw 2723400;
    if (prop.vt != VT_BSTR)
      throw 2723401;
    return prop.bstrVal;
}


static Utils7zip * gbl_utils7zip;

extern int global_use_utf16_conversion;

int  utilZip_init(void)
{
    // FIXME
    global_use_utf16_conversion = 1;

    // set the program's current locale from the user's environment variables
    // setlocale(LC_ALL,"");

    gbl_utils7zip = new Utils7zip;

    return 0;
}

int utilZip_setPathSuite(const wchar_t *path)
{
    bool archiveIsOpened, encrypted;
    RINOK(gbl_utils7zip->BindToPath(path, UString(), archiveIsOpened, encrypted));
/*
    CMyComPtr<IFolderSetFlatMode> folderSetFlatMode;
    _folder.QueryInterface(IID_IFolderSetFlatMode, &folderSetFlatMode);
    if (folderSetFlatMode)
      folderSetFlatMode->SetFlatMode(BoolToInt(_flatMode));
*/
    RINOK(gbl_utils7zip->LoadItems());
//    RINOK(InitColumns());

    // OutputDebugString(TEXT("Start Dir\n"));
    /*
    UInt32 numItems;
    _folder->GetNumberOfItems(&numItems);

    for(UInt32 itemIndex=0; itemIndex < ; itemIndex++)
    {
        NCOM::CPropVariant prop;
        if (_folder->GetProperty(itemIndex, kpidName, &prop) != S_OK)
          throw 2723400;
        if (prop.vt != VT_BSTR)
          throw 2723401;
        UString name = prop.bstrVal;
    }
    */
}

int utilZip_setPath(const wchar_t *path)
{
    return utilZip_setPathSuite(path);
}

int utilZip_GetNumberOfItems()
{
    return gbl_utils7zip->GetNumberOfItems();
}


UString utilZip_GetItemName(int ind)
{
    return gbl_utils7zip->GetItemName(ind);
}


// FIXME
UString LangString(UINT resourceID, UInt32 langID)
{
    return UString(); // FIXME
}

DWORD_PTR GetRealIconIndex(LPCTSTR path, DWORD attrib, int &iconIndex)
{
    // FIXME
  iconIndex = -1;
  return -1;
}

namespace NFsFolder {

        // FIXME
        bool wxw_CopyFile(LPCWSTR existingFile, LPCWSTR newFile, bool overwrite)
        {
                return false; // FIXME wxCopyFile(wxString(existingFile), wxString(newFile), overwrite);
        }
}

int CompareFileNames_ForFolderList(const wchar_t *s1, const wchar_t *s2)
{
  for (;;)
  {
    wchar_t c1 = *s1;
    wchar_t c2 = *s2;
    if ((c1 >= '0' && c1 <= '9') &&
        (c2 >= '0' && c2 <= '9'))
    {
      for (; *s1 == '0'; s1++);
      for (; *s2 == '0'; s2++);
      size_t len1 = 0;
      size_t len2 = 0;
      for (; (s1[len1] >= '0' && s1[len1] <= '9'); len1++);
      for (; (s2[len2] >= '0' && s2[len2] <= '9'); len2++);
      if (len1 < len2) return -1;
      if (len1 > len2) return 1;
      for (; len1 > 0; s1++, s2++, len1--)
      {
        if (*s1 == *s2) continue;
        return (*s1 < *s2) ? -1 : 1;
      }
      c1 = *s1;
      c2 = *s2;
    }
    s1++;
    s2++;
    if (c1 != c2)
    {
      // Probably we need to change the order for special characters like in Explorer.
      wchar_t u1 = MyCharUpper(c1);
      wchar_t u2 = MyCharUpper(c2);
      if (u1 < u2) return -1;
      if (u1 > u2) return 1;
    }
    if (c1 == 0) return 0;
  }
}


UString LangString(UInt32 langID)
{
    /* FIXME
  const wchar_t *s = g_Lang.Get(langID);
  if (s)
    return s;
    */
  return L"FIXME"; // MyLoadString(langID);
}


