package org.autojs.autojs.ui.floating

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.stardust.enhancedfloaty.FloatyService

class FloatyService : Service() {
    private var circularMenu: CircularMenu? = null
    override fun onCreate() {
        super.onCreate()
        startService(Intent(this, FloatyService::class.java))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == SHOW_CIRCULAR_MENU) {
            circularMenu = CircularMenu(this)
        } else if (action == HIED_CIRCULAR_MENU) {
            circularMenu?.close()
            circularMenu = null
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val SHOW_CIRCULAR_MENU = "org.autojs.autojs.ui.floating.CircularMenu.show"
        const val HIED_CIRCULAR_MENU = "org.autojs.autojs.ui.floating.CircularMenu.hide"
    }
}