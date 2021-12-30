// ElfHandler.cpp

#include "StdAfx.h"

#include "../../../C/CpuArch.h"

#include "../../Common/ComTry.h"
#include "../../Common/IntToString.h"
#include "../../Common/MyBuffer.h"

#include "../../Windows/PropVariantUtils.h"

#include "../Common/LimitedStreams.h"
#include "../Common/ProgressUtils.h"
#include "../Common/RegisterArc.h"
#include "../Common/StreamUtils.h"

#include "../Compress/CopyCoder.h"

using namespace NWindows;

static UInt16 Get16(const Byte *p, bool be) { if (be) return GetBe16(p); return GetUi16(p); }
static UInt32 Get32(const Byte *p, bool be) { if (be) return GetBe32(p); return GetUi32(p); }
static UInt64 Get64(const Byte *p, bool be) { if (be) return GetBe64(p); return GetUi64(p); }

#define G16(offs, v) v = Get16(p + (offs), be)
#define G32(offs, v) v = Get32(p + (offs), be)
#define G64(offs, v) v = Get64(p + (offs), be)

namespace NArchive {
namespace NElf {

/*
   ELF Structure for most files (real order can be different):
   Header
   Program (segment) header table (used at runtime)
     Segment1 (Section ... Section)
     Segment2
     ...
     SegmentN
   Section header table (the data for linking and relocation)
*/

#define ELF_CLASS_32 1
#define ELF_CLASS_64 2

#define ELF_DATA_2LSB 1
#define ELF_DATA_2MSB 2

static const UInt32 kHeaderSize32 = 0x34;
static const UInt32 kHeaderSize64 = 0x40;

static const UInt32 kSegmentSize32 = 0x20;
static const UInt32 kSegmentSize64 = 0x38;

static const UInt32 kSectionSize32 = 0x28;
static const UInt32 kSectionSize64 = 0x40;

struct CHeader
{
  bool Mode64;
  bool Be;
  Byte Os;
  Byte AbiVer;

  UInt16 Type;
  UInt16 Machine;
  // UInt32 Version;

  // UInt64 EntryVa;
  UInt64 ProgOffset;
  UInt64 SectOffset;
  UInt32 Flags;
  UInt16 HeaderSize;
  UInt16 SegmentEntrySize;
  UInt16 NumSegments;
  UInt16 SectionEntrySize;
  UInt16 NumSections;
  UInt16 NamesSectIndex;

  bool Parse(const Byte *buf);

  UInt64 GetHeadersSize() const { return (UInt64)HeaderSize +
      (UInt32)NumSegments * SegmentEntrySize +
      (UInt32)NumSections * SectionEntrySize; }
};

bool CHeader::Parse(const Byte *p)
{
  switch (p[4])
  {
    case ELF_CLASS_32: Mode64 = false; break;
    case ELF_CLASS_64: Mode64 = true; break;
    default: return false;
  }
  bool be;
  switch (p[5])
  {
    case ELF_DATA_2LSB: be = false; break;
    case ELF_DATA_2MSB: be = true; break;
    default: return false;
  }
  Be = be;
  if (p[6] != 1) // Version
    return false;
  Os = p[7];
  AbiVer = p[8];
  for (int i = 9; i < 16; i++)
    if (p[i] != 0)
      return false;

  G16(0x10, Type);
  G16(0x12, Machine);
  if (Get32(p + 0x14, be) != 1) // Version
    return false;

  if (Mode64)
  {
    // G64(0x18, EntryVa);
    G64(0x20, ProgOffset);
    G64(0x28, SectOffset);
    p += 0x30;
  }
  else
  {
    // G32(0x18, EntryVa);
    G32(0x1C, ProgOffset);
    G32(0x20, SectOffset);
    p += 0x24;
  }

  G32(0, Flags);
  G16(4, HeaderSize);
  if (HeaderSize != (Mode64 ? kHeaderSize64 : kHeaderSize32))
    return false;

  G16(6, SegmentEntrySize);
  G16(8, NumSegments);
  G16(10, SectionEntrySize);
  G16(12, NumSections);
  G16(14, NamesSectIndex);

  if (ProgOffset < HeaderSize && (ProgOffset != 0 || NumSegments != 0)) return false;
  if (SectOffset < HeaderSize && (SectOffset != 0 || NumSections != 0)) return false;

  if (SegmentEntrySize == 0) { if (NumSegments != 0) return false; }
  else if (SegmentEntrySize != (Mode64 ? kSegmentSize64 : kSegmentSize32)) return false;

  if (SectionEntrySize == 0) { if (NumSections != 0) return false; }
  else if (SectionEntrySize != (Mode64 ? kSectionSize64 : kSectionSize32)) return false;

  return true;
}

// The program header table itself.

#define PT_PHDR 6

static const char *g_SegnmentTypes[] =
{
  "Unused",
  "Loadable segment",
  "Dynamic linking tables",
  "Program interpreter path name",
  "Note section",
  "SHLIB",
  "Program header table",
  "TLS"
};

static const CUInt32PCharPair g_SegmentFlags[] =
{
  { 0, "Execute" },
  { 1, "Write" },
  { 2, "Read" }
};

struct CSegment
{
  UInt32 Type;
  UInt32 Flags;
  UInt64 Offset;
  UInt64 Va;
  // UInt64 Pa;
  UInt64 Size;
  UInt64 VSize;
  UInt64 Align;

  void UpdateTotalSize(UInt64 &totalSize)
  {
    UInt64 t = Offset + Size;
    if (totalSize < t)
      totalSize = t;
  }
  void Parse(const Byte *p, bool mode64, bool be);
};

void CSegment::Parse(const Byte *p, bool mode64, bool be)
{
  G32(0, Type);
  if (mode64)
  {
    G32(4, Flags);
    G64(8, Offset);
    G64(0x10, Va);
    // G64(0x18, Pa);
    G64(0x20, Size);
    G64(0x28, VSize);
    G64(0x30, Align);
  }
  else
  {
    G32(4, Offset);
    G32(8, Va);
    // G32(0x0C, Pa);
    G32(0x10, Size);
    G32(0x14, VSize);
    G32(0x18, Flags);
    G32(0x1C, Align);
  }
}

// Section_index = 0 means NO section

#define SHN_UNDEF 0

// Section types

#define SHT_NULL           0
#define SHT_PROGBITS       1
#define SHT_SYMTAB         2
#define SHT_STRTAB         3
#define SHT_RELA           4
#define SHT_HASH           5
#define SHT_DYNAMIC        6
#define SHT_NOTE           7
#define SHT_NOBITS         8
#define SHT_REL            9
#define SHT_SHLIB         10
#define SHT_DYNSYM        11
#define SHT_UNKNOWN12     12
#define SHT_UNKNOWN13     13
#define SHT_INIT_ARRAY    14
#define SHT_FINI_ARRAY    15
#define SHT_PREINIT_ARRAY 16
#define SHT_GROUP         17
#define SHT_SYMTAB_SHNDX  18


static const CUInt32PCharPair g_SectTypes[] =
{
  { 0, "NULL" },
  { 1, "PROGBITS" },
  { 2, "SYMTAB" },
  { 3, "STRTAB" },
  { 4, "RELA" },
  { 5, "HASH" },
  { 6, "DYNAMIC" },
  { 7, "NOTE" },
  { 8, "NOBITS" },
  { 9, "REL" },
  { 10, "SHLIB" },
  { 11, "DYNSYM" },
  { 12, "UNKNOWN12" },
  { 13, "UNKNOWN13" },
  { 14, "INIT_ARRAY" },
  { 15, "FINI_ARRAY" },
  { 16, "PREINIT_ARRAY" },
  { 17, "GROUP" },
  { 18, "SYMTAB_SHNDX" },
  { 0x6ffffff5, "GNU_ATTRIBUTES" },
  { 0x6ffffff6, "GNU_HASH" },
  { 0x6ffffffd, "GNU_verdef" },
  { 0x6ffffffe, "GNU_verneed" },
  { 0x6fffffff, "GNU_versym" },
  // { 0x70000001, "X86_64_UNWIND" },
  { 0x70000001, "ARM_EXIDX" },
  { 0x70000002, "ARM_PREEMPTMAP" },
  { 0x70000003, "ARM_ATTRIBUTES" },
  { 0x70000004, "ARM_DEBUGOVERLAY" },
  { 0x70000005, "ARM_OVERLAYSECTION" }
};

static const CUInt32PCharPair g_SectionFlags[] =
{
  { 0, "WRITE" },
  { 1, "ALLOC" },
  { 2, "EXECINSTR" },

  { 4, "MERGE" },
  { 5, "STRINGS" },
  { 6, "INFO_LINK" },
  { 7, "LINK_ORDER" },
  { 8, "OS_NONCONFORMING" },
  { 9, "GROUP" },
  { 10, "TLS" },
  { 11, "CP_SECTION" },
  { 12, "DP_SECTION" },
  { 13, "XCORE_SHF_CP_SECTION" },
  { 28, "64_LARGE" },
};

struct CSection
{
  UInt32 Name;
  UInt32 Type;
  UInt64 Flags;
  UInt64 Va;
  UInt64 Offset;
  UInt64 VSize;
  UInt32 Link;
  UInt32 Info;
  UInt64 AddrAlign;
  UInt64 EntSize;

  UInt64 GetSize() const { return Type == SHT_NOBITS ? 0 : VSize; }

  void UpdateTotalSize(UInt64 &totalSize)
  {
    UInt64 t = Offset + GetSize();
    if (totalSize < t)
      totalSize = t;
  }
  bool Parse(const Byte *p, bool mode64, bool be);
};

bool CSection::Parse(const Byte *p, bool mode64, bool be)
{
  G32(0, Name);
  G32(4, Type);
  if (mode64)
  {
    G64(0x08, Flags);
    G64(0x10, Va);
    G64(0x18, Offset);
    G64(0x20, VSize);
    G32(0x28, Link);
    G32(0x2C, Info);
    G64(0x30, AddrAlign);
    G64(0x38, EntSize);
  }
  else
  {
    G32(0x08, Flags);
    G32(0x0C, Va);
    G32(0x10, Offset);
    G32(0x14, VSize);
    G32(0x18, Link);
    G32(0x1C, Info);
    G32(0x20, AddrAlign);
    G32(0x24, EntSize);
  }
  if (EntSize >= ((UInt32)1 << 31))
    return false;
  if (EntSize >= ((UInt32)1 << 10) &&
      EntSize >= VSize &&
      VSize != 0)
    return false;
  return true;
}

static const CUInt32PCharPair g_Machines[] =
{
  { 0, "None" },
  { 1, "AT&T WE 32100" },
  { 2, "SPARC" },
  { 3, "Intel 386" },
  { 4, "Motorola 68000" },
  { 5, "Motorola 88000" },
  { 6, "Intel 486" },
  { 7, "Intel i860" },
  { 8, "MIPS" },
  { 9, "IBM S/370" },
  { 10, "MIPS RS3000 LE" },
  { 11, "RS6000" },

  { 15, "PA-RISC" },
  { 16, "nCUBE" },
  { 17, "Fujitsu VPP500" },
  { 18, "SPARC 32+" },
  { 19, "Intel i960" },
  { 20, "PowerPC" },
  { 21, "PowerPC 64-bit" },
  { 22, "IBM S/390" },
  { 23, "SPU" },

  { 36, "NEX v800" },
  { 37, "Fujitsu FR20" },
  { 38, "TRW RH-32" },
  { 39, "Motorola RCE" },
  { 40, "ARM" },
  { 41, "Alpha" },
  { 42, "Hitachi SH" },
  { 43, "SPARC-V9" },
  { 44, "Siemens Tricore" },
  { 45, "ARC" },
  { 46, "H8/300" },
  { 47, "H8/300H" },
  { 48, "H8S" },
  { 49, "H8/500" },
  { 50, "IA-64" },
  { 51, "Stanford MIPS-X" },
  { 52, "Motorola ColdFire" },
  { 53, "M68HC12" },
  { 54, "Fujitsu MMA" },
  { 55, "Siemens PCP" },
  { 56, "Sony nCPU" },
  { 57, "Denso NDR1" },
  { 58, "Motorola StarCore" },
  { 59, "Toyota ME16" },
  { 60, "ST100" },
  { 61, "Advanced Logic TinyJ" },
  { 62, "AMD64" },
  { 63, "Sony DSP" },


  { 66, "Siemens FX66" },
  { 67, "ST9+" },
  { 68, "ST7" },
  { 69, "MC68HC16" },
  { 70, "MC68HC11" },
  { 71, "MC68HC08" },
  { 72, "MC68HC05" },
  { 73, "Silicon Graphics SVx" },
  { 74, "ST19" },
  { 75, "Digital VAX" },
  { 76, "Axis CRIS" },
  { 77, "Infineon JAVELIN" },
  { 78, "Element 14 FirePath" },
  { 79, "LSI ZSP" },
  { 80, "MMIX" },
  { 81, "HUANY" },
  { 82, "SiTera Prism" },
  { 83, "Atmel AVR" },
  { 84, "Fujitsu FR30" },
  { 85, "Mitsubishi D10V" },
  { 86, "Mitsubishi D30V" },
  { 87, "NEC v850" },
  { 88, "Mitsubishi M32R" },
  { 89, "Matsushita MN10300" },
  { 90, "Matsushita MN10200" },
  { 91, "picoJava" },
  { 92, "OpenRISC" },
  { 93, "ARC Tangent-A5" },
  { 94, "Tensilica Xtensa" },
  { 95, "Alphamosaic VideoCore" },
  { 96, "Thompson MM GPP" },
  { 97, "National Semiconductor 32K" },
  { 98, "Tenor Network TPC" },
  { 99, "Trebia SNP 1000" },
  { 100, "ST200" },
  { 101, "Ubicom IP2xxx" },
  { 102, "MAX" },
  { 103, "NS CompactRISC" },
  { 104, "Fujitsu F2MC16" },
  { 105, "TI msp430" },
  { 106, "Blackfin (DSP)" },
  { 107, "SE S1C33" },
  { 108, "Sharp embedded" },
  { 109, "Arca RISC" },
  { 110, "Unicore" },
  { 111, "eXcess" },
  { 112, "DXP" },
  { 113, "Altera Nios II" },
  { 114, "NS CRX" },
  { 115, "Motorola XGATE" },
  { 116, "Infineon C16x/XC16x" },
  { 117, "Renesas M16C" },
  { 118, "Microchip Technology dsPIC30F" },
  { 119, "Freescale CE" },
  { 120, "Renesas M32C" },
  
  { 131, "Altium TSK3000" },
  { 132, "Freescale RS08" },
  { 133, "Analog Devices SHARC" },
  { 134, "Cyan Technology eCOG2" },
  { 135, "Sunplus S+core7 RISC" },
  { 136, "NJR 24-bit DSP" },
  { 137, "Broadcom VideoCore III" },
  { 138, "Lattice FPGA" },
  { 139, "SE C17" },
  { 140, "TI TMS320C6000" },
  { 141, "TI TMS320C2000" },
  { 142, "TI TMS320C55x" },
  
  { 160, "STM 64bit VLIW Data Signal" },
  { 161, "Cypress M8C" },
  { 162, "Renesas R32C" },
  { 163, "NXP TriMedia" },
  { 164, "Qualcomm Hexagon" },
  { 165, "Intel 8051" },
  { 166, "STMicroelectronics STxP7x" },
  { 167, "Andes" },
  { 168, "Cyan Technology eCOG1X" },
  { 169, "Dallas Semiconductor MAXQ30" },
  { 170, "NJR 16-bit DSP" },
  { 171, "M2000" },
  { 172, "Cray NV2" },
  { 173, "Renesas RX" },
  { 174, "Imagination Technologies META" },
  { 175, "MCST Elbrus" },
  { 176, "Cyan Technology eCOG16" },
  { 177, "National Semiconductor CR16" },
  { 178, "Freescale ETPUnit" },
  { 179, "Infineon SLE9X" },
  { 180, "Intel L10M" },
  { 181, "Intel K10M" },
  
  { 183, "ARM64" },
  
  { 185, "Atmel AVR32" },
  { 186, "STM8" },
  { 187, "Tilera TILE64" },
  { 188, "Tilera TILEPro" },
  { 189, "Xilinx MicroBlaze" },
  { 190, "NVIDIA CUDA" },
  { 191, "Tilera TILE-Gx" },
  { 192, "CloudShield" },
  { 193, "KIPO-KAIST Core-A 1st" },
  { 194, "KIPO-KAIST Core-A 2nd" },
  { 195, "Synopsys ARCompact V2" },
  { 196, "Open8" },
  { 197, "Renesas RL78" },
  { 198, "Broadcom VideoCore V" },
  { 199, "Renesas 78KOR" },
  { 200, "Freescale 56800EX" },

  { 47787, "Xilinx MicroBlaze" },
  // { 0x9026, "Alpha" }
};

static const CUInt32PCharPair g_OS[] =
{
  { 0, "None" },
  { 1, "HP-UX" },
  { 2, "NetBSD" },
  { 3, "Linux" },
  { 4, "Hurd" },

  { 6, "Solaris" },
  { 7, "AIX" },
  { 8, "IRIX" },
  { 9, "FreeBSD" },
  { 10, "TRU64" },
  { 11, "Novell Modesto" },
  { 12, "OpenBSD" },
  { 13, "OpenVMS" },
  { 14, "HP NSK" },
  { 15, "AROS" },
  { 16, "FenixOS" },
  { 64, "Bare-metal TMS320C6000" },
  { 65, "Linux TMS320C6000" },
  { 97, "ARM" },
  { 255, "Standalone" }
};

#define ET_NONE 0
#define ET_REL  1
#define ET_EXEC 2
#define ET_DYN  3
#define ET_CORE 4

static const char *g_Types[] =
{
  "None",
  "Relocatable file",
  "Executable file",
  "Shared object file",
  "Core file"
};

class CHandler:
  public IInArchive,
  public IArchiveAllowTail,
  public CMyUnknownImp
{
  CRecordVector<CSegment> _segments;
  CRecordVector<CSection> _sections;
  CByteBuffer _namesData;
  CMyComPtr<IInStream> _inStream;
  UInt64 _totalSize;
  CHeader _header;
  bool _headersError;
  bool _allowTail;

  void GetSectionName(UInt32 index, NCOM::CPropVariant &prop, bool showNULL) const;
  HRESULT Open2(IInStream *stream);
public:
  MY_UNKNOWN_IMP2(IInArchive, IArchiveAllowTail)
  INTERFACE_IInArchive(;)
  STDMETHOD(AllowTail)(Int32 allowTail);

  CHandler(): _allowTail(false) {}
};

void CHandler::GetSectionName(UInt32 index, NCOM::CPropVariant &prop, bool showNULL) const
{
  if (index >= _sections.Size())
    return;
  const CSection &section = _sections[index];
  UInt32 offset = section.Name;
  if (index == SHN_UNDEF /* && section.Type == SHT_NULL && offset == 0 */)
  {
    if (showNULL)
      prop = "NULL";
    return;
  }
  const Byte *p = _namesData;
  size_t size = _namesData.Size();
  for (size_t i = offset; i < size; i++)
    if (p[i] == 0)
    {
      prop = (const char *)(p + offset);
      return;
    }
}

static const Byte kArcProps[] =
{
  kpidCpu,
  kpidBit64,
  kpidBigEndian,
  kpidHostOS,
  kpidCharacts,
  kpidHeadersSize,
  kpidName
};

enum
{
  kpidLinkSection = kpidUserDefined,
  kpidInfoSection
};

static const CStatProp kProps[] =
{
  { NULL, kpidPath, VT_BSTR },
  { NULL, kpidSize, VT_UI8 },
  { NULL, kpidVirtualSize, VT_UI8 },
  { NULL, kpidOffset, VT_UI8 },
  { NULL, kpidVa, VT_UI8 },
  { NULL, kpidType, VT_BSTR },
  { NULL, kpidCharacts, VT_BSTR }
  , { "Link Section", kpidLinkSection, VT_BSTR}
  , { "Info Section", kpidInfoSection, VT_BSTR}
};

IMP_IInArchive_Props_WITH_NAME
IMP_IInArchive_ArcProps

STDMETHODIMP CHandler::GetArchiveProperty(PROPID propID, PROPVARIANT *value)
{
  COM_TRY_BEGIN
  NCOM::CPropVariant prop;
  switch (propID)
  {
    case kpidPhySize: prop = _totalSize; break;
    case kpidHeadersSize: prop = _header.GetHeadersSize(); break;
    case kpidBit64: if (_header.Mode64) prop = _header.Mode64; break;
    case kpidBigEndian: if (_header.Be) prop = _header.Be; break;
    case kpidShortComment:
    case kpidCpu: PAIR_TO_PROP(g_Machines, _header.Machine, prop); break;
    case kpidHostOS: PAIR_TO_PROP(g_OS, _header.Os, prop); break;
    case kpidCharacts: TYPE_TO_PROP(g_Types, _header.Type, prop); break;
    case kpidExtension:
    {
      const char *s = NULL;
      if (_header.Type == ET_DYN)
        s = "so";
      else if (_header.Type == ET_REL)
        s = "o";
      if (s)
        prop = s;
      break;
    }
    // case kpidIsSelfExe: prop = (_header.Type != ET_DYN)  && (_header.Type == ET_REL); break;
    case kpidErrorFlags:
    {
      UInt32 flags = 0;
      if (_headersError) flags |= kpv_ErrorFlags_HeadersError;
      if (flags != 0)
        prop = flags;
      break;
    }
  }
  prop.Detach(value);
  return S_OK;
  COM_TRY_END
}

STDMETHODIMP CHandler::GetProperty(UInt32 index, PROPID propID, PROPVARIANT *value)
{
  COM_TRY_BEGIN
  NCOM::CPropVariant prop;
  if (index < _segments.Size())
  {
    const CSegment &item = _segments[index];
    switch (propID)
    {
      case kpidPath:
      {
        char sz[16];
        ConvertUInt32ToString(index, sz);
        prop = sz;
        break;
      }
      case kpidOffset: prop = item.Offset; break;
      case kpidVa: prop = item.Va; break;
      case kpidSize:
      case kpidPackSize: prop = (UInt64)item.Size; break;
      case kpidVirtualSize: prop = (UInt64)item.VSize; break;
      case kpidType: TYPE_TO_PROP(g_SegnmentTypes, item.Type, prop); break;
      case kpidCharacts: FLAGS_TO_PROP(g_SegmentFlags, item.Flags, prop); break;
        
    }
  }
  else
  {
    index -= _segments.Size();
    const CSection &item = _sections[index];
    switch (propID)
    {
      case kpidPath: GetSectionName(index, prop, true); break;
      case kpidOffset: prop = item.Offset; break;
      case kpidVa: prop = item.Va; break;
      case kpidSize:
      case kpidPackSize: prop = (UInt64)(item.Type == SHT_NOBITS ? 0 : item.VSize); break;
      case kpidVirtualSize: prop = item.GetSize(); break;
      case kpidType: PAIR_TO_PROP(g_SectTypes, item.Type, prop); break;
      case kpidCharacts: FLAGS_TO_PROP(g_SectionFlags, (UInt32)item.Flags, prop); break;
      case kpidLinkSection: GetSectionName(item.Link, prop, false); break;
      case kpidInfoSection: GetSectionName(item.Info, prop, false); break;
    }
  }
  prop.Detach(value);
  return S_OK;
  COM_TRY_END
}

HRESULT CHandler::Open2(IInStream *stream)
{
  const UInt32 kStartSize = kHeaderSize64;
  Byte h[kStartSize];
  RINOK(ReadStream_FALSE(stream, h, kStartSize));
  if (h[0] != 0x7F || h[1] != 'E' || h[2] != 'L' || h[3] != 'F')
    return S_FALSE;
  if (!_header.Parse(h))
    return S_FALSE;

  _totalSize = _header.HeaderSize;

  bool addSegments = false;
  bool addSections = false;

  if (_header.NumSections > 1)
    addSections = true;
  else
    addSegments = true;

  if (_header.NumSegments != 0)
  {
    if (_header.ProgOffset > (UInt64)1 << 60) return S_FALSE;
    RINOK(stream->Seek(_header.ProgOffset, STREAM_SEEK_SET, NULL));
    size_t size = (size_t)_header.SegmentEntrySize * _header.NumSegments;
    
    CByteArr buf(size);
    
    RINOK(ReadStream_FALSE(stream, buf, size));
    
    UInt64 total = _header.ProgOffset + size;
    if (_totalSize < total)
      _totalSize = total;

    const Byte *p = buf;
    
    if (addSegments)
      _segments.ClearAndReserve(_header.NumSegments);
    for (unsigned i = 0; i < _header.NumSegments; i++, p += _header.SegmentEntrySize)
    {
      CSegment seg;
      seg.Parse(p, _header.Mode64, _header.Be);
      seg.UpdateTotalSize(_totalSize);
      if (addSegments)
        if (seg.Type != PT_PHDR)
          _segments.AddInReserved(seg);
    }
  }

  if (_header.NumSections != 0)
  {
    if (_header.SectOffset > (UInt64)1 << 60) return S_FALSE;
    RINOK(stream->Seek(_header.SectOffset, STREAM_SEEK_SET, NULL));
    size_t size = (size_t)_header.SectionEntrySize * _header.NumSections;
    
    CByteArr buf(size);
    
    RINOK(ReadStream_FALSE(stream, buf, size));

    UInt64 total = _header.SectOffset + size;
    if (_totalSize < total)
      _totalSize = total;

    const Byte *p = buf;
    
    if (addSections)
      _sections.ClearAndReserve(_header.NumSections);
    for (unsigned i = 0; i < _header.NumSections; i++, p += _header.SectionEntrySize)
    {
      CSection sect;
      if (!sect.Parse(p, _header.Mode64, _header.Be))
      {
        _headersError = true;
        return S_FALSE;
      }
      sect.UpdateTotalSize(_totalSize);
      if (addSections)
        _sections.AddInReserved(sect);
    }
  }

  if (addSections)
  {
    if (_header.NamesSectIndex < _sections.Size())
    {
      const CSection &sect = _sections[_header.NamesSectIndex];
      UInt64 size = sect.GetSize();
      if (size != 0
        && size < ((UInt64)1 << 31)
        && (Int64)sect.Offset >= 0)
      {
        _namesData.Alloc((size_t)size);
        RINOK(stream->Seek(sect.Offset, STREAM_SEEK_SET, NULL));
        RINOK(ReadStream_FALSE(stream, _namesData, (size_t)size));
      }
    }
    
    /*
    // we will not delete NULL sections, since we have links to section via indexes
    for (int i = _sections.Size() - 1; i >= 0; i--)
      if (_sections[i].Type == SHT_NULL)
        _items.Delete(i);
    */
  }

  if (!_allowTail)
  {
    UInt64 fileSize;
    RINOK(stream->Seek(0, STREAM_SEEK_END, &fileSize));
    if (fileSize > _totalSize)
      return S_FALSE;
  }

  return S_OK;
}

STDMETHODIMP CHandler::Open(IInStream *inStream,
    const UInt64 * /* maxCheckStartPosition */,
    IArchiveOpenCallback * /* openArchiveCallback */)
{
  COM_TRY_BEGIN
  Close();
  RINOK(Open2(inStream));
  _inStream = inStream;
  return S_OK;
  COM_TRY_END
}

STDMETHODIMP CHandler::Close()
{
  _totalSize = 0;
  _headersError = false;

  _inStream.Release();
  _segments.Clear();
  _sections.Clear();
  _namesData.Free();
  return S_OK;
}

STDMETHODIMP CHandler::GetNumberOfItems(UInt32 *numItems)
{
  *numItems = _segments.Size() + _sections.Size();
  return S_OK;
}

STDMETHODIMP CHandler::Extract(const UInt32 *indices, UInt32 numItems,
    Int32 testMode, IArchiveExtractCallback *extractCallback)
{
  COM_TRY_BEGIN
  bool allFilesMode = (numItems == (UInt32)(Int32)-1);
  if (allFilesMode)
    numItems = _segments.Size() + _sections.Size();
  if (numItems == 0)
    return S_OK;
  UInt64 totalSize = 0;
  UInt32 i;
  for (i = 0; i < numItems; i++)
  {
    UInt32 index = allFilesMode ? i : indices[i];
    totalSize += (index < _segments.Size()) ?
        _segments[index].Size :
        _sections[index - _segments.Size()].GetSize();
  }
  extractCallback->SetTotal(totalSize);

  UInt64 currentTotalSize = 0;
  UInt64 currentItemSize;
  
  NCompress::CCopyCoder *copyCoderSpec = new NCompress::CCopyCoder();
  CMyComPtr<ICompressCoder> copyCoder = copyCoderSpec;

  CLocalProgress *lps = new CLocalProgress;
  CMyComPtr<ICompressProgressInfo> progress = lps;
  lps->Init(extractCallback, false);

  CLimitedSequentialInStream *streamSpec = new CLimitedSequentialInStream;
  CMyComPtr<ISequentialInStream> inStream(streamSpec);
  streamSpec->SetStream(_inStream);

  for (i = 0; i < numItems; i++, currentTotalSize += currentItemSize)
  {
    lps->InSize = lps->OutSize = currentTotalSize;
    RINOK(lps->SetCur());
    Int32 askMode = testMode ?
        NExtract::NAskMode::kTest :
        NExtract::NAskMode::kExtract;
    UInt32 index = allFilesMode ? i : indices[i];
    UInt64 offset;
    if (index < _segments.Size())
    {
      const CSegment &item = _segments[index];
      currentItemSize = item.Size;
      offset = item.Offset;
    }
    else
    {
      const CSection &item = _sections[index - _segments.Size()];
      currentItemSize = item.GetSize();
      offset = item.Offset;
    }
    
    CMyComPtr<ISequentialOutStream> outStream;
    RINOK(extractCallback->GetStream(index, &outStream, askMode));
    if (!testMode && !outStream)
      continue;
      
    RINOK(extractCallback->PrepareOperation(askMode));
    RINOK(_inStream->Seek(offset, STREAM_SEEK_SET, NULL));
    streamSpec->Init(currentItemSize);
    RINOK(copyCoder->Code(inStream, outStream, NULL, NULL, progress));
    outStream.Release();
    RINOK(extractCallback->SetOperationResult(copyCoderSpec->TotalSize == currentItemSize ?
        NExtract::NOperationResult::kOK:
        NExtract::NOperationResult::kDataError));
  }
  return S_OK;
  COM_TRY_END
}

STDMETHODIMP CHandler::AllowTail(Int32 allowTail)
{
  _allowTail = IntToBool(allowTail);
  return S_OK;
}

static const Byte k_Signature[] = { 0x7F, 'E', 'L', 'F' };

REGISTER_ARC_I(
  "ELF", "elf", 0, 0xDE,
  k_Signature,
  0,
  NArcInfoFlags::kPreArc,
  NULL)

}}
