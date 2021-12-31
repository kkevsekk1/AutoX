
TARGET=MemLat

all: build

include ../makefile.inc


test: install
	adb shell $(DEVICE_DIR)/$(TARGET) 4 p5 n

# simple memlat test:
# MemLat 4 p5 n

