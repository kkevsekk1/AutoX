// Windows/Window.h

#ifndef __WINDOWS_WINDOW_H
#define __WINDOWS_WINDOW_H

#include "Windows/Defs.h"
#include "Common/MyString.h"

namespace NWindows {

HWND GetDlgItem(HWND dialogWindow, int ControlID);
void    MySetWindowText(HWND wnd, LPCWSTR s);

class CWindow
{
private:
   // bool ModifyStyleBase(int styleOffset, DWORD remove, DWORD add, UINT flags);
protected:
  HWND _window;
public:
  CWindow(HWND newWindow = NULL): _window(newWindow){};
  CWindow& operator=(HWND newWindow)
  {
    _window = newWindow;
    return *this;
  }
  operator HWND() const { return _window; }
  void Attach(HWND newWindow) { _window = newWindow; }
  HWND Detach()
  {
    HWND window = _window;
    _window = NULL;
    return window;
  }
  virtual void SetText(LPCWSTR s) { MySetWindowText(_window, s); }
  virtual bool GetText(CSysString &s);
  bool IsEnabled();
};

}

#endif

