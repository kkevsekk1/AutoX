// RegistryUtils.cpp

#include "StdAfx.h"

#include "../../../Common/IntToString.h"

#include "../../../Windows/Registry.h"

#include "RegistryUtils.h"

using namespace NWindows;
using namespace NRegistry;

#define REG_PATH_7Z TEXT("Software") TEXT(STRING_PATH_SEPARATOR) TEXT("7-Zip")

static const TCHAR *kCUBasePath = REG_PATH_7Z;
static const TCHAR *kCU_FMPath = REG_PATH_7Z TEXT(STRING_PATH_SEPARATOR) TEXT("FM");
// static const TCHAR *kLM_Path = REG_PATH_7Z TEXT(STRING_PATH_SEPARATOR) TEXT("FM");

static const WCHAR *kLangValueName = L"Lang";
static const WCHAR *kViewer = L"Viewer";
static const WCHAR *kEditor = L"Editor";
static const WCHAR *kDiff = L"Diff";
static const TCHAR *kShowDots = TEXT("ShowDots");
static const TCHAR *kShowRealFileIcons = TEXT("ShowRealFileIcons");
static const TCHAR *kShowSystemMenu = TEXT("ShowSystemMenu");

static const TCHAR *kFullRow = TEXT("FullRow");
static const TCHAR *kShowGrid = TEXT("ShowGrid");
static const TCHAR *kAlternativeSelection = TEXT("AlternativeSelection");
// static const TCHAR *kLockMemoryAdd = TEXT("LockMemoryAdd");
static const TCHAR *kLargePagesEnable = TEXT("LargePages");
static const TCHAR *kSingleClick = TEXT("SingleClick");
// static const TCHAR *kUnderline = TEXT("Underline");

static const TCHAR *kFlatViewName = TEXT("FlatViewArc");
// static const TCHAR *kShowDeletedFiles = TEXT("ShowDeleted");

static void SaveCuString(LPCTSTR keyPath, LPCWSTR valuePath, LPCWSTR value)
{
  CKey key;
  key.Create(HKEY_CURRENT_USER, keyPath);
  key.SetValue(valuePath, value);
}

static void ReadCuString(LPCTSTR keyPath, LPCWSTR valuePath, UString &res)
{
  res.Empty();
  CKey key;
  if (key.Open(HKEY_CURRENT_USER, keyPath, KEY_READ) == ERROR_SUCCESS)
    key.QueryValue(valuePath, res);
}

void SaveRegLang(const UString &path) { SaveCuString(kCUBasePath, kLangValueName, path); }
void ReadRegLang(UString &path) { ReadCuString(kCUBasePath, kLangValueName, path); }

void SaveRegEditor(bool useEditor, const UString &path) { SaveCuString(kCU_FMPath, useEditor ? kEditor : kViewer, path); }
void ReadRegEditor(bool useEditor, UString &path) { ReadCuString(kCU_FMPath, useEditor ? kEditor : kViewer, path); }

void SaveRegDiff(const UString &path) { SaveCuString(kCU_FMPath, kDiff, path); }
void ReadRegDiff(UString &path) { ReadCuString(kCU_FMPath, kDiff, path); }

static void Save7ZipOption(const TCHAR *value, bool enabled)
{
  CKey key;
  key.Create(HKEY_CURRENT_USER, kCUBasePath);
  key.SetValue(value, enabled);
}

static void SaveOption(const TCHAR *value, bool enabled)
{
  CKey key;
  key.Create(HKEY_CURRENT_USER, kCU_FMPath);
  key.SetValue(value, enabled);
}

static bool Read7ZipOption(const TCHAR *value, bool defaultValue)
{
  CKey key;
  if (key.Open(HKEY_CURRENT_USER, kCUBasePath, KEY_READ) == ERROR_SUCCESS)
  {
    bool enabled;
    if (key.QueryValue(value, enabled) == ERROR_SUCCESS)
      return enabled;
  }
  return defaultValue;
}

static bool ReadOption(const TCHAR *value, bool defaultValue)
{
  CKey key;
  if (key.Open(HKEY_CURRENT_USER, kCU_FMPath, KEY_READ) == ERROR_SUCCESS)
  {
    bool enabled;
    if (key.QueryValue(value, enabled) == ERROR_SUCCESS)
      return enabled;
  }
  return defaultValue;
}

/*
static void SaveLmOption(const TCHAR *value, bool enabled)
{
  CKey key;
  key.Create(HKEY_LOCAL_MACHINE, kLM_Path);
  key.SetValue(value, enabled);
}

static bool ReadLmOption(const TCHAR *value, bool defaultValue)
{
  CKey key;
  if (key.Open(HKEY_LOCAL_MACHINE, kLM_Path, KEY_READ) == ERROR_SUCCESS)
  {
    bool enabled;
    if (key.QueryValue(value, enabled) == ERROR_SUCCESS)
      return enabled;
  }
  return defaultValue;
}
*/

void SaveShowDots(bool showDots) { SaveOption(kShowDots, showDots); }
bool ReadShowDots() { return ReadOption(kShowDots, false); }

void SaveShowRealFileIcons(bool show)  { SaveOption(kShowRealFileIcons, show); }
bool ReadShowRealFileIcons() { return ReadOption(kShowRealFileIcons, false); }

void Save_ShowSystemMenu(bool show) { SaveOption(kShowSystemMenu, show); }
bool Read_ShowSystemMenu(){ return ReadOption(kShowSystemMenu, false); }

void SaveFullRow(bool enable) { SaveOption(kFullRow, enable); }
bool ReadFullRow() { return ReadOption(kFullRow, false); }

void SaveShowGrid(bool enable) { SaveOption(kShowGrid, enable); }
bool ReadShowGrid(){ return ReadOption(kShowGrid, false); }

void SaveAlternativeSelection(bool enable) { SaveOption(kAlternativeSelection, enable); }
bool ReadAlternativeSelection(){ return ReadOption(kAlternativeSelection, false); }

void SaveSingleClick(bool enable) { SaveOption(kSingleClick, enable); }
bool ReadSingleClick(){ return ReadOption(kSingleClick, false); }

/*
void SaveUnderline(bool enable) { SaveOption(kUnderline, enable); }
bool ReadUnderline(){ return ReadOption(kUnderline, false); }
*/

// void SaveLockMemoryAdd(bool enable) { SaveLmOption(kLockMemoryAdd, enable); }
// bool ReadLockMemoryAdd() { return ReadLmOption(kLockMemoryAdd, true); }

void SaveLockMemoryEnable(bool enable) { Save7ZipOption(kLargePagesEnable, enable); }
bool ReadLockMemoryEnable() { return Read7ZipOption(kLargePagesEnable, false); }

static CSysString GetFlatViewName(UInt32 panelIndex)
{
  TCHAR panelString[16];
  ConvertUInt32ToString(panelIndex, panelString);
  return (CSysString)kFlatViewName + panelString;
}

void SaveFlatView(UInt32 panelIndex, bool enable) { SaveOption(GetFlatViewName(panelIndex), enable); }
bool ReadFlatView(UInt32 panelIndex) { return ReadOption(GetFlatViewName(panelIndex), false); }

/*
void Save_ShowDeleted(bool enable) { SaveOption(kShowDeletedFiles, enable); }
bool Read_ShowDeleted() { return ReadOption(kShowDeletedFiles, false); }
*/