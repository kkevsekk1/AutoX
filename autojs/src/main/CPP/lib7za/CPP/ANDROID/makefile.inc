
# For emulator
# DEVICE_DIR=/data/app

# DEVICE_DIR=/sdcard/P7ZIP  # cannot do "chmod 777"

# For ACER ICONIA A500
# For SAMSUNG S II
# DEVICE_DIR=/data/local/P7ZIP

# For Samsung Galaxy Note 3
DEVICE_DIR=/data/local/tmp/P7ZIP

all: build

build:
	cd jni ; ndk-build NDK_DEBUG=0 V=1

install::
	adb shell mkdir $(DEVICE_DIR)
	adb push ./libs/armeabi/$(TARGET) $(DEVICE_DIR)
	adb shell chmod 777 $(DEVICE_DIR)/$(TARGET)

#	adb push ./libs/armeabi-v7a/$(TARGET) $(DEVICE_DIR)/$(TARGET)-v7a
#	adb shell chmod 777 $(DEVICE_DIR)/$(TARGET)-v7a


mkdir:
	adb shell mkdir $(DEVICE_DIR)

clean:
	rm -fr libs obj

