package com.stardust.autojs.util

import android.app.ActivityManager
import android.content.Context
import com.stardust.autojs.R


object ProcessUtils {
    private const val LOG_TAG = "ProcessUtils"

    @JvmStatic
    fun getProcessPid(process: Process): Int {
        return try {
            val pid = process.javaClass.getDeclaredField("pid")
            pid.isAccessible = true
            pid[process] as Int
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    fun isScriptProcess(context: Context): Boolean {
        val pid = android.os.Process.myPid()
        val processName = context.packageName + context.getString(R.string.text_script_process_name)
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in activityManager.runningAppProcesses) {
            if (processInfo.pid == pid && processInfo.processName == processName) {
                return true
            }
        }
        return false
    }
}
