// PanelItemOpen.cpp

#include "StdAfx.h"

// For compilers that support precompilation, includes "wx/wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif
#include "wx/mimetype.h"

#undef _WIN32

// #include <tlhelp32.h>

#include "../../../Common/AutoPtr.h"
#include "../../../Common/StringConvert.h"

// #include "../../../Windows/ProcessUtils.h"
#include "../../../Windows/FileName.h"
#include "../../../Windows/PropVariant.h"
#include "../../../Windows/PropVariantConv.h"

#include "../../Common/FileStreams.h"
#include "../../Common/StreamObjects.h"

#include "../Common/ExtractingFilePath.h"

#include "App.h"

#include "FileFolderPluginOpen.h"
#include "FormatUtils.h"
#include "LangUtils.h"
#include "RegistryUtils.h"
#include "UpdateCallback100.h"

#include "resource.h"

using namespace NWindows;
using namespace NSynchronization;
using namespace NFile;
using namespace NDir;

extern UInt64 g_RAM_Size;

#ifndef _UNICODE
extern bool g_IsNT;
#endif

static CFSTR kTempDirPrefix = FTEXT("7zO");

#if 0 // ifndef UNDER_CE

class CProcessSnapshot
{
  HANDLE _handle;
public:
  CProcessSnapshot(): _handle(INVALID_HANDLE_VALUE) {};
  ~CProcessSnapshot() { Close(); }

  bool Close()
  {
    if (_handle == INVALID_HANDLE_VALUE)
      return true;
    if (!::CloseHandle(_handle))
      return false;
    _handle = INVALID_HANDLE_VALUE;
    return true;
  }

  bool Create()
  {
    _handle = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    return (_handle != INVALID_HANDLE_VALUE);
  }

  bool GetFirstProcess(PROCESSENTRY32 *pe) { return BOOLToBool(Process32First(_handle, pe)); }
  bool GetNextProcess(PROCESSENTRY32 *pe) { return BOOLToBool(Process32Next(_handle, pe)); }
};

typedef DWORD (WINAPI *GetProcessIdFunc)(HANDLE process);

class CChildProcesses
{
  #ifndef UNDER_CE
  CRecordVector<DWORD> _ids;
  #endif
public:
  CRecordVector<HANDLE> Handles;
  CRecordVector<bool> NeedWait;

  ~CChildProcesses() { CloseAll(); }
  void DisableWait(int index) { NeedWait[index] = false; }
  
  void CloseAll()
  {
    FOR_VECTOR (i,  Handles)
    {
      HANDLE h = Handles[i];
      if (h != NULL)
        CloseHandle(h);
    }
    Handles.Clear();
    NeedWait.Clear();
  }

  void AddProcess(HANDLE h)
  {
    #ifndef UNDER_CE
    GetProcessIdFunc func = (GetProcessIdFunc)::GetProcAddress(::GetModuleHandleA("kernel32.dll"), "GetProcessId");
    if (func)
      _ids.AddToUniqueSorted(func(h));
    #endif
    Handles.Add(h);
    NeedWait.Add(true);
  }

  void Update()
  {
    #ifndef UNDER_CE
    CRecordVector<DWORD> ids, parents;
    {
      CProcessSnapshot snapshot;
      if (snapshot.Create())
      {
        PROCESSENTRY32 pe;
        memset(&pe, 0, sizeof(pe));
        pe.dwSize = sizeof(pe);
        BOOL res = snapshot.GetFirstProcess(&pe);
        while (res)
        {
          ids.Add(pe.th32ProcessID);
          parents.Add(pe.th32ParentProcessID);
          res = snapshot.GetNextProcess(&pe);
        }
      }
    }

    for (;;)
    {
      unsigned i;
      for (i = 0; i < ids.Size(); i++)
      {
        DWORD id = ids[i];
        if (_ids.FindInSorted(parents[i]) >= 0 &&
            _ids.FindInSorted(id) < 0)
        {
          HANDLE hProcess = OpenProcess(SYNCHRONIZE, FALSE, id);
          if (hProcess)
          {
            _ids.AddToUniqueSorted(id);
            Handles.Add(hProcess);
            NeedWait.Add(true);
            break;
          }
        }
      }
      if (i == ids.Size())
        break;
    }
    #endif
  }
};
#endif

struct CTmpProcessInfo: public CTempFileInfo
{
  HANDLE ProcessHandle; // CChildProcesses Processes;
  HWND Window;
  UString FullPathFolderPrefix;
  bool UsePassword;
  UString Password;

  CTmpProcessInfo(): UsePassword(false) {}
};

class CTmpProcessInfoRelease
{
  CTmpProcessInfo *_tmpProcessInfo;
public:
  bool _needDelete;
  CTmpProcessInfoRelease(CTmpProcessInfo &tpi):
      _tmpProcessInfo(&tpi), _needDelete(true) {}
  ~CTmpProcessInfoRelease()
  {
    if (_needDelete)
      _tmpProcessInfo->DeleteDirAndFile();
  }
};

HRESULT CPanel::OpenItemAsArchive(IInStream *inStream,
    const CTempFileInfo &tempFileInfo,
    const UString &virtualFilePath,
    const UString &arcFormat,
    bool &encrypted)
{
  encrypted = false;
  CFolderLink folderLink;
  (CTempFileInfo &)folderLink = tempFileInfo;
  if (inStream)
    folderLink.IsVirtual = true;
  else
  {
    if (!folderLink.FileInfo.Find(folderLink.FilePath))
      return ::GetLastError();
    if (folderLink.FileInfo.IsDir())
      return S_FALSE;
    folderLink.IsVirtual = false;
  }

  folderLink.VirtualPath = virtualFilePath;

  CMyComPtr<IFolderFolder> newFolder;

  // _passwordIsDefined = false;
  // _password.Empty();

  NDLL::CLibrary library;

  UString password;
  RINOK(OpenFileFolderPlugin(inStream,
      folderLink.FilePath.IsEmpty() ? us2fs(virtualFilePath) : folderLink.FilePath,
      arcFormat,
      &library, &newFolder, GetParent(), encrypted, password));
 
  folderLink.Password = password;
  folderLink.UsePassword = encrypted;

  if (_folder)
    folderLink.ParentFolderPath = GetFolderPath(_folder);
  else
    folderLink.ParentFolderPath = _currentFolderPrefix;
  if (!_parentFolders.IsEmpty())
    folderLink.ParentFolder = _folder;

  _parentFolders.Add(folderLink);
  _parentFolders.Back().Library.Attach(_library.Detach());

  ReleaseFolder();
  _library.Free();
  SetNewFolder(newFolder);
  _library.Attach(library.Detach());

  _flatMode = _flatModeForArc;

  CMyComPtr<IGetFolderArcProps> getFolderArcProps;
  _folder.QueryInterface(IID_IGetFolderArcProps, &getFolderArcProps);
  _thereAreDeletedItems = false;
  if (getFolderArcProps)
  {
    CMyComPtr<IFolderArcProps> arcProps;
    getFolderArcProps->GetFolderArcProps(&arcProps);
    if (arcProps)
    {
      /*
      UString s;
      UInt32 numLevels;
      if (arcProps->GetArcNumLevels(&numLevels) != S_OK)
        numLevels = 0;
      for (UInt32 level2 = 0; level2 <= numLevels; level2++)
      {
        UInt32 level = numLevels - level2;
        PROPID propIDs[] = { kpidError, kpidPath, kpidType, kpidErrorType } ;
        UString values[4];
        for (Int32 i = 0; i < 4; i++)
        {
          CMyComBSTR name;
          NCOM::CPropVariant prop;
          if (arcProps->GetArcProp(level, propIDs[i], &prop) != S_OK)
            continue;
          if (prop.vt != VT_EMPTY)
            values[i] = (prop.vt == VT_BSTR) ? prop.bstrVal : L"?";
        }
        UString s2;
        if (!values[3].IsEmpty())
        {
          s2 = L"Can not open the file as [" + values[3] + L"] archive";
          if (level2 != 0)
            s2 += L"\nThe file is open as [" + values[2] + L"] archive";
        }
        if (!values[0].IsEmpty())
        {
          if (!s2.IsEmpty())
            s2.Add_LF();
          s2 += L"[";
          s2 += values[2];
          s2 += L"] Error: ";
          s2 += values[0];
        }
        if (!s2.IsEmpty())
        {
          if (!s.IsEmpty())
            s += L"--------------------\n";
          s += values[1];
          s.Add_LF();
          s += s2;
        }
      }
      */
      /*
      if (!s.IsEmpty())
        MessageBoxWarning(s);
      else
      */
      // after MessageBoxWarning it throws exception in nested archives in Debug Mode. why ?.
        // MessageBoxWarning(L"test error");
    }
  }

  return S_OK;
}

HRESULT CPanel::OpenItemAsArchive(const UString &relPath, const UString &arcFormat, bool &encrypted)
{
  CTempFileInfo tfi;
  tfi.RelPath = relPath;
  tfi.FolderPath = us2fs(GetFsPath());
  const UString fullPath = GetFsPath() + relPath;
  tfi.FilePath = us2fs(fullPath);
  return OpenItemAsArchive(NULL, tfi, fullPath, arcFormat, encrypted);
}

HRESULT CPanel::OpenItemAsArchive(int index, const wchar_t *type)
{
  CDisableTimerProcessing disableTimerProcessing1(*this);
  CDisableNotify disableNotify(*this);
  bool encrypted;
  HRESULT res = OpenItemAsArchive(GetItemRelPath2(index), type ? type : L"", encrypted);
  if (res != S_OK)
  {
    RefreshTitle(true); // in case of error we must refresh changed title of 7zFM
    return res;
  }
  RefreshListCtrl();
  return S_OK;
}

HRESULT CPanel::OpenParentArchiveFolder()
{
  CDisableTimerProcessing disableTimerProcessing(*this);
  CDisableNotify disableNotify(*this);
  if (_parentFolders.Size() < 2)
    return S_OK;
  const CFolderLink &folderLinkPrev = _parentFolders[_parentFolders.Size() - 2];
  const CFolderLink &folderLink = _parentFolders.Back();
  NFind::CFileInfo newFileInfo;
  if (newFileInfo.Find(folderLink.FilePath))
  {
    if (folderLink.WasChanged(newFileInfo))
    {
      UString message = MyFormatNew(IDS_WANT_UPDATE_MODIFIED_FILE, folderLink.RelPath);
      if (::MessageBoxW((HWND)*this, message, L"7-Zip", MB_OKCANCEL | MB_ICONQUESTION) == IDOK)
      {
        if (OnOpenItemChanged(folderLink.FileIndex, fs2us(folderLink.FilePath),
            folderLinkPrev.UsePassword, folderLinkPrev.Password) != S_OK)
        {
          ::MessageBoxW((HWND)*this, MyFormatNew(IDS_CANNOT_UPDATE_FILE,
              fs2us(folderLink.FilePath)), L"7-Zip", MB_OK | MB_ICONSTOP);
          return S_OK;
        }
      }
    }
  }
  folderLink.DeleteDirAndFile();
  return S_OK;
}

static const char *kStartExtensions =
  #ifdef UNDER_CE
  " cab"
  #endif
  " exe bat com"
  " chm"
  " msi doc xls ppt pps wps wpt wks xlr wdb vsd pub"

  " docx docm dotx dotm xlsx xlsm xltx xltm xlsb xps"
  " xlam pptx pptm potx potm ppam ppsx ppsm xsn"
  " mpp"
  " msg"
  " dwf"

  " flv swf"
  
  " odt ods"
  " wb3"
  " pdf"
  " ";

static bool FindExt(const char *p, const UString &name)
{
  int dotPos = name.ReverseFind_Dot();
  if (dotPos < 0 || dotPos == (int)name.Len() - 1)
    return false;

  AString s;
  for (unsigned pos = dotPos + 1;; pos++)
  {
    wchar_t c = name[pos];
    if (c == 0)
      break;
    if (c >= 0x80)
      return false;
    s += (char)MyCharLower_Ascii((char)c);
  }
  for (unsigned i = 0; p[i] != 0;)
  {
    unsigned j;
    for (j = i; p[j] != ' '; j++);
    if (s.Len() == j - i && memcmp(p + i, (const char *)s, s.Len()) == 0)
      return true;
    i = j + 1;
  }
  return false;
}

static bool DoItemAlwaysStart(const UString &name)
{
  return FindExt(kStartExtensions, name);
}

UString GetQuotedString(const UString &s);

static void StartEditApplication(const UString &path, bool useEditor, HWND window /* , CProcess &process */ )
{
  UString command;
  ReadRegEditor(useEditor, command);
  if (command.IsEmpty())
  {
#ifdef _WIN32
    #ifdef UNDER_CE
    command = L"\\Windows\\";
    #else
    FString winDir;
    if (!GetWindowsDir(winDir))
      return 0;
    NName::NormalizeDirPathPrefix(winDir);
    command = fs2us(winDir);
    #endif
    command += L"notepad.exe";
#else
    command += L"vi";
#endif
  }

#ifdef _WIN32
  HRESULT res = process.Create(command, GetQuotedString(path), NULL);
  if (res != SZ_OK)
    ::MessageBoxW(window, LangString(IDS_CANNOT_START_EDITOR), L"7-Zip", MB_OK  | MB_ICONSTOP);
  return res;
#else
  wxString cmd = (const wchar_t *)command;
  long pid = wxExecute(cmd, wxEXEC_ASYNC);
  if (pid) return ;
  ::MessageBoxW(window, LangString(IDS_CANNOT_START_EDITOR), L"7-Zip", MB_OK  | MB_ICONSTOP);
#endif
}

void CApp::DiffFiles()
{
  const CPanel &panel = GetFocusedPanel();
  
  CRecordVector<UInt32> indices;
  panel.GetSelectedItemsIndices(indices);

  UString path1, path2;
  if (indices.Size() == 2)
  {
    path1 = panel.GetItemFullPath(indices[0]);
    path2 = panel.GetItemFullPath(indices[1]);
  }
  else if (indices.Size() == 1 && NumPanels >= 2)
  {
    const CPanel &destPanel = Panels[1 - LastFocusedPanel];
    path1 = panel.GetItemFullPath(indices[0]);
    CRecordVector<UInt32> indices2;
    destPanel.GetSelectedItemsIndices(indices2);
    if (indices2.Size() == 1)
      path2 = destPanel.GetItemFullPath(indices2[0]);
    else
    {
      UString relPath = panel.GetItemRelPath2(indices[0]);
      if (panel._flatMode && !destPanel._flatMode)
        relPath = panel.GetItemName(indices[0]);
      path2 = destPanel._currentFolderPrefix + relPath;
    }
  }
  else
    return;

  UString command;
  ReadRegDiff(command);
  if (command.IsEmpty())
    return;

  UString param = GetQuotedString(path1);
  param.Add_Space();
  param += GetQuotedString(path2);

#ifdef _WIN32
  HRESULT res = MyCreateProcess(command, param);
  if (res == SZ_OK)
    return;
#else
  wxString cmd = (const wchar_t *)command;
  cmd += L" ";
  cmd += (const wchar_t *)param;

  long pid = wxExecute(cmd, wxEXEC_ASYNC);
  if (pid) return ;
#endif
  ::MessageBoxW(_window, LangString(IDS_CANNOT_START_EDITOR), L"7-Zip", MB_OK  | MB_ICONSTOP);
}

#ifndef _UNICODE
typedef BOOL (WINAPI * ShellExecuteExWP)(LPSHELLEXECUTEINFOW lpExecInfo);
#endif

static void StartApplication(const UString &dir, const UString &path, HWND window /* , CProcess &process */ )
{
  // FIXME
  extern const TCHAR * nameWindowToUnix(const TCHAR * lpFileName);
  UString tmpPath = path;

  wxString filename(nameWindowToUnix(tmpPath));


  wxString ext = filename.AfterLast(_T('.'));

  printf("StartApplication(%ls) ext='%ls'\n",(const wchar_t *)filename,(const wchar_t *)ext);

  if ( ! ext.empty() )
  {
    wxFileType *ft = wxTheMimeTypesManager->GetFileTypeFromExtension(ext);
    // printf("StartApplication(%ls) ft=%p\n",(const wchar_t *)filename,ft);
    if (ft)
    {
      wxString cmd;
      // wxString type; ft->GetMimeType(&type);
      wxFileType::MessageParameters params(filename); // , type);
      bool ok = ft->GetOpenCommand(&cmd, params);
      // printf("StartApplication(%ls) ok=%d\n",(const wchar_t *)filename,(int)ok);
      delete ft;
      if ( ok )
      {
        printf("StartApplication(%ls) cmd='%ls'\n",(const wchar_t *)filename,(const wchar_t *)cmd);
        long pid = wxExecute(cmd, wxEXEC_ASYNC);
        if (pid) return ;
      }
    }	   
  }
  ::MessageBoxW(window, 
          // NError::MyFormatMessageW(::GetLastError()),
          L"There is no application associated with the given file name extension",
          L"7-Zip", MB_OK | MB_ICONSTOP);

}

static void StartApplicationDontWait(const UString &dir, const UString &path, HWND window)
{
   // CProcess process;
  StartApplication(dir, path, window /* , process */ );
}

void CPanel::EditItem(int index, bool useEditor)
{
  if (!_parentFolders.IsEmpty())
  {
    OpenItemInArchive(index, false, true, true, useEditor);
    return;
  }
   // CProcess process;
  StartEditApplication(GetItemFullPath(index), useEditor, (HWND)*this /* , process */ );
}

void CPanel::OpenFolderExternal(int index)
{
  UString fsPrefix = GetFsPath();
  UString name;
  if (index == kParentIndex)
  {
    int pos = fsPrefix.ReverseFind_PathSepar();
    if (pos >= 0 && pos == (int)fsPrefix.Len() - 1)
    {
      UString s = fsPrefix.Left(pos);
      pos = s.ReverseFind_PathSepar();
      if (pos >= 0)
        fsPrefix.SetFrom(s, pos + 1);
    }
    name = fsPrefix;
  }
  else
    name = fsPrefix + GetItemRelPath(index) + WCHAR_PATH_SEPARATOR;
  StartApplicationDontWait(fsPrefix, name, (HWND)*this);
}

bool CPanel::IsVirus_Message(const UString &name)
{
  UString name2;

  const wchar_t cRLO = (wchar_t)0x202E;
  bool isVirus = false;
  bool isSpaceError = false;
  name2 = name;
  
  if (name2.Find(cRLO) >= 0)
  {
    UString badString = cRLO;
    name2.Replace(badString, L"[RLO]");
    isVirus = true;
  }
  {
    const wchar_t *kVirusSpaces = L"     ";
    // const unsigned kNumSpaces = strlen(kVirusSpaces);
    for (;;)
    {
      int pos = name2.Find(kVirusSpaces);
      if (pos < 0)
        break;
      isVirus = true;
      isSpaceError = true;
      name2.Replace(kVirusSpaces, L" ");
    }
  }
  
  if (!isVirus)
    return false;

  UString s = LangString(IDS_VIRUS);
  
  if (!isSpaceError)
  {
    int pos1 = s.Find(L'(');
    if (pos1 >= 0)
    {
      int pos2 = s.Find(L')', pos1 + 1);
      if (pos2 >= 0)
      {
        s.Delete(pos1, pos2 + 1 - pos1);
        if (pos1 > 0 && s[pos1 - 1] == ' ' && s[pos1] == '.')
          s.Delete(pos1 - 1);
      }
    }
  }

  UString name3 = name;
  name3.Replace(L'\n', L'_');
  name2.Replace(L'\n', L'_');

  s.Add_LF(); s += name2;
  s.Add_LF(); s += name3;

  MessageBoxMyError(s);
  return true;
}

void CPanel::OpenItem(int index, bool tryInternal, bool tryExternal, const wchar_t *type)
{
  CDisableTimerProcessing disableTimerProcessing(*this);
  UString name = GetItemRelPath2(index);
  
  if (IsVirus_Message(name))
    return;

  if (!_parentFolders.IsEmpty())
  {
    OpenItemInArchive(index, tryInternal, tryExternal, false, false, type);
    return;
  }

  CDisableNotify disableNotify(*this);
  UString prefix = GetFsPath();
  UString fullPath = prefix + name;

  if (tryInternal)
    if (!tryExternal || !DoItemAlwaysStart(name))
    {
      HRESULT res = OpenItemAsArchive(index, type);
      disableNotify.Restore(); // we must restore to allow text notification update
      InvalidateList();
      if (res == S_OK || res == E_ABORT)
        return;
      if (res != S_FALSE)
      {
        MessageBoxError(res);
        return;
      }
    }
  
  if (tryExternal)
  {
    // SetCurrentDirectory opens HANDLE to folder!!!
    // NDirectory::MySetCurrentDirectory(prefix);
    StartApplicationDontWait(prefix, fullPath, (HWND)*this);
  }
}

class CThreadCopyFrom: public CProgressThreadVirt
{
  HRESULT ProcessVirt();
public:
  UString FullPath;
  UInt32 ItemIndex;

  CMyComPtr<IFolderOperations> FolderOperations;
  CMyComPtr<IProgress> UpdateCallback;
  CUpdateCallback100Imp *UpdateCallbackSpec;
};
  
HRESULT CThreadCopyFrom::ProcessVirt()
{
  return FolderOperations->CopyFromFile(ItemIndex, FullPath, UpdateCallback);
}
      
HRESULT CPanel::OnOpenItemChanged(UInt32 index, const wchar_t *fullFilePath,
    bool usePassword, const UString &password)
{
  if (!_folderOperations)
  {
    MessageBoxErrorLang(IDS_OPERATION_IS_NOT_SUPPORTED);
    return E_FAIL;
  }

  CThreadCopyFrom t;
  t.UpdateCallbackSpec = new CUpdateCallback100Imp;
  t.UpdateCallback = t.UpdateCallbackSpec;
  t.UpdateCallbackSpec->ProgressDialog = &t.ProgressDialog;
  t.ItemIndex = index;
  t.FullPath = fullFilePath;
  t.FolderOperations = _folderOperations;

  t.UpdateCallbackSpec->Init();
  t.UpdateCallbackSpec->PasswordIsDefined = usePassword;
  t.UpdateCallbackSpec->Password = password;


  RINOK(t.Create(GetItemName(index), (HWND)*this));
  return t.Result;
}

LRESULT CPanel::OnOpenItemChanged(LPARAM lParam)
{
  CTmpProcessInfo &tpi = *(CTmpProcessInfo *)lParam;
  if (tpi.FullPathFolderPrefix != _currentFolderPrefix)
    return 0;
  UInt32 fileIndex = tpi.FileIndex;
  UInt32 numItems;
  _folder->GetNumberOfItems(&numItems);
  
  // This code is not 100% OK for cases when there are several files with
  // tpi.RelPath name and there are changes in archive before update.
  // So tpi.FileIndex can point to another file.
 
  if (fileIndex >= numItems || GetItemRelPath(fileIndex) != tpi.RelPath)
  {
    UInt32 i;
    for (i = 0; i < numItems; i++)
      if (GetItemRelPath(i) == tpi.RelPath)
        break;
    if (i == numItems)
      return 0;
    fileIndex = i;
  }

  CSelectedState state;
  SaveSelectedState(state);

  CDisableNotify disableNotify(*this); // do we need it??

  HRESULT result = OnOpenItemChanged(fileIndex, fs2us(tpi.FilePath), tpi.UsePassword, tpi.Password);
  RefreshListCtrl(state);
  if (result != S_OK)
    return 0;
  return 1;
}

class CExitEventLauncher
{
public:
  NWindows::NSynchronization::CManualResetEvent _exitEvent;
  CExitEventLauncher()
  {
    if (_exitEvent.Create(false) != S_OK)
      throw 9387173;
  };
  ~CExitEventLauncher() {  _exitEvent.Set(); }
} g_ExitEventLauncher;

#ifdef _WIN32
static THREAD_FUNC_DECL MyThreadFunction(void *param)
{
  CMyAutoPtr<CTmpProcessInfo> tmpProcessInfoPtr((CTmpProcessInfo *)param);
  CTmpProcessInfo *tpi = tmpProcessInfoPtr.get();
  CChildProcesses &processes = tpi->Processes;

  for (;;)
  {
    CRecordVector<HANDLE> handles;
    CRecordVector<int> indices;
    
    FOR_VECTOR (i, processes.Handles)
    {
      if (processes.NeedWait[i])
      {
        handles.Add(processes.Handles[i]);
        indices.Add(i);
      }
    }
    
    if (handles.IsEmpty())
      break;

    handles.Add(g_ExitEventLauncher._exitEvent);

    DWORD waitResult = ::WaitForMultipleObjects(handles.Size(), &handles.Front(), FALSE, INFINITE);

    if (waitResult >= (DWORD)handles.Size() - 1)
    {
      processes.CloseAll();
      return waitResult >= (DWORD)handles.Size() ? 1 : 0;
    }
    processes.Update();
    processes.DisableWait(indices[waitResult]);
  }

  NFind::CFileInfo newFileInfo;
  if (newFileInfo.Find(tpi->FilePath))
  {
    if (tpi->WasChanged(newFileInfo))
    {
      UString message = MyFormatNew(IDS_WANT_UPDATE_MODIFIED_FILE, tpi->RelPath);
      if (::MessageBoxW(g_HWND, message, L"7-Zip", MB_OKCANCEL | MB_ICONQUESTION) == IDOK)
      {
        if (SendMessage(tpi->Window, kOpenItemChanged, 0, (LONG_PTR)tpi) != 1)
        {
          ::MessageBoxW(g_HWND, MyFormatNew(IDS_CANNOT_UPDATE_FILE,
              fs2us(tpi->FilePath)), L"7-Zip", MB_OK | MB_ICONSTOP);
          return 0;
        }
      }
    }
  }
  tpi->DeleteDirAndFile();
  return 0;
}
#endif

#if defined(_WIN32) && !defined(UNDER_CE)
static const FChar *k_ZoneId_StreamName = FTEXT(":Zone.Identifier");
#endif


#ifndef UNDER_CE

static void ReadZoneFile(CFSTR fileName, CByteBuffer &buf)
{
  buf.Free();
  NIO::CInFile file;
  if (!file.Open(fileName))
    return;
  UInt64 fileSize;
  if (!file.GetLength(fileSize))
    return;
  if (fileSize == 0 || fileSize >= ((UInt32)1 << 20))
    return;
  buf.Alloc((size_t)fileSize);
  UInt32 processed;
  if (file.Read(buf, (UInt32)fileSize, processed) && processed == fileSize)
    return;
  buf.Free();
}

static bool WriteZoneFile(CFSTR fileName, const CByteBuffer &buf)
{
  NIO::COutFile file;
  if (!file.Create(fileName, true))
    return false;
  UInt32 processed;
  if (!file.Write(buf, (UInt32)buf.Size(), processed))
    return false;
  return processed == buf.Size();
}

#endif

/*
class CBufSeqOutStream_WithFile:
  public ISequentialOutStream,
  public CMyUnknownImp
{
  Byte *_buffer;
  size_t _size;
  size_t _pos;


  size_t _fileWritePos;
  bool fileMode;
public:

  bool IsStreamInMem() const { return !fileMode; }
  size_t GetMemStreamWrittenSize() const { return _pos; }

  // ISequentialOutStream *FileStream;
  FString FilePath;
  COutFileStream *outFileStreamSpec;
  CMyComPtr<ISequentialOutStream> outFileStream;

  CBufSeqOutStream_WithFile(): outFileStreamSpec(NULL) {}

  void Init(Byte *buffer, size_t size)
  {
    fileMode = false;
    _buffer = buffer;
    _pos = 0;
    _size = size;
    _fileWritePos = 0;
  }

  HRESULT FlushToFile();
  size_t GetPos() const { return _pos; }

  MY_UNKNOWN_IMP
  STDMETHOD(Write)(const void *data, UInt32 size, UInt32 *processedSize);
};

static const UInt32 kBlockSize = ((UInt32)1 << 31);

STDMETHODIMP CBufSeqOutStream_WithFile::Write(const void *data, UInt32 size, UInt32 *processedSize)
{
  if (processedSize)
    *processedSize = 0;
  if (!fileMode)
  {
    if (_size - _pos >= size)
    {
      if (size != 0)
      {
        memcpy(_buffer + _pos, data, size);
        _pos += size;
      }
      if (processedSize)
        *processedSize = (UInt32)size;
      return S_OK;
    }

    fileMode = true;
  }
  RINOK(FlushToFile());
  return outFileStream->Write(data, size, processedSize);
}

HRESULT CBufSeqOutStream_WithFile::FlushToFile()
{
  if (!outFileStream)
  {
    outFileStreamSpec = new COutFileStream;
    outFileStream = outFileStreamSpec;
    if (!outFileStreamSpec->Create(FilePath, false))
    {
      outFileStream.Release();
      return E_FAIL;
      // MessageBoxMyError(UString(L"Can't create file ") + fs2us(tempFilePath));
    }
  }
  while (_fileWritePos != _pos)
  {
    size_t cur = _pos - _fileWritePos;
    UInt32 curSize = (cur < kBlockSize) ? (UInt32)cur : kBlockSize;
    UInt32 processedSizeLoc = 0;
    HRESULT res = outFileStream->Write(_buffer + _fileWritePos, curSize, &processedSizeLoc);
    _fileWritePos += processedSizeLoc;
    RINOK(res);
    if (processedSizeLoc == 0)
      return E_FAIL;
  }
  return S_OK;
}
*/

/*
static HRESULT GetTime(IFolderFolder *folder, UInt32 index, PROPID propID, FILETIME &filetime, bool &filetimeIsDefined)
{
  filetimeIsDefined = false;
  NCOM::CPropVariant prop;
  RINOK(folder->GetProperty(index, propID, &prop));
  if (prop.vt == VT_FILETIME)
  {
    filetime = prop.filetime;
    filetimeIsDefined = (filetime.dwHighDateTime != 0 || filetime.dwLowDateTime != 0);
  }
  else if (prop.vt != VT_EMPTY)
    return E_FAIL;
  return S_OK;
}
*/

void CPanel::OpenItemInArchive(int index, bool tryInternal, bool tryExternal, bool editMode, bool useEditor, const wchar_t *type)
{
  const UString name = GetItemName(index);
  const UString relPath = GetItemRelPath(index);
  
  if (IsVirus_Message(name))
    return;

  if (!_folderOperations)
  {
    MessageBoxErrorLang(IDS_OPERATION_IS_NOT_SUPPORTED);
    return;
  }

  bool tryAsArchive = tryInternal && (!tryExternal || !DoItemAlwaysStart(name));

  UString fullVirtPath = _currentFolderPrefix + relPath;

  CTempDir tempDirectory;
  if (!tempDirectory.Create(kTempDirPrefix))
  {
    MessageBoxLastError();
    return;
  }
  FString tempDir = tempDirectory.GetPath();
  FString tempDirNorm = tempDir;
  NName::NormalizeDirPathPrefix(tempDirNorm);
  const FString tempFilePath = tempDirNorm + us2fs(Get_Correct_FsFile_Name(name));

  CTempFileInfo tempFileInfo;
  tempFileInfo.FileIndex = index;
  tempFileInfo.RelPath = relPath;
  tempFileInfo.FolderPath = tempDir;
  tempFileInfo.FilePath = tempFilePath;
  tempFileInfo.NeedDelete = true;

  if (tryAsArchive)
  {
    CMyComPtr<IInArchiveGetStream> getStream;
    _folder.QueryInterface(IID_IInArchiveGetStream, &getStream);
    if (getStream)
    {
      CMyComPtr<ISequentialInStream> subSeqStream;
      getStream->GetStream(index, &subSeqStream);
      if (subSeqStream)
      {
        CMyComPtr<IInStream> subStream;
        subSeqStream.QueryInterface(IID_IInStream, &subStream);
        if (subStream)
        {
          bool encrypted;
          HRESULT res = OpenItemAsArchive(subStream, tempFileInfo, fullVirtPath, type ? type : L"", encrypted);
          if (res == S_OK)
          {
            tempDirectory.DisableDeleting();
            RefreshListCtrl();
            return;
          }
          if (res == E_ABORT)
            return;
          if (res != S_FALSE)
          {
            // probably we must show some message here
            // return;
          }
        }
      }
    }
  }


  CRecordVector<UInt32> indices;
  indices.Add(index);

  UStringVector messages;

  bool usePassword = false;
  UString password;
  if (_parentFolders.Size() > 0)
  {
    const CFolderLink &fl = _parentFolders.Back();
    usePassword = fl.UsePassword;
    password = fl.Password;
  }

  #if defined(_WIN32) && !defined(UNDER_CE)
  CByteBuffer zoneBuf;
  #ifndef _UNICODE
  if (g_IsNT)
  #endif
  if (_parentFolders.Size() > 0)
  {
    const CFolderLink &fl = _parentFolders.Front();
    if (!fl.IsVirtual && !fl.FilePath.IsEmpty())
      ReadZoneFile(fl.FilePath + k_ZoneId_StreamName, zoneBuf);
  }
  #endif


  CVirtFileSystem *virtFileSystemSpec = NULL;
  CMyComPtr<ISequentialOutStream> virtFileSystem;

  bool isAltStream = IsItem_AltStream(index);

  CCopyToOptions options;
  options.includeAltStreams = true;
  options.replaceAltStreamChars = isAltStream;
  
  if (tryAsArchive)
  {
    NCOM::CPropVariant prop;
    _folder->GetProperty(index, kpidSize, &prop);
    UInt64 fileLimit = g_RAM_Size / 4;
    UInt64 fileSize = 0;
    if (!ConvertPropVariantToUInt64(prop, fileSize))
      fileSize = fileLimit;
    if (fileSize <= fileLimit && fileSize > 0)
    {
      options.streamMode = true;
      virtFileSystemSpec = new CVirtFileSystem;
      virtFileSystem = virtFileSystemSpec;
      // we allow additional total size for small alt streams;
      virtFileSystemSpec->MaxTotalAllocSize = fileSize + (1 << 10);
      
      virtFileSystemSpec->DirPrefix = tempDirNorm;
      virtFileSystemSpec->Init();
      options.VirtFileSystem = virtFileSystem;
      options.VirtFileSystemSpec = virtFileSystemSpec;
    }
  }

  options.folder = fs2us(tempDirNorm);
  options.showErrorMessages = true;
  HRESULT result = CopyTo(options, indices, &messages, usePassword, password);

  if (_parentFolders.Size() > 0)
  {
    CFolderLink &fl = _parentFolders.Back();
    fl.UsePassword = usePassword;
    fl.Password = password;
  }

  if (!messages.IsEmpty())
    return;
  if (result != S_OK)
  {
    if (result != E_ABORT)
      MessageBoxError(result);
    return;
  }

  if (options.VirtFileSystem)
  {
    if (virtFileSystemSpec->IsStreamInMem())
    {
      const CVirtFile &file = virtFileSystemSpec->Files[0];

      size_t streamSize = (size_t)file.Size;
      CBufInStream *bufInStreamSpec = new CBufInStream;
      CMyComPtr<IInStream> bufInStream = bufInStreamSpec;
      bufInStreamSpec->Init(file.Data, streamSize, virtFileSystem);
      bool encrypted;
      if (OpenItemAsArchive(bufInStream, tempFileInfo, fullVirtPath, type ? type : L"", encrypted) == S_OK)
      {
        tempDirectory.DisableDeleting();
        RefreshListCtrl();
        return;
      }
      if (virtFileSystemSpec->FlushToDisk(true) != S_OK)
        return;
    }
  }


  #if defined(_WIN32) && !defined(UNDER_CE)
  if (zoneBuf.Size() != 0)
  {
    if (NFind::DoesFileExist(tempFilePath))
    {
      WriteZoneFile(tempFilePath + k_ZoneId_StreamName, zoneBuf);
    }
  }
  #endif


  if (tryAsArchive)
  {
    bool encrypted;
    if (OpenItemAsArchive(NULL, tempFileInfo, fullVirtPath, type ? type : L"", encrypted) == S_OK)
    {
      tempDirectory.DisableDeleting();
      RefreshListCtrl();
      return;
    }
  }

  CMyAutoPtr<CTmpProcessInfo> tmpProcessInfoPtr(new CTmpProcessInfo());
  CTmpProcessInfo *tpi = tmpProcessInfoPtr.get();
  tpi->FolderPath = tempDir;
  tpi->FilePath = tempFilePath;
  tpi->NeedDelete = true;
  tpi->UsePassword = usePassword;
  tpi->Password = password;

  if (!tpi->FileInfo.Find(tempFilePath))
    return;

  CTmpProcessInfoRelease tmpProcessInfoRelease(*tpi);

  if (!tryExternal)
    return;

  // CProcess process;
  // HRESULT res;
  if (editMode)
    /* res = */ StartEditApplication(fs2us(tempFilePath), useEditor, (HWND)*this /* , process */ );
  else
    /* res =  */ StartApplication(fs2us(tempDirNorm), fs2us(tempFilePath), (HWND)*this /* , process */ );

#ifdef _WIN32
  if ((HANDLE)process == 0)
    return;

  tpi->Window = (HWND)(*this);
  tpi->FullPathFolderPrefix = _currentFolderPrefix;
  tpi->FileIndex = index;
  tpi->RelPath = relPath;
  tpi->Processes.AddProcess(process.Detach());

  NWindows::CThread thread;
  if (thread.Create(MyThreadFunction, tpi) != S_OK)
    throw 271824;
#endif
  tempDirectory.DisableDeleting();
  tmpProcessInfoPtr.release();
  tmpProcessInfoRelease._needDelete = false;
}

/*
static const UINT64 kTimeLimit = UINT64(10000000) * 3600 * 24;

static bool CheckDeleteItem(UINT64 currentFileTime, UINT64 folderFileTime)
{
  return (currentFileTime - folderFileTime > kTimeLimit &&
      folderFileTime - currentFileTime > kTimeLimit);
}

void DeleteOldTempFiles()
{
  UString tempPath;
  if(!MyGetTempPath(tempPath))
    throw 1;

  UINT64 currentFileTime;
  NTime::GetCurUtcFileTime(currentFileTime);
  UString searchWildCard = tempPath + kTempDirPrefix + L"*.tmp";
  searchWildCard += WCHAR(NName::kAnyStringWildcard);
  NFind::CEnumeratorW enumerator(searchWildCard);
  NFind::CFileInfo fileInfo;
  while(enumerator.Next(fileInfo))
  {
    if (!fileInfo.IsDir())
      continue;
    const UINT64 &cTime = *(const UINT64 *)(&fileInfo.CTime);
    if(CheckDeleteItem(cTime, currentFileTime))
      RemoveDirectoryWithSubItems(tempPath + fileInfo.Name);
  }
}
*/
