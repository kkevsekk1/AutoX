package org.autojs.autojs.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Created by Stardust on 2017/4/22.
 * Modified by wilinz on 2022/5/23
 */
object BitmapTool {
    @JvmStatic
    fun scaleBitmap(origin: Bitmap?, newWidth: Int, newHeight: Int): Bitmap? {
        if (origin == null) {
            return null
        }
        val height = origin.height
        val width = origin.width
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight) // 使用后乘
        return Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
    }

    @JvmStatic
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        //fix bug, setBounds should be called here, otherwise it cannot be display bitmap
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}

fun Bitmap.writeTo(file: File) {
    file.outputStream().use { out ->
        compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

suspend fun saveIcon(context: Context, uri: Uri, file: File): File? {
    return withContext(Dispatchers.IO) {
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        try {
            uri.copyTo(context, file)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}