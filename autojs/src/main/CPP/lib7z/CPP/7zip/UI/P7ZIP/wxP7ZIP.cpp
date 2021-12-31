// wxFM.cpp

#include "StdAfx.h"

// For compilers that support precompilation, includes "wx/wx.h".
#include "wx/wxprec.h"
 
#ifdef __BORLANDC__
    #pragma hdrstop
#endif

// for all others, include the necessary headers (this file is usually all you
// need because it includes almost all "standard" wxWidgets headers)
#ifndef WX_PRECOMP
    #include "wx/wx.h"
#endif

#include "wx/dnd.h"

#undef _WIN32
 
#ifdef __WXMAC__

#define UInt32 max_UInt32
#include <ApplicationServices/ApplicationServices.h>
#undef UInt32

#endif

#include "Common/StringConvert.h"
#include "Windows/FileDir.h"

#define NEED_NAME_WINDOWS_TO_UNIX
#include "myPrivate.h" // global_use_utf16_conversion

using namespace NWindows;
using namespace NFile;

///////////////////////////////////////////

static LPCWSTR kArchiveNoNameSwitch = L" -an";

HRESULT MyCreateProcess(const UString &params,LPCWSTR curDir, bool waitFinish)
{
	printf("MyCreateProcess: waitFinish=%d\n",(unsigned)waitFinish);
	printf("\tparams : %ls\n",(const wchar_t*)params);

	printf("\tcurDir : %ls\n",(const wchar_t*)curDir);
	
	wxString cmd(params);
	wxString memoCurDir = wxGetCwd();
	
	if (curDir) {  // FIXME
		wxSetWorkingDirectory(wxString(curDir));
	}
	
	printf("MyCreateProcess: cmd='%ls'\n",(const wchar_t *)cmd);
	long pid = 0;
	if (waitFinish) pid = wxExecute(cmd, wxEXEC_SYNC); // FIXME process never ends and stays zombie ...
	else            pid = wxExecute(cmd, wxEXEC_ASYNC);
	
	if (curDir) {
		wxSetWorkingDirectory(memoCurDir);
	}
	
	// FIXME if (pid == 0) return E_FAIL;
	
	return S_OK;
}



static HRESULT ExtractGroupCommand(const UStringVector &archivePaths,
								   const UString &params)
{
	UString params2 = params;
//	AddLagePagesSwitch(params2);
	params2 += kArchiveNoNameSwitch;

	
	char tempFile[256];
	static int count = 1000;
	
	sprintf(tempFile,"/tmp/7zExtract_%d_%d.tmp",(int)getpid(),count++);
	
	FILE * file = fopen(tempFile,"w");
	if (file)
	{
		for (int i = 0; i < archivePaths.Size(); i++) {
			fprintf(file,"%ls\n",(const wchar_t *)archivePaths[i]);
			printf(" TMP_%d : '%ls'\n",i,(const wchar_t *)archivePaths[i]);
		}
		
		fclose(file);
	}
	params2 += L" -ai@";
	params2 += GetUnicodeString(tempFile);
	printf("ExtractGroupCommand : -%ls-\n",(const wchar_t *)params2);
	HRESULT res = MyCreateProcess(params2, 0, true);
	printf("ExtractGroupCommand : END\n");
	
	remove(tempFile);
	
	return res;
}


HRESULT TestArchives(const UStringVector &archivePaths)
{
	UString params;
	params = L"/Users/me/P7ZIP/bin/7zG"; // Get7zGuiPath();
	params += L" t";
	return ExtractGroupCommand(archivePaths, params);
}

// Define a new application type, each program should derive a class from wxApp
class MyApp : public wxApp
{
public:
    virtual bool OnInit();
};

// Create a new application object: this macro will allow wxWidgets to create
// the application object during program execution (it's better than using a
// static object for many reasons) and also implements the accessor function
// wxGetApp() which will return the reference of the right type (i.e. MyApp and
// not wxApp)
IMPLEMENT_APP(MyApp)


class DnDFile : public wxFileDropTarget
{
public:
    DnDFile(wxListBox *pOwner) { m_pOwner = pOwner; }
	
    virtual bool OnDropFiles(wxCoord x, wxCoord y,
                             const wxArrayString& filenames);
	
private:
    wxListBox *m_pOwner;
};




bool DnDFile::OnDropFiles(wxCoord, wxCoord, const wxArrayString& filenames)
{
    size_t nFiles = filenames.GetCount();
    wxString str;
    str.Printf( _T("%d files dropped"), (int)nFiles);
    m_pOwner->Append(str);
	
	UStringVector archivePaths;
		
    for ( size_t n = 0; n < nFiles; n++ )
    {
       // m_pOwner->Append(filenames[n]);
/*		
        if (wxFile::Exists(filenames[n]))
            m_pOwner->Append(wxT("  This file exists.") );
        else
            m_pOwner->Append(wxT("  This file doesn't exist.") );
*/
		// cmd = cmd + _T(" \"") + filenames[n] + _T("\"");
		const wchar_t * wx = 	filenames[n].wc_str ();
		archivePaths.Add(wx);
    }
	
	/*
	m_pOwner->Append(cmd);
	

	long pid = 0;
	
	pid = wxExecute(cmd, wxEXEC_ASYNC);
	 */
	
	TestArchives(archivePaths);
	
    return true;
}

//////////////////

class DnDFrame : public wxFrame
{
public:
    DnDFrame(wxFrame *frame, const wxChar *title, int x, int y, int w, int h);
    virtual ~DnDFrame();
	
	 void OnQuit(wxCommandEvent& event);
	
    DECLARE_EVENT_TABLE()
	
// private:
public:
    // GUI controls
    wxListBox  *m_ctrlFile;
	
};

enum
{
    Menu_Quit = 1,
    Menu_Drag,
    Menu_DragMoveDef,
    Menu_DragMoveAllow,
    Menu_NewFrame,
    Menu_About = 101,
    Menu_OpenFile,
    Menu_Help,
    Menu_Clear,
    Menu_Copy,
    Menu_Paste,
    Menu_CopyBitmap,
    Menu_PasteBitmap,
    Menu_PasteMFile,
    Menu_CopyFiles,
    Menu_Shape_New = 500,
    Menu_Shape_Edit,
    Menu_Shape_Clear,
    Menu_ShapeClipboard_Copy,
    Menu_ShapeClipboard_Paste,
    Button_Colour = 1001
};

DnDFrame::DnDFrame(wxFrame *frame, const wxChar *title, int x, int y, int w, int h)
: wxFrame(frame, wxID_ANY, title, wxPoint(x, y), wxSize(w, h))
{
    // frame icon and status bar
    // FIXME SetIcon(wxICON(sample));

	

    // construct menu
    wxMenu *file_menu = new wxMenu;
    file_menu->Append(Menu_Drag, _T("&Test drag..."));
    file_menu->AppendCheckItem(Menu_DragMoveDef, _T("&Move by default"));
    file_menu->AppendCheckItem(Menu_DragMoveAllow, _T("&Allow moving"));
    file_menu->AppendSeparator();
    file_menu->Append(Menu_NewFrame, _T("&New frame\tCtrl-N"));
    file_menu->AppendSeparator();
    file_menu->Append(Menu_OpenFile, _T("&Open file..."));
    file_menu->AppendSeparator();
    file_menu->Append(Menu_Quit, _T("E&xit\tCtrl-Q"));	
	
    wxMenuBar *menu_bar = new wxMenuBar;
    menu_bar->Append(file_menu, _T("&File"));
//#if wxUSE_LOG
//    menu_bar->Append(log_menu,  _T("&Log"));
//#endif // wxUSE_LOG
//    menu_bar->Append(clip_menu, _T("&Clipboard"));
//    menu_bar->Append(help_menu, _T("&Help"));
	
    SetMenuBar(menu_bar);
	
	
    // 
    // wxString strFile(_T("Drop files here!"));
	
	wxString str;
	wxString str2 = wxString::FromUTF8(getenv("P7ZIP_HOME_DIR"));
    str = wxString(_T("P7ZIP_HOME_DIR -")) + str2 + _T("-");
	
	wxString strFile(str);
	
    m_ctrlFile  = new wxListBox(this, wxID_ANY, wxDefaultPosition, wxDefaultSize, 1, &strFile ,
                                wxLB_HSCROLL | wxLB_ALWAYS_SB );
	

    // m_ctrlFile->Append(str);
	
								
	m_ctrlFile->SetDropTarget(new DnDFile(m_ctrlFile));
								
	wxBoxSizer *sizer_top = new wxBoxSizer( wxHORIZONTAL );
	sizer_top->Add(m_ctrlFile, 1, wxEXPAND );
	
   wxBoxSizer *sizer = new wxBoxSizer( wxVERTICAL );
sizer->Add(sizer_top, 2, wxEXPAND );
	
SetSizer(sizer);
sizer->SetSizeHints( this );
								
}

void DnDFrame::OnQuit(wxCommandEvent& WXUNUSED(event))
{
    Close(true);
}

DnDFrame::~DnDFrame()
{
/*	
#if wxUSE_LOG
		if ( m_pLog != NULL ) {
			if ( wxLog::SetActiveTarget(m_pLogPrev) == m_pLog )
				delete m_pLog;
		}
#endif // wxUSE_LOG
 */
}


BEGIN_EVENT_TABLE(DnDFrame, wxFrame)
EVT_MENU(Menu_Quit,       DnDFrame::OnQuit)
END_EVENT_TABLE()

// 'Main program' equivalent: the program execution "starts" here
bool MyApp::OnInit()
{
    // don't parse the command-line options !
    // : if ( !wxApp::OnInit() ) return false;

#ifdef __WXMAC__
ProcessSerialNumber PSN;
GetCurrentProcess(&PSN);
TransformProcessType(&PSN,kProcessTransformToForegroundApplication);
#endif


  { // define P7ZIP_HOME_DIR
    extern void my_windows_split_path(const AString &p_path, AString &dir , AString &base);
    static char p7zip_home_dir[MAX_PATH];

    UString fullPath;
    NDir::MyGetFullPathName(wxApp::argv[0], fullPath);
    AString afullPath = GetAnsiString(fullPath);

    AString dir,name;

    my_windows_split_path(afullPath,dir,name);

    const char *dir2 = nameWindowToUnix((const char *)dir);
    snprintf(p7zip_home_dir,sizeof(p7zip_home_dir),"P7ZIP_HOME_DIR=%s/",dir2);
    p7zip_home_dir[sizeof(p7zip_home_dir)-1] = 0;
    putenv(p7zip_home_dir);
    // DEBUG printf("putenv(%s)\n",p7zip_home_dir);
  }
	global_use_utf16_conversion = 1; // UNICODE !
	

    wxInitAllImageHandlers();

//    Main1(wxApp::argc,wxApp::argv);
	
    // create the main frame window
    DnDFrame *frame = new DnDFrame((wxFrame  *) NULL,
                                   _T("P7ZIP Drag-and-Drop"),
                                   10, 100, 750, 540);

	
	printf("P7ZIP_HOME_DIR : -%s-\n", getenv("P7ZIP_HOME_DIR"));
	

	
    // activate it
    frame->Show(true);
	
    SetTopWindow(frame);
	
	

	

  // success: wxApp::OnRun() will be called which will enter the main message
  // loop and the application will run. If we returned false here, the
  // application would exit immediately.
    return true;
}
