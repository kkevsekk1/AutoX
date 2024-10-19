package com.stardust.autojs.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.autojs.R

private const val TAG = "PermissionUtil"
object PermissionUtil {
    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            Environment.getExternalStorageDirectory().canRead()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun showPermissionDialog(
        context: Activity,
        storagePermissionRequestLauncher: ActivityResultLauncher<Unit>
    ) {
        if (!checkStoragePermission()) {
            MaterialDialog.Builder(context)
                .title(R.string.text_hint)
                .content(R.string.text_request_permission)
                .positiveText(R.string.ok)
                .negativeText(R.string.cancel)
                .onPositive { _, _ ->
                    storagePermissionRequestLauncher.launch(Unit)
                }.show()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
class StoragePermissionResultContract : ActivityResultContract<Unit, Boolean>() {
    override fun createIntent(context: Context, input: Unit): Intent {
        return Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return PermissionUtil.checkStoragePermission()
    }

}