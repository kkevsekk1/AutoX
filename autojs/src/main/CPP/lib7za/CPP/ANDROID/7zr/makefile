
TARGET=7zr

include ../makefile.inc

test: install
	adb shell $(DEVICE_DIR)/7zr b

test_crc: install
	adb shell $(DEVICE_DIR)/7zr -mm=crc b

test_complex: install
	adb shell $(DEVICE_DIR)/7zr "-mm=*" b

test2:
	adb push ./libs/armeabi-v7a/7zr $(DEVICE_DIR)/7zr-v7a
	adb shell chmod 777 $(DEVICE_DIR)/7zr-v7a
	adb shell $(DEVICE_DIR)/7zr-v7a b

test_all: install
	adb push ../../../check/test/7zr433_7zip_lzma.7z $(DEVICE_DIR)
	adb shell $(DEVICE_DIR)/7zr t  $(DEVICE_DIR)/7zr433_7zip_lzma.7z

bench: install test

# FIXME
debug:
	cd jni ; ndk-build NDK_DEBUG=1
	adb push ./libs/armeabi/7zr /data/app/
	adb push ./libs/armeabi/gdbserver /data/app/
	adb shell chmod 777 /data/app/7zr
	adb shell chmod 777 /data/app/gdbserver
	adb forward tcp:1234: tcp:1234
	adb shell  /data/app/gdbserver :1234 /data/app/7zr
	
