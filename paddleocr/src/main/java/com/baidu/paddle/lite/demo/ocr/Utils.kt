package com.baidu.paddle.lite.demo.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Environment
import java.io.*
import java.lang.Exception
import java.util.*
import kotlin.math.floor

object Utils {
    private val TAG = Utils::class.java.simpleName
    private fun copyFileFromAssets(appCtx: Context, srcPath: String, dstPath: String) {
        if (srcPath.isEmpty() || dstPath.isEmpty()) {
            return
        }
        appCtx.assets.open(srcPath).use {input->
            File(dstPath).outputStream().use { out->
                input.copyTo(out)
            }
        }
    }

    fun copyDirectoryFromAssets(appCtx: Context, srcDir: String, dstDir: String) {
        if (srcDir.isEmpty() || dstDir.isEmpty()) {
            return
        }
        try {
            if (!File(dstDir).exists()) {
                File(dstDir).mkdirs()
            }
            //             for (String fileName : appCtx.getAssets().list(srcDir)) {
//                 String srcSubPath = srcDir + File.separator + fileName;
//                 String dstSubPath = dstDir + File.separator + fileName;
//                 if (new File(srcSubPath).isDirectory()) {
//                     copyDirectoryFromAssets(appCtx, srcSubPath, dstSubPath);
//                 } else {
//                     copyFileFromAssets(appCtx, srcSubPath, dstSubPath);
//                 }
//             }
            // 由于存在Assets路径获取失败导致找不到模型的bug，这里直接使用完整路径进行复制
            var srcSubPath = srcDir + File.separator.toString() + "ch_ppocr_mobile_v2.0_cls_opt.nb"
            var dstSubPath = dstDir + File.separator.toString() + "ch_ppocr_mobile_v2.0_cls_opt.nb"
            copyFileFromAssets(appCtx, srcSubPath, dstSubPath)
            srcSubPath = srcDir + File.separator.toString() + "ch_ppocr_mobile_v2.0_det_opt.nb"
            dstSubPath = dstDir + File.separator.toString() + "ch_ppocr_mobile_v2.0_det_opt.nb"
            copyFileFromAssets(appCtx, srcSubPath, dstSubPath)
            srcSubPath = srcDir + File.separator.toString() + "ch_ppocr_mobile_v2.0_rec_opt.nb"
            dstSubPath = dstDir + File.separator.toString() + "ch_ppocr_mobile_v2.0_rec_opt.nb"
            copyFileFromAssets(appCtx, srcSubPath, dstSubPath)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun parseFloatsFromString(string: String, delimiter: String?): FloatArray {
        val pieces = string.trim { it <= ' ' }.lowercase(Locale.getDefault()).split(
            delimiter!!
        ).toTypedArray()
        val floats = FloatArray(pieces.size)
        for (i in pieces.indices) {
            floats[i] = pieces[i].trim { it <= ' ' }.toFloat()
        }
        return floats
    }

    fun parseLongsFromString(string: String, delimiter: String?): LongArray {
        val pieces = string.trim { it <= ' ' }.lowercase(Locale.getDefault()).split(
            delimiter!!
        ).toTypedArray()
        val longs = LongArray(pieces.size)
        for (i in pieces.indices) {
            longs[i] = pieces[i].trim { it <= ' ' }.toLong()
        }
        return longs
    }

    val sDCardDirectory: String
        get() = Environment.getExternalStorageDirectory().absolutePath

    // String hardware = android.os.Build.HARDWARE;
    // return hardware.equalsIgnoreCase("kirin810") || hardware.equalsIgnoreCase("kirin990");
    val isSupportedNPU: Boolean
        get() = false

    // String hardware = android.os.Build.HARDWARE;
    // return hardware.equalsIgnoreCase("kirin810") || hardware.equalsIgnoreCase("kirin990");
    fun resizeWithStep(bitmap: Bitmap, maxLength: Int, step: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val maxWH = Math.max(width, height)
        var ratio = 1f
        var newWidth = width
        var newHeight = height
        if (maxWH > maxLength) {
            ratio = maxLength * 1.0f / maxWH
            newWidth = floor((ratio * width).toDouble()).toInt()
            newHeight = floor((ratio * height).toDouble()).toInt()
        }
        newWidth -= newWidth % step
        if (newWidth == 0) {
            newWidth = step
        }
        newHeight -= newHeight % step
        if (newHeight == 0) {
            newHeight = step
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        return try {
            val bmRotated =
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            bmRotated
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            null
        }
    }
}