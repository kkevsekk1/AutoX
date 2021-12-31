// MessagesDialog_rc.cpp
 
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

#include <wx/listctrl.h>

#undef _WIN32

#include "Windows/Control/DialogImpl.h"
#include "MessagesDialogRes.h"

/*
IDD_MESSAGES DIALOG 0, 0, xSize, ySize  MY_MODAL_DIALOG_STYLE
CAPTION "7-Zip: Diagnostic messages"
MY_FONT
BEGIN
  DEFPUSHBUTTON "&Close", IDOK, bXPos, bYPos, bXSize, bYSize
  CONTROL "List1",IDL_MESSAGE,"SysListView32",
          LVS_REPORT | LVS_SHOWSELALWAYS | LVS_NOSORTHEADER | WS_BORDER | WS_TABSTOP, 
          marg, marg, xSize2, ySize2 - bYSize - 6
END

STRINGTABLE
BEGIN
  IDS_MESSAGES_DIALOG_MESSAGE_COLUMN "Message"
END
*/

class CMessagesDialogImpl : public NWindows::NControl::CModalDialogImpl
{
  public:
   CMessagesDialogImpl(NWindows::NControl::CModalDialog *dialog,wxWindow * parent , int id) :
	CModalDialogImpl(dialog,parent, id, wxT("7-Zip: Diagnostic messages"), wxDefaultPosition, wxDefaultSize,
			   wxDEFAULT_DIALOG_STYLE | wxRESIZE_BORDER | wxMAXIMIZE_BOX | wxMINIMIZE_BOX)
  {
	wxBoxSizer* topsizer = new wxBoxSizer(wxVERTICAL);


	wxListCtrl *list = new wxListCtrl(this, IDL_MESSAGE, wxDefaultPosition, wxSize(645,195), wxLC_REPORT );

#if 0
	list->InsertColumn(0, wxT("Col1"), wxLIST_FORMAT_LEFT);
	list->InsertColumn(1, wxT("Col2"), wxLIST_FORMAT_RIGHT);
	list->InsertItem(0, wxT("#1"));
	list->SetItem(0, 1, L"message 1");
	list->InsertItem(1, wxT("#2"));
	list->SetItem(1, 1, L"message 2");
#endif
	topsizer->Add(list ,  1, wxALL|wxEXPAND, 5);


	topsizer->Add(new wxButton(this, wxID_OK, _T("&Close")) ,  0, wxALL | wxALIGN_RIGHT, 5);

	this->OnInit();

	SetSizer(topsizer); // use the sizer for layout
	topsizer->SetSizeHints(this); // set size hints to honour minimum size
  }
private:
	// Any class wishing to process wxWindows events must use this macro
	DECLARE_EVENT_TABLE()
};

REGISTER_DIALOG(IDD_MESSAGES,CMessagesDialog,0)

BEGIN_EVENT_TABLE(CMessagesDialogImpl, wxDialog)
	EVT_BUTTON(wxID_ANY, CModalDialogImpl::OnAnyButton)
	EVT_MENU(WORKER_EVENT, CModalDialogImpl::OnWorkerEvent)
END_EVENT_TABLE()

