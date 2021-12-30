// Windows/Control/ComboBox.h

#ifndef __WINDOWS_WX_CONTROL_COMBOBOX_H
#define __WINDOWS_WX_CONTROL_COMBOBOX_H

#include "Windows/Window.h"
#include "Windows/Defs.h"


#include "Windows/Control/Window2.h" // NMHDR

#ifndef _WIN32
#define CB_ERR (-1)  // wxNOT_FOUND
#endif

typedef struct
{
	NMHDR hdr;
#define CBENF_ESCAPE 1
#define CBENF_RETURN 2
	int iWhy;
} NMCBEENDEDITW;

typedef NMCBEENDEDITW * PNMCBEENDEDITW;


class wxComboBox;

namespace NWindows {
	namespace NControl {

		class CComboBox // : public CWindow
		{
			wxComboBox* _choice;
		public:
			CComboBox() : _choice(0) {}

			void Attach(wxWindow * newWindow);
			wxWindow * Detach();
			operator HWND() const;

			int AddString(const TCHAR * txt);

			void SetText(LPCTSTR s);

			void GetText(CSysString &s);

			int GetCount() const ;
			void GetLBText(int index, CSysString &s);

			void SetCurSel(int index);
			int GetCurSel();

			void SetItemData(int index, int val);

			LRESULT GetItemData(int index);

			LRESULT GetItemData_of_CurSel() { return GetItemData(GetCurSel()); }

			void Enable(bool state);

			void ResetContent();
		};

		class CComboBoxEx : public CComboBox // : public CWindow
		{
		public:
			/* FIXME
  			LRESULT DeleteItem(int index)
    			{ return SendMessage(CBEM_DELETEITEM, index, 0); }
  			LRESULT InsertItem(COMBOBOXEXITEM *item)
    			{ return SendMessage(CBEM_INSERTITEM, 0, (LPARAM)item); }
  			DWORD SetExtendedStyle(DWORD exMask, DWORD exStyle)
    			{ return (DWORD)SendMessage(CBEM_SETEXTENDEDSTYLE, exMask, exStyle); }
  			HWND GetEditControl()
    			{ return (HWND)SendMessage(CBEM_GETEDITCONTROL, 0, 0); }
			*/
		};


	}
}

#endif // __WINDOWS_WX_CONTROL_COMBOBOX_H
