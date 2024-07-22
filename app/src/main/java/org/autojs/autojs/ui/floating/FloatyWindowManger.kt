package org.autojs.autojs.ui.floating

import android.content.Context
import android.content.Intent
import android.view.WindowManager
import com.stardust.app.GlobalAppContext
import com.stardust.app.GlobalAppContext.toast
import com.stardust.autojs.util.FloatingPermission
import com.stardust.enhancedfloaty.FloatyWindow
import org.autojs.autoxjs.R

/**
 * Created by Stardust on 2017/9/30.
 */
object FloatyWindowManger {

    @JvmStatic
    fun addWindow(context: Context?, window: FloatyWindow?): Boolean {
        checkNotNull(context) { "context is null" }
        context.startService(Intent(context, FloatyService::class.java))
        val hasPermission = FloatingPermission.ensurePermissionGranted(context)
        try {
            com.stardust.enhancedfloaty.FloatyService.addWindow(window)
            return true
            // SecurityException: https://github.com/hyb1996-guest/AutoJsIssueReport/issues/4781
        } catch (e: Exception) {
            e.printStackTrace()
            if (hasPermission) {
                FloatingPermission.manageDrawOverlays(context)
                toast(R.string.text_no_floating_window_permission)
            }
        }
        return false
    }

    fun showCircularMenu(): Boolean {
        val context = GlobalAppContext.get()
        context.startService(Intent(context, FloatyService::class.java).apply {
            action = FloatyService.SHOW_CIRCULAR_MENU
        })
        return true
    }

    fun hideCircularMenu() {
        val context = GlobalAppContext.get()
        context.startService(Intent(context, FloatyService::class.java).apply {
            action = FloatyService.HIED_CIRCULAR_MENU
        })
    }

    @JvmStatic
    fun getWindowType(): Int = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
}
