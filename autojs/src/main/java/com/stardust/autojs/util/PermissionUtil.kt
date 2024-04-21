package com.stardust.autojs.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.autojs.R


object PermissionUtil {
    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            Environment.getExternalStorageDirectory().canRead()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun requestStoragePermission(context: Activity, requestCode: Int) {
        val intent: Intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivityForResult(intent, requestCode)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showPermissionDialog(context: Activity) {
        if (!checkStoragePermission()) {
            MaterialDialog.Builder(context)
                .title(R.string.text_hint)
                .content(R.string.text_request_permission)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive { _, _ ->
                    requestStoragePermission(context, 951)
                }.show()
        }
    }
}