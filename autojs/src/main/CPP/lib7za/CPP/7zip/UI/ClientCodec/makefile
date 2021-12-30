PROG=../../../../bin/ClientCodec

LOCAL_FLAGS=-DUNICODE -D_UNICODE

include ../../../../makefile.crc32
include ../../../../makefile.machine

LIBS=$(LOCAL_LIBS_DLL)

OBJS = \
wine_date_and_time.o \
MyWindows.o \
ClientCodec.o \
MyString.o \
StringConvert.o \
PropVariant.o \
DLL.o 



.PHONY: test

#CODER=../../../bin/Codecs/BZip2.so # OK - ICompressWriteCoderProperties : NOT Found
#CODER=../../../bin/Codecs/Copy.so # OK - ICompressWriteCoderProperties : NOT Found
#CODER=../../../bin/Codecs/Deflate.so # OK - ICompressWriteCoderProperties : NOT Found
CODER=../../../bin/Codecs/LZMA.so # OK - ICompressWriteCoderProperties=5
#CODER=../../../bin/Codecs/PPMD.so # KO Encoder - ICompressWriteCoderProperties=5


test : all
	cp ClientCodec.cpp f1
	../../../../bin/ClientCodec $(CODER) i
	../../../../bin/ClientCodec $(CODER) e f1 f2
	../../../../bin/ClientCodec $(CODER) d f2 f3
	diff -q f1 f3

clean2:
	rm -f f1 f2 f3

include ../../../../makefile.glb

