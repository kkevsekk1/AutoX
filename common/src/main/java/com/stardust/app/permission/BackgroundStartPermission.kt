package com.stardust.app.permission

import android.app.AppOpsManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * @author wilinz
 * @date 2022/5/23
 */
object BackgroundStartPermission {

    private fun isXiaoMi(): Boolean {
        return checkManufacturer("xiaomi")
    }

    /**
    * @author plus1998
    * @date 2024/4/18
    * @desc 澎湃OS目前是兼容的
    */
    private fun isMiui(): Boolean {
        val value = getSystemProperty("ro.miui.ui.version.name")
        return !android.text.TextUtils.isEmpty(value)
    }

    private fun isOppo(): Boolean {
        return checkManufacturer("oppo")
    }

    private fun isVivo(): Boolean {
        return checkManufacturer("vivo")
    }

    private fun checkManufacturer(manufacturer: String): Boolean {
        return manufacturer.equals(Build.MANUFACTURER, true)
    }

    private fun getSystemProperty(propName: String): String? {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
        } catch (ex: IOException) {
            return null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    // ignore
                }
            }
        }
        return line
    }

    fun isBackgroundStartAllowed(context: Context): Boolean {
        return when {
            isXiaoMi() && isMiui() -> {
                isXiaomiBgStartPermissionAllowed(context)
            }

            isVivo() -> {
                isVivoBgStartPermissionAllowed(context)
            }

            isOppo() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                Settings.canDrawOverlays(context)
            }

            else -> true
        }
    }


    private fun isXiaomiBgStartPermissionAllowed(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        try {
            val op = 10021
            val method = ops.javaClass.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            val result =
                method.invoke(ops, op, android.os.Process.myUid(), context.packageName) as Int
            return result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun isVivoBgStartPermissionAllowed(context: Context): Boolean {
        return getVivoBgStartPermissionStatus(context) == 0
    }

    /**
     * 判断Vivo后台弹出界面状态， 1无权限，0有权限
     * @param context context
     */
    private fun getVivoBgStartPermissionStatus(context: Context): Int {
        val uri: Uri =
            Uri.parse("content://com.vivo.permissionmanager.provider.permission/start_bg_activity")
        val selection = "pkgname = ?"
        val selectionArgs = arrayOf(context.packageName)
        var state = 1
        try {
            context.contentResolver.query(uri, null, selection, selectionArgs, null)?.use {
                if (it.moveToFirst()) {
                    state = it.getInt(it.getColumnIndexOrThrow("currentstate"))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return state
    }

}