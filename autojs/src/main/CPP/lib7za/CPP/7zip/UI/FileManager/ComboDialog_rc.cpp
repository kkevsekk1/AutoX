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

#include "ComboDialogRes.h"

class ComboDialogImpl : public NWindows::NControl::CModalDialogImpl
{
  public:
   ComboDialogImpl(NWindows::NControl::CModalDialog *dialog,wxWindow * parent,int id) : CModalDialogImpl(dialog, parent, id, wxT("Combo"))
  {

	wxBoxSizer* topsizer = new wxBoxSizer(wxVERTICAL);


	topsizer->Add(new wxStaticText(this, IDT_COMBO, _T("")) , 0 ,wxALL | wxALIGN_LEFT, 5 );


	wxArrayString pathArray;
	wxComboBox *combo = new wxComboBox(this, IDC_COMBO, wxEmptyString, wxDefaultPosition, wxSize(200,-1), pathArray, wxCB_DROPDOWN|wxCB_SORT);

	topsizer->Add(combo, 0 ,wxALL | wxALIGN_LEFT, 5 );

	topsizer->Add(CreateButtonSizer(wxOK|wxCANCEL), 0, wxALL|wxEXPAND, 5);

	this->OnInit();

	SetSizer(topsizer); // use the sizer for layout
	topsizer->SetSizeHints(this); // set size hints to honour minimum size
  }
private:
	// Any class wishing to process wxWindows events must use this macro
	DECLARE_EVENT_TABLE()
};

REGISTER_DIALOG(IDD_COMBO,ComboDialog,0)

BEGIN_EVENT_TABLE(ComboDialogImpl, wxDialog)
	EVT_BUTTON(wxID_ANY,   CModalDialogImpl::OnAnyButton)
	EVT_CHECKBOX(wxID_ANY, CModalDialogImpl::OnAnyButton)
	EVT_MENU(WORKER_EVENT, CModalDialogImpl::OnWorkerEvent)
END_EVENT_TABLE()

