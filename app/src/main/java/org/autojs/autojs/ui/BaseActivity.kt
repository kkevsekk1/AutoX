package org.autojs.autojs.ui

import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.WindowInsetsController
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.stardust.app.GlobalAppContext.post
import com.stardust.theme.ThemeColorManager
import org.autojs.autojs.Pref
import org.autojs.autoxjs.R
import java.util.Arrays

/**
 * Created by Stardust on 2017/1/23.
 */
abstract class BaseActivity : AppCompatActivity() {
    @get:LayoutRes
    abstract val layoutId: Int

    private var mShouldApplyDayNightModeForOptionsMenu = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        initView()
    }

    protected open fun initView() = Unit

    protected fun applyDayNightMode() {
        post {
            if (Pref.isNightModeEnabled()) {
                setNightModeEnabled(Pref.isNightModeEnabled())
            }
        }
    }

    fun setNightModeEnabled(enabled: Boolean) {
        if (enabled) {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        } else {
            delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
        }
        if (delegate.applyDayNight()) {
            recreate()
        }
    }

    override fun onStart() {
        super.onStart()
        if ((window.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) == 0) {
            ThemeColorManager.addActivityStatusBar(this)
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : View?> `$`(resId: Int): T {
        return findViewById<View>(resId) as T
    }

    protected fun checkPermission(vararg permissions: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val requestPermissions = getRequestPermissions(arrayOf(*permissions) )
            if (requestPermissions.isNotEmpty()) {
                requestPermissions(requestPermissions, PERMISSION_REQUEST_CODE)
                return false
            }
            return true
        } else {
            val grantResults = IntArray(permissions.size)
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED)
            onRequestPermissionsResult(PERMISSION_REQUEST_CODE, permissions, grantResults)
            return false
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun getRequestPermissions(permissions: Array<String>): Array<String> {
        val list: MutableList<String> = ArrayList()
        for (permission in permissions) {
            if (checkSelfPermission(permission) == PackageManager.PERMISSION_DENIED) {
                list.add(permission)
            }
        }
        return list.toTypedArray<String>()
    }

    fun setToolbarAsBack(title: String?) {
        setToolbarAsBack(this, R.id.toolbar, title)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (mShouldApplyDayNightModeForOptionsMenu && Pref.isNightModeEnabled()) {
            for (i in 0 until menu.size()) {
                val drawable = menu.getItem(i).icon
                if (drawable != null) {
                    drawable.mutate()
                    drawable.setColorFilter(
                        ContextCompat.getColor(this, R.color.toolbar),
                        PorterDuff.Mode.SRC_ATOP
                    )
                }
            }
            mShouldApplyDayNightModeForOptionsMenu = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    companion object {
        protected const val PERMISSION_REQUEST_CODE: Int = 11186

        @JvmStatic
        fun setToolbarAsBack(activity: AppCompatActivity, id: Int, title: String?) {
            val toolbar = activity.findViewById<Toolbar>(id)
            toolbar.title = title
            activity.setSupportActionBar(toolbar)
            if (activity.supportActionBar != null) {
                toolbar.setNavigationOnClickListener { v: View? -> activity.finish() }
                activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            }
        }
    }
}
