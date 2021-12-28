# 
# build test_lib for armeabi and armeabi-v7a CPU
#


LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := test_lib
LOCAL_CFLAGS := -DANDROID_NDK  -fexceptions \
		-DNDEBUG -D_REENTRANT -DENV_UNIX \
  -DUNICODE \
  -D_UNICODE \
	-I../../../Windows \
	-I../../../Common \
	-I../../../../C \
-I../../../myWindows \
-I../../../ \
-I../../../include_windows


LOCAL_SRC_FILES := \
 ../../../myWindows/wine_date_and_time.cpp \
  ../../../myWindows/wine_GetXXXDefaultLangID.cpp \
 ../../../myWindows/myAddExeFlag.cpp \
 ../../../myWindows/mySplitCommandLine.cpp \
 ../../../myWindows/test_lib.cpp \
  ../../../Common/MyString.cpp \
 ../../../Common/MyWindows.cpp \
 ../../../Common/MyVector.cpp \

# Needed since ANDROID 5, these programs run on android-16 (Android 4.1+)
LOCAL_CFLAGS += -fPIE
LOCAL_LDFLAGS += -fPIE -pie

include $(BUILD_EXECUTABLE)
