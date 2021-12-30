
TARGET=PipeLen

all: build

include ../makefile.inc


test: install
	adb shell $(DEVICE_DIR)/$(TARGET) r0 n

# One pipelen test:
# pipelen r0 n
# All pipelen tests (too long) :
# pipelen n

