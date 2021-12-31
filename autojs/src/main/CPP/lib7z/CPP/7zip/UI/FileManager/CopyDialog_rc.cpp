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

#include "CopyDialogRes.h"

class CopyDialogImpl : public NWindows::NControl::CModalDialogImpl
{
  public:
   CopyDialogImpl(NWindows::NControl::CModalDialog *dialog,wxWindow * parent,int id) : CModalDialogImpl(dialog, parent, id, wxT("Copy"))
  {

	wxBoxSizer* topsizer = new wxBoxSizer(wxVERTICAL);


	topsizer->Add(new wxStaticText(this, IDT_COPY, _T("")) , 0 ,wxEXPAND | wxALL | wxALIGN_LEFT, 5 );


	{
	wxBoxSizer *pathSizer = new wxBoxSizer(wxHORIZONTAL);

	wxArrayString pathArray;
	wxComboBox *combo = new wxComboBox(this, IDC_COPY, wxEmptyString, wxDefaultPosition, wxSize(600,-1), pathArray, wxCB_DROPDOWN|wxCB_SORT);
	wxButton *button = new wxButton(this, IDB_COPY_SET_PATH, wxT("..."), wxDefaultPosition, wxDefaultSize, wxBU_EXACTFIT);
	pathSizer->Add(combo, 1, wxLEFT|wxRIGHT|wxEXPAND, 5);
	pathSizer->Add(button, 0, wxLEFT|wxRIGHT|wxEXPAND, 5);

	topsizer->Add(pathSizer, 0 ,wxALL | wxALIGN_LEFT, 5 );
	}

//	topsizer->Add(new wxStaticText(this, IDT_COPY_INFO, _T("line1\nline2\nline3\nline4\n")) , 0 ,wxEXPAND | wxALL | wxALIGN_LEFT, 5 );
	topsizer->Add(new wxStaticText(this, IDT_COPY_INFO, _T("")) , 0 ,wxEXPAND | wxALL | wxALIGN_LEFT, 5 );
	  
	topsizer->Add(CreateButtonSizer(wxOK|wxCANCEL), 0, wxALL|wxEXPAND, 5);

	this->OnInit();

	SetSizer(topsizer); // use the sizer for layout
	topsizer->SetSizeHints(this); // set size hints to honour minimum size
  }
private:
	// Any class wishing to process wxWindows events must use this macro
	DECLARE_EVENT_TABLE()
};

static CStringTable g_stringTable[] =
{
	{ IDS_SET_FOLDER, L"Specify a location for output folder." },
	{ 0 , 0 }
};


REGISTER_DIALOG(IDD_COPY,CopyDialog,g_stringTable)

BEGIN_EVENT_TABLE(CopyDialogImpl, wxDialog)
	EVT_BUTTON(wxID_ANY,   CModalDialogImpl::OnAnyButton)
	EVT_CHECKBOX(wxID_ANY, CModalDialogImpl::OnAnyButton)
	EVT_MENU(WORKER_EVENT, CModalDialogImpl::OnWorkerEvent)
END_EVENT_TABLE()

