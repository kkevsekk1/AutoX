// RegistryAssociations.h

#ifndef __REGISTRYASSOCIATIONS_H
#define __REGISTRYASSOCIATIONS_H

#include "Common/MyString.h"

namespace NRegistryAssociations {

  /*
  struct CExtInfo
  {
    UString Ext;
    UStringVector Plugins;
    // bool Enabled;
  };
  bool ReadInternalAssociation(const wchar_t *ext, CExtInfo &extInfo);
  void ReadInternalAssociations(CObjectVector<CExtInfo> &items);
  void WriteInternalAssociations(const CObjectVector<CExtInfo> &items);
  */

  bool CheckShellExtensionInfo(const CSysString &extension, UString &iconPath, int &iconIndex);

  // void ReadCompressionInfo(NZipSettings::NCompression::CInfo &anInfo,
  void DeleteShellExtensionInfo(const CSysString &extension);

  void AddShellExtensionInfo(const CSysString &extension,
      const UString &programTitle,
      const UString &programOpenCommand,
      const UString &iconPath, int iconIndex,
      const void *shellNewData, int shellNewDataSize);


  ///////////////////////////
  // ContextMenu
  /*
  bool CheckContextMenuHandler();
  void AddContextMenuHandler();
  void DeleteContextMenuHandler();
  */

}

// bool GetProgramDirPrefix(CSysString &aFolder);

#endif
