package org.autojs.autojs.tool

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.stardust.pio.copyToAndClose
import java.io.File


fun Uri.copyTo(context: Context, file: File) {
    context.contentResolver.openInputStream(this)?.copyToAndClose(file.outputStream())
}

fun File.toContentUri(context: Context): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", this)
    } else {
        Uri.fromFile(this)
    }
}

fun parseUriOrNull(uriString: String): Uri? {
    return if (uriString.matches(Regex("^.+://.+$"))) Uri.parse(uriString)
    else null
}