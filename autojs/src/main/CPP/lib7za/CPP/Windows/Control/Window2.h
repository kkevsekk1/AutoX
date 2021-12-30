// Windows/Control/Window2.h

#ifndef __WINDOWS_CONTROL_WINDOW2_H
#define __WINDOWS_CONTROL_WINDOW2_H

#include "Windows/Window.h"
#include "Windows/Defs.h"

namespace NWindows {
namespace NControl {

class CWindow2 // : public CWindow
{
  // LRESULT DefProc(UINT message, WPARAM wParam, LPARAM lParam);
  HWND _window;
public:
  CWindow2(HWND newWindow = NULL): _window(newWindow){};
  CWindow2& operator=(HWND newWindow)
  {
    _window = newWindow;
    return *this;
  }

  virtual ~CWindow2() {}

  operator HWND() const { return 0; } // FIXME { return _window; }


#ifdef _WIN32
  bool CreateEx(DWORD exStyle, LPCTSTR className,
      LPCTSTR windowName, DWORD style,
      int x, int y, int width, int height,
      HWND parentWindow, HMENU idOrHMenu,
      HINSTANCE instance);

  #ifndef _UNICODE
  bool CreateEx(DWORD exStyle, LPCWSTR className,
      LPCWSTR windowName, DWORD style,
      int x, int y, int width, int height,
      HWND parentWindow, HMENU idOrHMenu,
      HINSTANCE instance);
  #endif
#endif

  virtual LRESULT OnMessage(UINT message, WPARAM wParam, LPARAM lParam);
  virtual bool OnCreate(CREATESTRUCT * /* createStruct */) { return true; }
  // virtual LRESULT OnCommand(WPARAM wParam, LPARAM lParam);
  virtual bool OnCommand(WPARAM wParam, LPARAM lParam, LRESULT &result);
  virtual bool OnCommand(int code, int itemID, LPARAM lParam, LRESULT &result);
  virtual bool OnSize(WPARAM /* wParam */, int /* xSize */, int /* ySize */) { return false; }
  virtual bool OnNotify(UINT /* controlID */, LPNMHDR /* lParam */, LRESULT & /* result */) { return false; }
  virtual void OnDestroy() { /* FIXME PostQuitMessage(0); */ }
  virtual void OnClose() { /* FIXME Destroy(); */ }
  /*
  virtual LRESULT  OnHelp(LPHELPINFO helpInfo) { OnHelp(); };
  virtual LRESULT  OnHelp() {};
  virtual bool OnButtonClicked(int buttonID, HWND buttonHWND);
  virtual void OnOK() {};
  virtual void OnCancel() {};
  */

#ifdef _WIN32
  LONG_PTR SetMsgResult(LONG_PTR newLongPtr )
    { return SetLongPtr(DWLP_MSGRESULT, newLongPtr); }
  LONG_PTR GetMsgResult() const
    { return GetLongPtr(DWLP_MSGRESULT); }
#endif
};

}}

#endif

