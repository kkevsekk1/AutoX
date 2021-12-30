// Windows/Window.cpp

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
 
#ifndef _UNICODE
#include "Common/StringConvert.h"
#endif
#include "Windows/Window.h"

void verify_main_thread(void);

class LockGUI
{
	bool _IsMain;
	public:
		LockGUI() {
			verify_main_thread();
			
			_IsMain = wxThread::IsMain();
			if (!_IsMain) {
				printf("LockGUI-Windows\n");
				abort(); // FIXME wxMutexGuiEnter();
			}
	       	}
		~LockGUI() { if (!_IsMain) wxMutexGuiLeave(); }
};

namespace NWindows {

HWND GetDlgItem(HWND dialogWindow, int ControlID)
{
	LockGUI lock;
	if (dialogWindow) return dialogWindow->FindWindow(ControlID);
	return 0;
}

void MySetWindowText(HWND wnd, LPCWSTR s)
{ 
	if (wnd == 0) return;

	LockGUI lock;

	wxString str = s;
	/*
	int id = wnd->GetId();
	if (  (id != wxID_OK) && (id != wxID_CANCEL) && (id != wxID_HELP) && (id != wxID_YES) && (id != wxID_NO))
	*/
	{
		wnd->SetLabel(str);
	}
}

	bool CWindow::GetText(CSysString &s)
	{
	  	wxString str;
		{
			LockGUI lock;
	  		str = _window->GetLabel();
		}
	  	s = str;
	  	return true;
	}

	bool CWindow::IsEnabled()
	{
		LockGUI lock;
		return _window->IsEnabled();
	}
}

////////////////////////////////// Windows Compatibility
#include <sys/resource.h>

void Sleep(unsigned millisec)
{
	wxMilliSleep(millisec);
}

t_processID GetCurrentProcess(void)  {
	return getpid();
}

void SetPriorityClass(t_processID pid , int priority) {
	setpriority(PRIO_PROCESS,pid,priority);
}

