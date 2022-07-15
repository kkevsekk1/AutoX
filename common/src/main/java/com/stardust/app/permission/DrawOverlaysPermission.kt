package com.stardust.app.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import com.stardust.app.permission.PermissionsSettingsUtil.launchAppPermissionsSettings

/**
 * @author wilinz
 * @date 2022/5/23
 */
object DrawOverlaysPermission {
    fun isCanDrawOverlays(context: Context): Boolean {
        if (VERSION.SDK_INT >= 23) {
            return Settings.canDrawOverlays(context)
        }
        return true
    }

    fun ActivityResultLauncher<Intent>.launchCanDrawOverlaysSettings(packageName: String) {
        try {
            launch(getCanDrawOverlaysIntent(packageName))
        } catch (e: Exception) {
            e.printStackTrace()
            launchAppPermissionsSettings(packageName)
        }
    }

    fun getCanDrawOverlaysIntent(packageName: String): Intent? {
        val intent = if (VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
        } else {
            null
        }
        return intent
    }
}
