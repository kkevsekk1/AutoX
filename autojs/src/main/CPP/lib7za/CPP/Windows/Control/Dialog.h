// Windows/Control/Dialog.h

#ifndef __WINDOWS_CONTROL_DIALOG_H
#define __WINDOWS_CONTROL_DIALOG_H

#include "Windows/Window.h"

#ifndef _WIN32

#define WM_SETTEXT (6000) // wxID_HIGHEST + 1
#define WM_USER    (6999) // wxID_HIGHEST + 1000
#define WM_APP     (26999) // wxID_HIGHEST + 22000

#endif

#ifndef _WIN32
#define CBN_SELCHANGE       1
#endif

#define BST_CHECKED 1
#define BST_UNCHECKED 0
// #define BST_INDETERMINATE  0x0002

// FIXME #define wsprintf(a,b,c,d,e) swprintf(a,9999,b,c,d,e)  // FIXME

namespace NWindows {
	namespace NControl {

		class CModalDialogImpl;

		class CDialog
		{
		protected:
			CModalDialogImpl * _window;
		public:
			operator HWND() const { return HWND(_window); }

			bool OnInit(CModalDialogImpl * window) { 
				_window = window;
				return OnInit();
		       	}
			virtual bool OnInit() { return false; }
			virtual void OnOK() {}
			virtual void OnCancel() {}
			virtual void OnHelp() {}
			virtual bool OnButtonClicked(int buttonID, wxWindow * buttonHWND) { return false; }
			virtual bool OnMessage(UINT message, WPARAM wParam, LPARAM lParam) { return false; }
			virtual bool OnCommand(int code, int itemID, LPARAM lParam) { return false; }
			virtual bool OnTimer(WPARAM /* timerID */, LPARAM /* callback */) { return false; }

			void NormalizeSize(bool fullNormalize = false)  { /* FIXME */ }
			void NormalizePosition() { /* FIXME */ }
		};

		class CModalDialog : public CDialog
		{
		public:


			////////////////// COMPATIBILITY

			bool CheckRadioButton(int firstButtonID, int lastButtonID, int checkButtonID)
			{
/*
				for(int id = firstButtonID; id <= lastButtonID; id++)
				{
					CheckButton(id,id == checkButtonID);
				}
*/
				this->CheckButton(checkButtonID,true);

				return true;
			}


			bool CheckButton(int buttonID, UINT checkState);
			bool CheckButton(int buttonID, bool checkState)
			{
				return CheckButton(buttonID, UINT(checkState ? BST_CHECKED : BST_UNCHECKED));
			}


			UINT IsButtonChecked(int buttonID) const;

			bool IsButtonCheckedBool(long buttonID) const
				{ return (IsButtonChecked(buttonID) == BST_CHECKED); }

			void EnableItem(int id, bool state);

			void SetItemText(int id, const TCHAR *txt);

			wxWindow * GetItem(long id) const ;

			void ShowItem(int itemID, int cmdShow) const;

			void ShowItem_Bool(int itemID, bool show) const { ShowItem(itemID, show ? SW_SHOW: SW_HIDE); }


			void HideItem(int itemID) const { ShowItem(itemID, SW_HIDE); }

			void End(int result);

			void SetText(const TCHAR *_title); // {  _dialog->SetTitle(_title); }

			bool GetText(CSysString &s);

			INT_PTR Create(int id , HWND parentWindow);

			void PostMsg(UINT message);

			virtual void OnHelp() {}

			UINT_PTR SetTimer(UINT_PTR idEvent , unsigned milliseconds);

			void KillTimer(UINT_PTR idEvent);

			virtual void OnOK() { End(IDOK); }
			virtual void OnCancel() { End(IDCANCEL); }
		};

class CDialogChildControl : public NWindows::CWindow
{
public:
  CDialogChildControl() {}

  int m_ID;
  void Init(const NWindows::NControl::CModalDialog &parentDialog, int id)
  {
    m_ID = id;
    this->Attach(parentDialog.GetItem(id));
  }
  virtual void SetText(LPCWSTR s);
  virtual bool GetText(CSysString &s);
};

}
}

struct CStringTable
{
	unsigned int id;
	const wchar_t *str;
};

struct CDialogInfo
{
	int id;
	NWindows::NControl::CModalDialogImpl * (*createDialog)(NWindows::NControl::CModalDialog * dialog, HWND parentWindow);
	CStringTable * stringTable;
};

void RegisterDialog(const CDialogInfo *dialogInfo);

#define REGISTER_DIALOG_NAME(x) CRegister ## x

#define REGISTER_DIALOG(id,x,stringTable) \
	static NWindows::NControl::CModalDialogImpl * myCreate##x(NWindows::NControl::CModalDialog * dialog,HWND parentWindow) \
	{ return new x##Impl(dialog,parentWindow,id); } \
	static struct CDialogInfo g_DialogInfo = { id , myCreate##x, stringTable }; \
	struct REGISTER_DIALOG_NAME(x) { \
		REGISTER_DIALOG_NAME(x)() { RegisterDialog(&g_DialogInfo); }}; \
	static REGISTER_DIALOG_NAME(x) g_RegisterDialog;

#define REGISTER_STRINGTABLE(stringTable) \
	static struct CDialogInfo g_DialogInfo = { -1 , 0 , stringTable }; \
	struct REGISTER_DIALOG_NAME(x) { \
		REGISTER_DIALOG_NAME(x)() { RegisterDialog(&g_DialogInfo); }}; \
	static REGISTER_DIALOG_NAME(x) g_RegisterDialog;

#endif

