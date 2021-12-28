// CompressCall.cpp

#include "StdAfx.h"

// For compilers that support precompilation, includes "wx/wx.h".
#include "wx/wxprec.h"

#ifdef __BORLANDC__
#pragma hdrstop
#endif

#ifndef WX_PRECOMP
#include "wx/wx.h"
#endif

#undef _WIN32

#include <wchar.h>

#include "../../../Common/IntToString.h"
#include "../../../Common/MyCom.h"
#include "../../../Common/Random.h"
#include "../../../Common/StringConvert.h"

#include "../../../Windows/DLL.h"
#include "../../../Windows/ErrorMsg.h"
#include "../../../Windows/FileDir.h"
// #include "../../../Windows/FileMapping.h"
// #include "../../../Windows/ProcessUtils.h"
#include "../../../Windows/Synchronization.h"

#include "../FileManager/RegistryUtils.h"

#include "CompressCall.h"

using namespace NWindows;

#define MY_TRY_BEGIN try {

#define MY_TRY_FINISH } \
  catch(...) { ErrorMessageHRESULT(E_FAIL); return E_FAIL; }

#define MY_TRY_FINISH_VOID } \
  catch(...) { ErrorMessageHRESULT(E_FAIL); }

#ifdef _WIN32
static const char *k7zGui = "7zG.exe";
#else
static const char *k7zGui = "7zG";
#endif

static const char *kShowDialogSwitch = " -ad";
static const char *kEmailSwitch = " -seml.";
static const char *kIncludeSwitch = " -i";
static const char *kArchiveTypeSwitch = " -t";
static const char *kArcIncludeSwitches = " -an -ai";
static const char *kHashIncludeSwitches = " -i";
static const char *kStopSwitchParsing = " --";
static const char *kLargePagesDisable = " -slp-";

extern HWND g_HWND;

UString GetQuotedString(const UString &s)
{
  UString s2 = L'\"';
  s2 += s;
  s2 += L'\"';
  return s2;
}

static void ErrorMessage(LPCWSTR message)
{
  MessageBoxW(g_HWND, message, L"7-Zip", MB_ICONERROR | MB_OK);
}

static void ErrorMessageHRESULT(HRESULT res, LPCWSTR s = NULL)
{
  UString s2 = NError::MyFormatMessage(res);
  if (s)
  {
    s2.Add_LF();
    s2 += s;
  }
  ErrorMessage(s2);
}

static HRESULT Call7zGui(const UString &params,
    // LPCWSTR curDir,
    bool waitFinish,
    NSynchronization::CBaseEvent *event)
{
  UString imageName = fs2us(NWindows::NDLL::GetModuleDirPrefix());
  imageName.AddAscii(k7zGui);

#ifdef _WIN32
  CProcess process;
  WRes res = process.Create(imageName, params, NULL); // curDir);
  if (res != 0)
  {
    ErrorMessageHRESULT(res, imageName);
    return res;
  }
  if (waitFinish)
    process.Wait();
  else if (event != NULL)
  {
    HANDLE handles[] = { process, *event };
    ::WaitForMultipleObjects(ARRAY_SIZE(handles), handles, FALSE, INFINITE);
  }
#else
	printf("MyCreateProcess: waitFinish=%d event=%p\n",(unsigned)waitFinish,event);
	printf("\timageName : %ls\n",(const wchar_t*)imageName);
	printf("\tparams : %ls\n",(const wchar_t*)params);
	// printf("\tcurDir : %ls\n",(const wchar_t*)curDir);

	wxString cmd;
	cmd = (const wchar_t*)imageName;
	cmd += L" ";
	cmd += (const wchar_t*)params;
	wxString memoCurDir = wxGetCwd();

/*
	if (curDir) {  // FIXME
		wxSetWorkingDirectory(wxString(curDir));


		// under MacOSX, a bundle does not keep the current directory
		// between 7zFM and 7zG ...
		// So, try to use the environment variable P7ZIP_CURRENT_DIR

		char p7zip_current_dir[MAX_PATH];

		AString aCurPath = GetAnsiString(curDir);

		const char *dir2 = nameWindowToUnix((const char *)aCurPath);

		snprintf(p7zip_current_dir,sizeof(p7zip_current_dir),"P7ZIP_CURRENT_DIR=%s/",dir2);

		p7zip_current_dir[sizeof(p7zip_current_dir)-1] = 0;

		putenv(p7zip_current_dir);

		printf("putenv(%s)\n",p7zip_current_dir);

	}
*/

	printf("MyCreateProcess: cmd='%ls'\n",(const wchar_t *)cmd);
	long pid = 0;
	if (waitFinish) pid = wxExecute(cmd, wxEXEC_SYNC); // FIXME process never ends and stays zombie ...
	else            pid = wxExecute(cmd, wxEXEC_ASYNC);

//	if (curDir) wxSetWorkingDirectory(memoCurDir);


	// FIXME if (pid == 0) return E_FAIL;
#endif
  return S_OK;
}

static void AddLagePagesSwitch(UString &params)
{
  if (!ReadLockMemoryEnable())
    params.AddAscii(kLargePagesDisable);
}

class CRandNameGenerator
{
  CRandom _random;
public:
  CRandNameGenerator() { _random.Init(); }
  void GenerateName(UString &s, const char *prefix)
  {
    s.AddAscii(prefix);
    char temp[16];
    ConvertUInt32ToString((UInt32)(unsigned)_random.Generate(), temp);
    s.AddAscii(temp);
  }
};

#ifdef _WIN32
static HRESULT CreateMap(const UStringVector &names,
    CFileMapping &fileMapping, NSynchronization::CManualResetEvent &event,
    UString &params)
{
  size_t totalSize = 1;
  {
    FOR_VECTOR (i, names)
      totalSize += (names[i].Len() + 1);
  }
  totalSize *= sizeof(wchar_t);
  
  CRandNameGenerator random;

  UString mappingName;
  for (;;)
  {
    random.GenerateName(mappingName, "7zMap");

    WRes res = fileMapping.Create(PAGE_READWRITE, totalSize, GetSystemString(mappingName));
    if (fileMapping.IsCreated() && res == 0)
      break;
    if (res != ERROR_ALREADY_EXISTS)
      return res;
    fileMapping.Close();
  }
  
  UString eventName;
  for (;;)
  {
    random.GenerateName(eventName, "7zEvent");
    WRes res = event.CreateWithName(false, GetSystemString(eventName));
    if (event.IsCreated() && res == 0)
      break;
    if (res != ERROR_ALREADY_EXISTS)
      return res;
    event.Close();
  }

  params += L'#';
  params += mappingName;
  params += L':';
  char temp[32];
  ConvertUInt64ToString(totalSize, temp);
  params.AddAscii(temp);
  
  params += L':';
  params += eventName;

  LPVOID data = fileMapping.Map(FILE_MAP_WRITE, 0, totalSize);
  if (!data)
    return E_FAIL;
  CFileUnmapper unmapper(data);
  {
    wchar_t *cur = (wchar_t *)data;
    *cur++ = 0; // it means wchar_t strings (UTF-16 in WIN32)
    FOR_VECTOR (i, names)
    {
      const UString &s = names[i];
      unsigned len = s.Len() + 1;
      wmemcpy(cur, (const wchar_t *)s, len);
      cur += len;
    }
  }
  return S_OK;
}
#endif

HRESULT CompressFiles(
    const UString &arcPathPrefix,
    const UString &arcName,
    const UString &arcType,
    bool addExtension,
    const UStringVector &names,
    bool email, bool showDialog, bool waitFinish)
{
  MY_TRY_BEGIN
  UString params = L'a';
  
#ifdef _WIN32
  CFileMapping fileMapping;
  NSynchronization::CManualResetEvent event;
  params.AddAscii(kIncludeSwitch);
  RINOK(CreateMap(names, fileMapping, event, params));
#else
  NSynchronization::CManualResetEvent event;
  char tempFile[256];
  static int count = 1000;

  sprintf(tempFile,"/tmp/7zCompress_%d_%d.tmp",(int)getpid(),count++);

  FILE * file = fopen(tempFile,"w");
  if (file)
  {
    for (int i = 0; i < names.Size(); i++) {
	  fprintf(file,"%ls\n",(const wchar_t *)names[i]);
	  printf(" TMP_%d : '%ls'\n",i,(const wchar_t *)names[i]);
   }

    fclose(file);
  }
  params += L" -i@";
  params += GetUnicodeString(tempFile);
#endif

  if (!arcType.IsEmpty())
  {
    params.AddAscii(kArchiveTypeSwitch);
    params += arcType;
  }

  if (email)
    params.AddAscii(kEmailSwitch);

  if (showDialog)
    params.AddAscii(kShowDialogSwitch);

  AddLagePagesSwitch(params);

  if (arcName.IsEmpty())
    params.AddAscii(" -an");

  if (addExtension)
    params.AddAscii(" -saa");
  else
    params.AddAscii(" -sae");

  params.AddAscii(kStopSwitchParsing);
  params.Add_Space();
  
  if (!arcName.IsEmpty())
  {
    params += GetQuotedString(
    // #ifdef UNDER_CE
      arcPathPrefix +
    // #endif
    arcName);
  }
  
  return Call7zGui(params,
      // (arcPathPrefix.IsEmpty()? 0: (LPCWSTR)arcPathPrefix),
      waitFinish, &event);
  MY_TRY_FINISH
}

static void ExtractGroupCommand(const UStringVector &arcPaths, UString &params, bool isHash)
{
  AddLagePagesSwitch(params);
  params.AddAscii(isHash ? kHashIncludeSwitches : kArcIncludeSwitches);
#ifdef _WIN32
  CFileMapping fileMapping;
  NSynchronization::CManualResetEvent event;
  HRESULT result = CreateMap(arcPaths, fileMapping, event, params);
  if (result == S_OK)
    result = Call7zGui(params, false, &event);
  if (result != S_OK)
    ErrorMessageHRESULT(result);
#else
  HRESULT result = S_OK;
  NSynchronization::CManualResetEvent event;
  char tempFile[256];
  static int count = 1000;

  sprintf(tempFile,"/tmp/7zExtract_%d_%d.tmp",(int)getpid(),count++);

  FILE * file = fopen(tempFile,"w");
  if (file)
  {
    for (int i = 0; i <  arcPaths.Size(); i++) {
	  fprintf(file,"%ls\n",(const wchar_t *)arcPaths[i]);
	  printf(" TMP_%d : '%ls'\n",i,(const wchar_t *)arcPaths[i]);
    }

    fclose(file);
  }
  params += L"@";
  params += GetUnicodeString(tempFile);
  printf("ExtractGroupCommand : -%ls-\n",(const wchar_t *)params);
  if (result == S_OK)
    result = Call7zGui(params, true, &event);  // FIXME false => true
  printf("ExtractGroupCommand : END\n");
  remove(tempFile);
  if (result != S_OK)
    ErrorMessageHRESULT(result);
#endif
}

void ExtractArchives(const UStringVector &arcPaths, const UString &outFolder, bool showDialog, bool elimDup)
{
  MY_TRY_BEGIN
  UString params = L'x';
  if (!outFolder.IsEmpty())
  {
    params.AddAscii(" -o");
    params += GetQuotedString(outFolder);
  }
  if (elimDup)
    params.AddAscii(" -spe");
  if (showDialog)
    params.AddAscii(kShowDialogSwitch);
  ExtractGroupCommand(arcPaths, params, false);
  MY_TRY_FINISH_VOID
}

void TestArchives(const UStringVector &arcPaths)
{
  MY_TRY_BEGIN
  UString params = L't';
  ExtractGroupCommand(arcPaths, params, false);
  MY_TRY_FINISH_VOID
}

void CalcChecksum(const UStringVector &paths, const UString &methodName)
{
  MY_TRY_BEGIN
  UString params = L'h';
  if (!methodName.IsEmpty())
  {
    params.AddAscii(" -scrc");
    params += methodName;
  }
  ExtractGroupCommand(paths, params, true);
  MY_TRY_FINISH_VOID
}

void Benchmark(bool totalMode)
{
  MY_TRY_BEGIN
  HRESULT result = Call7zGui(totalMode ? L"b -mm=*" : L"b", false, NULL);
  if (result != S_OK)
    ErrorMessageHRESULT(result);
  MY_TRY_FINISH_VOID
}
