package com.stardust.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import com.stardust.app.GlobalAppContext
import android.os.Looper
import android.os.Build
import android.content.pm.PackageManager
import android.content.pm.PackageInfo
import android.graphics.drawable.Drawable
import android.os.Handler
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.stardust.R
import java.lang.Exception

/**
 * Created by Stardust on 2018/3/22.
 */
object GlobalAppContext {
    @SuppressLint("StaticFieldLeak")
    private var sApplicationContext: Context? = null
    private var sHandler: Handler? = null
    fun set(a: Application) {
        sHandler = Handler(Looper.getMainLooper())
        sApplicationContext = a.applicationContext
    }

    @JvmStatic
    fun get(): Context {
        checkNotNull(sApplicationContext) { "Call GlobalAppContext.set() to set a application context" }
        return sApplicationContext!!
    }

    @JvmStatic
    fun getString(resId: Int): String {
        return get().getString(resId)
    }

    @JvmStatic
    fun getString(resId: Int, vararg formatArgs: Any?): String {
        return get().getString(resId, *formatArgs)
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun getColor(id: Int): Int {
        return get().getColor(id)
    }

    @JvmStatic
    fun toast(message: String?) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(get(), message, Toast.LENGTH_SHORT).show()
            return
        }
        sHandler!!.post { Toast.makeText(get(), message, Toast.LENGTH_SHORT).show() }
    }

    @JvmStatic
    fun toast(resId: Int) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(get(), resId, Toast.LENGTH_SHORT).show()
            return
        }
        sHandler!!.post { Toast.makeText(get(), resId, Toast.LENGTH_SHORT).show() }
    }

    @JvmStatic
    fun toast(resId: Int, vararg args: Any?) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(get(), getString(resId, *args), Toast.LENGTH_SHORT).show()
            return
        }
        sHandler!!.post {
            Toast.makeText(get(), getString(resId, *args), Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun post(r: Runnable?) {
        sHandler!!.post(r!!)
    }

    @JvmStatic
    fun postDelayed(r: Runnable?, m: Long) {
        sHandler!!.postDelayed(r!!, m)
    }

    @JvmStatic
    @get:Synchronized
    val appName: String
        get() {
            try {
                val packageManager = get().packageManager
                val packageInfo = packageManager.getPackageInfo(
                    get().packageName, 0
                )
                return packageManager.getApplicationLabel(packageInfo.applicationInfo).toString()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return ""
        }

    @JvmStatic
    @get:Synchronized
    val appIcon: Drawable?
        get() {
            try {
                val packageManager = get()!!.packageManager
                val packageInfo = packageManager.getPackageInfo(
                    get()!!.packageName, 0
                )
                return packageManager.getApplicationIcon(packageInfo.applicationInfo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
}