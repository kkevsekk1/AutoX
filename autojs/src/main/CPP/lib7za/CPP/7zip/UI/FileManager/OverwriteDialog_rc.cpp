// OverwriteDialog_rc.cpp

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

#include "OverwriteDialogRes.h"
#include "Windows/Control/DialogImpl.h"

/*
IDD_OVERWRITE DIALOG 0, 0, xSize, ySize MY_MODAL_DIALOG_STYLE
CAPTION "Confirm File Replace"
MY_FONT
BEGIN
  LTEXT "Destination folder already contains processed file.", IDT_OVERWRITE_HEADER, marg, 7, xSize2, 8
  LTEXT "Would you like to replace the existing file", IDT_OVERWRITE_QUESTION_BEGIN, marg, 28, xSize2, 8
  ICON  "", IDI_OVERWRITE_OLD_FILE,             marg,  44, iconSize, iconSize
  LTEXT "", IDT_OVERWRITE_OLD_FILE_SIZE_TIME,      fiXPos,  44,  fiXSize,  fiYSize, SS_NOPREFIX
  LTEXT "with this one?",IDT_OVERWRITE_QUESTION_END, marg,  98,   xSize2,        8
  ICON  "",IDI_OVERWRITE_NEW_FILE,              marg, 114, iconSize, iconSize
  LTEXT "",IDT_OVERWRITE_NEW_FILE_SIZE_TIME,       fiXPos, 114,  fiXSize,  fiYSize, SS_NOPREFIX
  PUSHBUTTON "&Yes",         IDYES,                             78, b2YPos, bXSize, bYSize
  PUSHBUTTON "Yes to &All",  IDB_YES_TO_ALL,  152, b2YPos, bXSize, bYSize
  PUSHBUTTON "&No",          IDNO,                             226, b2YPos, bXSize, bYSize
  PUSHBUTTON "No to A&ll",   IDB_NO_TO_ALL,   300, b2YPos, bXSize, bYSize
  PUSHBUTTON "A&uto Rename", IDB_AUTO_RENAME, 181, b1YPos,    109, bYSize
  PUSHBUTTON "&Cancel",      IDCANCEL,                         300, b1YPos, bXSize, bYSize
END
*/

class COverwriteDialogImpl : public NWindows::NControl::CModalDialogImpl
{
 public:
   COverwriteDialogImpl(NWindows::NControl::CModalDialog *dialog,wxWindow * parent , int id) : CModalDialogImpl(dialog,parent, id, wxT("Confirm File Replace"))
  {
	///Sizer for adding the controls created by users
	wxBoxSizer* topsizer = new wxBoxSizer(wxVERTICAL);

	topsizer->Add(new wxStaticText(this, IDT_OVERWRITE_HEADER, _T("Destination folder already contains processed file.")) , 0 ,wxALL | wxALIGN_LEFT, 5 );
	topsizer->Add(new wxStaticText(this, IDT_OVERWRITE_QUESTION_BEGIN, _T("Would you like to replace the existing file")) , 0 ,wxALL | wxALIGN_LEFT, 5 );

	// FIXME ICON  "", IDI_OVERWRITE_OLD_FILE,             marg,  44, iconSize, iconSize
	topsizer->Add(new wxStaticText(this, IDT_OVERWRITE_OLD_FILE_SIZE_TIME, _T(""),
		wxDefaultPosition, wxDefaultSize, wxALIGN_LEFT) , 0 ,wxALL | wxALIGN_LEFT, 15 );
	topsizer->Add(new wxStaticText(this, IDT_OVERWRITE_QUESTION_END, _T("with this one?")) , 0 ,wxALL | wxALIGN_LEFT, 5 );

	// FIXME ICON  "",IDI_OVERWRITE_NEW_FILE,              marg, 114, iconSize, iconSize
	topsizer->Add(new wxStaticText(this, IDT_OVERWRITE_NEW_FILE_SIZE_TIME, _T(""),
		wxDefaultPosition, wxDefaultSize, wxALIGN_LEFT) , 0 ,wxALL | wxALIGN_LEFT, 15 );

	wxBoxSizer* Sizer1 = new wxBoxSizer(wxHORIZONTAL);
	Sizer1->Add(new wxButton(this, wxID_YES, _T("&Yes")) ,  0, wxALL | wxALIGN_RIGHT, 5);
	Sizer1->Add(new wxButton(this, IDB_YES_TO_ALL, _T("Yes to &All")) ,  0, wxALL | wxALIGN_RIGHT, 5);
	Sizer1->Add(new wxButton(this, wxID_NO, _T("&No")) ,  0, wxALL | wxALIGN_RIGHT, 5);
	Sizer1->Add(new wxButton(this, IDB_NO_TO_ALL, _T("No to A&ll")) ,  0, wxALL | wxALIGN_RIGHT, 5);
	topsizer->Add(Sizer1 ,  0, wxALL | wxALIGN_RIGHT, 5);

	wxBoxSizer* Sizer2 = new wxBoxSizer(wxHORIZONTAL);
	Sizer2->Add(new wxButton(this, IDB_AUTO_RENAME, _T("A&uto Rename")) ,  0, wxALL | wxALIGN_RIGHT, 5);
	Sizer2->Add(new wxButton(this, wxID_CANCEL, _T("&Cancel")) ,  0, wxALL | wxALIGN_RIGHT, 5);
	topsizer->Add(Sizer2 ,  1, wxALL | wxALIGN_RIGHT, 5);

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
	{ IDS_FILE_SIZE, L"{0} bytes" },
	{ 0 , 0 }
};

REGISTER_DIALOG(IDD_OVERWRITE,COverwriteDialog,g_stringTable)

BEGIN_EVENT_TABLE(COverwriteDialogImpl, wxDialog)
	EVT_BUTTON(wxID_ANY, CModalDialogImpl::OnAnyButton)
	EVT_MENU(WORKER_EVENT, CModalDialogImpl::OnWorkerEvent)
END_EVENT_TABLE()

