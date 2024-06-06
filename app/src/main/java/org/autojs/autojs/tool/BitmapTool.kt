package org.autojs.autojs.tool

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Created by Stardust on 2017/4/22.
 * Modified by wilinz on 2022/5/23
 */
object BitmapTool {
    @JvmStatic
    fun scaleBitmap(origin: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        return origin.scale(newWidth, newHeight, false)
    }

    @JvmStatic
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        return drawable.toBitmap()
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
