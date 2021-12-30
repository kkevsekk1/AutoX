# 
# build PipeLen for armeabi and armeabi-v7a CPU
#


LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := PipeLen
LOCAL_CFLAGS := -DANDROID_NDK  -fexceptions \
		-DNDEBUG -D_REENTRANT -DENV_UNIX \
  -DUNICODE \
  -D_UNICODE \
-I../../../../Utils/CPUTest/PipeLen


LOCAL_SRC_FILES := \
 ../../../../Utils/CPUTest/PipeLen/PipeLen.cpp

include $(BUILD_EXECUTABLE)
