PROG=test_lib

LOCAL_FLAGS=-I. -I.. -I../include_windows -DUNICODE -D_UNICODE

include ../../makefile.crc32
include ../../makefile.machine

TARGET_FLAGS=$(CC_EXE)
LOCAL_LINK=$(LINK_EXE)
LIBS=$(LOCAL_LIBS)

OBJS= \
	wine_date_and_time.o \
	mySplitCommandLine.o \
	myAddExeFlag.o \
	wine_GetXXXDefaultLangID.o \
	test_lib.o \
	MyVector.o \
	MyString.o \
	MyWindows.o \
	CpuArch.o

include ../../makefile.glb

