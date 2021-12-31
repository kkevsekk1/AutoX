
TARGET=7za

include ../makefile.inc

test: install
	adb shell $(DEVICE_DIR)/7za b

test_crc: install
	adb shell $(DEVICE_DIR)/7za -mm=crc b

test_complex: install
	adb shell $(DEVICE_DIR)/7za "-mm=*" b

test2:
	adb push ./libs/armeabi-v7a/7za $(DEVICE_DIR)/7za-v7a
	adb shell chmod 777 $(DEVICE_DIR)/7za-v7a
	adb shell $(DEVICE_DIR)/7za-v7a b

test_all: install
	adb push ../../../check/test/7za433_7zip_lzma.7z $(DEVICE_DIR)
	adb shell $(DEVICE_DIR)/7za t  $(DEVICE_DIR)/7za433_7zip_lzma.7z

bench: install test

# FIXME
debug:
	cd jni ; ndk-build NDK_DEBUG=1
	adb push ./libs/armeabi/7za /data/app/
	adb push ./libs/armeabi/gdbserver /data/app/
	adb shell chmod 777 /data/app/7za
	adb shell chmod 777 /data/app/gdbserver
	adb forward tcp:1234: tcp:1234
	adb shell  /data/app/gdbserver :1234 /data/app/7za
	
