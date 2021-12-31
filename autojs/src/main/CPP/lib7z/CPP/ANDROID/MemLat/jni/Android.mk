# 
# build MemLat for armeabi and armeabi-v7a CPU
#


LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := MemLat
LOCAL_CFLAGS := -DANDROID_NDK  -fexceptions \
  -DNDEBUG -D_REENTRANT -DENV_UNIX \
  -DNUM_DIRECT_TESTS=6 -DNUM_PAR_TESTS=5 \
  -I../../../../Utils/CPUTest/MemLat


LOCAL_SRC_FILES := \
 ../../../../Utils/CPUTest/MemLat/MemLat.cpp \
 ../../../../Utils/CPUTest/MemLat/Walk.c

include $(BUILD_EXECUTABLE)
