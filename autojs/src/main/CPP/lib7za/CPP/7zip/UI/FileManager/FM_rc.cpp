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
#include "wx/artprov.h"
#include "wx/imaglist.h"

#undef _WIN32
 
#include "resource.h"

#include "PropertyNameRes.h"

#include "App.h"

#include "Windows/Window.h" // FIXME
#include "Windows/Control/DialogImpl.h"
#include "Windows/Control/ListView.h"
#include "Windows/Control/Window2.h"

#define static const
#include "../GUI/p7zip_32.xpm"
#undef static

extern HWND g_HWND;

#define BASE_ID_PANEL_1 (1000 + 100 * 0)
#define BASE_ID_PANEL_2 (1000 + 100 * 1)

////////////////////////////////////// Tool bar images
#include "res/AddPNG.h"
#include "res/Add2PNG.h"
#include "res/ExtractPNG.h"
#include "res/Extract2PNG.h"
#include "res/TestPNG.h"
#include "res/Test2PNG.h"
#include "res/CopyPNG.h"
#include "res/Copy2PNG.h"
#include "res/MovePNG.h"
#include "res/Move2PNG.h"
#include "res/DeletePNG.h"
#include "res/Delete2PNG.h"
#include "res/InfoPNG.h"
#include "res/Info2PNG.h"

#include "LangUtils.h"

#include <wx/mstream.h>
#define wxGetBitmapFromMemory(name) _wxGetBitmapFromMemory(name ## _png, sizeof(name ## _png))

static inline wxBitmap _wxGetBitmapFromMemory(const unsigned char *data, int length) {
	wxMemoryInputStream is(data, length);
	return wxBitmap(wxImage(is, wxBITMAP_TYPE_ANY, -1), -1);
}

///////////////////////////////////// SevenZipPanel.h /////////////////////

#include <wx/listctrl.h>

typedef wxListCtrl CExplorerListCtrl;

class MyFrame;
class myToolBar;

class SevenZipPanel : public wxPanel
{
	static int count;
	
	MyFrame *m_frame;
	
	CExplorerListCtrl *m_pListCtrlExplorer;
	NWindows::NControl::CWindow2 *_wList;
	

	wxBitmapButton *m_pBmpButtonParentFolder;
	wxComboBox *m_pComboBoxPath;
	wxStatusBar *m_pStatusBar;

	wxImageList imgList;

	int _panelIndex;

	// wxString m_currentDirectory;

	// int m_nbDirs;

	// wxString m_prompt;

public:
	SevenZipPanel(MyFrame *frame, wxWindow *parent,int id,int panelIndex);

	void registerWindow2(NWindows::NControl::CWindow2 *w)
	{
		_wList = w;
		_wList->OnMessage(WM_CREATE,0,0);
	}
		
	void OnAnyButton( wxCommandEvent &event );
	void OnSelected(wxListEvent& event);
	void OnDeselected(wxListEvent& event);
	void OnActivated(wxListEvent& event);
	void OnFocused(wxListEvent& event);
	void OnLeftDownBeginDrag(wxListEvent& event);
	void OnRightClick(wxListEvent& event);
	void OnColumnClick(wxListEvent& event);

	void OnLeftDown(wxMouseEvent &event );
	void OnRightDown(wxMouseEvent &event );
	
	void OnTextEnter(wxCommandEvent& event);
	
        void WriteText(const wxString& text) {
		printf("DEBUG : %ls",(const wchar_t *)text);
        }

	/* Don't work ...
	void OnCloseWindow(wxCloseEvent& WXUNUSED(event)) {
		_wList->OnDestroy();
	}
	*/

	void evt_destroy() {
		_wList->OnDestroy();
	}


private:
		DECLARE_EVENT_TABLE()
};



///////////////////////////////////// SevenZipPanel.h /////////////////////



class MyFrame: public wxFrame
{
public:
    // ctor
 MyFrame(void (*fct)(HWND),wxFrame *frame, const wxString& title, int x, int y, int w, int h);
    // virtual ~MyFrame();

	void registerWindow2(int baseID,NWindows::NControl::CWindow2 *w)
	{
		printf("MyFrame::registerWindow2(%d,%p)\n",baseID,w);
		switch (baseID)
		{
			case BASE_ID_PANEL_1: _panel1->registerWindow2(w); break;
			case BASE_ID_PANEL_2: _panel2->registerWindow2(w); break;
			default: printf("FIXME - MyFrame::registerWindow2\n");
		}
	}

	void PopulateToolbar(wxToolBar* toolBar);
	void RecreateToolbar();


protected:
	// callbacks
	void OnWorkerEvent(wxCommandEvent& event);
	void OnAnyMenu(wxCommandEvent& event)
	{
		extern bool OnMenuCommand(HWND hWnd, int id);
		extern void ExecuteCommand(UINT commandID);

		int wmId = event.GetId();

		if (wmId >= kMenuCmdID_Toolbar_Start && wmId < kMenuCmdID_Toolbar_End)
		{
			ExecuteCommand(wmId);
			return ; // 0;
		}
		OnMenuCommand(this, wmId);
	}
	void OnCloseWindow(wxCloseEvent& WXUNUSED(event))
	{
		if (_panel1) _panel1->evt_destroy();
		if (_panel2) _panel2->evt_destroy();

		extern void main_WM_DESTROY();
		main_WM_DESTROY();
		Destroy();
	}
private:
	SevenZipPanel * _panel1;
	SevenZipPanel * _panel2;
	myToolBar     * m_toolBar;
    DECLARE_EVENT_TABLE()
};

BEGIN_EVENT_TABLE(MyFrame, wxFrame)
	EVT_MENU(WORKER_EVENT, MyFrame::OnWorkerEvent)
	EVT_MENU(wxID_ANY, MyFrame::OnAnyMenu)
	EVT_CLOSE(MyFrame::OnCloseWindow)
END_EVENT_TABLE()


static bool TEST_create(HWND hWnd) // FIXME
{
extern HWND g_HWND;
	 CMyListView _listView;
	
	int _baseID = 1000;
	
	HWND w = NWindows::GetDlgItem(g_HWND, _baseID + 1);
	if (w == 0) 
	{
		printf("Can't find id=%d\n",_baseID + 1);
		return false;
	}
	printf("CPanel::OnCreate : _listView.Attach(%p)\n",w);
	_listView.Attach(w);
	
	_listView.SetRedraw(false);
	
	_listView.DeleteAllItems();
	
	_listView.DeleteColumn(1);
		
	_listView.InsertColumn(0, L"toto", 100);
	
//	_listView.SetItemCount(1);

	_listView.InsertItem(0, L"item 1");


	_listView.SetRedraw(true);	
	
	return true;
}

// My frame constructor
MyFrame::MyFrame(void (*wm_create)(HWND),wxFrame *frame, const wxString& title,
                 int x, int y, int w, int h)
       : wxFrame(frame, wxID_ANY, title, wxPoint(x, y), wxSize(w, h))
{
printf("===MyFrame::MyFrame===BEGIN===\n");

	m_toolBar = 0;

	this->SetIcon(wxICON(p7zip_32));
	
	g_HWND = this; // FIXME
	
	SetMinSize(wxSize(800,700));
	
	wxBoxSizer *topsizer = new wxBoxSizer( wxVERTICAL );

	_panel1 = new SevenZipPanel(this,this,BASE_ID_PANEL_1,0);  // FIXME panelIndex = 0
	_panel2 = 0;
	topsizer->Add(
		_panel1,
		1,            // make vertically stretchable
		wxEXPAND |    // make horizontally stretchable
		wxALL,        //   and make border all around
		0 );         // set border width to 10

	// Create the toolbar
	// FIXME RecreateToolbar();
printf("===MyFrame::MyFrame===WM_CREATE===\n");
	wm_create(this);
// FIXME	TEST_create(this);
	
	
       // Create the toolbar // FIXME
	RecreateToolbar();


printf("===MyFrame::MyFrame===SIZER===\n");

	SetSizer( topsizer );      // use the sizer for layout

	topsizer->SetSizeHints( this );   // set size hints to honour minimum size
printf("===MyFrame::MyFrame===END===\n");
}

void myCreateHandle(int n);
void MyFrame::OnWorkerEvent(wxCommandEvent& event)
{
	int n = event.GetInt();
// printf(" MyFrame::OnWorkerEvent(n=%d)\n",n);
	myCreateHandle(n);
}

wxWindow * g_window=0;
HWND myCreateAndShowMainWindow(LPCTSTR title,void (*fct)(HWND))
{
   MyFrame *frame = new MyFrame(fct,(wxFrame *)NULL, title, 40, 40, 800, 600);

   g_window = frame; 
  
   // Don't Show the frame !
   frame->Show(true); // FIXME

   // FIXME : SetTopWindow(g_HWND);

   return frame;
}


class myToolBar
{
	wxToolBar * m_toolbar;

	bool m_bShowText;

public:
	myToolBar(wxToolBar * toolbar,bool bShowText ) : m_toolbar(toolbar), m_bShowText(bShowText)  { }

	myToolBar* AddTool(int toolId, const wxString& label, const wxBitmap& bitmap1, const wxString& shortHelpString = _T(""), wxItemKind kind = wxITEM_NORMAL)
	{
		wxString text = wxEmptyString;
		if (m_bShowText) text = label;

		wxSize tb_size = m_toolbar->GetToolBitmapSize();
		int tb_witdh = tb_size.GetWidth();
		int tb_height = tb_size.GetHeight();

		if ((bitmap1.GetWidth() > tb_witdh) || ( bitmap1.GetHeight()> tb_height))
		{
			wxBitmap bmp(bitmap1.ConvertToImage().Scale(tb_witdh, tb_height));
			m_toolbar->AddTool(toolId,text,bmp,shortHelpString,kind);
		}
		else
		{
			m_toolbar->AddTool(toolId,text,bitmap1,shortHelpString,kind);
		}

		return this;
	}

	void SetToolBitmapSize(const wxSize& size)
	{
		m_toolbar->SetToolBitmapSize(size);
	}

	bool Realize()
	{
		return m_toolbar->Realize();
	}

	void AddSeparator() { m_toolbar->AddSeparator(); }
};

void MyFrame::PopulateToolbar(wxToolBar* p_toolBar)
{/*
 toolBar->AddTool(wxID_NEW, _T("New"),toolBarBitmaps[Tool_new], wxNullBitmap, wxITEM_NORMAL,
 _T("New file"), _T("This is help for new file tool"));
 */
   m_toolBar = new myToolBar(p_toolBar,true);

   const int kWidth  = 24;
   const int kHeight = 24;

	
   UString msg;

   // FIXME toolBar->SetToolBitmapSize(wxSize(24,24));
   m_toolBar->SetToolBitmapSize(wxSize(kWidth,kHeight));

    msg = LangString(IDS_ADD); //  kMenuCmdID_Toolbar_Add,     IDB_ADD,     IDB_ADD2,     IDS_ADD },
    if (msg == L"") msg = L"Add";
	m_toolBar->AddTool(kMenuCmdID_Toolbar_Add, (const wchar_t *)msg, wxGetBitmapFromMemory(ADD2));
	
    msg = LangString(IDS_EXTRACT); // { kMenuCmdID_Toolbar_Extract, IDB_EXTRACT, IDB_EXTRACT2, IDS_EXTRACT },
    if (msg == L"") msg = L"Extract";	
	m_toolBar->AddTool(kMenuCmdID_Toolbar_Extract,(const wchar_t *)msg, wxGetBitmapFromMemory(EXTRACT2));
	
    msg = LangString(IDS_TEST); // { kMenuCmdID_Toolbar_Test,    IDB_TEST,    IDB_TEST2,    IDS_TEST }
    if (msg == L"") msg = L"Test";	
   m_toolBar->AddTool(kMenuCmdID_Toolbar_Test,(const wchar_t *)msg, wxGetBitmapFromMemory(TEST2));
	
   m_toolBar->AddSeparator();

    msg = LangString(IDS_BUTTON_COPY); // { IDM_COPY_TO,    IDB_COPY,   IDB_COPY2,   IDS_BUTTON_COPY },
    if (msg == L"") msg = L"Copy";		
    m_toolBar->AddTool(IDS_BUTTON_COPY, (const wchar_t *)msg, wxGetBitmapFromMemory(COPY2));
	
    msg = LangString(IDS_BUTTON_MOVE); // { IDM_MOVE_TO,    IDB_MOVE,   IDB_MOVE2,   IDS_BUTTON_MOVE }
    if (msg == L"") msg = L"Move";		
    m_toolBar->AddTool(IDM_MOVE_TO, (const wchar_t *)msg, wxGetBitmapFromMemory(MOVE2));
	
    msg = LangString(IDS_BUTTON_DELETE); // { IDM_DELETE,     IDB_DELETE, IDB_DELETE2, IDS_BUTTON_DELETE } ,
    if (msg == L"") msg = L"Delete";	
    m_toolBar->AddTool(IDM_DELETE, (const wchar_t *)msg, wxGetBitmapFromMemory(DELETE2));
	
    msg = LangString(IDS_BUTTON_INFO); // { IDM_PROPERTIES, IDB_INFO,   IDB_INFO2,   IDS_BUTTON_INFO }
    if (msg == L"") msg = L"Info";	
    m_toolBar->AddTool(IDM_PROPERTIES, (const wchar_t *)msg, wxGetBitmapFromMemory(INFO2));

#if 0
	////////////////////////////////////////////////////////

	/* FIXME
	if (g_mimeDatabase)
	{
		toolBar.AddSeparator();

		TryMime(&toolBar, _T("txt"));
		TryMime(&toolBar, _T("rar"));
		TryMime(&toolBar, _T("7z"));
	}

	toolBar.AddSeparator();

	wxIcon i_plus = wxArtProvider::GetIcon(wxART_ADD_BOOKMARK    , wxART_TOOLBAR  , wxSize(kWidth,kHeight));
	m_toolBar->AddTool(wxID_ANY, wxT("Add Bookmark"), i_plus);

	wxIcon i_go_up_dir = wxArtProvider::GetIcon(wxART_GO_DIR_UP   , wxART_TOOLBAR  , wxSize(kWidth,kHeight));
	m_toolBar->AddTool(wxID_ANY, wxT("Go up dir"), i_go_up_dir);

	wxIcon i_folder = wxArtProvider::GetIcon(wxART_FOLDER   , wxART_TOOLBAR  , wxSize(kWidth,kHeight));
	m_toolBar->AddTool(wxID_ANY, wxT("Folder"), i_folder);

	wxIcon i_missing_image = wxArtProvider::GetIcon(wxART_MISSING_IMAGE   , wxART_TOOLBAR  , wxSize(kWidth,kHeight));
	m_toolBar->AddTool(wxID_ANY, wxT("missing image"), i_missing_image);
	*/

	///////////////////////////////////////////////////////

#endif
	m_toolBar->Realize();

	// toolBar->SetRows(!(toolBar->IsVertical()) ? m_rows : 10 / m_rows);
}
void MyFrame::RecreateToolbar()
{
	// delete and recreate the toolbar
	wxToolBar *toolBar = GetToolBar();
	// long style = toolBar ? toolBar->GetWindowStyle() : TOOLBAR_STYLE;

	SetToolBar(NULL);

	delete toolBar;
	/*
	style &= ~(wxTB_HORIZONTAL | wxTB_VERTICAL | wxTB_BOTTOM | wxTB_RIGHT | wxTB_HORZ_LAYOUT);
	switch( m_toolbarPosition )
	{
	case TOOLBAR_LEFT:style |= wxTB_LEFT; break;
	case TOOLBAR_TOP: style |= wxTB_TOP;break;
	case TOOLBAR_RIGHT:style |= wxTB_RIGHT;break;
	case TOOLBAR_BOTTOM:style |= wxTB_BOTTOM;break;
	}
	*/
	long style = wxTB_FLAT | wxTB_NODIVIDER | wxTB_TEXT; // TOOLBAR_STYLE | wxTB_TOP;
	/*
	if ( m_showTooltips ) style &= ~wxTB_NO_TOOLTIPS;
	else                  style |= wxTB_NO_TOOLTIPS;

	if ( style & wxTB_TEXT && !(style & wxTB_NOICONS) && m_horzText ) style |= wxTB_HORZ_LAYOUT;
	*/
	toolBar = CreateToolBar(style, wxID_ANY);

	PopulateToolbar(toolBar);
}

void registerWindow2(int baseID,NWindows::NControl::CWindow2 *w)
{
	MyFrame * f = (MyFrame *) g_HWND;
	f->registerWindow2(baseID,w);
	
}


/////////////////////////////////////////////////////////
#include "LangUtils.h"

static const UINT kOpenBookmarkMenuID = 730;  // FIXME / duplicate
static const UINT kSetBookmarkMenuID = 740;


void rc_MyLoadMenu(HWND hWnd)
{
	wxFrame *hwnd = (wxFrame *)hWnd;
	wxMenu *m;
	wxMenu *m_file = m = new wxMenu;
	{
		m->Append(IDM_OPEN, _T("&Open"));  // FIXME "&Open\tEnter" - don't use Enter to support combobox enter ...
		m->Append(IDM_OPEN_INSIDE,_T("Open &Inside\tCtrl+PgDn"));
		m->Append(IDM_OPEN_INSIDE_ONE,_T("Open Inside *"));
		m->Append(IDM_OPEN_INSIDE_PARSER,_T("Open Inside #"));
		m->Append(IDM_OPEN_OUTSIDE,_T("Open O&utside\tShift+Enter"));
		m->Append(IDM_FILE_VIEW,_T("&View\tF3"));
		m->Append(IDM_FILE_EDIT,_T("&Edit\tF4"));
		m->AppendSeparator();
		m->Append(IDM_RENAME,_T("Rena&me\tF2"));
		m->Append(IDM_COPY_TO,_T("&Copy To...\tF5"));
		m->Append(IDM_MOVE_TO,_T("&Move To...\tF6"));
		m->Append(IDM_DELETE,_T("&Delete\tDel"));
		m->AppendSeparator();
		m->Append(IDM_SPLIT,_T("&Split file..."));
		m->Append(IDM_COMBINE,_T("Com&bine files..."));
		m->AppendSeparator();
		m->Append(IDM_PROPERTIES,_T("P&roperties\tAlt+Enter"));
		m->Append(IDM_COMMENT,_T("Comme&nt\tCtrl+Z"));

		wxMenu * subMenuCRC = new wxMenu;
		subMenuCRC->Append(IDM_CRC32   ,_T("CRC-32"));
		subMenuCRC->Append(IDM_CRC64   ,_T("CRC-64"));
		subMenuCRC->Append(IDM_SHA1    ,_T("SHA-1"));
		subMenuCRC->Append(IDM_SHA256  ,_T("SHA-256"));
		subMenuCRC->Append(IDM_HASH_ALL,_T("*"));
		m->AppendSubMenu(subMenuCRC,_T("CRC"));

		m->Append(IDM_DIFF,_T("Di&ff"));
		m->AppendSeparator();
		m->Append(IDM_CREATE_FOLDER,_T("Create Folder\tF7"));
		m->Append(IDM_CREATE_FILE,_T("Create File\tCtrl+N"));
		m->AppendSeparator();
		m->Append(IDEXIT,_T("E&xit\tAlt+F4"));   
	}
	wxMenu *m_edit = m = new wxMenu;
	{
		// m->Append(IDM_EDIT_CUT, _T("Cu&t\tCtrl+X"))->Enable(true);              // GRAYED
		// m->Append(IDM_EDIT_COPY, _T("&Copy\tCtrl+C"))->Enable(true);            // GRAYED
		// m->Append(IDM_EDIT_PASTE, _T("&Paste\tCtrl+V"))->Enable(true);          // GRAYED
		// m->AppendSeparator();
		m->Append(IDM_SELECT_ALL, _T("Select &All\tShift+[Grey +]")); 
		m->Append(IDM_DESELECT_ALL, _T("Deselect All\tShift+[Grey -]")); 
		m->Append(IDM_INVERT_SELECTION, _T("&Invert Selection\tGrey *"));   
		m->Append(IDM_SELECT, _T("Select...\tGrey +"));           
		m->Append(IDM_DESELECT, _T("Deselect...\tGrey -"));        
// FIXME		m->Append(IDM_SELECT_BY_TYPE, _T("Select by Type\tAlt+[Grey+]")); 
// FIXME		m->Append(IDM_DESELECT_BY_TYPE, _T("Deselect by Type\tAlt+[Grey -]")); 
	}
	wxMenu *m_view = m = new wxMenu;
	{
/*
		m->AppendRadioItem(IDM_VIEW_LARGE_ICONS, _T("Lar&ge Icons\tCtrl+1"));        
		m->AppendRadioItem(IDM_VIEW_SMALL_ICONS, _T("S&mall Icons\tCtrl+2"));      
		m->AppendRadioItem(IDM_VIEW_LIST, _T("&List\tCtrl+3"));             
		m->AppendRadioItem(IDM_VIEW_DETAILS, _T("&Details\tCtrl+4"))->Check(true);  // CHECKED
		m->AppendSeparator();
		m->Append(IDM_VIEW_ARANGE_BY_NAME, _T("Name\tCtrl+F3"));               
		m->Append(IDM_VIEW_ARANGE_BY_TYPE, _T("Type\tCtrl+F4"));             
		m->Append(IDM_VIEW_ARANGE_BY_DATE, _T("Date\tCtrl+F5"));             
		m->Append(IDM_VIEW_ARANGE_BY_SIZE, _T("Size\tCtrl+F6"));               
		m->Append(IDM_VIEW_ARANGE_NO_SORT, _T("Unsorted\tCtrl+F7"));           
		m->AppendSeparator();
		m->AppendCheckItem(IDM_VIEW_FLAT_VIEW, _T("Flat View"));                   
		m->AppendCheckItem(IDM_VIEW_TWO_PANELS, _T("&2 Panels\tF9")); 

		{
			wxMenu* subMenu = new wxMenu;
			subMenu->AppendCheckItem(IDM_VIEW_ARCHIVE_TOOLBAR, _T("Archive Toolbar"));            
			subMenu->AppendCheckItem(IDM_VIEW_STANDARD_TOOLBAR, _T("Standard Toolbar"));            
			subMenu->AppendSeparator();
			subMenu->AppendCheckItem(IDM_VIEW_TOOLBARS_LARGE_BUTTONS, _T("Large Buttons"));               
			subMenu->AppendCheckItem(IDM_VIEW_TOOLBARS_SHOW_BUTTONS_TEXT, _T("Show Buttons Text"));           
			m->Append(12112, _T("Toolbars"), subMenu); // FIXME ID ?
		}
		m->AppendSeparator();
*/
		// NO "/" is used on Unix Path ... m->Append(IDM_OPEN_ROOT_FOLDER, _T("Open Root Folder\t" STRING_PATH_SEPARATOR));        
		m->Append(IDM_OPEN_ROOT_FOLDER, _T("Open Root Folder\t" "\\"));        

		m->Append(IDM_OPEN_PARENT_FOLDER, _T("Up One Level\tBackspace"));
		m->Append(IDM_FOLDERS_HISTORY, _T("Folders History...\tAlt+F12"));
		m->AppendSeparator();
		m->Append(IDM_VIEW_REFRESH, _T("&Refresh\tCtrl+R"));
	}
	wxMenu *m_favorites = m = new wxMenu;
	{
		{
			wxMenu* subMenu = new wxMenu;
			for (int i = 0; i < 10; i++)
			{
				UString s = LangString(IDS_BOOKMARK);
				s += L" ";
				wchar_t c = (wchar_t)(L'0' + i);
				s += c;
				s += L"\tAlt+Shift+";
				s += c;
				subMenu->Append( kSetBookmarkMenuID + i, wxString(s));
			}

			m->Append(12111, _T("&Add folder to Favorites as"), subMenu); // FIXME ID ?
		}
		m->AppendSeparator();
		for (int i = 0; i < 10; i++)
		{
      UString s = g_App.AppState.FastFolders.GetString(i);
      const int kMaxSize = 100;
      const int kFirstPartSize = kMaxSize / 2;
      if (s.Len() > kMaxSize)
      {
        s.Delete(kFirstPartSize, s.Len() - kMaxSize);
        s.Insert(kFirstPartSize, L" ... ");
      }
      if (s.IsEmpty())
        s = L'-';
      s += L"\tAlt+";
      s += (wchar_t)(L'0' + i);
			// menu.AppendItem(MF_STRING, kOpenBookmarkMenuID + i, s);
			m->Append( kOpenBookmarkMenuID + i, wxString(s));
		}
		
	}
	wxMenu *m_tools = m = new wxMenu;
	{
//		m->Append(IDM_OPTIONS, _T("&Options..."));
		m->Append(IDM_BENCHMARK, _T("&Benchmark"));
	}
	wxMenu *m_help = m = new wxMenu;
	{
		m->Append(IDM_HELP_CONTENTS, _T("&Contents...\tF1"));
		m->AppendSeparator();
		m->Append(IDM_ABOUT, _T("&About 7-Zip..."));
	}

	wxMenuBar *menuBar = new wxMenuBar;

	menuBar->Append(m_file, _T("&File"));
	menuBar->Append(m_edit, _T("&Edit"));
	menuBar->Append(m_view, _T("&View"));
	menuBar->Append(m_favorites, _T("F&avorites"));
	menuBar->Append(m_tools, _T("&Tools"));
	menuBar->Append(m_help, _T("&Help"));
	hwnd->SetMenuBar(menuBar);
}

//////////////////////////////////////////////////////////////////


static CStringTable g_stringTable[] =
{
  /* resource.rc */	  
  /***************/
  { IDS_BOOKMARK   ,L"Bookmark" },

  { IDS_OPTIONS    ,L"Options" },

  { IDS_N_SELECTED_ITEMS  ,L"{0} object(s) selected" },

  { IDS_FILE_EXIST  ,L"File {0} is already exist" },
  { IDS_WANT_UPDATE_MODIFIED_FILE  ,L"File '{0}' was modified.\nDo you want to update it in the archive?" },
  { IDS_CANNOT_UPDATE_FILE   ,L"Can not update file\n'{0}'" },
  { IDS_CANNOT_START_EDITOR  ,L"Cannot start editor." },
  { IDS_VIRUS                ,L"The file looks like a virus (the file name contains long spaces in name)." },
  { IDS_MESSAGE_UNSUPPORTED_OPERATION_FOR_LONG_PATH_FOLDER ,L"The operation cannot be called from a folder that has a long path." },
  { IDS_SELECT_ONE_FILE      ,L"You must select one file" },
  { IDS_SELECT_FILES         ,L"You must select one or more files" },
  { IDS_TOO_MANY_ITEMS       ,L"Too many items" },

  { IDS_COPY      ,L"Copy" },
  { IDS_MOVE      ,L"Move" },
  { IDS_COPY_TO   ,L"Copy to:" },
  { IDS_MOVE_TO   ,L"Move to:" },
  { IDS_COPYING   ,L"Copying..." },
  { IDS_MOVING    ,L"Moving..." },
  { IDS_RENAMING  ,L"Renaming..." },

  { IDS_OPERATION_IS_NOT_SUPPORTED  ,L"Operation is not supported." },
  { IDS_ERROR_RENAMING  ,L"Error Renaming File or Folder" },
  { IDS_CONFIRM_FILE_COPY   ,L"Confirm File Copy" },
  { IDS_WANT_TO_COPY_FILES  ,L"Are you sure you want to copy files to archive" },

  { IDS_CONFIRM_FILE_DELETE   ,L"Confirm File Delete" },
  { IDS_CONFIRM_FOLDER_DELETE ,L"Confirm Folder Delete" },
  { IDS_CONFIRM_ITEMS_DELETE  ,L"Confirm Multiple File Delete" },
  { IDS_WANT_TO_DELETE_FILE   ,L"Are you sure you want to delete '{0}'?" },
  { IDS_WANT_TO_DELETE_FOLDER ,L"Are you sure you want to delete the folder '{0}' and all its contents?" },
  { IDS_WANT_TO_DELETE_ITEMS  ,L"Are you sure you want to delete these {0} items?" },
  { IDS_DELETING              ,L"Deleting..." },
  { IDS_ERROR_DELETING        ,L"Error Deleting File or Folder" },
  { IDS_ERROR_LONG_PATH_TO_RECYCLE  ,L"The system cannot move a file with long path to the Recycle Bin" },
  
  { IDS_CREATE_FOLDER       ,L"Create Folder" },
  { IDS_CREATE_FILE         ,L"Create File" },
  { IDS_CREATE_FOLDER_NAME  ,L"Folder name:" },
  { IDS_CREATE_FILE_NAME    ,L"File Name:" },
  { IDS_CREATE_FOLDER_DEFAULT_NAME  ,L"New Folder" },
  { IDS_CREATE_FILE_DEFAULT_NAME    ,L"New File" },
  { IDS_CREATE_FOLDER_ERROR ,L"Error Creating Folder" },
  { IDS_CREATE_FILE_ERROR   ,L"Error Creating File" },

  { IDS_COMMENT      ,L"Comment" },
  { IDS_COMMENT2     ,L"&Comment:" },
  { IDS_SELECT       ,L"Select" },
  { IDS_DESELECT     ,L"Deselect" },
  { IDS_SELECT_MASK  ,L"Mask:" },

  { IDS_PROPERTIES   ,L"Properties" },
  { IDS_FOLDERS_HISTORY  ,L"Folders History" },

  { IDS_COMPUTER   ,L"Computer" },
  { IDS_NETWORK    ,L"Network" },
  { IDS_DOCUMENTS  ,L"Documents" },
  { IDS_SYSTEM     ,L"System" },

  { IDS_ADD            ,L"Add" },
  { IDS_EXTRACT        ,L"Extract" },
  { IDS_TEST           ,L"Test" },
  { IDS_BUTTON_COPY    ,L"Copy" },
  { IDS_BUTTON_MOVE    ,L"Move" },
  { IDS_BUTTON_DELETE  ,L"Delete" },
  { IDS_BUTTON_INFO    ,L"Info" },

  { IDS_SPLITTING            ,L"Splitting..." },
  { IDS_SPLIT_CONFIRM_TITLE  ,L"Confirm Splitting" },
  { IDS_SPLIT_CONFIRM_MESSAGE  ,L"Are you sure you want to split file into {0} volumes?" },
  { IDS_SPLIT_VOL_MUST_BE_SMALLER ,L"Volume size must be smaller than size of original file" },

  { IDS_COMBINE     ,L"Combine Files" },
  { IDS_COMBINE_TO  ,L"&Combine to:" },
  { IDS_COMBINING   ,L"Combining..." },
  { IDS_COMBINE_SELECT_ONE_FILE  ,L"Select only first part of split file" },
  { IDS_COMBINE_CANT_DETECT_SPLIT_FILE  ,L"Can not detect file as split file" },
  { IDS_COMBINE_CANT_FIND_MORE_THAN_ONE_PART  ,L"Can not find more than one part of split file" },



  /* PropertyName.rc */	  
  /*******************/
  { IDS_PROP_PATH         ,L"Path" },
  { IDS_PROP_NAME         ,L"Name" },
  { IDS_PROP_EXTENSION    ,L"Extension" },
  { IDS_PROP_IS_FOLDER    ,L"Folder" },
  { IDS_PROP_SIZE         ,L"Size" },
  { IDS_PROP_PACKED_SIZE  ,L"Packed Size" },
  { IDS_PROP_ATTRIBUTES   ,L"Attributes" },
  { IDS_PROP_CTIME        ,L"Created" },
  { IDS_PROP_ATIME        ,L"Accessed" },
  { IDS_PROP_MTIME        ,L"Modified" },
  { IDS_PROP_SOLID        ,L"Solid" },
  { IDS_PROP_C0MMENTED    ,L"Commented" },
  { IDS_PROP_ENCRYPTED    ,L"Encrypted" },
  { IDS_PROP_SPLIT_BEFORE ,L"Split Before" },
  { IDS_PROP_SPLIT_AFTER  ,L"Split After" },
  { IDS_PROP_DICTIONARY_SIZE ,L"Dictionary" },
  { IDS_PROP_CRC          ,L"CRC" },
  { IDS_PROP_FILE_TYPE    ,L"Type" },
  { IDS_PROP_ANTI         ,L"Anti" },
  { IDS_PROP_METHOD       ,L"Method" },
  { IDS_PROP_HOST_OS      ,L"Host OS" },
  { IDS_PROP_FILE_SYSTEM  ,L"File System" },
  { IDS_PROP_USER         ,L"User" },
  { IDS_PROP_GROUP        ,L"Group" },
  { IDS_PROP_BLOCK        ,L"Block" },
  { IDS_PROP_COMMENT      ,L"Comment" },
  { IDS_PROP_POSITION     ,L"Position" },
  { IDS_PROP_PREFIX       ,L"Path Prefix" },
  { IDS_PROP_FOLDERS      ,L"Folders" },
  { IDS_PROP_FILES        ,L"Files" },
  { IDS_PROP_VERSION      ,L"Version" },
  { IDS_PROP_VOLUME       ,L"Volume" },
  { IDS_PROP_IS_VOLUME    ,L"Multivolume" },
  { IDS_PROP_OFFSET       ,L"Offset" },
  { IDS_PROP_LINKS        ,L"Links" },
  { IDS_PROP_NUM_BLOCKS   ,L"Blocks" },
  { IDS_PROP_NUM_VOLUMES  ,L"Volumes" },

  { IDS_PROP_BIT64        ,L"64-bit" },
  { IDS_PROP_BIG_ENDIAN   ,L"Big-endian" },
  { IDS_PROP_CPU          ,L"CPU" },
  { IDS_PROP_PHY_SIZE     ,L"Physical Size" },
  { IDS_PROP_HEADERS_SIZE ,L"Headers Size" },
  { IDS_PROP_CHECKSUM     ,L"Checksum" },
  { IDS_PROP_CHARACTS     ,L"Characteristics" },
  { IDS_PROP_VA           ,L"Virtual Address" },
  { IDS_PROP_ID           ,L"ID" },
  { IDS_PROP_SHORT_NAME   ,L"Short Name" },
  { IDS_PROP_CREATOR_APP  ,L"Creator Application" },
  { IDS_PROP_SECTOR_SIZE  ,L"Sector Size" },
  { IDS_PROP_POSIX_ATTRIB ,L"Mode" },
  { IDS_PROP_SYM_LINK     ,L"Symbolic Link" },
  { IDS_PROP_ERROR        ,L"Error" },
  { IDS_PROP_TOTAL_SIZE   ,L"Total Size" },
  { IDS_PROP_FREE_SPACE   ,L"Free Space" },
  { IDS_PROP_CLUSTER_SIZE ,L"Cluster Size" },
  { IDS_PROP_VOLUME_NAME  ,L"Label" },
  { IDS_PROP_LOCAL_NAME   ,L"Local Name" },
  { IDS_PROP_PROVIDER     ,L"Provider" },
  { IDS_PROP_NT_SECURITY  ,L"NT Security" },
  { IDS_PROP_ALT_STREAM   ,L"Alternate Stream" },
  { IDS_PROP_AUX          ,L"Aux" },
  { IDS_PROP_DELETED      ,L"Deleted" },
  { IDS_PROP_IS_TREE      ,L"Is Tree" },
  { IDS_PROP_SHA1         ,L"SHA-1" },
  { IDS_PROP_SHA256       ,L"SHA-256" },
  { IDS_PROP_ERROR_TYPE   ,L"Error Type" },
  { IDS_PROP_NUM_ERRORS   ,L"Errors" },
  { IDS_PROP_ERROR_FLAGS  ,L"Errors" },
  { IDS_PROP_WARNING_FLAGS ,L"Warnings" },
  { IDS_PROP_WARNING      ,L"Warning" },
  { IDS_PROP_NUM_STREAMS  ,L"Streams" },
  { IDS_PROP_NUM_ALT_STREAMS ,L"Alternate Streams" },
  { IDS_PROP_ALT_STREAMS_SIZE ,L"Alternate Streams Size" },
  { IDS_PROP_VIRTUAL_SIZE ,L"Virtual Size" },
  { IDS_PROP_UNPACK_SIZE  ,L"Unpack Size" },
  { IDS_PROP_TOTAL_PHY_SIZE ,L"Total Physical Size" },
  { IDS_PROP_VOLUME_INDEX ,L"Volume Index" },
  { IDS_PROP_SUBTYPE      ,L"SubType" },
  { IDS_PROP_SHORT_COMMENT ,L"Short Comment" },
  { IDS_PROP_CODE_PAGE    ,L"Code Page" },
  { IDS_PROP_IS_NOT_ARC_TYPE  ,L"Is not archive type" },
  { IDS_PROP_PHY_SIZE_CANT_BE_DETECTED ,L"Physical Size can't be detected" },
  { IDS_PROP_ZEROS_TAIL_IS_ALLOWED ,L"Zeros Tail Is Allowed" },
  { IDS_PROP_TAIL_SIZE ,L"Tail Size" },
  { IDS_PROP_EMB_STUB_SIZE ,L"Embedded Stub Size" },
  { IDS_PROP_NT_REPARSE   ,L"Link" },
  { IDS_PROP_HARD_LINK    ,L"Hard Link" },
  { IDS_PROP_INODE        ,L"iNode" },
  { IDS_PROP_STREAM_ID    ,L"Stream ID" },
 
	{ 0 , 0 }
};

REGISTER_STRINGTABLE(g_stringTable)

/////////////////////////////////////////////////////

#include "res/ParentFolder.h"

	SevenZipPanel::SevenZipPanel(MyFrame *frame, wxWindow *parent,int id,int panelIndex) :
	       	wxPanel(parent,id) , m_frame(frame), _wList(0)
	{
		_panelIndex = panelIndex;

		int _baseID = id; // FIXME
		int _listID = _baseID + 1;
		int _comboBoxID = _baseID + 3;
		int _statusBarID = _comboBoxID + 1;
		int kParentFolderID = 100; // FIXME Panel.h


		///Sizer for adding the controls created by users
		wxBoxSizer* pMainSizer = new wxBoxSizer(wxVERTICAL);
		int sizes[] = {150, 250, 350, -1};
		wxArrayString pathArray;
		wxBoxSizer *pPathSizer = new wxBoxSizer(wxHORIZONTAL);
		m_pBmpButtonParentFolder = new wxBitmapButton(this, kParentFolderID, wxGetBitmapFromMemory(PARENT_FOLDER), wxDefaultPosition, wxDefaultSize, wxBU_AUTODRAW);
		m_pComboBoxPath = new wxComboBox(this, _comboBoxID, wxEmptyString, wxDefaultPosition, wxSize(300,-1), pathArray, wxCB_DROPDOWN | wxCB_SORT );
		pPathSizer->Add(m_pBmpButtonParentFolder, 0, wxALL|wxEXPAND, 0);
		pPathSizer->Add(m_pComboBoxPath, 1, wxALL|wxEXPAND, 5);

		m_pListCtrlExplorer = new CExplorerListCtrl(this,_listID,wxDefaultPosition, wxSize(300,300),
			wxLC_REPORT | // wxLC_EDIT_LABELS |   FIXME
			wxSUNKEN_BORDER);

		printf("DEBUG : new CExplorerListCtrl(id=%d) => %p\n",_listID,m_pListCtrlExplorer);

		m_pStatusBar = new wxStatusBar(this, _statusBarID);
		m_pStatusBar->SetFieldsCount(4, sizes);
		pMainSizer->Add(pPathSizer, 0, wxALL|wxEXPAND, 0);
		pMainSizer->Add(m_pListCtrlExplorer, 1, wxALL|wxEXPAND, 0);
		pMainSizer->Add(m_pStatusBar, 0, wxALL|wxEXPAND, 0);
		SetSizer(pMainSizer);
		SetAutoLayout (true);
		SetMinSize(wxSize(800,400));
		Layout();

		
		// m_pListCtrlExplorer->SetDropTarget(new DnDFile(this));

	}

	void SevenZipPanel::OnAnyButton( wxCommandEvent &event )
	{
		count++;

		int id = event.GetId();

		wxString msg = wxString::Format(_T("P %d : button %d \n"), count,id);

		WriteText(msg);

		_wList->OnMessage(WM_COMMAND , id , 0);
	}

	void SevenZipPanel::OnSelected(wxListEvent& event)
	{
		const wxListItem & item = event.GetItem();
		count++;

		wxString msg = wxString::Format(_T("P %d : OnSelected %d \n"), count,event.GetId());

		WriteText(msg);

		NMLISTVIEW info;
		info.hdr.hwndFrom = m_pListCtrlExplorer;
		info.hdr.code     = LVN_ITEMCHANGED;
		info.uOldState    = 0;
		info.uNewState    = LVIS_SELECTED;
		info.lParam       = item.GetData(); // event.GetIndex(); // FIXME ? event.GetData();
		_wList->OnMessage(WM_NOTIFY , event.GetId() , (LPARAM)&info);
		/*
		if ( GetWindowStyle() & wxLC_REPORT )
		{
		wxListItem info;
		info.m_itemId = event.m_itemIndex;
		info.m_col = 1;
		info.m_mask = wxLIST_MASK_TEXT;
		if ( GetItem(info) )
		{
		wxLogMessage(wxT("Value of the 2nd field of the selected item: %s"),
		info.m_text.c_str());
		}
		else
		{
		wxFAIL_MSG(wxT("wxListCtrl::GetItem() failed"));
		}
		}
		*/
	}

	

	void SevenZipPanel::OnDeselected(wxListEvent& event)
	{
		const wxListItem & item = event.GetItem();
		count++;
		wxString msg = wxString::Format(_T("P %d : OnDeselected %d \n"), count,event.GetId());
		WriteText(msg);

		NMLISTVIEW info;
		info.hdr.hwndFrom = m_pListCtrlExplorer;
		info.hdr.code     = LVN_ITEMCHANGED;
		info.uOldState    = LVIS_SELECTED;
		info.uNewState    = 0;
		info.lParam       = item.GetData(); // event.GetIndex(); // FIXME ? event.GetData();
		_wList->OnMessage(WM_NOTIFY , event.GetId() , (LPARAM)&info);
	}

	void SevenZipPanel::OnColumnClick(wxListEvent& event)
	{
		count++;
		wxString msg = wxString::Format(_T("P %d : OnColumnClick %d col=%d\n"), count,event.GetId(),event.GetColumn());
		WriteText(msg);

		NMLISTVIEW info;
		info.hdr.hwndFrom = m_pListCtrlExplorer;
		info.hdr.code     = LVN_COLUMNCLICK;
		info.iSubItem     = event.GetColumn();
		_wList->OnMessage(WM_NOTIFY , event.GetId() , (LPARAM)&info);

	}


	void SevenZipPanel::OnActivated(wxListEvent& event)
	{
		count++;

		int ind = event.GetIndex();

		NMHDR info;
		info.hwndFrom = m_pListCtrlExplorer;
		info.code     = NM_DBLCLK;
		_wList->OnMessage(WM_NOTIFY , event.GetId() , (LPARAM)&info);

		/*
		if ((ind >= 0) && ( ind < m_nbDirs))
		{
			wxString msg = wxString::Format(_T("P %d : OnActivated %d : DIR = %d\n"), count,event.GetId(),ind);
			WriteText(msg);

			wxString name = m_pListCtrlExplorer->GetItemText(ind);

			wxFileName filename (m_currentDirectory,name);
			BinPath(filename.GetFullPath());

		}
		else
		*/
		{
			wxString msg = wxString::Format(_T("P %d : OnActivated %d : FILE = %d\n"), count,event.GetId(),ind);
			WriteText(msg);
		}
	}

	void SevenZipPanel::OnFocused(wxListEvent& event)
	{
		count++;

		wxString msg = wxString::Format(_T("P %d : OnFocused %d \n"), count,event.GetId());

		WriteText(msg);

		event.Skip();
	}

	void SevenZipPanel::OnLeftDownBeginDrag(wxListEvent& event)
	{
		count++;

		wxString msg = wxString::Format(_T("P %d : OnLeftDownBeginDrag %d \n"), count,event.GetId());
		WriteText(msg);

#if 0		
		if (   m_pListCtrlExplorer->GetSelectedItemCount() < 1) return ;

        // start drag operation
        wxFileDataObject filesData;


	long item = -1;
    for ( ;; )
    {
        item = m_pListCtrlExplorer->GetNextItem(item,
                                     wxLIST_NEXT_ALL,
                                     wxLIST_STATE_SELECTED);
        if ( item == -1 )
            break;

        // this item is selected - do whatever is needed with it
        // wxLogMessage("Item %ld is selected.", item);
		wxString file = m_currentDirectory + _T("/") + m_pListCtrlExplorer->GetItemText(item);

		filesData.AddFile(file);

    }

		msg = wxString::Format(_T("P %d : wxDropSource %d \n"), count,event.GetId());
		WriteText(msg);

        wxDropSource source(filesData, this,
                            wxDROP_ICON(dnd_copy),
                            wxDROP_ICON(dnd_move),
                            wxDROP_ICON(dnd_none));

        int flags = 0;
		/*
        if ( m_moveByDefault )
            flags |= wxDrag_DefaultMove;
        else if ( m_moveAllow )
            flags |= wxDrag_AllowMove;
			*/
		flags |= wxDrag_AllowMove;

		msg = wxString::Format(_T("P %d : DoDragDrop %d \n"), count,event.GetId());
		WriteText(msg);


        wxDragResult result = source.DoDragDrop(flags);


        const wxChar *pc;
        switch ( result )
        {
            case wxDragError:   pc = _T("Error!");    break;
            case wxDragNone:    pc = _T("Nothing");   break;
            case wxDragCopy:    pc = _T("Copied");    break;
            case wxDragMove:    pc = _T("Moved");     break;
            case wxDragCancel:  pc = _T("Cancelled"); break;
            default:            pc = _T("Huh?");      break;
        }

        WriteText(wxString(_T("    Drag result: ")) + pc);
#endif
	}

void SevenZipPanel::OnLeftDown(wxMouseEvent &WXUNUSED(event) )
{
	WriteText(_T("OnLeftDown"));
#if 0
    if ( !m_strText.empty() )
    {
        // start drag operation
        wxTextDataObject textData(m_strText);
        wxDropSource source(textData, this,
                            wxDROP_ICON(dnd_copy),
                            wxDROP_ICON(dnd_move),
                            wxDROP_ICON(dnd_none));

        int flags = 0;
        if ( m_moveByDefault )
            flags |= wxDrag_DefaultMove;
        else if ( m_moveAllow )
            flags |= wxDrag_AllowMove;

        wxDragResult result = source.DoDragDrop(flags);

#if wxUSE_STATUSBAR
        const wxChar *pc;
        switch ( result )
        {
            case wxDragError:   pc = _T("Error!");    break;
            case wxDragNone:    pc = _T("Nothing");   break;
            case wxDragCopy:    pc = _T("Copied");    break;
            case wxDragMove:    pc = _T("Moved");     break;
            case wxDragCancel:  pc = _T("Cancelled"); break;
            default:            pc = _T("Huh?");      break;
        }

        SetStatusText(wxString(_T("Drag result: ")) + pc);
#else
        wxUnusedVar(result);
#endif // wxUSE_STATUSBAR
    }
#endif // wxUSE_DRAG_AND_DROP
}

void SevenZipPanel::OnRightClick(wxListEvent& event)
{
	wxPoint point = event.GetPoint(); 

	WriteText(_T("OnRightClick"));
    wxMenu menu; // (_T("Dnd sample menu"));

    menu.Append(wxID_ANY, _T("&Test drag..."));
    menu.AppendSeparator();
    menu.Append(wxID_ANY, _T("item1"));
    menu.Append(wxID_ANY, _T("item2"));
    menu.Append(wxID_ANY, _T("item3"));
    menu.Append(wxID_ANY, _T("&About"));

    PopupMenu( &menu, point.x, point.y );
}

void SevenZipPanel::OnTextEnter(wxCommandEvent& event)
{	
	count++;

	NMCBEENDEDITW info;
	info.hdr.hwndFrom = m_pComboBoxPath;
	info.hdr.code     = CBEN_ENDEDITW;
	info.iWhy         = CBENF_RETURN;
	
	_wList->OnMessage(WM_NOTIFY , event.GetId() , (LPARAM)&info);
	
	{
		wxString msg = wxString::Format(_T("P %d : OnTextEnter %d\n"), count,event.GetId());
		WriteText(msg);
	}
}

int SevenZipPanel::count = 0;

BEGIN_EVENT_TABLE(SevenZipPanel, wxPanel)
// EVT_MENU(wxID_ANY, SevenZipPanel::OnAnyMenu)
// EVT_LISTBOX   (wxID_ANY,       MyPanel::OnListBox)
// EVT_LISTBOX_DCLICK(wxID_ANY,          MyPanel::OnAnyListBoxDoubleClick)
EVT_BUTTON    (wxID_ANY,      SevenZipPanel::OnAnyButton)

 // EVT_CLOSE(SevenZipPanel::OnCloseWindow)

/////////////////
EVT_LIST_ITEM_SELECTED(wxID_ANY,   SevenZipPanel::OnSelected)
EVT_LIST_ITEM_DESELECTED(wxID_ANY, SevenZipPanel::OnDeselected)
EVT_LIST_ITEM_ACTIVATED(wxID_ANY,  SevenZipPanel::OnActivated)
EVT_LIST_ITEM_FOCUSED(wxID_ANY,  SevenZipPanel::OnFocused)

EVT_LIST_BEGIN_DRAG(wxID_ANY, SevenZipPanel::OnLeftDownBeginDrag)
// FIXME - add for menu on item - EVT_LIST_ITEM_RIGHT_CLICK(wxID_ANY, SevenZipPanel::OnRightClick)

EVT_LIST_COL_CLICK(wxID_ANY, SevenZipPanel::OnColumnClick)


EVT_TEXT_ENTER(wxID_ANY, SevenZipPanel::OnTextEnter)  // FIXME - not called


END_EVENT_TABLE()



void appClose(void)
{
	g_window->Close(true);
}
