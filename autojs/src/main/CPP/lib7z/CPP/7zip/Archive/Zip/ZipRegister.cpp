// ZipRegister.cpp

#include "StdAfx.h"

#include "../../Common/RegisterArc.h"

#include "ZipHandler.h"

namespace NArchive {
namespace NZip {

static const Byte k_Signature[] = {
    4, 0x50, 0x4B, 0x03, 0x04,
    4, 0x50, 0x4B, 0x05, 0x06,
    6, 0x50, 0x4B, 0x07, 0x08, 0x50, 0x4B,
    6, 0x50, 0x4B, 0x30, 0x30, 0x50, 0x4B };

REGISTER_ARC_IO(
  "zip", "zip z01 zipx jar xpi odt ods docx xlsx epub", 0, 1,
  k_Signature,
  0,
  NArcInfoFlags::kFindSignature |
  NArcInfoFlags::kMultiSignature |
  NArcInfoFlags::kUseGlobalOffset,
  IsArc_Zip)
 
}}
