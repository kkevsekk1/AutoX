// ExtractDialog.cpp

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

#include "ExtractRes.h"
#include "ExtractDialogRes.h"

/*
IDD_EXTRACT  DIALOG  0, 0, xs, ys  MY_MODAL_DIALOG_STYLE  MY_FONT
CAPTION "Extract"
BEGIN
  LTEXT     "E&xtract to:", IDT_EXTRACT_EXTRACT_TO, m, m, xc, 8
  COMBOBOX  IDC_EXTRACT_PATH, m, m + 12, xc - bxsDots - 12, 100, MY_COMBO_WITH_EDIT
  PUSHBUTTON  "...", IDB_EXTRACT_SET_PATH, xs - m - bxsDots, m + 12 - 2, bxsDots, bys, WS_GROUP

  CONTROL   "", IDX_EXTRACT_NAME_ENABLE, MY_CHECKBOX, m, m + 34, 12, 10
  EDITTEXT  IDE_EXTRACT_NAME, m + 12 + 2, m + 32, g1xs - 12 - 2, 14, ES_AUTOHSCROLL

  LTEXT     "Path mode:", IDT_EXTRACT_PATH_MODE, m, m + 52, g1xs, 8
  COMBOBOX  IDC_EXTRACT_PATH_MODE, m, m + 64, g1xs, 140, MY_COMBO

  CONTROL   "Eliminate duplication of root folder", IDX_EXTRACT_ELIM_DUP, MY_CHECKBOX,
            m, m + 84, g1xs, 10

  LTEXT     "Overwrite mode:", IDT_EXTRACT_OVERWRITE_MODE, m, m + 104, g1xs, 8
  COMBOBOX  IDC_EXTRACT_OVERWRITE_MODE, m, m + 116, g1xs, 140, MY_COMBO


  GROUPBOX  "Password", IDG_PASSWORD, g2x, m + 36, g2xs, GROUP_Y_SIZE
  EDITTEXT  IDE_EXTRACT_PASSWORD, g2x2, m + 50, g2xs2, 14, ES_PASSWORD | ES_AUTOHSCROLL
  CONTROL   "Show Password", IDX_PASSWORD_SHOW, MY_CHECKBOX, g2x2, m + 72, g2xs2, 10

//  CONTROL   "Restore alternate data streams", IDX_EXTRACT_ALT_STREAMS, MY_CHECKBOX,
//            g2x, m + 104, g2xs, 10
  CONTROL   "Restore file security", IDX_EXTRACT_NT_SECUR, MY_CHECKBOX,
            g2x, m + 104, g2xs, 10
  
  DEFPUSHBUTTON  "OK",     IDOK,     bx3, by, bxs, bys, WS_GROUP
  PUSHBUTTON     "Cancel", IDCANCEL, bx2, by, bxs, bys
  PUSHBUTTON     "Help",   IDHELP,   bx1, by, bxs, bys
END
*/


class CExtractDialogImpl : public NWindows::NControl::CModalDialogImpl
{
 public:
   CExtractDialogImpl(NWindows::NControl::CModalDialog *dialog,wxWindow * parent , int id) : CModalDialogImpl(dialog,parent, id, wxT("Extract"))
  {
	wxStaticText *m_pStaticTextExtractTo;
	wxTextCtrl *m_pTextCtrlPassword;
	wxButton *m_pButtonBrowse;
	wxComboBox *m_pComboBoxExtractTo;
	wxCheckBox *m_pCheckBoxShowPassword;

	wxTextCtrl *m_pTextCtrlName;
	wxCheckBox *m_pCheckBoxNameEnable;

	wxComboBox *m_pPathMode;

	wxCheckBox * m_pCheckBoxElimDup;
	wxCheckBox * m_pCheckBoxNtSecur;

	wxComboBox *m_pOverWriteMode;

	///Sizer for adding the controls created by users
	wxBoxSizer* topsizer = new wxBoxSizer(wxVERTICAL);

	wxArrayString pathArray;
	m_pStaticTextExtractTo = new wxStaticText(this, IDT_EXTRACT_EXTRACT_TO, wxT("E&xtract To:"));
	wxBoxSizer *pPathSizer = new wxBoxSizer(wxHORIZONTAL);
	// m_pComboBoxExtractTo = new wxComboBox(this, IDC_EXTRACT_PATH, wxEmptyString, wxDefaultPosition, wxDefaultSize, pathArray, wxCB_DROPDOWN|wxCB_SORT);
	m_pComboBoxExtractTo = new wxComboBox(this, IDC_EXTRACT_PATH, wxEmptyString, wxDefaultPosition, wxSize(600,-1), pathArray, wxCB_DROPDOWN|wxCB_SORT);
	m_pButtonBrowse = new wxButton(this, IDB_EXTRACT_SET_PATH, wxT("..."), wxDefaultPosition, wxDefaultSize, wxBU_EXACTFIT);
	pPathSizer->Add(m_pComboBoxExtractTo, 1, wxLEFT|wxRIGHT|wxEXPAND, 5);
	pPathSizer->Add(m_pButtonBrowse, 0, wxLEFT|wxRIGHT|wxEXPAND, 5);

	wxBoxSizer *pControlSizer = new wxBoxSizer(wxHORIZONTAL);

	wxStaticBoxSizer * grpPathMode = new wxStaticBoxSizer(new wxStaticBox(this,IDT_EXTRACT_PATH_MODE,_T("Path mode :")),wxVERTICAL);

	m_pPathMode = new wxComboBox(this, IDC_EXTRACT_PATH_MODE, wxEmptyString, wxDefaultPosition, wxDefaultSize, pathArray, wxCB_DROPDOWN);

	grpPathMode->Add(m_pPathMode, 1, wxLEFT|wxRIGHT|wxEXPAND, 5);

	wxBoxSizer *pLeftSizer  = new wxBoxSizer(wxVERTICAL);
	wxBoxSizer *pRightSizer = new wxBoxSizer(wxVERTICAL);


	m_pTextCtrlName = new wxTextCtrl(this, IDE_EXTRACT_NAME, wxEmptyString, wxDefaultPosition, wxDefaultSize);
	m_pCheckBoxNameEnable = new wxCheckBox(this, IDX_EXTRACT_NAME_ENABLE, wxT(""));

	m_pCheckBoxElimDup = new wxCheckBox(this, IDX_EXTRACT_ELIM_DUP, wxT("Eliminate duplication of root folder"));

	m_pCheckBoxNtSecur = new wxCheckBox(this, IDX_EXTRACT_NT_SECUR, wxT("Restore file security"));

	wxStaticBoxSizer * grpOverWriteMode = new wxStaticBoxSizer(new wxStaticBox(this,IDT_EXTRACT_OVERWRITE_MODE,wxT("Overwrite mode :")),wxVERTICAL);
	m_pOverWriteMode = new wxComboBox(this, IDC_EXTRACT_OVERWRITE_MODE, wxEmptyString, wxDefaultPosition, wxDefaultSize, pathArray, wxCB_DROPDOWN);
	grpOverWriteMode->Add(m_pOverWriteMode, 1, wxLEFT|wxRIGHT|wxEXPAND, 5);



	wxStaticBoxSizer *pPasswordSizer = new wxStaticBoxSizer(new wxStaticBox(this,IDG_PASSWORD,wxT("Password")),wxVERTICAL);
	m_pTextCtrlPassword = new wxTextCtrl(this, IDE_EXTRACT_PASSWORD, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxTE_PASSWORD);
	m_pCheckBoxShowPassword = new wxCheckBox(this, IDX_PASSWORD_SHOW, wxT("Show Password"));
	pPasswordSizer->Add(m_pTextCtrlPassword, 0, wxALL|wxEXPAND, 5);
	pPasswordSizer->Add(m_pCheckBoxShowPassword, 0, wxALL|wxEXPAND, 5);

	pLeftSizer->Add(m_pCheckBoxNameEnable, 0, wxALL|wxEXPAND, 5);
	pLeftSizer->Add(m_pTextCtrlName, 1, wxALL|wxEXPAND, 5);

	pLeftSizer->Add(grpPathMode, 1, wxALL|wxEXPAND, 5);
	pLeftSizer->Add(m_pCheckBoxElimDup, 1, wxALL|wxEXPAND, 5);
	pLeftSizer->Add(grpOverWriteMode, 0, wxALL|wxEXPAND, 5);

	pRightSizer->Add(pPasswordSizer, 0, wxALL|wxEXPAND, 5);
	pRightSizer->Add(m_pCheckBoxNtSecur, 1, wxALL|wxEXPAND, 5);

	pControlSizer->Add(pLeftSizer, 1, wxALL|wxEXPAND, 5);
	pControlSizer->Add(pRightSizer, 1, wxLEFT | wxRIGHT | wxEXPAND, 5);

	topsizer->Add(m_pStaticTextExtractTo, 0, wxALL | wxEXPAND , 10);
	topsizer->Add(pPathSizer, 0, wxLEFT | wxRIGHT | wxBOTTOM | wxEXPAND , 5);
	topsizer->Add(pControlSizer, 1, wxALL | wxEXPAND , 5);
	topsizer->Add(CreateButtonSizer(wxOK | wxCANCEL | wxHELP), 0, wxALL | wxEXPAND , 5);

	this->OnInit();

	SetSizer(topsizer); // use the sizer for layout
	topsizer->SetSizeHints(this); // set size hints to honour minimum size
  }
private:
	// Any class wishing to process wxWindows events must use this macro
	DECLARE_EVENT_TABLE()
};

REGISTER_DIALOG(IDD_EXTRACT,CExtractDialog,0)

BEGIN_EVENT_TABLE(CExtractDialogImpl, wxDialog)
	EVT_BUTTON(wxID_ANY, CModalDialogImpl::OnAnyButton)
	EVT_CHECKBOX(wxID_ANY, CModalDialogImpl::OnAnyButton)
	EVT_MENU(WORKER_EVENT, CModalDialogImpl::OnWorkerEvent)
END_EVENT_TABLE()

