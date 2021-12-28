// PasswordDialog.cpp

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

#include "Windows/Control/DialogImpl.h"

#include "PasswordDialogRes.h"

class CPasswordDialogImpl : public NWindows::NControl::CModalDialogImpl
{
  public:
   CPasswordDialogImpl(NWindows::NControl::CModalDialog *dialog,wxWindow * parent,int id) : CModalDialogImpl(dialog, parent, id, wxT("Enter password"))
  {
	bool bShowPassword = false;

	wxBoxSizer* topsizer = new wxBoxSizer(wxVERTICAL);

	{
	wxStaticBoxSizer *passwdSizer = new wxStaticBoxSizer(new wxStaticBox(this,IDT_PASSWORD_ENTER,_T("&Enter password:")),wxVERTICAL);

	wxTextCtrl *TxtPasswd = new wxTextCtrl(this, IDE_PASSWORD_PASSWORD, L"", 
		wxDefaultPosition, wxSize(260,-1), bShowPassword?wxTE_LEFT:wxTE_PASSWORD );

	wxCheckBox *ChkShowPasswd = new wxCheckBox(this, IDX_PASSWORD_SHOW, wxT("&Show password"));

	ChkShowPasswd->SetValue(bShowPassword);
	passwdSizer->Add(TxtPasswd, 0, wxALL, 5);
	passwdSizer->Add(ChkShowPasswd, 0, wxALL, 5);

	topsizer->Add(passwdSizer, 0, wxALL, 5);
	}
	topsizer->Add(CreateButtonSizer(wxOK|wxCANCEL), 0, wxALL|wxEXPAND, 5);

	this->OnInit();

	SetSizer(topsizer); // use the sizer for layout
	topsizer->SetSizeHints(this); // set size hints to honour minimum size
  }
private:
	// Any class wishing to process wxWindows events must use this macro
	DECLARE_EVENT_TABLE()
};

REGISTER_DIALOG(IDD_PASSWORD,CPasswordDialog,0)

BEGIN_EVENT_TABLE(CPasswordDialogImpl, wxDialog)
	EVT_BUTTON(wxID_ANY,   CModalDialogImpl::OnAnyButton)
	EVT_CHECKBOX(wxID_ANY, CModalDialogImpl::OnAnyButton)
	EVT_MENU(WORKER_EVENT, CModalDialogImpl::OnWorkerEvent)
END_EVENT_TABLE()

