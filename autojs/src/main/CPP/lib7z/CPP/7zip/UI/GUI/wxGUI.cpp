// wxGUI.cpp

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

#undef _WIN32

#ifdef __WXMAC__

#define UInt32 max_UInt32
#include <ApplicationServices/ApplicationServices.h>
#undef UInt32

#endif

#define static const
#include "../GUI/p7zip_32.xpm"
#undef static

#undef ACTIVATE_DIALOG_TESTS

#ifdef _WIN32
#error 5
#endif

#include "Windows/Window.h"
#include "Windows/Control/DialogImpl.h"

#include "Common/StringConvert.h"

bool GetProgramFolderPath(UString &folder)
{
  const char *p7zip_home_dir = getenv("P7ZIP_HOME_DIR");
  if (p7zip_home_dir == 0) p7zip_home_dir="./";

  folder = MultiByteToUnicodeString(p7zip_home_dir);

  return true;
}


// FIXME

static pthread_t g_main_thread;

bool is_main_thread(void)
{
	return ( g_main_thread == pthread_self() );
}

void verify_main_thread(void)
{
		if ( ! is_main_thread() )
		{
			printf("verify_main_thread-wxGUI\n");
			abort();
		}
}









int Main1(int argc,TCHAR **argv);

#include "Windows/Registry.h"
using namespace NWindows;
using namespace NRegistry;


#include "Common/StringConvert.h"
#include "Windows/FileDir.h"
#include "Windows/Synchronization.h"

#include "ExtractRes.h"
#include "../Explorer/MyMessages.h"

#include "../FileManager/resourceGui.h"
#include "ExtractGUI.h"
#include "UpdateGUI.h"
#include "BenchmarkDialog.h"
#include "../FileManager/RegistryUtils.h"

using namespace NWindows;
using namespace NFile;

#include "../FileManager/ProgramLocation.h"

static LPCWSTR kHelpFileName = L"help/";

void ShowHelpWindow(HWND hwnd, LPCWSTR topicFile)
{
  UString path;
  if (!::GetProgramFolderPath(path))
    return;
  path += kHelpFileName;
  path += topicFile;
  printf("ShowHelpWindow(%p,%ls)=>%ls\n",hwnd,topicFile,(const wchar_t *)path);
  // HtmlHelp(hwnd, GetSystemString(path), HH_DISPLAY_TOPIC, NULL);
  wxString path2(path);
  wxLaunchDefaultBrowser(path2);
}

////////////////////////////// TRIES ///////////////////////////////////

#ifdef ACTIVATE_DIALOG_TESTS
static void ErrorMessage(const wchar_t *message)
{
  MessageBox(0,message, wxT("7-Zip GUI"),wxICON_ERROR);
}

#include "../FileManager/PasswordDialog.h"
#include "../FileManager/MessagesDialog.h"
#include "../FileManager/OverwriteDialog.h"
#include "Windows/Thread.h"

void myErrorMsg(const wchar_t *message)
{
	MessageBox(0,message, wxT("Message"),wxICON_ERROR);
}

void testCMessagesDialog()
{
	UStringVector Messages;

	Messages.Add(L"message 1");
	Messages.Add(L"message 2");
	Messages.Add(L"message 3");
	Messages.Add(L"message 4");
	Messages.Add(L"message 5");
	Messages.Add(L"message 6");
	Messages.Add(L"message 7");
	Messages.Add(L"message 8");
	Messages.Add(L"message 9");

	CMessagesDialog messagesDialog;
    messagesDialog.Messages = &Messages;
   int ret = messagesDialog.Create( 0  ); // ParentWindow

   	if (ret == IDOK) myErrorMsg(wxT("CMessagesDialog => IDOK"));
	else if (ret == IDCANCEL) myErrorMsg(wxT("CMessagesDialog => IDCANCEL"));
	else  myErrorMsg(wxT("CMessagesDialog => ?"));

}

void testCOverwriteDialog()
{
SYSTEMTIME systemTime;
GetSystemTime( &systemTime );


const wchar_t *existName = L"existName";
FILETIME data_existTime;
FILETIME *existTime = &data_existTime ;
UInt64 data_existSize = 1234;
UInt64 *existSize = &data_existSize;
const wchar_t *newName = L"newName";
FILETIME data_newTime;
FILETIME *newTime = &data_newTime;
UInt64 data_newSize = 45678;
UInt64 *newSize = &data_newSize;
Int32 data_answer=0;
Int32 *answer = &data_answer;

SystemTimeToFileTime( &systemTime , &data_existTime);
SystemTimeToFileTime( &systemTime , &data_newTime);

  COverwriteDialog dialog;

  dialog.OldFileInfo.Time = *existTime;
  dialog.OldFileInfo.TimeIsDefined = true; // FIXME : look again at the sample !

  dialog.OldFileInfo.SizeIsDefined = (existSize != NULL);
  if (dialog.OldFileInfo.SizeIsDefined)
    dialog.OldFileInfo.Size = *existSize;
  dialog.OldFileInfo.Name = existName;

  if (newTime == 0)
    dialog.NewFileInfo.TimeIsDefined = false;
  else
  {
    dialog.NewFileInfo.TimeIsDefined = true;
    dialog.NewFileInfo.Time = *newTime;
  }
  
  dialog.NewFileInfo.SizeIsDefined = (newSize != NULL);
  if (dialog.NewFileInfo.SizeIsDefined)
    dialog.NewFileInfo.Size = *newSize;
  dialog.NewFileInfo.Name = newName;
  
  /*
  NOverwriteDialog::NResult::EEnum writeAnswer = 
    NOverwriteDialog::Execute(oldFileInfo, newFileInfo);
  */
  INT_PTR writeAnswer = dialog.Create(NULL); // ParentWindow doesn't work with 7z
  
  switch(writeAnswer)
  {
  case IDCANCEL: myErrorMsg(wxT("COverwriteDialog => IDCANCEL")); break;
  case IDNO:     myErrorMsg(wxT("COverwriteDialog => IDNO")); break;
  case IDC_BUTTON_OVERWRITE_NO_TO_ALL: myErrorMsg(wxT("COverwriteDialog => IDC_BUTTON_OVERWRITE_NO_TO_ALL")); break;
  case IDC_BUTTON_OVERWRITE_YES_TO_ALL:myErrorMsg(wxT("COverwriteDialog => IDC_BUTTON_OVERWRITE_YES_TO_ALL")); break;
  case IDC_BUTTON_OVERWRITE_AUTO_RENAME:myErrorMsg(wxT("COverwriteDialog => IDC_BUTTON_OVERWRITE_AUTO_RENAME")); break;
  case IDYES:  myErrorMsg(wxT("COverwriteDialog => IDYES")); break;
  default:  myErrorMsg(wxT("COverwriteDialog => default")); break;
  }
}

void testCPasswordDialog()
{
    CPasswordDialog dialog;

	int ret = dialog.Create(0);
	if (ret == IDOK) {
    		UString Password = dialog.Password;
		UString msg  = wxT("CPasswordDialog => IDOK password=\"");
		msg += Password;
		msg += wxT("\"");
		myErrorMsg(msg);
	}
	else if (ret == IDCANCEL) myErrorMsg(wxT("CPasswordDialog => IDCANCEL"));
	else  myErrorMsg(wxT("CPasswordDialog => ?"));

}

struct CThreadProgressDialog
{
  CProgressDialog * ProgressDialog;
  static THREAD_FUNC_DECL MyThreadFunction(void *param)
  {
    ((CThreadProgressDialog *)param)->Result = ((CThreadProgressDialog *)param)->Process();
    return 0;
  }
  HRESULT Result;
  HRESULT Process()
  {
	Sleep(1000);
	int total = 1000;

	ProgressDialog->ProgressSynch.SetTitleFileName(L"SetTitleFileName");
	ProgressDialog->ProgressSynch.SetNumFilesTotal(100);
	ProgressDialog->ProgressSynch.SetNumFilesCur(1);
	ProgressDialog->ProgressSynch.SetProgress(total, 0);
	// ProgressDialog.ProgressSynch.SetRatioInfo(inSize, outSize);
	// ProgressDialog.ProgressSynch.SetCurrentFileName(name);

	ProgressDialog->ProgressSynch.SetPos(total/10);
	ProgressDialog->ProgressSynch.SetCurrentFileName(L"File1");
	Sleep(1000);
	ProgressDialog->ProgressSynch.SetPos(total/2);
	ProgressDialog->ProgressSynch.SetCurrentFileName(L"File2");
	Sleep(1000);
	ProgressDialog->ProgressSynch.SetPos(total);
	ProgressDialog->ProgressSynch.SetCurrentFileName(L"File3");
	Sleep(1000);
	ProgressDialog->MyClose();
	return 0;
  }
};

void testCProgressDialog()
{
  CProgressDialog ProgressDialog;

  CThreadProgressDialog benchmarker;
  benchmarker.ProgressDialog = &ProgressDialog;
  NWindows::CThread thread;
  thread.Create(CThreadProgressDialog::MyThreadFunction, &benchmarker);

  //  void StartProgressDialog(const UString &title)
  int ret = ProgressDialog.Create(L"testCProgressDialog", 0);

	if (ret == IDOK) myErrorMsg(wxT("CProgressDialog => IDOK"));
	else if (ret == IDCANCEL) myErrorMsg(wxT("CProgressDialog => IDCANCEL"));
	else  myErrorMsg(wxT("CProgressDialog => ?"));

}

void testDialog(int num)
{
	NWindows::NControl::CModalDialog dialog;

	printf("Generic Dialog(%d)\n",num);
	int ret = dialog.Create(num, 0);
	if (ret == IDOK) myErrorMsg(wxT("Generic Dialog => IDOK"));
	else if (ret == IDCANCEL) myErrorMsg(wxT("Generic Dialog => IDCANCEL"));
	else  myErrorMsg(wxT("Generic Dialog => ?"));
}

void testMessageBox()
{
	int ret = MessageBoxW(0, L"test yes/no/cancel", 
            L"7-Zip", MB_YESNOCANCEL | MB_ICONQUESTION | MB_TASKMODAL);
	if (ret == IDYES) myErrorMsg(wxT("MessageBoxW => IDYES"));
	else if (ret == IDNO) myErrorMsg(wxT("MessageBoxW => IDNO"));
	else if (ret == IDCANCEL) myErrorMsg(wxT("MessageBoxW => IDCANCEL"));
	else  myErrorMsg(wxT("MessageBoxW => ?"));
}

static void testRegistry()
{
	SaveRegLang(L"fr");

	UString langFile;
	ReadRegLang(langFile);

	printf("testRegistry : -%ls-\n",(const wchar_t *)langFile);
}


int Main2(int argc,TCHAR **argv);

int Main3(int argc,wxChar **argv)
{
	testRegistry();

	int num = -1;

	if (argc >=2 )
	{
		num = argv[1][0] - L'0';
	}
	printf("num=%d\n",num);


	switch(num)
	{
		case 0:
		{
			TCHAR **argv2 = (TCHAR **)calloc(argc,sizeof(*argv));

			argv2[0] = argv[0];
			for(int i = 2; i < argc; i++) argv2[i-1] = argv[i];

			return Main2(argc-1,argv2);
		}
	// TODO Benchmark
	// TODO CCompressDialog
	// TODO CExtractDialog ?
		case 1 : testCMessagesDialog();  break;
		case 2 : testCOverwriteDialog(); break;
	 	case 3 : testCPasswordDialog();  break;
		case 4 : testCProgressDialog();  break;
		case 5 : testMessageBox();  break;
		case 9 : 
			if (argc >= 3)
			{
				AString str = GetAnsiString(argv[2]);
				int num = atoi((const char*)str);
				testDialog(num);
			}
			else
			{
				printf("usage : 7zG 9 <windowID>\n");
			}
		      	break;
		default :
			printf("usage : 7zG number\n");

	};

	return 0;
}

#endif // ACTIVATE_DIALOG_TESTS

static const TCHAR *kCUBasePath = TEXT("Software/7-ZIP");
static const WCHAR *kLangValueName = L"Lang";

void SaveRegLang(const UString &langFile)
{
  CKey key;
  key.Create(HKEY_CURRENT_USER, kCUBasePath);
  key.SetValue(kLangValueName, langFile);
}

void ReadRegLang(UString &langFile)
{
  langFile.Empty();
  CKey key;
  if (key.Open(HKEY_CURRENT_USER, kCUBasePath, KEY_READ) == ERROR_SUCCESS)
    key.QueryValue(kLangValueName, langFile);
}


//////////////////////////////////

#define NEED_NAME_WINDOWS_TO_UNIX
#include "myPrivate.h" // global_use_utf16_conversion

void mySplitCommandLineW(int numArguments, TCHAR  **arguments,UStringVector &parts) {

  parts.Clear();
  for(int ind=0;ind < numArguments; ind++) {
      UString tmp = arguments[ind];
      // tmp.Trim(); " " is a valid filename ...
      if (!tmp.IsEmpty()) {
        parts.Add(tmp);
// DEBUG printf("ARG %d : '%ls'\n",ind,(const wchar_t *)tmp);
      }
  }
}

// ----------------------------------------------------------------------------
// private classes
// ----------------------------------------------------------------------------

// Define a new frame type
class MyFrame: public wxFrame
{
public:
    // ctor
    MyFrame(wxFrame *frame, const wxString& title, int x, int y, int w, int h);
    // virtual ~MyFrame();

    // operations
    void WriteText(const wxString& text) { m_txtctrl->WriteText(text); }
    
protected:
    // callbacks
    void OnWorkerEvent(wxCommandEvent& event);
private:
    // just some place to put our messages in
    wxTextCtrl *m_txtctrl;
    DECLARE_EVENT_TABLE()
};

BEGIN_EVENT_TABLE(MyFrame, wxFrame)
    EVT_MENU(WORKER_EVENT, MyFrame::OnWorkerEvent)
    // EVT_IDLE(MyFrame::OnIdle)
END_EVENT_TABLE()

// My frame constructor
MyFrame::MyFrame(wxFrame *frame, const wxString& title,
                 int x, int y, int w, int h)
       : wxFrame(frame, wxID_ANY, title, wxPoint(x, y), wxSize(w, h))
{
	this->SetIcon(wxICON(p7zip_32));
    
#if wxUSE_STATUSBAR
    CreateStatusBar(2);
#endif // wxUSE_STATUSBAR

    m_txtctrl = new wxTextCtrl(this, wxID_ANY, _T(""), wxPoint(0, 0), wxSize(0, 0), wxTE_MULTILINE | wxTE_READONLY);
}

void myCreateHandle(int n);
wxWindow * g_window=0;

void MyFrame::OnWorkerEvent(wxCommandEvent& event)
{
	int n = event.GetInt();
	myCreateHandle(n);
}


// Define a new application type, each program should derive a class from wxApp
class MyApp : public wxApp
{
public:
    // override base class virtuals
    // ----------------------------

    // this one is called on application startup and is a good place for the app
    // initialization (doing it here and not in the ctor allows to have an error
    // return: if OnInit() returns false, the application terminates)
    virtual bool OnInit();
};

// Create a new application object: this macro will allow wxWidgets to create
// the application object during program execution (it's better than using a
// static object for many reasons) and also implements the accessor function
// wxGetApp() which will return the reference of the right type (i.e. MyApp and
// not wxApp)
IMPLEMENT_APP(MyApp)



time_t g_T0 = 0;

/* FIXME : to erase ?
class MyThread : public wxThread
{
	int _argc;
	TCHAR **_argv;
public:
	MyThread(int argc,TCHAR **argv): wxThread(),_argc(argc), _argv(argv) {}

	// thread execution starts here
	virtual void *Entry()
	{
#ifdef ACTIVATE_DIALOG_TESTS
		int ret = Main3(_argc,_argv);
#else
		int ret = Main1(_argc,_argv);
#endif
		exit(ret);
	}
};
*/

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
	
	
	g_main_thread = pthread_self();

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

  g_T0 = time(0);
  // DEBUG printf("MAIN Thread : 0x%lx\n",wxThread::GetCurrentId());

  // Create the main frame window
  MyFrame *frame = new MyFrame((wxFrame *)NULL, _T("7-zip Main Window"), 50, 50, 450, 340);
  // Don't Show the frame !
  // frame->Show(true);

  g_window = frame;

  SetTopWindow(frame);

/* FIXME ?	
    MyThread *thread = new MyThread(wxApp::argc,wxApp::argv);
    thread->Create(); //  != wxTHREAD_NO_ERROR
    thread->Run();

  // success: wxApp::OnRun() will be called which will enter the main message
  // loop and the application will run. If we returned false here, the
  // application would exit immediately.
    return true;
*/
	
	int ret = Main1(wxApp::argc,wxApp::argv);
	
	exit(ret);
	
	return false;
}

//////////////////////////////////////////

#include "resource2.h"
#include "resource3.h"
#include "ExtractRes.h"
// #include "resourceGui.h"
#include "../FileManager/PropertyNameRes.h"


static CStringTable g_stringTable[] =
{
  /* GUI/resource2.rc */	  


    { IDS_PROGRESS_COMPRESSING  ,L"Compressing" },
    { IDS_ARCHIVES_COLON  ,L"Archives:" },

  /* ../FileManager/resourceGui.rc */
  { IDS_MESSAGE_NO_ERRORS     ,L"There are no errors" },

  { IDS_PROGRESS_TESTING      ,L"Testing" },

  { IDS_CHECKSUM_CALCULATING    ,L"Checksum calculating..." },
  { IDS_CHECKSUM_INFORMATION    ,L"Checksum information" },
  { IDS_CHECKSUM_CRC_DATA       ,L"CRC checksum for data:" },
  { IDS_CHECKSUM_CRC_DATA_NAMES ,L"CRC checksum for data and names:" },
  { IDS_CHECKSUM_CRC_STREAMS_NAMES ,L"CRC checksum for streams and names:" },

  { IDS_INCORRECT_VOLUME_SIZE ,L"Incorrect volume size" },

  { IDS_OPENNING  ,L"Opening..." },
  { IDS_SCANNING  ,L"Scanning..." },

  /* ../FileManager/PropertyName.rc */
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



  /* Extract.rc */	  
  /**************/
  { IDS_MEM_ERROR  ,L"The system cannot allocate the required amount of memory" },
  { IDS_CANNOT_CREATE_FOLDER  ,L"Cannot create folder '{0}'" },
  { IDS_UPDATE_NOT_SUPPORTED  ,L"Update operations are not supported for this archive." },
  { IDS_CANT_OPEN_ARCHIVE  ,L"Can not open file '{0}' as archive" },
  { IDS_CANT_OPEN_ENCRYPTED_ARCHIVE  ,L"Can not open encrypted archive '{0}'. Wrong password?" },
  { IDS_UNSUPPORTED_ARCHIVE_TYPE  ,L"Unsupported archive type" },

  { IDS_CANT_OPEN_AS_TYPE   ,L"Can not open the file as {0} archive" },
  { IDS_IS_OPEN_AS_TYPE     ,L"The file is open as {0} archive" },
  { IDS_IS_OPEN_WITH_OFFSET ,L"The archive is open with offset" },

  { IDS_PROGRESS_EXTRACTING ,L"Extracting" },

  { IDS_PROGRESS_SKIPPING ,L"Skipping" },

  { IDS_EXTRACT_SET_FOLDER  ,L"Specify a location for extracted files." },

  { IDS_EXTRACT_PATHS_FULL    ,L"Full pathnames" },
  { IDS_EXTRACT_PATHS_NO      ,L"No pathnames" },
  { IDS_EXTRACT_PATHS_ABS     ,L"Absolute pathnames" },
  { IDS_PATH_MODE_RELAT       ,L"Relative pathnames" },

  { IDS_EXTRACT_OVERWRITE_ASK             ,L"Ask before overwrite" },
  { IDS_EXTRACT_OVERWRITE_WITHOUT_PROMPT  ,L"Overwrite without prompt" },
  { IDS_EXTRACT_OVERWRITE_SKIP_EXISTING   ,L"Skip existing files" },
  { IDS_EXTRACT_OVERWRITE_RENAME          ,L"Auto rename" },
  { IDS_EXTRACT_OVERWRITE_RENAME_EXISTING ,L"Auto rename existing files" },

  { IDS_EXTRACT_MESSAGE_UNSUPPORTED_METHOD   ,L"Unsupported compression method for '{0}'." },
  { IDS_EXTRACT_MESSAGE_DATA_ERROR           ,L"Data error in '{0}'. File is broken" },
  { IDS_EXTRACT_MESSAGE_CRC_ERROR            ,L"CRC failed in '{0}'. File is broken." },
  { IDS_EXTRACT_MESSAGE_DATA_ERROR_ENCRYPTED ,L"Data error in encrypted file '{0}'. Wrong password?" },
  { IDS_EXTRACT_MESSAGE_CRC_ERROR_ENCRYPTED  ,L"CRC failed in encrypted file '{0}'. Wrong password?" },

  { IDS_EXTRACT_MSG_WRONG_PSW_GUESS          ,L"Wrong password?" },
  // { IDS_EXTRACT_MSG_ENCRYPTED            ,L"Encrypted file" },

  { IDS_EXTRACT_MSG_UNSUPPORTED_METHOD   ,L"Unsupported compression method" },
  { IDS_EXTRACT_MSG_DATA_ERROR           ,L"Data error" },
  { IDS_EXTRACT_MSG_CRC_ERROR            ,L"CRC failed" },
  { IDS_EXTRACT_MSG_UNAVAILABLE_DATA     ,L"Unavailable data" },
  { IDS_EXTRACT_MSG_UEXPECTED_END        ,L"Unexpected end of data" },
  { IDS_EXTRACT_MSG_DATA_AFTER_END       ,L"There are some data after the end of the payload data" },
  { IDS_EXTRACT_MSG_IS_NOT_ARC           ,L"Is not archive" },
  { IDS_EXTRACT_MSG_HEADERS_ERROR        ,L"Headers Error" },
  { IDS_EXTRACT_MSG_WRONG_PSW_CLAIM      ,L"Wrong password" },

  { IDS_OPEN_MSG_UNAVAILABLE_START  ,L"Unavailable start of archive" },
  { IDS_OPEN_MSG_UNCONFIRMED_START  ,L"Unconfirmed start of archive" },
  // { IDS_OPEN_MSG_ERROR_FLAGS + 5  ,L"Unexpected end of archive" },
  // { IDS_OPEN_MSG_ERROR_FLAGS + 6  ,L"There are data after the end of archive" },
  { IDS_OPEN_MSG_UNSUPPORTED_FEATURE  ,L"Unsupported feature" },


  // resource3.rc
  { IDS_PROGRESS_REMOVE     ,L"Removing" },

  { IDS_PROGRESS_ADD        ,L"Adding" },
  { IDS_PROGRESS_UPDATE     ,L"Updating" },
  { IDS_PROGRESS_ANALYZE    ,L"Analyzing" },
  { IDS_PROGRESS_REPLICATE  ,L"Replicating" },
  { IDS_PROGRESS_REPACK     ,L"Repacking" },

  { IDS_PROGRESS_DELETE     ,L"Deleting" },
  { IDS_PROGRESS_HEADER     ,L"Header creating" },


	{ 0 , 0 }
};

REGISTER_STRINGTABLE(g_stringTable)

