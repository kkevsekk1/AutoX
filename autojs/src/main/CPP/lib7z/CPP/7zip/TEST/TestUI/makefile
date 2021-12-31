
TARGET_FLAGS=\
	-I. \
	`wx-config --unicode=yes --cxxflags`

include ../../../../makefile.crc32
include ../../../../makefile.machine

LIBS=`wx-config --unicode=yes --libs` $(LOCAL_LIBS_DLL)

include ../../../../makefile.glb

