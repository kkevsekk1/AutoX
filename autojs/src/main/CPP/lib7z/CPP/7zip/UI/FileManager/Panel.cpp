// Panel.cpp

#include "StdAfx.h"

// #include <Windowsx.h>

#include "../../../Common/IntToString.h"
#include "../../../Common/StringConvert.h"

#include "../../../Windows/FileName.h"
#include "../../../Windows/ErrorMsg.h"
#include "../../../Windows/PropVariant.h"
#include "../../../Windows/Thread.h"

#include "../../PropID.h"

#include "resource.h"
#include "../GUI/ExtractRes.h"

#include "../Common/ArchiveName.h"
#include "../Common/CompressCall.h"

#include "../Agent/IFolderArchive.h"

#include "App.h"
#include "ExtractCallback.h"
#include "FSFolder.h"
#include "FormatUtils.h"
#include "Panel.h"
#include "RootFolder.h"

#include "PropertyNameRes.h"

using namespace NWindows;
using namespace NControl;

#ifndef _UNICODE
extern bool g_IsNT;
#endif

static const UINT_PTR kTimerID = 1;
static const UINT kTimerElapse = 1000;

// FIXME static DWORD kStyles[4] = { LVS_ICON, LVS_SMALLICON, LVS_LIST, LVS_REPORT };

// static const int kCreateFolderID = 101;

extern HINSTANCE g_hInstance;
extern DWORD g_ComCtl32Version;

void CPanel::Release()
{
  // It's for unloading COM dll's: don't change it.
  CloseOpenFolders();
  // FIXME _sevenZipContextMenu.Release();
  // FIXME _systemContextMenu.Release();
}

CPanel::~CPanel()
{
  CloseOpenFolders();
}

HWND CPanel::GetParent()
{
#ifdef _WIN32
  HWND h = CWindow2::GetParent();
  return (h == 0) ? _mainWindow : h;
#else
  return 0;
#endif
}

static LPCWSTR kClassName = L"7-Zip::Panel";


HRESULT CPanel::Create(HWND mainWindow, HWND parentWindow, UINT id,
    const UString &currentFolderPrefix,
    const UString &arcFormat,
    CPanelCallback *panelCallback, CAppState *appState,
    bool &archiveIsOpened, bool &encrypted)
{
  _mainWindow = mainWindow;
  _processTimer = true;
  _processNotify = true;
  _processStatusBar = true;

  _panelCallback = panelCallback;
  _appState = appState;
  // _index = index;
  _baseID = id;
  _comboBoxID = _baseID + 3;
  _statusBarID = _comboBoxID + 1;

  UString cfp = currentFolderPrefix;

  if (!currentFolderPrefix.IsEmpty())
    if (currentFolderPrefix[0] == L'.')
    {
      FString cfpF;
      if (NFile::NDir::MyGetFullPathName(us2fs(currentFolderPrefix), cfpF))
        cfp = fs2us(cfpF);
    }

  RINOK(BindToPath(cfp, arcFormat, archiveIsOpened, encrypted));

#ifdef _WIN32
  if (!CreateEx(0, kClassName, 0, WS_CHILD | WS_VISIBLE,
      0, 0, _xSize, 260,
      parentWindow, (HMENU)(UINT_PTR)id, g_hInstance))
    return E_FAIL;
#else
  {
  	// printf("WARNING CPanel::Create => CreateEx\n");
  	// this->OnCreate(0);
  	extern void registerWindow2(int baseID,NWindows::NControl::CWindow2 *w);
	registerWindow2(_baseID,this);
  }
#endif
  return S_OK;
}

LRESULT CPanel::OnMessage(UINT message, WPARAM wParam, LPARAM lParam)
{
  switch (message)
  {
    case kShiftSelectMessage:
      OnShiftSelectMessage();
      return 0;
    case kReLoadMessage:
      RefreshListCtrl(_selectedState);
      return 0;
    case kSetFocusToListView:
      _listView.SetFocus();
      return 0;
    case kOpenItemChanged:
      return OnOpenItemChanged(lParam);
    case kRefresh_StatusBar:
      if (_processStatusBar)
        Refresh_StatusBar();
      return 0;
#ifdef _WIN32
    #ifdef UNDER_CE
    case kRefresh_HeaderComboBox:
      LoadFullPathAndShow();
      return 0;
    #endif
    case WM_TIMER:
      OnTimer();
      return 0;
    case WM_CONTEXTMENU:
      if (OnContextMenu(HANDLE(wParam), GET_X_LPARAM(lParam), GET_Y_LPARAM(lParam)))
        return 0;
      break;
    /*
    case WM_DROPFILES:
      CompressDropFiles(HDROP(wParam));
      return 0;
    */
#endif
  }
  return CWindow2::OnMessage(message, wParam, lParam);
}

LRESULT CMyListView::OnMessage(UINT message, WPARAM wParam, LPARAM lParam)
{
#ifdef _WIN32
  if (message == WM_CHAR)
  {
    UINT scanCode = (UINT)((lParam >> 16) & 0xFF);
    bool extended = ((lParam & 0x1000000) != 0);
    UINT virtualKey = MapVirtualKey(scanCode, 1);
    if (virtualKey == VK_MULTIPLY || virtualKey == VK_ADD ||
        virtualKey == VK_SUBTRACT)
      return 0;
    if ((wParam == '/' && extended)
        || wParam == '\\' || wParam == '/')
    {
      _panel->OpenDrivesFolder();
      return 0;
    }
  }
  else if (message == WM_SYSCHAR)
  {
    // For Alt+Enter Beep disabling
    UINT scanCode = (UINT)(lParam >> 16) & 0xFF;
    UINT virtualKey = MapVirtualKey(scanCode, 1);
    if (virtualKey == VK_RETURN || virtualKey == VK_MULTIPLY ||
        virtualKey == VK_ADD || virtualKey == VK_SUBTRACT)
      return 0;
  }
  /*
  else if (message == WM_SYSKEYDOWN)
  {
    // return 0;
  }
  */
  else if (message == WM_KEYDOWN)
  {
    bool alt = IsKeyDown(VK_MENU);
    bool ctrl = IsKeyDown(VK_CONTROL);
    bool shift = IsKeyDown(VK_SHIFT);
    switch (wParam)
    {
      /*
      case VK_RETURN:
      {
        if (shift && !alt && !ctrl)
        {
          _panel->OpenSelectedItems(false);
          return 0;
        }
        break;
      }
      */
      case VK_NEXT:
      {
        if (ctrl && !alt && !shift)
        {
          _panel->OpenFocusedItemAsInternal();
          return 0;
        }
        break;
      }
      case VK_PRIOR:
      if (ctrl && !alt && !shift)
      {
        _panel->OpenParentFolder();
        return 0;
      }
    }
  }
  #ifdef UNDER_CE
  else if (message == WM_KEYUP)
  {
    if (wParam == VK_F2) // it's VK_TSOFT2
    {
      // Activate Menu
      ::PostMessage(g_HWND, WM_SYSCOMMAND, SC_KEYMENU, 0);
      return 0;
    }
  }
  #endif
  else if (message == WM_SETFOCUS)
  {
    _panel->_lastFocusedIsList = true;
    _panel->_panelCallback->PanelWasFocused();
  }
#endif
  return CListView2::OnMessage(message, wParam, lParam);
}

/*
static LRESULT APIENTRY ComboBoxSubclassProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
{
  CWindow tempDialog(hwnd);
  CMyComboBox *w = (CMyComboBox *)(tempDialog.GetUserDataLongPtr());
  if (w == NULL)
    return 0;
  return w->OnMessage(message, wParam, lParam);
}

LRESULT CMyComboBox::OnMessage(UINT message, WPARAM wParam, LPARAM lParam)
{
  return CallWindowProc(_origWindowProc, *this, message, wParam, lParam);
}
*/

#ifdef _WIN32
#ifndef UNDER_CE

static LRESULT APIENTRY ComboBoxEditSubclassProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam)
{
  CWindow tempDialog(hwnd);
  CMyComboBoxEdit *w = (CMyComboBoxEdit *)(tempDialog.GetUserDataLongPtr());
  if (w == NULL)
    return 0;
  return w->OnMessage(message, wParam, lParam);
}

#endif

LRESULT CMyComboBoxEdit::OnMessage(UINT message, WPARAM wParam, LPARAM lParam)
{
  // See MSDN / Subclassing a Combo Box / Creating a Combo-box Toolbar
  switch (message)
  {
    case WM_SYSKEYDOWN:
      switch (wParam)
      {
        case VK_F1:
        case VK_F2:
        {
          // check ALT
          if ((lParam & (1<<29)) == 0)
            break;
          bool alt = IsKeyDown(VK_MENU);
          bool ctrl = IsKeyDown(VK_CONTROL);
          bool shift = IsKeyDown(VK_SHIFT);
          if (alt && !ctrl && !shift)
          {
            _panel->_panelCallback->SetFocusToPath(wParam == VK_F1 ? 0 : 1);
            return 0;
          }
          break;
        }
      }
      break;
    case WM_KEYDOWN:
      switch (wParam)
      {
        case VK_TAB:
          // SendMessage(hwndMain, WM_ENTER, 0, 0);
          _panel->SetFocusToList();
          return 0;
        case VK_F9:
        {
          bool alt = IsKeyDown(VK_MENU);
          bool ctrl = IsKeyDown(VK_CONTROL);
          bool shift = IsKeyDown(VK_SHIFT);
          if (!alt && !ctrl && !shift)
          {
            g_App.SwitchOnOffOnePanel();;
            return 0;
          }
          break;
        }
      }
      break;
    case WM_CHAR:
      switch (wParam)
      {
        case VK_TAB:
        case VK_ESCAPE:
          return 0;
      }
  }
  #ifndef _UNICODE
  if (g_IsNT)
    return CallWindowProcW(_origWindowProc, *this, message, wParam, lParam);
  else
  #endif
    return CallWindowProc(_origWindowProc, *this, message, wParam, lParam);
}

bool CPanel::OnCreate(CREATESTRUCT * /* createStruct */)
{
  // _virtualMode = false;
  // _sortIndex = 0;
  _sortID = kpidName;
  _ascending = true;
  _lastFocusedIsList = true;

  DWORD style = WS_CHILD | WS_VISIBLE; //  | WS_BORDER ; // | LVS_SHAREIMAGELISTS; //  | LVS_SHOWSELALWAYS;;

  style |= LVS_SHAREIMAGELISTS;
  // style  |= LVS_AUTOARRANGE;
  style |= WS_CLIPCHILDREN;
  style |= WS_CLIPSIBLINGS;

  const UInt32 kNumListModes = ARRAY_SIZE(kStyles);
  if (_ListViewMode >= kNumListModes)
    _ListViewMode = kNumListModes - 1;

  style |= kStyles[_ListViewMode]
    | WS_TABSTOP
    | LVS_EDITLABELS;
  if (_mySelectMode)
    style |= LVS_SINGLESEL;

  /*
  if (_virtualMode)
    style |= LVS_OWNERDATA;
  */

  DWORD exStyle;
  exStyle = WS_EX_CLIENTEDGE;

  if (!_listView.CreateEx(exStyle, style, 0, 0, 116, 260,
      *this, (HMENU)(UINT_PTR)(_baseID + 1), g_hInstance, NULL))
    return false;

  _listView.SetUnicodeFormat();
  _listView._panel = this;
  _listView.SetWindowProc();

  _listView.SetImageList(GetSysImageList(true), LVSIL_SMALL);
  _listView.SetImageList(GetSysImageList(false), LVSIL_NORMAL);

  // _exStyle |= LVS_EX_HEADERDRAGDROP;
  // DWORD extendedStyle = _listView.GetExtendedListViewStyle();
  // extendedStyle |= _exStyle;
  //  _listView.SetExtendedListViewStyle(extendedStyle);
  SetExtendedStyle();

  _listView.Show(SW_SHOW);
  _listView.InvalidateRect(NULL, true);
  _listView.Update();
  
  // Ensure that the common control DLL is loaded.
  INITCOMMONCONTROLSEX icex;

  icex.dwSize = sizeof(INITCOMMONCONTROLSEX);
  icex.dwICC  = ICC_BAR_CLASSES;
  InitCommonControlsEx(&icex);

  TBBUTTON tbb [ ] =
  {
    // {0, 0, TBSTATE_ENABLED, BTNS_SEP, 0L, 0},
    {VIEW_PARENTFOLDER, kParentFolderID, TBSTATE_ENABLED, BTNS_BUTTON, 0L, 0},
    // {0, 0, TBSTATE_ENABLED, BTNS_SEP, 0L, 0},
    // {VIEW_NEWFOLDER, kCreateFolderID, TBSTATE_ENABLED, BTNS_BUTTON, 0L, 0},
  };

  #ifndef UNDER_CE
  if (g_ComCtl32Version >= MAKELONG(71, 4))
  #endif
  {
    icex.dwSize = sizeof(INITCOMMONCONTROLSEX);
    icex.dwICC  = ICC_COOL_CLASSES | ICC_BAR_CLASSES;
    InitCommonControlsEx(&icex);
    
    // if there is no CCS_NOPARENTALIGN, there is space of some pixels after rebar (Incorrect GetWindowRect ?)

    _headerReBar.Attach(::CreateWindowEx(WS_EX_TOOLWINDOW,
      REBARCLASSNAME,
      NULL, WS_VISIBLE | WS_BORDER | WS_CHILD |
      WS_CLIPCHILDREN | WS_CLIPSIBLINGS
      | CCS_NODIVIDER
      | CCS_NOPARENTALIGN
      | CCS_TOP
      | RBS_VARHEIGHT
      | RBS_BANDBORDERS
      ,0,0,0,0, *this, NULL, g_hInstance, NULL));
  }

  DWORD toolbarStyle =  WS_CHILD | WS_VISIBLE ;
  if (_headerReBar)
  {
    toolbarStyle |= 0
      // | WS_CLIPCHILDREN
      // | WS_CLIPSIBLINGS

      | TBSTYLE_TOOLTIPS
      | CCS_NODIVIDER
      | CCS_NORESIZE
      | TBSTYLE_FLAT
      ;
  }

  _headerToolBar.Attach(::CreateToolbarEx ((*this), toolbarStyle,
      _baseID + 2, 11,
      (HINSTANCE)HINST_COMMCTRL,
      IDB_VIEW_SMALL_COLOR,
      (LPCTBBUTTON)&tbb, ARRAY_SIZE(tbb),
      0, 0, 0, 0, sizeof (TBBUTTON)));

  #ifndef UNDER_CE
  // Load ComboBoxEx class
  icex.dwSize = sizeof(INITCOMMONCONTROLSEX);
  icex.dwICC = ICC_USEREX_CLASSES;
  InitCommonControlsEx(&icex);
  #endif
  
  _headerComboBox.CreateEx(0,
      #ifdef UNDER_CE
      WC_COMBOBOXW
      #else
      WC_COMBOBOXEXW
      #endif
      , NULL,
    WS_BORDER | WS_VISIBLE |WS_CHILD | CBS_DROPDOWN | CBS_AUTOHSCROLL,
      0, 0, 100, 520,
      ((_headerReBar == 0) ? (HWND)*this : _headerToolBar),
      (HMENU)(UINT_PTR)(_comboBoxID),
      g_hInstance, NULL);
  #ifndef UNDER_CE
  _headerComboBox.SetUnicodeFormat(true);

  _headerComboBox.SetImageList(GetSysImageList(true));

  _headerComboBox.SetExtendedStyle(CBES_EX_PATHWORDBREAKPROC, CBES_EX_PATHWORDBREAKPROC);

  /*
  _headerComboBox.SetUserDataLongPtr(LONG_PTR(&_headerComboBox));
  _headerComboBox._panel = this;
  _headerComboBox._origWindowProc =
      (WNDPROC)_headerComboBox.SetLongPtr(GWLP_WNDPROC,
      LONG_PTR(ComboBoxSubclassProc));
  */
  _comboBoxEdit.Attach(_headerComboBox.GetEditControl());

  // _comboBoxEdit.SendMessage(CCM_SETUNICODEFORMAT, (WPARAM)(BOOL)TRUE, 0);

  _comboBoxEdit.SetUserDataLongPtr(LONG_PTR(&_comboBoxEdit));
  _comboBoxEdit._panel = this;
   #ifndef _UNICODE
   if (g_IsNT)
     _comboBoxEdit._origWindowProc =
      (WNDPROC)_comboBoxEdit.SetLongPtrW(GWLP_WNDPROC, LONG_PTR(ComboBoxEditSubclassProc));
   else
   #endif
     _comboBoxEdit._origWindowProc =
      (WNDPROC)_comboBoxEdit.SetLongPtr(GWLP_WNDPROC, LONG_PTR(ComboBoxEditSubclassProc));

  #endif

  if (_headerReBar)
  {
    REBARINFO     rbi;
    rbi.cbSize = sizeof(REBARINFO);  // Required when using this struct.
    rbi.fMask  = 0;
    rbi.himl   = (HIMAGELIST)NULL;
    _headerReBar.SetBarInfo(&rbi);
    
    // Send the TB_BUTTONSTRUCTSIZE message, which is required for
    // backward compatibility.
    // _headerToolBar.SendMessage(TB_BUTTONSTRUCTSIZE, (WPARAM)sizeof(TBBUTTON), 0);
    SIZE size;
    _headerToolBar.GetMaxSize(&size);
    
    REBARBANDINFO rbBand;
    rbBand.cbSize = sizeof(REBARBANDINFO);  // Required
    rbBand.fMask = RBBIM_STYLE | RBBIM_CHILD | RBBIM_CHILDSIZE | RBBIM_SIZE;
    rbBand.fStyle = RBBS_NOGRIPPER;
    rbBand.cxMinChild = size.cx;
    rbBand.cyMinChild = size.cy;
    rbBand.cyChild = size.cy;
    rbBand.cx = size.cx;
    rbBand.hwndChild  = _headerToolBar;
    _headerReBar.InsertBand(-1, &rbBand);

    RECT rc;
    ::GetWindowRect(_headerComboBox, &rc);
    rbBand.cxMinChild = 30;
    rbBand.cyMinChild = rc.bottom - rc.top;
    rbBand.cx = 1000;
    rbBand.hwndChild  = _headerComboBox;
    _headerReBar.InsertBand(-1, &rbBand);
    // _headerReBar.MaximizeBand(1, false);
  }

  _statusBar.Create(WS_CHILD | WS_VISIBLE, L"Status", (*this), _statusBarID);
  // _statusBar2.Create(WS_CHILD | WS_VISIBLE, L"Status", (*this), _statusBarID + 1);

  int sizes[] = {160, 250, 350, -1};
  _statusBar.SetParts(4, sizes);
  // _statusBar2.SetParts(5, sizes);

  /*
  RECT rect;
  GetClientRect(&rect);
  OnSize(0, RECT_SIZE_X(rect), RECT_SIZE_Y(rect));
  */

  SetTimer(kTimerID, kTimerElapse);

  // InitListCtrl();
  RefreshListCtrl();
  
  return true;
}
#else
bool CPanel::OnCreate(CREATESTRUCT * /* createStruct */)
{
   printf("WARNING CPanel::OnCreate\n");
  _sortID = kpidName;
  _ascending = true;
  _lastFocusedIsList = true;

  extern HWND g_HWND;
  HWND w = GetDlgItem(g_HWND, _comboBoxID);
  if (w == 0) 
  {
	  printf("Can't find id=%d\n",_comboBoxID);
	  return false;
  }
  printf("CPanel::OnCreate : _headerComboBox.Attach(%p)\n",w);
  _headerComboBox.Attach(w);

  w = GetDlgItem(g_HWND, _statusBarID);
  if (w == 0) 
  {
	  printf("Can't find id=%d\n",_statusBarID);
	  return false;
  }
  printf("CPanel::OnCreate : _statusBar.Attach(%p)\n",w);
  _statusBar.Attach(w);

  w = GetDlgItem(g_HWND, _baseID + 1);
  if (w == 0) 
  {
	  printf("Can't find id=%d\n",_baseID + 1);
	  return false;
  }
  printf("CPanel::OnCreate : _listView.Attach(%p)\n",w);
  _listView.Attach(w);
  
  _listView.SetUnicodeFormat(true);

  // _listView.SetUserDataLongPtr(LONG_PTR(&_listView));
  _listView._panel = this;

  // FIXME _listView.SetImageList(GetSysImageList(true), LVSIL_SMALL);
  // FIXME _listView.SetImageList(GetSysImageList(false), LVSIL_NORMAL);

  // FIXME SetExtendedStyle();

  // FIXME _listView.Show(SW_SHOW);
  // FIXME _listView.InvalidateRect(NULL, true);
  _listView.Update();
  
  /* FIXME
  _headerToolBar.Attach(::CreateToolbarEx ((*this), toolbarStyle,
      _baseID + 2, 11,
      (HINSTANCE)HINST_COMMCTRL,
      IDB_VIEW_SMALL_COLOR,
      (LPCTBBUTTON)&tbb, sizeof(tbb) / sizeof(tbb[0]),
      0, 0, 0, 0, sizeof (TBBUTTON)));
      */

  /* FIXME
  _headerComboBox.CreateEx(0, WC_COMBOBOXEXW, NULL,
    WS_BORDER | WS_VISIBLE |WS_CHILD | CBS_DROPDOWN | CBS_AUTOHSCROLL,
      0, 0, 100, 520,
      ((_headerReBar == 0) ? HWND(*this) : _headerToolBar),
      (HMENU)(UINT_PTR)(_comboBoxID),
      g_hInstance, NULL);
  _headerComboBox.SendMessage(CBEM_SETUNICODEFORMAT, (WPARAM)(BOOL)TRUE, 0);

  _headerComboBox.SetImageList(GetSysImageList(true));

  _headerComboBox.SetExtendedStyle(CBES_EX_PATHWORDBREAKPROC, CBES_EX_PATHWORDBREAKPROC);
  */

  // FIXME _comboBoxEdit.Attach(_headerComboBox.GetEditControl());

  _comboBoxEdit._panel = this;

  // FIXME if (_headerReBar) ...

  // _statusBar.Create(WS_CHILD | WS_VISIBLE, L"Status", (*this), _statusBarID);

  // int sizes[] = {150, 250, 350, -1};
  // _statusBar.SetParts(4, sizes);

  // SetTimer(kTimerID, kTimerElapse);

  // InitListCtrl();
  RefreshListCtrl();
  
  return true;
}
#endif

void CPanel::OnDestroy()
{
  SaveListViewInfo();
  CWindow2::OnDestroy();
}

#ifdef _WIN32
void CPanel::ChangeWindowSize(int xSize, int ySize)
{
  if ((HWND)*this == 0)
    return;
  int kHeaderSize;
  int kStatusBarSize;
  // int kStatusBar2Size;
  RECT rect;
  if (_headerReBar)
    _headerReBar.GetWindowRect(&rect);
  else
    _headerToolBar.GetWindowRect(&rect);

  kHeaderSize = RECT_SIZE_Y(rect);

  _statusBar.GetWindowRect(&rect);
  kStatusBarSize = RECT_SIZE_Y(rect);
  
  // _statusBar2.GetWindowRect(&rect);
  // kStatusBar2Size = RECT_SIZE_Y(rect);
 
  int yListViewSize = MyMax(ySize - kHeaderSize - kStatusBarSize, 0);
  const int kStartXPos = 32;
  if (_headerReBar)
  {
  }
  else
  {
    _headerToolBar.Move(0, 0, xSize, 0);
    _headerComboBox.Move(kStartXPos, 2,
        MyMax(xSize - kStartXPos - 10, kStartXPos), 0);
  }
  _listView.Move(0, kHeaderSize, xSize, yListViewSize);
  _statusBar.Move(0, kHeaderSize + yListViewSize, xSize, kStatusBarSize);
  // _statusBar2.MoveWindow(0, kHeaderSize + yListViewSize + kStatusBarSize, xSize, kStatusBar2Size);
  // _statusBar.MoveWindow(0, 100, xSize, kStatusBarSize);
  // _statusBar2.MoveWindow(0, 200, xSize, kStatusBar2Size);
}

bool CPanel::OnSize(WPARAM /* wParam */, int xSize, int ySize)
{
  if ((HWND)*this == 0)
    return true;
  if (_headerReBar)
    _headerReBar.Move(0, 0, xSize, 0);
  ChangeWindowSize(xSize, ySize);
  return true;
}

bool CPanel::OnNotifyReBar(LPNMHDR header, LRESULT & /* result */)
{
  switch (header->code)
  {
    case RBN_HEIGHTCHANGE:
    {
      RECT rect;
      GetWindowRect(&rect);
      ChangeWindowSize(RECT_SIZE_X(rect), RECT_SIZE_Y(rect));
      return false;
    }
  }
  return false;
}
#endif

bool CPanel::OnNotify(UINT /* controlID */, LPNMHDR header, LRESULT &result)
{
  if (!_processNotify)
    return false;
  if (header->hwndFrom == _headerComboBox)
    return OnNotifyComboBox(header, result);
#ifdef _WIN32
  else if (header->hwndFrom == _headerReBar)
    return OnNotifyReBar(header, result);
#endif
  else if (header->hwndFrom == _listView)
    return OnNotifyList(header, result);
#ifdef _WIN32
  else if (::GetParent(header->hwndFrom) == _listView &&
      header->code == NM_RCLICK)
    return OnRightClick((MY_NMLISTVIEW_NMITEMACTIVATE *)header, result);
#endif
  return false;
}

bool CPanel::OnCommand(int code, int itemID, LPARAM lParam, LRESULT &result)
{
 printf("CPanel::OnCommand(code=%d,itemID=%d)\n",code,itemID);
  if (itemID == kParentFolderID)
  {
    OpenParentFolder();
    result = 0;
    return true;
  }
  /*
  if (itemID == kCreateFolderID)
  {
    CreateFolder();
    result = 0;
    return true;
  }
  */
  if (itemID == _comboBoxID)
  {
    if (OnComboBoxCommand(code, lParam, result))
      return true;
  }
  return CWindow2::OnCommand(code, itemID, lParam, result);
}

void CPanel::MessageBoxInfo(LPCWSTR message, LPCWSTR caption)
  { ::MessageBoxW((HWND)*this, message, caption, MB_OK); }
void CPanel::MessageBox(LPCWSTR message, LPCWSTR caption)
  { ::MessageBoxW((HWND)*this, message, caption, MB_OK | MB_ICONSTOP); }
void CPanel::MessageBox(LPCWSTR message)
  { MessageBox(message, L"7-Zip"); }
/* FIXME
void CPanel::MessageBoxWarning(LPCWSTR message)
  { ::MessageBoxW(NULL, message, L"7-Zip", MB_OK | MB_ICONWARNING); }
*/
void CPanel::MessageBoxMyError(LPCWSTR message)
  { MessageBox(message, L"7-Zip"); }


void CPanel::MessageBoxError(HRESULT errorCode, LPCWSTR caption)
{
  MessageBox(HResultToMessage(errorCode), caption);
}

void CPanel::MessageBoxError2Lines(LPCWSTR message, HRESULT errorCode)
{
  UString m = message;
  if (errorCode != 0)
  {
    m.Add_LF();
    m += HResultToMessage(errorCode);
  }
  MessageBoxMyError(m);
}

void CPanel::MessageBoxError(HRESULT errorCode)
  { MessageBoxError(errorCode, L"7-Zip"); }
void CPanel::MessageBoxLastError(LPCWSTR caption)
  { MessageBoxError(::GetLastError(), caption); }
void CPanel::MessageBoxLastError()
  { MessageBoxLastError(L"7-Zip"); }

void CPanel::MessageBoxErrorLang(UINT resourceID)
  { MessageBox(LangString(resourceID)); }


void CPanel::SetFocusToList()
{
  _listView.SetFocus();
  // SetCurrentPathText();
}

void CPanel::SetFocusToLastRememberedItem()
{
  if (_lastFocusedIsList)
    SetFocusToList();
  // FIXME else
    // FIXME _headerComboBox.SetFocus();
}

UString CPanel::GetFolderTypeID() const
{
  {
    NCOM::CPropVariant prop;
    if (_folder->GetFolderProperty(kpidType, &prop) == S_OK)
      if (prop.vt == VT_BSTR)
        return (const wchar_t *)prop.bstrVal;
  }
  return UString();
}

bool CPanel::IsFolderTypeEqTo(const char *s) const
{
  return StringsAreEqual_Ascii(GetFolderTypeID(), s);
}

bool CPanel::IsRootFolder() const { return IsFolderTypeEqTo("RootFolder"); }
bool CPanel::IsFSFolder() const { return IsFolderTypeEqTo("FSFolder"); }
bool CPanel::IsFSDrivesFolder() const { return IsFolderTypeEqTo("FSDrives"); }
bool CPanel::IsAltStreamsFolder() const { return IsFolderTypeEqTo("AltStreamsFolder"); }
bool CPanel::IsArcFolder() const
{
  return GetFolderTypeID().IsPrefixedBy_Ascii_NoCase("7-Zip");
}

UString CPanel::GetFsPath() const
{
  if (IsFSDrivesFolder() && !IsDeviceDrivesPrefix() && !IsSuperDrivesPrefix())
    return UString();
  return _currentFolderPrefix;
}

UString CPanel::GetDriveOrNetworkPrefix() const
{
  if (!IsFSFolder())
    return UString();
  UString drive = GetFsPath();
  // FIXME drive.DeleteFrom(NFile::NName::GetRootPrefixSize(drive));
  return drive;
}

void CPanel::SetListViewMode(UInt32 index)
{
  if (index >= 4)
    return;
  _ListViewMode = index;
#ifdef _WIN32
  DWORD oldStyle = (DWORD)_listView.GetStyle();
  DWORD newStyle = kStyles[index];

  // DWORD tickCount1 = GetTickCount();
  if ((oldStyle & LVS_TYPEMASK) != newStyle)
    _listView.SetStyle((oldStyle & ~LVS_TYPEMASK) | newStyle);
  // RefreshListCtrlSaveFocused();
  /*
  DWORD tickCount2 = GetTickCount();
  char s[256];
  sprintf(s, "SetStyle = %5d",
      tickCount2 - tickCount1
      );
  OutputDebugStringA(s);
  */
#endif
}

void CPanel::ChangeFlatMode()
{
  _flatMode = !_flatMode;
  if (_parentFolders.Size() > 0)
    _flatModeForArc = _flatMode;
  else
    _flatModeForDisk = _flatMode;
  RefreshListCtrlSaveFocused();
}

/*
void CPanel::Change_ShowNtfsStrems_Mode()
{
  _showNtfsStrems_Mode = !_showNtfsStrems_Mode;
  if (_parentFolders.Size() > 0)
    _showNtfsStrems_ModeForArc = _showNtfsStrems_Mode;
  else
    _showNtfsStrems_ModeForDisk = _showNtfsStrems_Mode;
  RefreshListCtrlSaveFocused();
}
*/

void CPanel::Post_Refresh_StatusBar()
{
  // FIXME if (_processStatusBar)
    // FIXME PostMessage(kRefresh_StatusBar);
}

void CPanel::AddToArchive()
{
  CRecordVector<UInt32> indices;
  GetOperatedItemIndices(indices);
  if (!Is_IO_FS_Folder())
  {
    MessageBoxErrorLang(IDS_OPERATION_IS_NOT_SUPPORTED);
    return;
  }
  if (indices.Size() == 0)
  {
    MessageBoxErrorLang(IDS_SELECT_FILES);
    return;
  }
  UStringVector names;

  const UString curPrefix = GetFsPath();
  UString destCurDirPrefix = curPrefix;
  if (IsFSDrivesFolder())
    destCurDirPrefix = ROOT_FS_FOLDER;

  FOR_VECTOR (i, indices)
    names.Add(curPrefix + GetItemRelPath2(indices[i]));
  bool fromPrev = (names.Size() > 1);
  const UString arcName = CreateArchiveName(names.Front(), fromPrev, false);
  HRESULT res = CompressFiles(destCurDirPrefix, arcName, L"",
      true, // addExtension
      names, false, true, false);
  if (res != S_OK)
  {
    if (destCurDirPrefix.Len() >= MAX_PATH)
      MessageBoxErrorLang(IDS_MESSAGE_UNSUPPORTED_OPERATION_FOR_LONG_PATH_FOLDER);
  }
  // KillSelection();
}

static UString GetSubFolderNameForExtract(const UString &arcPath)
{
  UString s = arcPath;
  int slashPos = s.ReverseFind_PathSepar();
  int dotPos = s.ReverseFind_Dot();
  if (dotPos <= slashPos + 1)
    s += L'~';
  else
  {
    s.DeleteFrom(dotPos);
    s.TrimRight();
  }
  return s;
}

void CPanel::GetFilePaths(const CRecordVector<UInt32> &indices, UStringVector &paths, bool allowFolders)
{
  const UString prefix = GetFsPath();
  FOR_VECTOR (i, indices)
  {
    int index = indices[i];
    if (!allowFolders && IsItem_Folder(index))
    {
      paths.Clear();
      break;
    }
    paths.Add(prefix + GetItemRelPath2(index));
  }
  if (paths.Size() == 0)
  {
    MessageBoxErrorLang(IDS_SELECT_FILES);
    return;
  }
}

void CPanel::ExtractArchives()
{
  if (_parentFolders.Size() > 0)
  {
    _panelCallback->OnCopy(false, false);
    return;
  }
  CRecordVector<UInt32> indices;
  GetOperatedItemIndices(indices);
  UStringVector paths;
  GetFilePaths(indices, paths);
  if (paths.IsEmpty())
    return;
  
  UString outFolder = GetFsPath();
  if (indices.Size() == 1)
    outFolder += GetSubFolderNameForExtract(GetItemRelPath(indices[0]));
  else
    outFolder += L'*';
  outFolder.Add_PathSepar();
  
  ::ExtractArchives(paths, outFolder
      , true // showDialog
      , false // elimDup
      );
}

/*
static void AddValuePair(UINT resourceID, UInt64 value, UString &s)
{
  AddLangString(s, resourceID);
  char sz[32];
  s += L": ";
  ConvertUInt64ToString(value, sz);
  s.AddAsciiStr(sz);
  s.Add_LF();
}
*/

class CThreadTest: public CProgressThreadVirt
{
  HRESULT ProcessVirt();
public:
  CRecordVector<UInt32> Indices;
  CExtractCallbackImp *ExtractCallbackSpec;
  CMyComPtr<IFolderArchiveExtractCallback> ExtractCallback;
  CMyComPtr<IArchiveFolder> ArchiveFolder;
};

// actually now we don't need CThreadTest, since now we call CopyTo for "test command

/*
HRESULT CThreadTest::ProcessVirt()
{
  RINOK(ArchiveFolder->Extract(&Indices[0], Indices.Size(),
      true, // includeAltStreams
      false, // replaceAltStreamColon
      NExtract::NPathMode::kFullPathnames,
      NExtract::NOverwriteMode::kAskBefore,
      NULL, // path
      BoolToInt(true), // testMode
      ExtractCallback));
  if (ExtractCallbackSpec->IsOK())
  {
    UString s;
    AddValuePair(IDS_PROP_FOLDERS, ExtractCallbackSpec->NumFolders, s);
    AddValuePair(IDS_PROP_FILES, ExtractCallbackSpec->NumFiles, s);
    // AddValuePair(IDS_PROP_SIZE, ExtractCallbackSpec->UnpackSize, s);
    // AddSizePair(IDS_COMPRESSED_COLON, Stat.PackSize, s);
    s.Add_LF();
    AddLangString(s, IDS_MESSAGE_NO_ERRORS);
    FinalMessage.OkMessage.Message = s;
  }
  return S_OK;
}
*/

/*
static void AddSizePair(UInt32 langID, UInt64 value, UString &s)
{
  char sz[32];
  AddLangString(s, langID);
  s += L' ';
  ConvertUInt64ToString(value, sz);
  s.AddAsciiStr(sz);
  ConvertUInt64ToString(value >> 20, sz);
  s.AddAsciiStr(" (");
  s.AddAsciiStr(sz);
  s.AddAsciiStr(" MB)");
  s.Add_LF();
}
*/

void CPanel::TestArchives()
{
  CRecordVector<UInt32> indices;
  GetOperatedIndicesSmart(indices);
  CMyComPtr<IArchiveFolder> archiveFolder;
  _folder.QueryInterface(IID_IArchiveFolder, &archiveFolder);
  if (archiveFolder)
  {
    CCopyToOptions options;
    options.streamMode = true;
    options.showErrorMessages = true;
    options.testMode = true;

    UStringVector messages;
    HRESULT res = CopyTo(options, indices, &messages);
    if (res != S_OK)
    {
      if (res != E_ABORT)
        MessageBoxError(res);
    }
    return;

    /*
    {
    CThreadTest extracter;

    extracter.ArchiveFolder = archiveFolder;
    extracter.ExtractCallbackSpec = new CExtractCallbackImp;
    extracter.ExtractCallback = extracter.ExtractCallbackSpec;
    extracter.ExtractCallbackSpec->ProgressDialog = &extracter.ProgressDialog;
    if (!_parentFolders.IsEmpty())
    {
      const CFolderLink &fl = _parentFolders.Back();
      extracter.ExtractCallbackSpec->PasswordIsDefined = fl.UsePassword;
      extracter.ExtractCallbackSpec->Password = fl.Password;
    }

    if (indices.IsEmpty())
      return;

    extracter.Indices = indices;
    
    UString title = LangString(IDS_PROGRESS_TESTING);
    UString progressWindowTitle = L"7-Zip"; // LangString(IDS_APP_TITLE);
    
    extracter.ProgressDialog.CompressingMode = false;
    extracter.ProgressDialog.MainWindow = GetParent();
    extracter.ProgressDialog.MainTitle = progressWindowTitle;
    extracter.ProgressDialog.MainAddTitle = title + L' ';
    
    extracter.ExtractCallbackSpec->OverwriteMode = NExtract::NOverwriteMode::kAskBefore;
    extracter.ExtractCallbackSpec->Init();
    
    if (extracter.Create(title, GetParent()) != S_OK)
      return;
    
    }
    RefreshTitleAlways();
    return;
    */
  }

  if (!IsFSFolder())
  {
    MessageBoxErrorLang(IDS_OPERATION_IS_NOT_SUPPORTED);
    return;
  }
  UStringVector paths;
  GetFilePaths(indices, paths, true);
  if (paths.IsEmpty())
    return;
  ::TestArchives(paths);
}
