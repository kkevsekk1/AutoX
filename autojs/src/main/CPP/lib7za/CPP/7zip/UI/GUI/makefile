
TARGET_FLAGS=`wx-config --unicode=yes --cxxflags`

include ../../../../makefile.crc32
include ../../../../makefile.machine

LIBS=`wx-config --unicode=yes --libs` $(LOCAL_LIBS_DLL)

#

GUI_OBJS = \
 BenchmarkDialog.o \
 BenchmarkDialog_rc.o \
 CompressDialog.o \
 CompressDialog_rc.o \
 ExtractDialog.o \
 ExtractDialog_rc.o \
 GUI.o \
 HashGUI.o \
 ExtractGUI.o \
 UpdateCallbackGUI.o \
 UpdateGUI.o \
 wxGUI.o \

COMMON_OBJS = \
CommandLineParser.o \
CRC.o \
IntToString.o \
NewHandler.o \
Lang.o \
ListFileUtils.o \
MyString.o \
MyVector.o \
StringConvert.o \
StringToInt.o \
UTFConvert.o \
Wildcard.o \

# CommonDialog.o MemoryLock.o ResourceString.o ResourceString.o Shell.o
WIN_OBJS = \
DLL.o \
ErrorMsg.o \
FileDir.o \
FileFind.o \
FileIO.o \
FileName.o \
PropVariant.o \
PropVariantConv.o \
Registry.o \
Synchronization.o \
System.o \
TimeUtils.o \
Window.o \

WIN_CTRL_OBJS = \
Dialog.o \
Controls.o \

7ZIP_COMMON_OBJS = \
CreateCoder.o \
FilePathAutoRename.o \
FileStreams.o \
FilterCoder.o \
LimitedStreams.o \
MethodProps.o \
ProgressUtils.o \
PropId.o \
StreamObjects.o \
StreamUtils.o \
UniqBlocks.o \

UI_COMMON_OBJS = \
ArchiveCommandLine.o \
ArchiveExtractCallback.o \
ArchiveOpenCallback.o \
Bench.o \
DefaultName.o \
EnumDirItems.o \
Extract.o \
ExtractingFilePath.o \
HashCalc.o \
LoadCodecs.o \
OpenArchive.o \
PropIDUtils.o \
SetProperties.o \
SortUtils.o \
TempFiles.o \
Update.o \
UpdateAction.o \
UpdateCallback.o \
UpdatePair.o \
UpdateProduce.o \
WorkDir.o \
ZipRegistry.o \

AR_COMMON_OBJS = \
 OutStreamWithCRC.o

FM_OBJS_OUT = \
MessagesDialog.o \
MessagesDialog_rc.o \

FM_OBJS = \
FormatUtils.o \
ProgramLocation.o \
SplitUtils.o \
StringUtils.o \
ExtractCallback.o \
LangUtils.o \
OverwriteDialog.o \
OverwriteDialog_rc.o \
ProgressDialog2.o \
ProgressDialog2_rc.o \
PasswordDialog.o \
PasswordDialog_rc.o \

C_OBJS = \
  Alloc.o \
  CpuArch.o \
  Threads.o \

OBJS_OUT=\
  $(GUI_OBJS) \
  $(COMMON_OBJS) \
  $(WIN_OBJS) \
  $(WIN_CTRL_OBJS) \
  $(7ZIP_COMMON_OBJS) \
  $(UI_COMMON_OBJS) \
  $(AR_COMMON_OBJS) \
  $(FM_OBJS)\
  MyMessages.o \
  CopyCoder.o \
  $(C_OBJS) \
  MyWindows.o \
  wine_date_and_time.o \
  wine_GetXXXDefaultLangID.o \
  7zCrc.o $(OBJ_CRC32)

include ../../../../makefile.glb

