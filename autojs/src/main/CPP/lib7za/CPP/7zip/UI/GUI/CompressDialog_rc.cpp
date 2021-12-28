// CompressDialog.cpp

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

#include "resource2.h"
#include "Windows/Control/DialogImpl.h"

#include "CompressDialogRes.h"

class CCompressDialogImpl : public NWindows::NControl::CModalDialogImpl
{
  public:
   CCompressDialogImpl(NWindows::NControl::CModalDialog *dialog,wxWindow * parent , int id) : CModalDialogImpl(dialog,parent, id, wxT("Add to Archive"))
  {
	wxStaticText *m_pStaticTextMemoryCompress;
	wxStaticText *m_pStaticTextMemoryDecompress;
	wxTextCtrl *m_pTextCtrlPassword;
	wxTextCtrl *m_pTextCtrlRePassword;
	wxTextCtrl *m_pTextCtrlParameters;
	wxComboBox *m_pComboBoxArchiveName;
	wxComboBox *m_pComboBoxArchiveFormat;
	wxComboBox *m_pComboBoxCompressionLevel;
	wxComboBox *m_pComboBoxCompressionMethod;
	wxComboBox *m_pComboBoxDictionarySize;
	wxComboBox *m_pComboBoxWordSize;
	wxComboBox *m_pComboBoxUpdateMode;
	wxComboBox *m_pComboBoxPathMode;
	wxComboBox *m_pComboBoxEncryptionMethod;
	wxComboBox *m_pComboBoxSplitToVolumes;
	wxCheckBox *m_pCheckBoxShowPassword;
	wxCheckBox *m_pCheckBoxEncryptFileNames;
	wxButton   *m_pButtonBrowse;


	///Sizer for adding the controls created by users
	wxBoxSizer* topsizer = new wxBoxSizer(wxVERTICAL);

	wxStaticText *pStaticTextArchive = new wxStaticText(this, IDT_COMPRESS_ARCHIVE, wxT("&Archive:"));
	wxBoxSizer *pArchiveNameSizer = new wxBoxSizer(wxHORIZONTAL);
	m_pComboBoxArchiveName = new wxComboBox(this, IDC_COMPRESS_ARCHIVE, wxEmptyString, wxDefaultPosition, wxSize(600,-1), wxArrayString(), wxCB_DROPDOWN|wxCB_SORT);
	m_pButtonBrowse = new wxButton(this, IDB_COMPRESS_SET_ARCHIVE, wxT("..."), wxDefaultPosition, wxDefaultSize, wxBU_EXACTFIT);
	pArchiveNameSizer->Add(m_pComboBoxArchiveName, 1, wxALL|wxEXPAND, 5);
	pArchiveNameSizer->Add(m_pButtonBrowse, 0, wxALL|wxEXPAND, 5);
	wxBoxSizer *pControlSizer = new wxBoxSizer(wxHORIZONTAL);
	wxBoxSizer *pLeftSizer = new wxBoxSizer(wxVERTICAL);

	wxBoxSizer *pCompressionOptionsSizer = new wxBoxSizer(wxHORIZONTAL);
	wxBoxSizer *pCompressionStaticSizer = new wxBoxSizer(wxVERTICAL);
	wxBoxSizer *pCompressionComboSizer = new wxBoxSizer(wxVERTICAL);

	wxStaticText *pStaticTextArchiveFormat = new wxStaticText(this, IDT_COMPRESS_FORMAT, wxT("Archive &format:"));
	/*
	wxArrayString archiveFormatArray;
	archiveFormatArray.Add(wxT("7z"));
	archiveFormatArray.Add(wxT("Tar"));
	archiveFormatArray.Add(wxT("Zip"));
	m_pComboBoxArchiveFormat = new wxComboBox(this, IDC_COMPRESS_COMBO_FORMAT, archiveFormatArray.Item(0), wxDefaultPosition, wxDefaultSize, archiveFormatArray, wxCB_READONLY);
	*/

	m_pComboBoxArchiveFormat = new wxComboBox(this, IDC_COMPRESS_FORMAT, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_READONLY);

	wxStaticText *pStaticTextCompressionLevel = new wxStaticText(this, IDT_COMPRESS_LEVEL, wxT("Compression &level:"));
	wxArrayString compressionLevelArray;
	/*
	compressionLevelArray.Add(wxT("Store"));
	compressionLevelArray.Add(wxT("Fastest"));
	compressionLevelArray.Add(wxT("Fast"));
	compressionLevelArray.Add(wxT("Normal"));
	compressionLevelArray.Add(wxT("Maximum"));
	compressionLevelArray.Add(wxT("Ultra"));
	m_pComboBoxCompressionLevel = new wxComboBox(this, IDC_COMPRESS_COMBO_LEVEL, compressionLevelArray.Item(0), wxDefaultPosition, wxDefaultSize, compressionLevelArray, wxCB_READONLY);
	*/
	m_pComboBoxCompressionLevel = new wxComboBox(this, IDC_COMPRESS_LEVEL, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_READONLY);

	wxStaticText *pStaticTextCompressionMethod = new wxStaticText(this, IDT_COMPRESS_METHOD, wxT("Compression &method:"));
/*
	wxArrayString compressionMethodArray;
	compressionMethodArray.Add(wxT("LZMA"));
	compressionMethodArray.Add(wxT("PPMd"));
	compressionMethodArray.Add(wxT("BZip2"));
	m_pComboBoxCompressionMethod = new wxComboBox(this, IDC_COMPRESS_COMBO_METHOD, compressionMethodArray.Item(0), wxDefaultPosition, wxDefaultSize, compressionMethodArray, wxCB_READONLY);
*/
	m_pComboBoxCompressionMethod = new wxComboBox(this, IDC_COMPRESS_METHOD, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_READONLY);

	wxStaticText *pStaticTextDictionarySize = new wxStaticText(this, IDT_COMPRESS_DICTIONARY, wxT("&Dictionary size:"));
/*
	wxArrayString dictionarySizeArray;
	dictionarySizeArray.Add(wxT("64 KB"));
	dictionarySizeArray.Add(wxT("1 MB"));
	dictionarySizeArray.Add(wxT("1 MB"));
	dictionarySizeArray.Add(wxT("2 MB"));
	dictionarySizeArray.Add(wxT("3 MB"));
	dictionarySizeArray.Add(wxT("4 MB"));
	dictionarySizeArray.Add(wxT("6 MB"));
	dictionarySizeArray.Add(wxT("8 MB"));
	dictionarySizeArray.Add(wxT("12 MB"));
	dictionarySizeArray.Add(wxT("16 MB"));
	dictionarySizeArray.Add(wxT("24 MB"));
	dictionarySizeArray.Add(wxT("32 MB"));
	dictionarySizeArray.Add(wxT("48 MB"));
	dictionarySizeArray.Add(wxT("64 MB"));
	dictionarySizeArray.Add(wxT("96 MB"));
	dictionarySizeArray.Add(wxT("128 MB"));
	m_pComboBoxDictionarySize = new wxComboBox(this, IDC_COMPRESS_COMBO_DICTIONARY, dictionarySizeArray.Item(0), wxDefaultPosition, wxDefaultSize, dictionarySizeArray, wxCB_READONLY);
*/
	m_pComboBoxDictionarySize = new wxComboBox(this, IDC_COMPRESS_DICTIONARY, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_READONLY);

	wxStaticText *pStaticTextNumberOfThreads = new wxStaticText(this, IDT_COMPRESS_THREADS, wxT("&Number of CPU threads:"));
/*
	wxArrayString numberOfThreadsArray;
	numberOfThreadsArray.Add(wxT("1"));
	numberOfThreadsArray.Add(wxT("2"));
	wxComboBox *m_pComboBoxNumberOfThreads = new wxComboBox(this, IDC_COMPRESS_COMBO_THREADS, numberOfThreadsArray.Item(0), wxDefaultPosition, wxDefaultSize, numberOfThreadsArray, wxCB_READONLY);
*/
	wxComboBox *m_pComboBoxNumberOfThreads = new wxComboBox(this, IDC_COMPRESS_THREADS, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_READONLY);






	wxStaticText *pStaticTextWordSize = new wxStaticText(this, IDT_COMPRESS_ORDER, wxT("&Word size:"));
/*
	wxArrayString wordSizeArray;
	wordSizeArray.Add(wxT("8"));
	wordSizeArray.Add(wxT("12"));
	wordSizeArray.Add(wxT("16"));
	wordSizeArray.Add(wxT("24"));
	wordSizeArray.Add(wxT("32"));
	wordSizeArray.Add(wxT("48"));
	wordSizeArray.Add(wxT("64"));
	wordSizeArray.Add(wxT("96"));
	wordSizeArray.Add(wxT("128"));
	wordSizeArray.Add(wxT("192"));
	wordSizeArray.Add(wxT("256"));
	wordSizeArray.Add(wxT("273"));
	m_pComboBoxWordSize = new wxComboBox(this, IDC_COMPRESS_COMBO_ORDER, wordSizeArray.Item(0), wxDefaultPosition, wxDefaultSize, wordSizeArray, wxCB_READONLY);
*/
	m_pComboBoxWordSize = new wxComboBox(this, IDC_COMPRESS_ORDER, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_READONLY);


	wxStaticText *pStaticTextBlockSize = new wxStaticText(this, IDT_COMPRESS_SOLID, wxT("&Solid Block size:"));

	wxComboBox *m_pComboBoxBlockSize = new wxComboBox(this, IDC_COMPRESS_SOLID, wxEmptyString, wxDefaultPosition, wxDefaultSize,  wxArrayString(), wxCB_READONLY);

	pCompressionStaticSizer->Add(pStaticTextArchiveFormat, 1, wxALL|wxEXPAND, 5);
	pCompressionStaticSizer->Add(pStaticTextCompressionLevel, 1, wxALL|wxEXPAND, 5);
	pCompressionStaticSizer->Add(pStaticTextCompressionMethod, 1, wxALL|wxEXPAND, 5);
	pCompressionStaticSizer->Add(pStaticTextDictionarySize, 1, wxALL|wxEXPAND, 5);
	pCompressionStaticSizer->Add(pStaticTextWordSize, 1, wxALL|wxEXPAND, 5);
	pCompressionStaticSizer->Add(pStaticTextBlockSize, 1, wxALL|wxEXPAND, 5);
	pCompressionStaticSizer->Add(pStaticTextNumberOfThreads, 1, wxALL|wxEXPAND, 5);
	pCompressionComboSizer->Add(m_pComboBoxArchiveFormat, 1, wxALL|wxEXPAND, 5);
	pCompressionComboSizer->Add(m_pComboBoxCompressionLevel, 1, wxALL|wxEXPAND, 5);
	pCompressionComboSizer->Add(m_pComboBoxCompressionMethod, 1, wxALL|wxEXPAND, 5);
	pCompressionComboSizer->Add(m_pComboBoxDictionarySize, 1, wxALL|wxEXPAND, 5);
	pCompressionComboSizer->Add(m_pComboBoxWordSize, 1, wxALL|wxEXPAND, 5);
	pCompressionComboSizer->Add(m_pComboBoxBlockSize, 1, wxALL|wxEXPAND, 5);
	pCompressionComboSizer->Add(m_pComboBoxNumberOfThreads, 1, wxALL|wxEXPAND, 5);
	pCompressionOptionsSizer->Add(pCompressionStaticSizer, 1, wxALL|wxEXPAND, 0);
	pCompressionOptionsSizer->Add(pCompressionComboSizer, 1, wxALL|wxEXPAND, 0);

	wxBoxSizer *pMemoryUsageSizer = new wxBoxSizer(wxHORIZONTAL);
	wxBoxSizer *pMemoryUsageLabelSizer = new wxBoxSizer(wxVERTICAL);
	wxBoxSizer *pMemoryUsageInfoSizer = new wxBoxSizer(wxVERTICAL);
	wxStaticText *pStaticTextCompressMemoryUsage = new wxStaticText(this, IDT_COMPRESS_MEMORY, wxT("Memory usage for Compressing:"));
	m_pStaticTextMemoryCompress = new wxStaticText(this, IDT_COMPRESS_MEMORY_VALUE, wxT("709 MB"), wxDefaultPosition, wxDefaultSize, wxALIGN_RIGHT);
	wxStaticText *pStaticTextDecompressMemoryUsage = new wxStaticText(this, IDT_COMPRESS_MEMORY_DE, wxT("Memory usage for Decompressing"));
	m_pStaticTextMemoryDecompress = new wxStaticText(this, IDT_COMPRESS_MEMORY_DE_VALUE, wxT("66 MB"), wxDefaultPosition, wxDefaultSize, wxALIGN_RIGHT);
	pMemoryUsageLabelSizer->Add(pStaticTextCompressMemoryUsage, 1, wxALL|wxEXPAND, 5);
	pMemoryUsageLabelSizer->Add(pStaticTextDecompressMemoryUsage, 1, wxALL|wxEXPAND, 5);
	pMemoryUsageInfoSizer->Add(m_pStaticTextMemoryCompress, 1, wxALL|wxEXPAND, 5);
	pMemoryUsageInfoSizer->Add(m_pStaticTextMemoryDecompress, 1, wxALL|wxEXPAND, 5);
	pMemoryUsageSizer->Add(pMemoryUsageLabelSizer, 1, wxALL|wxEXPAND, 0);
	pMemoryUsageSizer->Add(pMemoryUsageInfoSizer, 0, wxALL|wxEXPAND, 0);

	wxStaticText *pStaticSplit2Volumes = new wxStaticText(this, IDT_SPLIT_TO_VOLUMES, wxT("Split to &volumes, bytes:"));
/*
	wxArrayString split2VolumesArray;
	split2VolumesArray.Add(wxT("1457664 - 3.5\" floppy"));
	split2VolumesArray.Add(wxT("650M - CD"));
	split2VolumesArray.Add(wxT("700M - CD"));
	split2VolumesArray.Add(wxT("4480M - DVD"));
	m_pComboBoxSplitToVolumes = new wxComboBox(this, IDC_COMPRESS_COMBO_VOLUME, wxEmptyString, wxDefaultPosition, wxDefaultSize, split2VolumesArray, wxCB_DROPDOWN);
*/
	m_pComboBoxSplitToVolumes = new wxComboBox(this, IDC_COMPRESS_VOLUME, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_DROPDOWN);

	pLeftSizer->Add(pCompressionOptionsSizer, 0, wxALL|wxEXPAND, 0);
	pLeftSizer->Add(pMemoryUsageSizer, 1, wxALL|wxEXPAND, 0);
	pLeftSizer->Add(pStaticSplit2Volumes, 0, wxALL|wxEXPAND, 5);
	pLeftSizer->Add(m_pComboBoxSplitToVolumes, 0, wxALL|wxEXPAND, 5);

	wxBoxSizer *pRightSizer = new wxBoxSizer(wxVERTICAL);
	wxStaticText *pStaticTextUpdateMode = new wxStaticText(this, IDT_COMPRESS_UPDATE_MODE, wxT("&Update mode:"));
/*
	wxArrayString updateModeArray;
	updateModeArray.Add(wxT("Add and replace files"));
	updateModeArray.Add(wxT("Update and add files"));
	updateModeArray.Add(wxT("Freshen existing files"));
	updateModeArray.Add(wxT("Synchronize files"));
	m_pComboBoxUpdateMode = new wxComboBox(this, IDC_COMPRESS_COMBO_UPDATE_MODE, updateModeArray.Item(0), wxDefaultPosition, wxDefaultSize, updateModeArray, wxCB_READONLY);
*/
	m_pComboBoxUpdateMode = new wxComboBox(this, IDC_COMPRESS_UPDATE_MODE, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_READONLY);


	wxStaticText *pStaticTextPathMode = new wxStaticText(this, IDT_COMPRESS_PATH_MODE, wxT("Path mode:"));
	m_pComboBoxPathMode = new wxComboBox(this, IDC_COMPRESS_PATH_MODE, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_READONLY);




	wxStaticBoxSizer *pOptionsSizer = new wxStaticBoxSizer(new wxStaticBox(this,IDG_COMPRESS_OPTIONS,_T("Options")),wxVERTICAL);
	wxCheckBox *m_pCheckBoxSFXArchive = new wxCheckBox(this, IDX_COMPRESS_SFX, wxT("Create SF&X archive"));
	pOptionsSizer->Add(m_pCheckBoxSFXArchive, 1, wxALL|wxEXPAND, 5);
	wxCheckBox *m_pCheckBoxCompressSharedFiles = new wxCheckBox(this, IDX_COMPRESS_SHARED, wxT("Compress shared files"));
	m_pCheckBoxCompressSharedFiles->Show(false); // this option is useless undex Unix ...
	pOptionsSizer->Add(m_pCheckBoxCompressSharedFiles, 1, wxALL|wxEXPAND, 5);

	wxStaticBoxSizer * pEncryptSizer = new wxStaticBoxSizer(new wxStaticBox(this,IDG_COMPRESS_ENCRYPTION,_T("Encryption")),wxVERTICAL);
	wxStaticText *pStaticTextPassword = new wxStaticText(this, IDT_PASSWORD_ENTER, wxT("Enter password:"));
	m_pTextCtrlPassword = new wxTextCtrl(this, IDE_COMPRESS_PASSWORD1, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxTE_PASSWORD);
	wxStaticText *pStaticTextRePassword = new wxStaticText(this, IDT_PASSWORD_REENTER, wxT("Re-enter password:"));
	m_pTextCtrlRePassword = new wxTextCtrl(this, IDE_COMPRESS_PASSWORD2, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxTE_PASSWORD);
	m_pCheckBoxShowPassword = new wxCheckBox(this, IDX_PASSWORD_SHOW, wxT("Show Password"));
	wxBoxSizer *pEncryptionMethodSizer = new wxBoxSizer(wxHORIZONTAL);
	wxStaticText *pStaticTextEncryptionMethod = new wxStaticText(this, IDT_COMPRESS_ENCRYPTION_METHOD, wxT("&Encryption method:"));
/*
	wxArrayString encryptionMethodArray;
	encryptionMethodArray.Add(wxT("AES-256"));
	m_pComboBoxEncryptionMethod =new wxComboBox(this, IDC_COMPRESS_COMBO_ENCRYPTION_METHOD, encryptionMethodArray.Item(0), wxDefaultPosition, wxDefaultSize, encryptionMethodArray, wxCB_READONLY);
*/
	m_pComboBoxEncryptionMethod =new wxComboBox(this, IDC_COMPRESS_ENCRYPTION_METHOD, wxEmptyString, wxDefaultPosition, wxDefaultSize, wxArrayString(), wxCB_READONLY);

	pEncryptionMethodSizer->Add(pStaticTextEncryptionMethod, 1, wxALL|wxEXPAND, 5);
	pEncryptionMethodSizer->Add(m_pComboBoxEncryptionMethod, 1, wxALL|wxEXPAND, 5);
	m_pCheckBoxEncryptFileNames = new wxCheckBox(this, IDX_COMPRESS_ENCRYPT_FILE_NAMES, wxT("Encrypt file &names"));
	pEncryptSizer->Add(pStaticTextPassword, 0, wxALL|wxEXPAND, 5);
	pEncryptSizer->Add(m_pTextCtrlPassword, 0, wxLEFT|wxRIGHT|wxEXPAND, 5);
	pEncryptSizer->Add(pStaticTextRePassword, 0, wxALL|wxEXPAND, 5);
	pEncryptSizer->Add(m_pTextCtrlRePassword, 0, wxLEFT|wxRIGHT|wxBOTTOM|wxEXPAND, 5);
	pEncryptSizer->Add(m_pCheckBoxShowPassword, 0, wxALL|wxEXPAND, 5);
	pEncryptSizer->Add(pEncryptionMethodSizer, 0, wxLEFT|wxRIGHT|wxEXPAND, 0);
	pEncryptSizer->Add(m_pCheckBoxEncryptFileNames, 0, wxALL|wxEXPAND, 5);

	pRightSizer->Add(pStaticTextUpdateMode, 0, wxALL|wxEXPAND, 5);
	pRightSizer->Add(m_pComboBoxUpdateMode, 0, wxALL|wxEXPAND, 5);

	pRightSizer->Add(pStaticTextPathMode, 0, wxALL|wxEXPAND, 5);
	pRightSizer->Add(m_pComboBoxPathMode, 0, wxALL|wxEXPAND, 5);

	pRightSizer->Add(pOptionsSizer, 1, wxALL|wxEXPAND, 5);
	pRightSizer->Add(pEncryptSizer, 0, wxALL|wxEXPAND, 5);

	pControlSizer->Add(pLeftSizer, 1, wxALL|wxEXPAND, 5);
	pControlSizer->Add(pRightSizer, 1, wxALL|wxEXPAND, 5);
	wxStaticText *pStaticTextParameters = new wxStaticText(this, IDT_COMPRESS_PARAMETERS, wxT("&Parameters:"));
	m_pTextCtrlParameters = new wxTextCtrl(this, IDE_COMPRESS_PARAMETERS, wxEmptyString);
	topsizer->Add(pStaticTextArchive, 0, wxLEFT | wxRIGHT | wxTOP |wxEXPAND, 10);
	topsizer->Add(pArchiveNameSizer, 1, wxLEFT | wxRIGHT |wxEXPAND, 5);
	topsizer->Add(pControlSizer, 0, wxALL|wxEXPAND, 5);
	topsizer->Add(pStaticTextParameters, 0, wxLEFT | wxRIGHT | wxBOTTOM |wxEXPAND, 10);
	topsizer->Add(m_pTextCtrlParameters, 0, wxLEFT | wxRIGHT |wxEXPAND, 10);
	topsizer->Add(CreateButtonSizer(wxOK | wxCANCEL | wxHELP), 0, wxALL|wxEXPAND, 10);

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
    { IDS_PASSWORD_NOT_MATCH  ,L"Passwords do not match" },
    { IDS_PASSWORD_USE_ASCII  ,L"Use only English letters, numbers and special characters (!, #, $, ...) for password." },
    { IDS_PASSWORD_TOO_LONG   ,L"Password is too long" },

    { IDS_METHOD_STORE    ,L"Store" },
    { IDS_METHOD_FASTEST  ,L"Fastest" },
    { IDS_METHOD_FAST     ,L"Fast" },
    { IDS_METHOD_NORMAL   ,L"Normal" },
    { IDS_METHOD_MAXIMUM  ,L"Maximum" },
    { IDS_METHOD_ULTRA    ,L"Ultra" },

    { IDS_COMPRESS_UPDATE_MODE_ADD    ,L"Add and replace files" },
    { IDS_COMPRESS_UPDATE_MODE_UPDATE ,L"Update and add files" },
    { IDS_COMPRESS_UPDATE_MODE_FRESH  ,L"Freshen existing files" },
    { IDS_COMPRESS_UPDATE_MODE_SYNC   ,L"Synchronize files" },

    { IDS_OPEN_TYPE_ALL_FILES  ,L"All Files" },
    { IDS_COMPRESS_SET_ARCHIVE_BROWSE  ,L"Browse" },

    { IDS_COMPRESS_NON_SOLID  ,L"Non-solid" },
    { IDS_COMPRESS_SOLID      ,L"Solid" },

    { IDS_SPLIT_CONFIRM  ,L"Specified volume size: {0} bytes.\nAre you sure you want to split archive into such volumes?" },


	{ 0 , 0 }
};

REGISTER_DIALOG(IDD_COMPRESS,CCompressDialog,g_stringTable)

BEGIN_EVENT_TABLE(CCompressDialogImpl, wxDialog)
	EVT_BUTTON(wxID_ANY,   CModalDialogImpl::OnAnyButton)
	EVT_CHECKBOX(wxID_ANY, CModalDialogImpl::OnAnyButton)
	EVT_COMBOBOX(wxID_ANY, CModalDialogImpl::OnAnyChoice)
	EVT_MENU(WORKER_EVENT, CModalDialogImpl::OnWorkerEvent)
END_EVENT_TABLE()

