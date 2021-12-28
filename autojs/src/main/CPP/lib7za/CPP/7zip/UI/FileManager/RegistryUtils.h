// RegistryUtils.h

#ifndef __REGISTRY_UTILS_H
#define __REGISTRY_UTILS_H

#include "../../../Common/MyTypes.h"
#include "../../../Common/MyString.h"

void SaveRegLang(const UString &path);
void ReadRegLang(UString &path);

void SaveRegEditor(bool useEditor, const UString &path);
void ReadRegEditor(bool useEditor, UString &path);

void SaveRegDiff(const UString &path);
void ReadRegDiff(UString &path);

void SaveShowDots(bool showDots);
bool ReadShowDots();

void SaveShowRealFileIcons(bool show);
bool ReadShowRealFileIcons();

void Save_ShowSystemMenu(bool showSystemMenu);
bool Read_ShowSystemMenu();

void SaveFullRow(bool enable);
bool ReadFullRow();

void SaveShowGrid(bool enable);
bool ReadShowGrid();

void SaveAlternativeSelection(bool enable);
bool ReadAlternativeSelection();

// void SaveLockMemoryAdd(bool enable);
// bool ReadLockMemoryAdd();

bool ReadLockMemoryEnable();
void SaveLockMemoryEnable(bool enable);

void SaveSingleClick(bool enable);
bool ReadSingleClick();

/*
void SaveUnderline(bool enable);
bool ReadUnderline();
*/

void SaveFlatView(UInt32 panelIndex, bool enable);
bool ReadFlatView(UInt32 panelIndex);

/*
void Save_ShowDeleted(bool enable);
bool Read_ShowDeleted();
*/

#endif
