// Windows/Control/StatusBar.h

#ifndef __WINDOWS_CONTROL_STATUSBAR_H
#define __WINDOWS_CONTROL_STATUSBAR_H

#include "Windows/Window.h"
#include "Windows/Defs.h"

class wxStatusBar;

namespace NWindows {
namespace NControl {

class CStatusBar // : public NWindows::CWindow
{
	wxStatusBar * _statusBar;
public:
	CStatusBar() : _statusBar(0) {}

	void Attach(wxWindow * newWindow);
	wxWindow * Detach();

	void SetText(int index, LPCTSTR text);
	
/* FIXME
  bool Create(LONG style, LPCTSTR text, HWND hwndParent, UINT id)
    { return (_window = ::CreateStatusWindow(style, text, hwndParent, id)) != 0; }
  bool SetParts(int numParts, const int *edgePostions)
    { return LRESULTToBool(SendMessage(SB_SETPARTS, numParts, (LPARAM)edgePostions)); }
  bool SetText(LPCTSTR text)
    { return CWindow::SetText(text); }

  bool SetText(int index, LPCTSTR text, UINT type)
    { return LRESULTToBool(SendMessage(SB_SETTEXT, index | type, (LPARAM)text)); }
  bool SetText(int index, LPCTSTR text)
    { return SetText(index, text, 0); }
  void Simple(bool simple)
    { SendMessage(SB_SIMPLE, BoolToBOOL(simple), 0); }

  #ifndef _UNICODE
  bool Create(LONG style, LPCWSTR text, HWND hwndParent, UINT id)
    { return (_window = ::CreateStatusWindowW(style, text, hwndParent, id)) != 0; }
  bool SetText(LPCWSTR text)
    { return CWindow::SetText(text); }
  bool SetText(int index, LPCWSTR text, UINT type)
    { return LRESULTToBool(SendMessage(SB_SETTEXTW, index | type, (LPARAM)text)); }
  bool SetText(int index, LPCWSTR text)
    { return SetText(index, text, 0); }
  #endif
*/  
};

}}

#endif

