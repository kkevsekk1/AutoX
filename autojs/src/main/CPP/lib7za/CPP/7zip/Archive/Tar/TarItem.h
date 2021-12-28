// TarItem.h

#ifndef __ARCHIVE_TAR_ITEM_H
#define __ARCHIVE_TAR_ITEM_H

#include "../Common/ItemNameUtils.h"

#include "TarHeader.h"

namespace NArchive {
namespace NTar {

struct CSparseBlock
{
  UInt64 Offset;
  UInt64 Size;
};

struct CItem
{
  AString Name;
  UInt64 PackSize;
  UInt64 Size;
  Int64 MTime;

  UInt32 Mode;
  UInt32 UID;
  UInt32 GID;
  UInt32 DeviceMajor;
  UInt32 DeviceMinor;

  AString LinkName;
  AString User;
  AString Group;

  char Magic[8];
  char LinkFlag;
  bool DeviceMajorDefined;
  bool DeviceMinorDefined;

  CRecordVector<CSparseBlock> SparseBlocks;

  bool IsSymLink() const { return LinkFlag == NFileHeader::NLinkFlag::kSymLink && (Size == 0); }
  bool IsHardLink() const { return LinkFlag == NFileHeader::NLinkFlag::kHardLink; }
  bool IsSparse() const { return LinkFlag == NFileHeader::NLinkFlag::kSparse; }
  UInt64 GetUnpackSize() const { return IsSymLink() ? LinkName.Len() : Size; }
  bool IsPaxExtendedHeader() const
  {
    switch (LinkFlag)
    {
      case 'g':
      case 'x':
      case 'X':  // Check it
        return true;
    }
    return false;
  }

  bool IsDir() const
  {
    switch (LinkFlag)
    {
      case NFileHeader::NLinkFlag::kDirectory:
      case NFileHeader::NLinkFlag::kDumpDir:
        return true;
      case NFileHeader::NLinkFlag::kOldNormal:
      case NFileHeader::NLinkFlag::kNormal:
      case NFileHeader::NLinkFlag::kSymLink:
        return NItemName::HasTailSlash(Name, CP_OEMCP);
    }
    return false;
  }

  bool IsUstarMagic() const
  {
    for (int i = 0; i < 5; i++)
      if (Magic[i] != NFileHeader::NMagic::kUsTar_00[i])
        return false;
    return true;
  }

  UInt64 GetPackSizeAligned() const { return (PackSize + 0x1FF) & (~((UInt64)0x1FF)); }
};

struct CItemEx: public CItem
{
  UInt64 HeaderPos;
  unsigned HeaderSize;
  bool NameCouldBeReduced;
  bool LinkNameCouldBeReduced;

  UInt64 GetDataPosition() const { return HeaderPos + HeaderSize; }
  UInt64 GetFullSize() const { return HeaderSize + PackSize; }
};

}}

#endif
