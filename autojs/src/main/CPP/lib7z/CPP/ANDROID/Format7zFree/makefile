
TARGET=lib7z.so

all: build

include ../makefile.inc

install::
	adb shell mv $(DEVICE_DIR)/$(TARGET) $(DEVICE_DIR)/7z.so

