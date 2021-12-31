# The ARMv7 is significanly faster due to the use of the hardware FPU
APP_ABI := armeabi
# p7zip armeabi and armeabi-v7a run at the same speed (p7zip does not use FPU)
# APP_ABI := armeabi armeabi-v7a
#APP_PLATFORM := android-8
