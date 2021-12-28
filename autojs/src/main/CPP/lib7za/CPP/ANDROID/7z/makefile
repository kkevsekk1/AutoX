
TARGET=7z

include ../makefile.inc

test: install
	adb shell $(DEVICE_DIR)/$(TARGET) b

test_all: install
	adb push ../../../check/test/7za433_7zip_lzma.7z $(DEVICE_DIR)
	adb shell $(DEVICE_DIR)/7za t  $(DEVICE_DIR)/7za433_7zip_lzma.7z

