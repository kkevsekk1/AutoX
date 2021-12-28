
# gcov *.c *.cpp

#  -fprofile-arcs -ftest-coverage

# gprof
# Compiling a Program for Profiling : -pg
# Once the program is compiled for profiling, you must run it in order to generate the information that gprof needs
# gprof 7za.exe > gprof.output



#  Profile guided optimization with gcc
#  ======================================
#  Compile all code with -fprofile-generate in {C|CXX|LD}FLAGS  : -fprofile-generate
#  Run the binary
#  Run your application/benchmark against that binary
#  Recompile all code with -fprofile-use (above steps will place lots of .gcda files in source tree)

LOCAL_FLAGS=\
  -pg -g2 \
  -DBREAK_HANDLER \
  -DUNICODE -D_UNICODE -DUNIX_USE_WIN_FILE \
	-I../../Archive \
	-I../../Archive/7z \
	-I../../Archive/BZip2 \
	-I../../Archive/Common \
	-I../../Archive/GZip \
	-I../../Archive/Cab \
	-I../../Archive/Lzma \
	-I../../Archive/Tar \
	-I../../Archive/Zip \
	-I../../Archive/Split \
	-I../../Archive/Z \
	-I../../Compress \
       	-I../../Crypto \
	-I../../UI/Console \
	-I../../UI/Common \
	-I../../../Windows \
	-I../../../Common \
	-I../../../7zip/Common \
	-I../../../../C \
-I../../../myWindows \
-I../../../ \
-I../../../include_windows

include ../../../../makefile.machine

SRCS=\
  ../../../../CPP/7zip/Archive/7z/7zCompressionMode.cpp \
  ../../../../CPP/7zip/Archive/7z/7zDecode.cpp \
  ../../../../CPP/7zip/Archive/7z/7zEncode.cpp \
  ../../../../CPP/7zip/Archive/7z/7zExtract.cpp \
  ../../../../CPP/7zip/Archive/7z/7zFolderInStream.cpp \
  ../../../../CPP/7zip/Archive/7z/7zFolderOutStream.cpp \
  ../../../../CPP/7zip/Archive/7z/7zHandler.cpp \
  ../../../../CPP/7zip/Archive/7z/7zHandlerOut.cpp \
  ../../../../CPP/7zip/Archive/7z/7zHeader.cpp \
  ../../../../CPP/7zip/Archive/7z/7zIn.cpp \
  ../../../../CPP/7zip/Archive/7z/7zOut.cpp \
  ../../../../CPP/7zip/Archive/7z/7zProperties.cpp \
  ../../../../CPP/7zip/Archive/7z/7zRegister.cpp \
  ../../../../CPP/7zip/Archive/7z/7zSpecStream.cpp \
  ../../../../CPP/7zip/Archive/7z/7zUpdate.cpp \
  ../../../../CPP/7zip/Archive/Bz2Handler.cpp \
  ../../../../CPP/7zip/Archive/Cab/CabBlockInStream.cpp \
  ../../../../CPP/7zip/Archive/Cab/CabHandler.cpp \
  ../../../../CPP/7zip/Archive/Cab/CabHeader.cpp \
  ../../../../CPP/7zip/Archive/Cab/CabIn.cpp \
  ../../../../CPP/7zip/Archive/Cab/CabRegister.cpp \
  ../../../../CPP/7zip/Archive/Common/CoderMixer2.cpp \
  ../../../../CPP/7zip/Archive/Common/DummyOutStream.cpp \
  ../../../../CPP/7zip/Archive/Common/FindSignature.cpp \
  ../../../../CPP/7zip/Archive/Common/HandlerOut.cpp \
  ../../../../CPP/7zip/Archive/Common/InStreamWithCRC.cpp \
  ../../../../CPP/7zip/Archive/Common/ItemNameUtils.cpp \
  ../../../../CPP/7zip/Archive/Common/MultiStream.cpp \
  ../../../../CPP/7zip/Archive/Common/OutStreamWithCRC.cpp \
  ../../../../CPP/7zip/Archive/Common/ParseProperties.cpp \
  ../../../../CPP/7zip/Archive/DeflateProps.cpp \
  ../../../../CPP/7zip/Archive/GzHandler.cpp \
  ../../../../CPP/7zip/Archive/LzmaHandler.cpp \
  ../../../../CPP/7zip/Archive/PpmdHandler.cpp \
  ../../../../CPP/7zip/Archive/SplitHandler.cpp \
  ../../../../CPP/7zip/Archive/Tar/TarHandler.cpp \
  ../../../../CPP/7zip/Archive/Tar/TarHandlerOut.cpp \
  ../../../../CPP/7zip/Archive/Tar/TarHeader.cpp \
  ../../../../CPP/7zip/Archive/Tar/TarIn.cpp \
  ../../../../CPP/7zip/Archive/Tar/TarOut.cpp \
  ../../../../CPP/7zip/Archive/Tar/TarRegister.cpp \
  ../../../../CPP/7zip/Archive/Tar/TarUpdate.cpp \
  ../../../../CPP/7zip/Archive/XzHandler.cpp \
  ../../../../CPP/7zip/Archive/ZHandler.cpp \
  ../../../../CPP/7zip/Archive/Zip/ZipAddCommon.cpp \
  ../../../../CPP/7zip/Archive/Zip/ZipHandler.cpp \
  ../../../../CPP/7zip/Archive/Zip/ZipHandlerOut.cpp \
  ../../../../CPP/7zip/Archive/Zip/ZipIn.cpp \
  ../../../../CPP/7zip/Archive/Zip/ZipItem.cpp \
  ../../../../CPP/7zip/Archive/Zip/ZipOut.cpp \
  ../../../../CPP/7zip/Archive/Zip/ZipRegister.cpp \
  ../../../../CPP/7zip/Archive/Zip/ZipUpdate.cpp \
  ../../../../CPP/7zip/Common/CWrappers.cpp \
  ../../../../CPP/7zip/Common/CreateCoder.cpp \
  ../../../../CPP/7zip/Common/FilePathAutoRename.cpp \
  ../../../../CPP/7zip/Common/FileStreams.cpp \
  ../../../../CPP/7zip/Common/FilterCoder.cpp \
  ../../../../CPP/7zip/Common/InBuffer.cpp \
  ../../../../CPP/7zip/Common/InOutTempBuffer.cpp \
  ../../../../CPP/7zip/Common/LimitedStreams.cpp \
  ../../../../CPP/7zip/Common/MemBlocks.cpp \
  ../../../../CPP/7zip/Common/MethodId.cpp \
  ../../../../CPP/7zip/Common/MethodProps.cpp \
  ../../../../CPP/7zip/Common/OffsetStream.cpp \
  ../../../../CPP/7zip/Common/OutBuffer.cpp \
  ../../../../CPP/7zip/Common/OutMemStream.cpp \
  ../../../../CPP/7zip/Common/ProgressMt.cpp \
  ../../../../CPP/7zip/Common/ProgressUtils.cpp \
  ../../../../CPP/7zip/Common/PropId.cpp \
  ../../../../CPP/7zip/Common/StreamBinder.cpp \
  ../../../../CPP/7zip/Common/StreamObjects.cpp \
  ../../../../CPP/7zip/Common/StreamUtils.cpp \
  ../../../../CPP/7zip/Common/UniqBlocks.cpp \
  ../../../../CPP/7zip/Common/VirtThread.cpp \
  ../../../../CPP/7zip/Compress/BZip2Crc.cpp \
  ../../../../CPP/7zip/Compress/BZip2Decoder.cpp \
  ../../../../CPP/7zip/Compress/BZip2Encoder.cpp \
  ../../../../CPP/7zip/Compress/BZip2Register.cpp \
  ../../../../CPP/7zip/Compress/Bcj2Coder.cpp \
  ../../../../CPP/7zip/Compress/Bcj2Register.cpp \
  ../../../../CPP/7zip/Compress/BcjCoder.cpp \
  ../../../../CPP/7zip/Compress/BcjRegister.cpp \
  ../../../../CPP/7zip/Compress/BitlDecoder.cpp \
  ../../../../CPP/7zip/Compress/BranchMisc.cpp \
  ../../../../CPP/7zip/Compress/BranchRegister.cpp \
  ../../../../CPP/7zip/Compress/ByteSwap.cpp \
  ../../../../CPP/7zip/Compress/CopyCoder.cpp \
  ../../../../CPP/7zip/Compress/CopyRegister.cpp \
  ../../../../CPP/7zip/Compress/Deflate64Register.cpp \
  ../../../../CPP/7zip/Compress/DeflateDecoder.cpp \
  ../../../../CPP/7zip/Compress/DeflateEncoder.cpp \
  ../../../../CPP/7zip/Compress/DeflateRegister.cpp \
  ../../../../CPP/7zip/Compress/DeltaFilter.cpp \
  ../../../../CPP/7zip/Compress/ImplodeDecoder.cpp \
  ../../../../CPP/7zip/Compress/ImplodeHuffmanDecoder.cpp \
  ../../../../CPP/7zip/Compress/LzOutWindow.cpp \
  ../../../../CPP/7zip/Compress/Lzma2Decoder.cpp \
  ../../../../CPP/7zip/Compress/Lzma2Encoder.cpp \
  ../../../../CPP/7zip/Compress/Lzma2Register.cpp \
  ../../../../CPP/7zip/Compress/LzmaDecoder.cpp \
  ../../../../CPP/7zip/Compress/LzmaEncoder.cpp \
  ../../../../CPP/7zip/Compress/LzmaRegister.cpp \
  ../../../../CPP/7zip/Compress/LzxDecoder.cpp \
  ../../../../CPP/7zip/Compress/PpmdDecoder.cpp \
  ../../../../CPP/7zip/Compress/PpmdEncoder.cpp \
  ../../../../CPP/7zip/Compress/PpmdRegister.cpp \
  ../../../../CPP/7zip/Compress/PpmdZip.cpp \
  ../../../../CPP/7zip/Compress/QuantumDecoder.cpp \
  ../../../../CPP/7zip/Compress/ShrinkDecoder.cpp \
  ../../../../CPP/7zip/Compress/ZDecoder.cpp \
  ../../../../CPP/7zip/Crypto/7zAes.cpp \
  ../../../../CPP/7zip/Crypto/7zAesRegister.cpp \
  ../../../../CPP/7zip/Crypto/HmacSha1.cpp \
  ../../../../CPP/7zip/Crypto/MyAes.cpp \
  ../../../../CPP/7zip/Crypto/MyAesReg.cpp \
  ../../../../CPP/7zip/Crypto/Pbkdf2HmacSha1.cpp \
  ../../../../CPP/7zip/Crypto/RandGen.cpp \
  ../../../../CPP/7zip/Crypto/WzAes.cpp \
  ../../../../CPP/7zip/Crypto/ZipCrypto.cpp \
  ../../../../CPP/7zip/Crypto/ZipStrong.cpp \
  ../../../../CPP/7zip/UI/Common/ArchiveCommandLine.cpp \
  ../../../../CPP/7zip/UI/Common/ArchiveExtractCallback.cpp \
  ../../../../CPP/7zip/UI/Common/ArchiveOpenCallback.cpp \
  ../../../../CPP/7zip/UI/Common/Bench.cpp \
  ../../../../CPP/7zip/UI/Common/DefaultName.cpp \
  ../../../../CPP/7zip/UI/Common/EnumDirItems.cpp \
  ../../../../CPP/7zip/UI/Common/Extract.cpp \
  ../../../../CPP/7zip/UI/Common/ExtractingFilePath.cpp \
  ../../../../CPP/7zip/UI/Common/HashCalc.cpp \
  ../../../../CPP/7zip/UI/Common/LoadCodecs.cpp \
  ../../../../CPP/7zip/UI/Common/OpenArchive.cpp \
  ../../../../CPP/7zip/UI/Common/PropIDUtils.cpp \
  ../../../../CPP/7zip/UI/Common/SetProperties.cpp \
  ../../../../CPP/7zip/UI/Common/SortUtils.cpp \
  ../../../../CPP/7zip/UI/Common/TempFiles.cpp \
  ../../../../CPP/7zip/UI/Common/Update.cpp \
  ../../../../CPP/7zip/UI/Common/UpdateAction.cpp \
  ../../../../CPP/7zip/UI/Common/UpdateCallback.cpp \
  ../../../../CPP/7zip/UI/Common/UpdatePair.cpp \
  ../../../../CPP/7zip/UI/Common/UpdateProduce.cpp \
  ../../../../CPP/7zip/UI/Console/BenchCon.cpp \
  ../../../../CPP/7zip/UI/Console/ConsoleClose.cpp \
  ../../../../CPP/7zip/UI/Console/ExtractCallbackConsole.cpp \
  ../../../../CPP/7zip/UI/Console/HashCon.cpp \
  ../../../../CPP/7zip/UI/Console/List.cpp \
  ../../../../CPP/7zip/UI/Console/Main.cpp \
  ../../../../CPP/7zip/UI/Console/MainAr.cpp \
  ../../../../CPP/7zip/UI/Console/OpenCallbackConsole.cpp \
  ../../../../CPP/7zip/UI/Console/PercentPrinter.cpp \
  ../../../../CPP/7zip/UI/Console/UpdateCallbackConsole.cpp \
  ../../../../CPP/7zip/UI/Console/UserInputUtils.cpp \
  ../../../../CPP/Common/CRC.cpp \
  ../../../../CPP/Common/CommandLineParser.cpp \
  ../../../../CPP/Common/CrcReg.cpp \
  ../../../../CPP/Common/IntToString.cpp \
  ../../../../CPP/Common/ListFileUtils.cpp \
  ../../../../CPP/Common/MyString.cpp \
  ../../../../CPP/Common/MyVector.cpp \
  ../../../../CPP/Common/MyWindows.cpp \
  ../../../../CPP/Common/Sha1Reg.cpp \
  ../../../../CPP/Common/Sha256Reg.cpp \
  ../../../../CPP/Common/StdInStream.cpp \
  ../../../../CPP/Common/StdOutStream.cpp \
  ../../../../CPP/Common/StringConvert.cpp \
  ../../../../CPP/Common/StringToInt.cpp \
  ../../../../CPP/Common/UTFConvert.cpp \
  ../../../../CPP/Common/Wildcard.cpp \
  ../../../../CPP/Common/XzCrc64Reg.cpp \
  ../../../../CPP/Windows/DLL.cpp \
  ../../../../CPP/Windows/ErrorMsg.cpp \
  ../../../../CPP/Windows/FileDir.cpp \
  ../../../../CPP/Windows/FileFind.cpp \
  ../../../../CPP/Windows/FileIO.cpp \
  ../../../../CPP/Windows/FileName.cpp \
  ../../../../CPP/Windows/PropVariant.cpp \
  ../../../../CPP/Windows/PropVariantConv.cpp \
  ../../../../CPP/Windows/Synchronization.cpp \
  ../../../../CPP/Windows/System.cpp \
  ../../../../CPP/Windows/TimeUtils.cpp \
  ../../../../CPP/myWindows/myAddExeFlag.cpp \
  ../../../../CPP/myWindows/mySplitCommandLine.cpp \
  ../../../../CPP/myWindows/wine_date_and_time.cpp \

SRCS_C=\
  ../../../../C/7zCrc.c \
  ../../../../C/7zCrcOpt.c \
  ../../../../C/7zStream.c \
  ../../../../C/Aes.c \
  ../../../../C/Alloc.c \
  ../../../../C/Bcj2.c \
  ../../../../C/Bcj2Enc.c \
  ../../../../C/Bra.c \
  ../../../../C/Bra86.c \
  ../../../../C/BraIA64.c \
  ../../../../C/BwtSort.c \
  ../../../../C/CpuArch.c \
  ../../../../C/Delta.c \
  ../../../../C/HuffEnc.c \
  ../../../../C/LzFind.c \
  ../../../../C/LzFindMt.c \
  ../../../../C/Lzma2Dec.c \
  ../../../../C/Lzma2Enc.c \
  ../../../../C/LzmaDec.c \
  ../../../../C/LzmaEnc.c \
  ../../../../C/MtCoder.c \
  ../../../../C/Ppmd7.c \
  ../../../../C/Ppmd7Dec.c \
  ../../../../C/Ppmd7Enc.c \
  ../../../../C/Ppmd8.c \
  ../../../../C/Ppmd8Dec.c \
  ../../../../C/Ppmd8Enc.c \
  ../../../../C/Sha1.c \
  ../../../../C/Sha256.c \
  ../../../../C/Sort.c \
  ../../../../C/Threads.c \
  ../../../../C/Xz.c \
  ../../../../C/XzCrc64.c \
  ../../../../C/XzCrc64Opt.c \
  ../../../../C/XzDec.c \
  ../../../../C/XzEnc.c \
  ../../../../C/XzIn.c \

all : copy 7za.exe
	
7za.exe:
	$(CXX) $(LOCAL_FLAGS) -c *.cpp 
	$(CC)  $(LOCAL_FLAGS) -c *.c
	$(CXX) $(LOCAL_FLAGS) *.o -o 7za.exe $(LOCAL_LIBS_DLL)
	ld -r *.o -o  7za.ld
	nm -u -C 7za.ld > 7za.undefined
copy:
	rm -f *.cpp *.c
	cp -p $(SRCS) $(SRCS_C) .

clean:
	rm -f 7za.exe 7za.ld 7za.undefined *.cpp *.c *.gcov *.gcda *.gcno *.o core *~ gmon*  gprof.output

