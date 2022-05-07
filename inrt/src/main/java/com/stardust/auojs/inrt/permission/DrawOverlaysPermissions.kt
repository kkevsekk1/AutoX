package com.stardust.auojs.inrt.permission

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.provider.Settings
import com.linsh.utilseverywhere.AppUtils.getPackageName

object DrawOverlaysPermissions {
    fun isCanDrawOverlays(context: Context): Boolean {
        if (VERSION.SDK_INT >= 23) {
            return Settings.canDrawOverlays(context)
        }
        return true
    }

    @SuppressLint("InlinedApi")
    fun getCanDrawOverlaysIntent(): Intent? {
        val sdkInt = Build.VERSION.SDK_INT
        when {
            sdkInt >= Build.VERSION_CODES.O -> { //8.0以上
                return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            }
            sdkInt >= Build.VERSION_CODES.M -> { //6.0-8.0
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:" + getPackageName())
                return intent
            }
            else -> { //4.4-6.0一下
                //无需处理了
            }
        }
        return null
    }
}
