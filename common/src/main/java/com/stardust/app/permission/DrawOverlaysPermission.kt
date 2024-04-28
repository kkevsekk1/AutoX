package com.stardust.app.permission

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import com.stardust.app.permission.PermissionsSettingsUtil.launchAppPermissionsSettings

/**
 * @author wilinz
 * @date 2022/5/23
 */
object DrawOverlaysPermission {
    fun isCanDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun ActivityResultLauncher<Intent>.launchCanDrawOverlaysSettings(packageName: String) {
        try {
            launch(getCanDrawOverlaysIntent(packageName))
        } catch (e: Exception) {
            e.printStackTrace()
            launchAppPermissionsSettings(packageName)
        }
    }

    private fun getCanDrawOverlaysIntent(packageName: String): Intent {
        val intent =
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.parse("package:$packageName")
            }
        return intent
    }
}
