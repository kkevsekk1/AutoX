package com.stardust.autojs.project

/**
 * @author wilinz
 * @date 2022/5/23
 */
object Constant {
    object Libraries {
        val OPEN_CV = listOf("libopencv_java4.so")
        val GOOGLE_ML_KIT_OCR = listOf("libmlkit_google_ocr_pipeline.so")
        val PADDLE_OCR = listOf(
            "libc++_shared.so",
            "libpaddle_light_api_shared.so",
            "libhiai.so",
            "libhiai_ir.so",
            "libhiai_ir_build.so",
            "libNative.so"
        )
        val TESSERACT_OCR = listOf(
            "libtesseract.so",
            "libpng.so",
            "libleptonica.so",
            "libjpeg.so",
        )
        val P7ZIP = listOf("libp7zip.so")
        val TERMINAL_EMULATOR = listOf(
            "libjackpal-androidterm5.so",
            "libjackpal-termexec2.so"
        )
    }

    object Assets {
        const val PADDLE_OCR = "/models"
        const val PROJECT = "/project"
        const val GOOGLE_ML_KIT_OCR = "/mlkit-google-ocr-models"
    }

    object Permissions {
        const val ACCESSIBILITY_SERVICES = "accessibility_services"
        const val BACKGROUND_START = "background_start"
        const val DRAW_OVERLAY = "draw_overlay"
    }

    object Protocol {
        const val ASSETS = "file:///android_asset"
    }

    object Abi {
        const val ARM64_V8A = "arm64-v8a"
        const val ARMEABI_V7A = "armeabi-v7a"
        const val X86 = "x86"
        const val X86_64 = "x86_64"
        val abis = listOf(ARM64_V8A, ARMEABI_V7A, X86, X86_64)
    }

    object ResourceId {
        const val LAUNCHER_ICON = "ic_launcher"
        const val SPLASH_ICON = "autojs_logo"
    }
}