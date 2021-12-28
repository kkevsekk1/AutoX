// PanelItems.cpp

#include "StdAfx.h"

#include "../../../../C/Sort.h"

#include "../../../Windows/FileName.h"
#include "../../../Windows/Menu.h"
#include "../../../Windows/PropVariant.h"
#include "../../../Windows/PropVariantConv.h"

#include "../../PropID.h"

#include "resource.h"

#include "LangUtils.h"
#include "Panel.h"
#include "PropertyName.h"
#include "RootFolder.h"

using namespace NWindows;

static bool GetColumnVisible(PROPID propID, bool isFsFolder)
{
  if (isFsFolder)
  {
    switch (propID)
    {
      case kpidATime:
      case kpidAttrib:
      case kpidPackSize:
      case kpidINode:
      case kpidLinks:
      case kpidNtReparse:
        return false;
    }
  }
  return true;
}

static int GetColumnWidth(PROPID propID, VARTYPE /* varType */)
{
  switch (propID)
  {
    case kpidName: return 160;
  }
  return 100;
}

static int GetColumnAlign(PROPID propID, VARTYPE varType)
{
  switch (propID)
  {
    case kpidCTime:
    case kpidATime:
    case kpidMTime:
      return LVCFMT_LEFT;
  }

  switch (varType)
  {
    case VT_UI1:
    case VT_I2:
    case VT_UI2:
    case VT_I4:
    case VT_INT:
    case VT_UI4:
    case VT_UINT:
    case VT_I8:
    case VT_UI8:
    case VT_BOOL:
      return LVCFMT_RIGHT;

    case VT_EMPTY:
    case VT_I1:
    case VT_FILETIME:
    case VT_BSTR:
      return LVCFMT_LEFT;

    default:
      return LVCFMT_CENTER;
  }
}

HRESULT CPanel::InitColumns()
{
printf("CPanel::InitColumns\n");
  if (_needSaveInfo)
    SaveListViewInfo();

  _listView.DeleteAllItems();
  _selectedStatusVector.Clear();
  // printf("CPanel::InitColumns : _selectedStatusVector.Clear()\n");

  ReadListViewInfo();

  // PROPID sortID;
  /*
  if (_listViewInfo.SortIndex >= 0)
    sortID = _listViewInfo.Columns[_listViewInfo.SortIndex].PropID;
  */
  // sortID = _listViewInfo.SortID;

  _ascending = _listViewInfo.Ascending;

  _properties.Clear();

  _needSaveInfo = true;
  bool isFsFolder = IsFSFolder() || IsAltStreamsFolder();

  {
    UInt32 numProps;
    _folder->GetNumberOfProperties(&numProps);

    for (UInt32 i = 0; i < numProps; i++)
    {
      CMyComBSTR name;
      PROPID propID;
      VARTYPE varType;
      HRESULT res = _folder->GetPropertyInfo(i, &name, &propID, &varType);

      if (res != S_OK)
      {
        /* We can return ERROR, but in that case, other code will not be called,
           and user can see empty window without error message. So we just ignore that field */
        continue;
      }
      if (propID == kpidIsDir)
        continue;
      CItemProperty prop;
      prop.Type = varType;
      prop.ID = propID;
      prop.Name = GetNameOfProperty(propID, name);
      prop.Order = -1;
      prop.IsVisible = GetColumnVisible(propID, isFsFolder);
      prop.Width = GetColumnWidth(propID, varType);
      prop.IsRawProp = false;
      _properties.Add(prop);
    }
  }

  if (_folderRawProps)
  {
    UInt32 numProps;
    _folderRawProps->GetNumRawProps(&numProps);
    for (UInt32 i = 0; i < numProps; i++)
    {
      CMyComBSTR name;
      PROPID propID;
      RINOK(_folderRawProps->GetRawPropInfo(i, &name, &propID));

      CItemProperty prop;
      prop.Type = VT_EMPTY;
      prop.ID = propID;
      prop.Name = GetNameOfProperty(propID, name);
      prop.Order = -1;
      prop.IsVisible = GetColumnVisible(propID, isFsFolder);
      prop.Width = GetColumnWidth(propID, VT_BSTR);;
      prop.IsRawProp = true;
      _properties.Add(prop);
    }
  }

  // InitColumns2(sortID);

  for (;;)
    if (!_listView.DeleteColumn(0))
      break;

  unsigned order = 0;
  unsigned i;
  for (i = 0; i < _listViewInfo.Columns.Size(); i++)
  {
    const CColumnInfo &columnInfo = _listViewInfo.Columns[i];
    int index = _properties.FindItemWithID(columnInfo.PropID);
    if (index >= 0)
    {
      CItemProperty &item = _properties[index];
      item.IsVisible = columnInfo.IsVisible;
      item.Width = columnInfo.Width;
      if (columnInfo.IsVisible)
        item.Order = order++;
      continue;
    }
  }

  for (i = 0; i < _properties.Size(); i++)
  {
    CItemProperty &item = _properties[i];
    if (item.Order < 0)
      item.Order = order++;
  }

  _visibleProperties.Clear();
  for (i = 0; i < _properties.Size(); i++)
  {
    const CItemProperty &prop = _properties[i];
    if (prop.IsVisible)
      _visibleProperties.Add(prop);
  }

  // _sortIndex = 0;
  _sortID = kpidName;
  /*
  if (_listViewInfo.SortIndex >= 0)
  {
    int sortIndex = _properties.FindItemWithID(sortID);
    if (sortIndex >= 0)
      _sortIndex = sortIndex;
  }
  */
  _sortID = _listViewInfo.SortID;

  _visibleProperties.Sort();
  for (i = 0; i < _visibleProperties.Size(); i++)
  {
    InsertColumn(i);
  }
  return S_OK;
}

void CPanel::InsertColumn(unsigned index)
{
  const CItemProperty &prop = _visibleProperties[index];
  LV_COLUMNW column;
  column.mask = LVCF_FMT | LVCF_WIDTH | LVCF_TEXT | LVCF_SUBITEM | LVCF_ORDER;
  column.cx = prop.Width;
  column.fmt = GetColumnAlign(prop.ID, prop.Type);
  // FIXME not available column.iOrder = prop.Order;
  // iOrder must be <= _listView.ItemCount
  column.iSubItem = index;
  column.pszText = const_cast<wchar_t *>((const wchar_t *)prop.Name);
  // printf("CPanel::InsertColumn(%d)=>'%ls'\n",index,column.pszText); fflush(stdout);
  _listView.InsertColumn(index, &column);
}

HRESULT CPanel::RefreshListCtrl()
{
  return RefreshListCtrl(UString(), -1, true, UStringVector());
}

// int CALLBACK CompareItems(LPARAM lParam1, LPARAM lParam2, LPARAM lpData);
int
#if defined(__WIN32__) && !defined(__WXMICROWIN__) // FIXME
  wxCALLBACK
#endif
 CompareItems_WX(long item1, long item2, long sortData);


void CPanel::GetSelectedNames(UStringVector &selectedNames)
{
  CRecordVector<UInt32> indices;
  GetSelectedItemsIndices(indices);
  selectedNames.ClearAndReserve(indices.Size());
  FOR_VECTOR (i, indices)
    selectedNames.AddInReserved(GetItemRelPath(indices[i]));

  /*
  for (int i = 0; i < _listView.GetItemCount(); i++)
  {
    const int kSize = 1024;
    WCHAR name[kSize + 1];
    LVITEMW item;
    item.iItem = i;
    item.pszText = name;
    item.cchTextMax = kSize;
    item.iSubItem = 0;
    item.mask = LVIF_TEXT | LVIF_PARAM;
    if (!_listView.GetItem(&item))
      continue;
    int realIndex = GetRealIndex(item);
    if (realIndex == kParentIndex)
      continue;
    if (_selectedStatusVector[realIndex])
      selectedNames.Add(item.pszText);
  }
  */
  selectedNames.Sort();
}

void CPanel::SaveSelectedState(CSelectedState &s)
{
  s.FocusedName.Empty();
  s.SelectedNames.Clear();
  s.FocusedItem = _listView.GetFocusedItem();
  {
    if (s.FocusedItem >= 0)
    {
      int realIndex = GetRealItemIndex(s.FocusedItem);
      if (realIndex != kParentIndex)
        s.FocusedName = GetItemRelPath(realIndex);
        /*
        const int kSize = 1024;
        WCHAR name[kSize + 1];
        LVITEMW item;
        item.iItem = focusedItem;
        item.pszText = name;
        item.cchTextMax = kSize;
        item.iSubItem = 0;
        item.mask = LVIF_TEXT;
        if (_listView.GetItem(&item))
        focusedName = item.pszText;
      */
    }
  }
  GetSelectedNames(s.SelectedNames);
}

HRESULT CPanel::RefreshListCtrl(const CSelectedState &s)
{
printf("CPanel::RefreshListCtrl\n");
  bool selectFocused = s.SelectFocused;
  if (_mySelectMode)
    selectFocused = true;
  return RefreshListCtrl(s.FocusedName, s.FocusedItem, selectFocused, s.SelectedNames);
}

HRESULT CPanel::RefreshListCtrlSaveFocused()
{
  CSelectedState state;
  SaveSelectedState(state);
  return RefreshListCtrl(state);
}

void CPanel::SetFocusedSelectedItem(int index, bool select)
{
  UINT state = LVIS_FOCUSED;
  if (select)
    state |= LVIS_SELECTED;
  _listView.SetItemState(index, state, state);
  if (!_mySelectMode && select)
  {
    int realIndex = GetRealItemIndex(index);
    if (realIndex != kParentIndex)
    {
       // printf("CPanel::SetFocusedSelectedItem(%d,%d) : _selectedStatusVector[%d]=%d => true\n",index,select,realIndex,_selectedStatusVector[realIndex]);
      _selectedStatusVector[realIndex] = true;
    }
  }
}

HRESULT CPanel::RefreshListCtrl(const UString &focusedName, int focusedPos, bool selectFocused,
    const UStringVector &selectedNames)
{
  _dontShowMode = false;
  LoadFullPathAndShow();
  // OutputDebugStringA("=======\n");
  // OutputDebugStringA("s1 \n");
  CDisableTimerProcessing timerProcessing(*this);
  CDisableNotify disableNotify(*this);

  if (focusedPos < 0)
    focusedPos = 0;

  _listView.SetRedraw(false);
  // m_RedrawEnabled = false;

#ifdef _WIN32
  LVITEMW item;
  ZeroMemory(&item, sizeof(item));
#else
  LVITEMW item = { 0 };
#endif

  // DWORD tickCount0 = GetTickCount();
  _enableItemChangeNotify = false;
  _listView.DeleteAllItems();
  _enableItemChangeNotify = true;


  int listViewItemCount = 0;

  // printf("&&&&&&&&&&&&&& _selectedStatusVector.Clear() &&&&&&&&&&&&&&\n");
  _selectedStatusVector.Clear();
  // _realIndices.Clear();
  _startGroupSelect = 0;

  _selectionIsDefined = false;

  // m_Files.Clear();

  if (!_folder)
  {
    // throw 1;
    SetToRootFolder();
  }

  // FIXME _headerToolBar.EnableButton(kParentFolderID, !IsRootFolder());

  {
    CMyComPtr<IFolderSetFlatMode> folderSetFlatMode;
    _folder.QueryInterface(IID_IFolderSetFlatMode, &folderSetFlatMode);
    if (folderSetFlatMode)
      folderSetFlatMode->SetFlatMode(BoolToInt(_flatMode));
  }

  /*
  {
    CMyComPtr<IFolderSetShowNtfsStreamsMode> setShow;
    _folder.QueryInterface(IID_IFolderSetShowNtfsStreamsMode, &setShow);
    if (setShow)
      setShow->SetShowNtfsStreamsMode(BoolToInt(_showNtfsStrems_Mode));
  }
  */

  // DWORD tickCount1 = GetTickCount();
  RINOK(_folder->LoadItems());
  // DWORD tickCount2 = GetTickCount();
  RINOK(InitColumns());

  // OutputDebugString(TEXT("Start Dir\n"));
  UInt32 numItems;
  _folder->GetNumberOfItems(&numItems);

  bool showDots = _showDots && !IsRootFolder();

  _listView.SetItemCount(numItems + (showDots ? 1 : 0));

  _selectedStatusVector.ClearAndReserve(numItems);
  // printf("_selectedStatusVector.ClearAndReserve(%d)\n",numItems);
  int cursorIndex = -1;

  CMyComPtr<IFolderGetSystemIconIndex> folderGetSystemIconIndex;
  if (!Is_Slow_Icon_Folder() || _showRealFileIcons)
    _folder.QueryInterface(IID_IFolderGetSystemIconIndex, &folderGetSystemIconIndex);

  if (!IsFSFolder())
  {
    CMyComPtr<IGetFolderArcProps> getFolderArcProps;
    _folder.QueryInterface(IID_IGetFolderArcProps, &getFolderArcProps);
    _thereAreDeletedItems = false;
    if (getFolderArcProps)
    {
      CMyComPtr<IFolderArcProps> arcProps;
      getFolderArcProps->GetFolderArcProps(&arcProps);
      if (arcProps)
      {
        UInt32 numLevels;
        if (arcProps->GetArcNumLevels(&numLevels) != S_OK)
          numLevels = 0;
        NCOM::CPropVariant prop;
        if (arcProps->GetArcProp(numLevels - 1, kpidIsDeleted, &prop) == S_OK)
          if (prop.vt == VT_BOOL && VARIANT_BOOLToBool(prop.boolVal))
            _thereAreDeletedItems = true;
      }
    }
  }

  if (showDots)
  {
    UString itemName = L"..";
    item.iItem = listViewItemCount;
    if (itemName == focusedName)
      cursorIndex = item.iItem;
    item.mask = LVIF_TEXT | LVIF_PARAM | LVIF_IMAGE;
    int subItem = 0;
    item.iSubItem = subItem++;
    item.lParam = kParentIndex;
    item.pszText = const_cast<wchar_t *>((const wchar_t *)itemName); // FIXME item.pszText = LPSTR_TEXTCALLBACKW;
    UInt32 attrib = FILE_ATTRIBUTE_DIRECTORY;
    item.iImage = _extToIconMap.GetIconIndex(attrib, itemName);
    if (item.iImage < 0)
      item.iImage = 0;
    if (_listView.InsertItem(&item) == -1)
      return E_FAIL;

    listViewItemCount++;
  }

  // OutputDebugStringA("S1\n");

  UString correctedName;
  UString itemName;
  UString relPath;

  printf("ADD ITEMS - BEGIN\n");
  for (UInt32 i = 0; i < numItems; i++)
  {
    const wchar_t *name = NULL;
    unsigned nameLen = 0;

    if (_folderGetItemName)
      _folderGetItemName->GetItemName(i, &name, &nameLen);
    if (!name)
    {
      GetItemName(i, itemName);
      name = itemName;
      nameLen = itemName.Len();
    }

    bool selected = false;

    if (!focusedName.IsEmpty() || !selectedNames.IsEmpty())
    {
      relPath.Empty();

      // relPath += GetItemPrefix(i);
      // change it (_flatMode)
      if (i != kParentIndex && _flatMode)
      {
        const wchar_t *prefix = NULL;
        if (_folderGetItemName)
        {
          unsigned prefixLen = 0;
          _folderGetItemName->GetItemPrefix(i, &prefix, &prefixLen);
          if (prefix)
            relPath += prefix;
        }
        if (!prefix)
        {
          NCOM::CPropVariant prop;
          if (_folder->GetProperty(i, kpidPrefix, &prop) != S_OK)
            throw 2723400;
          if (prop.vt == VT_BSTR)
            relPath += prop.bstrVal;
        }
      }
      relPath += name;
      if (relPath == focusedName)
        cursorIndex = listViewItemCount;
      if (selectedNames.FindInSorted(relPath) >= 0)
        selected = true;
    }

    _selectedStatusVector.AddInReserved(selected);
    // printf("_selectedStatusVector.AddInReserved(%d)\n",selected);


    item.mask = LVIF_TEXT | LVIF_PARAM | LVIF_IMAGE;

    if (!_mySelectMode)
      if (selected)
      {
        item.mask |= LVIF_STATE;
        item.state = LVIS_SELECTED;
      }

    int subItem = 0;
    item.iItem = listViewItemCount;

    item.iSubItem = subItem++;
    item.lParam = i;

    /*
    int finish = nameLen - 4;
    int j;
    for (j = 0; j < finish; j++)
    {
      if (name[j    ] == ' ' &&
          name[j + 1] == ' ' &&
          name[j + 2] == ' ' &&
          name[j + 3] == ' ' &&
          name[j + 4] == ' ')
        break;
    }
    if (j < finish)
    {
      correctedName.Empty();
      correctedName = L"virus";
      int pos = 0;
      for (;;)
      {
        int posNew = itemName.Find(L"     ", pos);
        if (posNew < 0)
        {
          correctedName += itemName.Ptr(pos);
          break;
        }
        correctedName += itemName.Mid(pos, posNew - pos);
        correctedName += L" ... ";
        pos = posNew;
        while (itemName[++pos] == ' ');
      }
      item.pszText = const_cast<wchar_t *>((const wchar_t *)correctedName);
    }
    else
    */
    {
      item.pszText = const_cast<wchar_t *>((const wchar_t *)name); // FIXME item.pszText = LPSTR_TEXTCALLBACKW;
      /* LPSTR_TEXTCALLBACKW works, but in some cases there are problems,
      since we block notify handler. */
    }

    UInt32 attrib = 0;
    // for (int yyy = 0; yyy < 6000000; yyy++) {
    NCOM::CPropVariant prop;
    RINOK(_folder->GetProperty(i, kpidAttrib, &prop));
    if (prop.vt == VT_UI4)
    {
      // char s[256]; sprintf(s, "attrib = %7x", attrib); OutputDebugStringA(s);
      attrib = prop.ulVal;
    }
    else if (IsItem_Folder(i))
      attrib |= FILE_ATTRIBUTE_DIRECTORY;
    // }

    bool defined = false;
#ifdef _WIN32
    if (folderGetSystemIconIndex)
    {
      folderGetSystemIconIndex->GetSystemIconIndex(i, &item.iImage);
      defined = (item.iImage > 0);
    }
#endif
    if (!defined)
    {
      if (_currentFolderPrefix.IsEmpty())
      {
        int iconIndexTemp;
        GetRealIconIndex(us2fs((UString)name) + FCHAR_PATH_SEPARATOR, attrib, iconIndexTemp);
        item.iImage = iconIndexTemp;
      }
      else
      {
        item.iImage = _extToIconMap.GetIconIndex(attrib, name);
      }
    }

    if (item.iImage < 0)
      item.iImage = 0;

    if (_listView.InsertItem(&item) == -1)
      return E_FAIL;


    // FIXME Added
    item.pszText = (LPWSTR)malloc(4096); // FIXME
    for(int col=1;col <  _listView.GetColumnCount(); col++)
    {
      item.iSubItem = col;
      item.cchTextMax = 4096 / sizeof(item.pszText[0]);
      this->SetItemText(item);
      _listView.SetItem(&item);
    }
    free(item.pszText); item.pszText = 0;

    listViewItemCount++;
  }
  printf("ADD ITEMS - END\n");

  // OutputDebugStringA("End2\n");

#if _WIN32
  // FIXME : with wxWidget, after sortitems, the item is unselected and unfocused
  if (_listView.GetItemCount() > 0 && cursorIndex >= 0)
    SetFocusedSelectedItem(cursorIndex, selectFocused);
#endif
  // DWORD tickCount3 = GetTickCount();
  SetSortRawStatus();
  _listView.SortItems(CompareItems_WX, (LPARAM)this);



  // DWORD tickCount4 = GetTickCount();
  if (cursorIndex < 0 && _listView.GetItemCount() > 0)
  {
    if (focusedPos >= _listView.GetItemCount())
      focusedPos = _listView.GetItemCount() - 1;
    // we select item only in showDots mode.
    SetFocusedSelectedItem(focusedPos, showDots);
  }
  // m_RedrawEnabled = true;
  // DWORD tickCount5 = GetTickCount();
  _listView.EnsureVisible(_listView.GetFocusedItem(), false);
  // DWORD tickCount6 = GetTickCount();

  disableNotify.SetMemMode_Enable();
  disableNotify.Restore();
  _listView.SetRedraw(true);
  // DWORD tickCount7 = GetTickCount();
  _listView.InvalidateRect(NULL, true);
  // DWORD tickCount8 = GetTickCount();
  // OutputDebugStringA("End1\n");
  /*
  _listView.UpdateWindow();
  */
  Refresh_StatusBar();
  // DWORD tickCount9 = GetTickCount();
  /*
  char s[256];
  sprintf(s,
      // "attribMap = %5d, extMap = %5d, "
      "delete = %5d, load = %5d, list = %5d, sort = %5d, end = %5d",
      // _extToIconMap._attribMap.Size(),
      // _extToIconMap._extMap.Size(),
      tickCount1 - tickCount0,
      tickCount2 - tickCount1,
      tickCount3 - tickCount2,
      tickCount4 - tickCount3,
      tickCount5 - tickCount4
      );
  sprintf(s,
      "5 = %5d, 6 = %5d, 7 = %5d, 8 = %5d, 9 = %5d",
      tickCount5 - tickCount4,
      tickCount6 - tickCount5,
      tickCount7 - tickCount6,
      tickCount8 - tickCount7,
      tickCount9 - tickCount8
      );
  OutputDebugStringA(s);
  */
  return S_OK;
}

void CPanel::GetSelectedItemsIndices(CRecordVector<UInt32> &indices) const
{
  indices.Clear();
  /*
  int itemIndex = -1;
  while ((itemIndex = _listView.GetNextSelectedItem(itemIndex)) != -1)
  {
    LPARAM param;
    if (_listView.GetItemParam(itemIndex, param))
      indices.Add(param);
  }
  */
  FOR_VECTOR (i, _selectedStatusVector)
    if (_selectedStatusVector[i])
      indices.Add(i);
  // HeapSort(&indices.Front(), indices.Size());
}

void CPanel::GetOperatedItemIndices(CRecordVector<UInt32> &indices) const
{
  GetSelectedItemsIndices(indices);
  if (!indices.IsEmpty())
    return;
  if (_listView.GetSelectedCount() == 0)
    return;
  int focusedItem = _listView.GetFocusedItem();
  if (focusedItem >= 0)
  {
    if (_listView.IsItemSelected(focusedItem))
    {
      int realIndex = GetRealItemIndex(focusedItem);
      if (realIndex != kParentIndex)
        indices.Add(realIndex);
    }
  }
}

void CPanel::GetAllItemIndices(CRecordVector<UInt32> &indices) const
{
  indices.Clear();
  UInt32 numItems;
  if (_folder->GetNumberOfItems(&numItems) == S_OK)
    for (UInt32 i = 0; i < numItems; i++)
      indices.Add(i);
}

void CPanel::GetOperatedIndicesSmart(CRecordVector<UInt32> &indices) const
{
  GetOperatedItemIndices(indices);
  if (indices.IsEmpty() || (indices.Size() == 1 && indices[0] == (UInt32)(Int32)-1))
    GetAllItemIndices(indices);
}

/*
void CPanel::GetOperatedListViewIndices(CRecordVector<UInt32> &indices) const
{
  indices.Clear();
  int numItems = _listView.GetItemCount();
  for (int i = 0; i < numItems; i++)
  {
    int realIndex = GetRealItemIndex(i);
    if (realIndex >= 0)
      if (_selectedStatusVector[realIndex])
        indices.Add(i);
  }
  if (indices.IsEmpty())
  {
    int focusedItem = _listView.GetFocusedItem();
      if (focusedItem >= 0)
        indices.Add(focusedItem);
  }
}
*/

void CPanel::EditItem(bool useEditor)
{
  if (!useEditor)
  {
    CMyComPtr<IFolderCalcItemFullSize> calcItemFullSize;
    _folder.QueryInterface(IID_IFolderCalcItemFullSize, &calcItemFullSize);
    if (calcItemFullSize)
    {
      bool needRefresh = false;
      CRecordVector<UInt32> indices;
      GetOperatedItemIndices(indices);
      FOR_VECTOR (i, indices)
      {
        UInt32 index = indices[i];
        if (IsItem_Folder(index))
        {
          calcItemFullSize->CalcItemFullSize(index, NULL);
          needRefresh = true;
        }
      }
      if (needRefresh)
      {
        // _listView.RedrawItem(0);
        // _listView.RedrawAllItems();
        InvalidateList();
        return;
      }
    }
  }


  int focusedItem = _listView.GetFocusedItem();
  if (focusedItem < 0)
    return;
  int realIndex = GetRealItemIndex(focusedItem);
  if (realIndex == kParentIndex)
    return;
  if (!IsItem_Folder(realIndex))
    EditItem(realIndex, useEditor);
}

void CPanel::OpenFocusedItemAsInternal(const wchar_t *type)
{
  int focusedItem = _listView.GetFocusedItem();
  if (focusedItem < 0)
    return;
  int realIndex = GetRealItemIndex(focusedItem);
  if (IsItem_Folder(realIndex))
    OpenFolder(realIndex);
  else
    OpenItem(realIndex, true, false, type);
}

void CPanel::OpenSelectedItems(bool tryInternal)
{
  CRecordVector<UInt32> indices;
  GetOperatedItemIndices(indices);
  if (indices.Size() > 20)
  {
    MessageBoxErrorLang(IDS_TOO_MANY_ITEMS);
    return;
  }

  int focusedItem = _listView.GetFocusedItem();

  // printf("###### CPanel::OpenSelectedItems(tryInternal=%d)-1 focusedItem=%d indices.Size()=%d\n",(int)tryInternal,focusedItem,(int)indices.Size());

  if (focusedItem >= 0)
  {
    int realIndex = GetRealItemIndex(focusedItem);
    if (realIndex == kParentIndex && (tryInternal || indices.Size() == 0) && _listView.IsItemSelected(focusedItem))
      indices.Insert(0, realIndex);
      // printf("###### CPanel::OpenSelectedItems(tryInternal=%d) realIndex=%d indices.Size()=%d\n",(int)tryInternal,realIndex,(int)indices.Size());
  }

  bool dirIsStarted = false;
  FOR_VECTOR (i, indices)
  {
    UInt32 index = indices[i];
    // CFileInfo &aFile = m_Files[index];
    if (IsItem_Folder(index))
    {
      if (!dirIsStarted)
      {
        if (tryInternal)
        {
          OpenFolder(index);
          dirIsStarted = true;
          break;
        }
        else
          OpenFolderExternal(index);
      }
    }
    else
      OpenItem(index, (tryInternal && indices.Size() == 1), true);
  }
}

UString CPanel::GetItemName(int itemIndex) const
{
  if (itemIndex == kParentIndex)
    return L"..";
  NCOM::CPropVariant prop;
  if (_folder->GetProperty(itemIndex, kpidName, &prop) != S_OK)
    throw 2723400;
  if (prop.vt != VT_BSTR)
    throw 2723401;
  return prop.bstrVal;
}

UString CPanel::GetItemName_for_Copy(int itemIndex) const
{
  if (itemIndex == kParentIndex)
    return L"..";
  {
    NCOM::CPropVariant prop;
    if (_folder->GetProperty(itemIndex, kpidOutName, &prop) == S_OK)
    {
      if (prop.vt == VT_BSTR)
        return prop.bstrVal;
      if (prop.vt != VT_EMPTY)
        throw 2723401;
    }
  }
  return GetItemName(itemIndex);
}

void CPanel::GetItemName(int itemIndex, UString &s) const
{
  if (itemIndex == kParentIndex)
  {
    s = L"..";
    return;
  }
  NCOM::CPropVariant prop;
  if (_folder->GetProperty(itemIndex, kpidName, &prop) != S_OK)
    throw 2723400;
  if (prop.vt != VT_BSTR)
    throw 2723401;
  s.SetFromBstr(prop.bstrVal);
}

UString CPanel::GetItemPrefix(int itemIndex) const
{
  if (itemIndex == kParentIndex)
    return UString();
  NCOM::CPropVariant prop;
  if (_folder->GetProperty(itemIndex, kpidPrefix, &prop) != S_OK)
    throw 2723400;
  UString prefix;
  if (prop.vt == VT_BSTR)
    prefix.SetFromBstr(prop.bstrVal);
  return prefix;
}

UString CPanel::GetItemRelPath(int itemIndex) const
{
  return GetItemPrefix(itemIndex) + GetItemName(itemIndex);
}

UString CPanel::GetItemRelPath2(int itemIndex) const
{
  UString s = GetItemRelPath(itemIndex);
  #if defined(_WIN32) && !defined(UNDER_CE)
  if (s.Len() == 2 && NFile::NName::IsDrivePath2(s))
  {
    if (IsFSDrivesFolder() && !IsDeviceDrivesPrefix())
      s.Add_PathSepar();
  }
  #endif
  return s;
}

UString CPanel::GetItemFullPath(int itemIndex) const
{
  return GetFsPath() + GetItemRelPath2(itemIndex);
}

bool CPanel::GetItem_BoolProp(UInt32 itemIndex, PROPID propID) const
{
  NCOM::CPropVariant prop;
  if (_folder->GetProperty(itemIndex, propID, &prop) != S_OK)
    throw 2723400;
  if (prop.vt == VT_BOOL)
    return VARIANT_BOOLToBool(prop.boolVal);
  if (prop.vt == VT_EMPTY)
    return false;
  throw 2723401;
}

bool CPanel::IsItem_Deleted(int itemIndex) const
{
  if (itemIndex == kParentIndex)
    return false;
  return GetItem_BoolProp(itemIndex, kpidIsDeleted);
}

bool CPanel::IsItem_Folder(int itemIndex) const
{
  if (itemIndex == kParentIndex)
    return true;
  return GetItem_BoolProp(itemIndex, kpidIsDir);
}

bool CPanel::IsItem_AltStream(int itemIndex) const
{
  if (itemIndex == kParentIndex)
    return false;
  return GetItem_BoolProp(itemIndex, kpidIsAltStream);
}

UInt64 CPanel::GetItemSize(int itemIndex) const
{
  if (itemIndex == kParentIndex)
    return 0;
  if (_folderGetItemName)
    return _folderGetItemName->GetItemSize(itemIndex);
  NCOM::CPropVariant prop;
  if (_folder->GetProperty(itemIndex, kpidSize, &prop) != S_OK)
    throw 2723400;
  UInt64 val = 0;
  if (ConvertPropVariantToUInt64(prop, val))
    return val;
  return 0;
}

void CPanel::ReadListViewInfo()
{
  _typeIDString = GetFolderTypeID();
  if (!_typeIDString.IsEmpty())
    _listViewInfo.Read(_typeIDString);
}

void CPanel::SaveListViewInfo()
{
  unsigned i;

  printf("CPanel::SaveListViewInfo - BEGIN\n");
  for (i = 0; i < _visibleProperties.Size(); i++)
  {
    CItemProperty &prop = _visibleProperties[i];
    LVCOLUMN winColumnInfo;
    winColumnInfo.mask = LVCF_ORDER | LVCF_WIDTH;
    if (!_listView.GetColumn(i, &winColumnInfo))
      throw 1;
    // FIXME printf("CPanel::SaveListViewInfo - i=%d  ID=%d Order=%d => %d\n",i,prop.ID,prop.Order,winColumnInfo.iOrder);
    // FIXME prop.Order = winColumnInfo.iOrder;  winColumnInfo.iOrder not set with _listView.GetColumn
    prop.Width = winColumnInfo.cx;
  }
  printf("CPanel::SaveListViewInfo - END\n");

  CListViewInfo viewInfo;

  // PROPID sortPropID = _properties[_sortIndex].ID;
  PROPID sortPropID = _sortID;

  _visibleProperties.Sort();

  for (i = 0; i < _visibleProperties.Size(); i++)
  {
    const CItemProperty &prop = _visibleProperties[i];
    CColumnInfo columnInfo;
    columnInfo.IsVisible = prop.IsVisible;
    columnInfo.PropID = prop.ID;
    columnInfo.Width = prop.Width;
    viewInfo.Columns.Add(columnInfo);
  }

  for (i = 0; i < _properties.Size(); i++)
  {
    const CItemProperty &prop = _properties[i];
    if (!prop.IsVisible)
    {
      CColumnInfo columnInfo;
      columnInfo.IsVisible = prop.IsVisible;
      columnInfo.PropID = prop.ID;
      columnInfo.Width = prop.Width;
      viewInfo.Columns.Add(columnInfo);
    }
  }

  viewInfo.SortID = sortPropID;
  viewInfo.Ascending = _ascending;
  if (!_listViewInfo.IsEqual(viewInfo))
  {
    viewInfo.Save(_typeIDString);
    _listViewInfo = viewInfo;
  }
}

#ifdef _WIN32
bool CPanel::OnRightClick(MY_NMLISTVIEW_NMITEMACTIVATE *itemActiveate, LRESULT &result)
{
  if (itemActiveate->hdr.hwndFrom == HWND(_listView))
    return false;
  POINT point;
  ::GetCursorPos(&point);
  ShowColumnsContextMenu(point.x, point.y);
  result = TRUE;
  return true;
}

void CPanel::ShowColumnsContextMenu(int x, int y)
{
  CMenu menu;
  CMenuDestroyer menuDestroyer(menu);

  menu.CreatePopup();

  const int kCommandStart = 100;
  FOR_VECTOR (i, _properties)
  {
    const CItemProperty &prop = _properties[i];
    UINT flags =  MF_STRING;
    if (prop.IsVisible)
      flags |= MF_CHECKED;
    if (i == 0)
      flags |= MF_GRAYED;
    menu.AppendItem(flags, kCommandStart + i, prop.Name);
  }

  int menuResult = menu.Track(TPM_LEFTALIGN | TPM_RETURNCMD | TPM_NONOTIFY, x, y, _listView);

  if (menuResult >= kCommandStart && menuResult <= kCommandStart + (int)_properties.Size())
  {
    int index = menuResult - kCommandStart;
    CItemProperty &prop = _properties[index];
    prop.IsVisible = !prop.IsVisible;

    if (prop.IsVisible)
    {
      unsigned num = _visibleProperties.Size();
      prop.Order = num;
      _visibleProperties.Add(prop);
      InsertColumn(num);
    }
    else
    {
      int visibleIndex = _visibleProperties.FindItemWithID(prop.ID);
      if (visibleIndex >= 0)
      {
        _visibleProperties.Delete(visibleIndex);
        /*
        if (_sortIndex == index)
        {
        _sortIndex = 0;
        _ascending = true;
        }
        */
        if (_sortID == prop.ID)
        {
          _sortID = kpidName;
          _ascending = true;
        }

        _listView.DeleteColumn(visibleIndex);
      }
    }
  }
}
#endif

void CPanel::OnReload()
{
  HRESULT res = RefreshListCtrlSaveFocused();
  if (res != S_OK)
    MessageBoxError(res);
}

void CPanel::OnTimer()
{
  if (!_processTimer)
    return;
  if (!AutoRefresh_Mode)
    return;
  CMyComPtr<IFolderWasChanged> folderWasChanged;
  if (_folder.QueryInterface(IID_IFolderWasChanged, &folderWasChanged) != S_OK)
    return;
  Int32 wasChanged;
  if (folderWasChanged->WasChanged(&wasChanged) != S_OK)
    return;
  if (wasChanged == 0)
    return;
  OnReload();
}
