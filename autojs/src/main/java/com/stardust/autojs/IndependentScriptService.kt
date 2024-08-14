package com.stardust.autojs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.stardust.autojs.core.pref.Pref
import com.stardust.autojs.core.pref.PrefKey
import com.stardust.autojs.servicecomponents.ScriptBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class IndependentScriptService : Service() {
    val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        Log.i(TAG, "Pid: ${Process.myPid()}")
        val isForeground = Pref.getDefault(this).getBoolean(PrefKey.KEY_FOREGROUND_SERVIE, false)
        Log.d(TAG, "isForeground: $isForeground")
        if (isForeground) {
            startForeground()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    private fun startForeground() {
        ServiceCompat.startForeground(
            this, 25, buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            },
        )
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val name: CharSequence = "AutoJS Service"
        val description = "script foreground service"
        val channel = NotificationChannel(
            CHANEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT
        )
        channel.description = description
        channel.enableLights(false)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText("前台服务运行中")
            .setSmallIcon(R.drawable.autojs_logo)
            .setWhen(System.currentTimeMillis())
            .setChannelId(CHANEL_ID)
            .setVibrate(LongArray(0))
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_START_FOREGROUND -> startForeground()

            ACTION_STOP_FOREGROUND -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return ScriptBinder(this, scope)
    }


    companion object {
        private const val TAG = "ScriptService"
        private val CHANEL_ID = IndependentScriptService::class.java.name + "_foreground"
        const val ACTION_START_FOREGROUND = "action_start_foreground"
        const val ACTION_STOP_FOREGROUND = "action_stop_foreground"
        fun startForeground(context: Context) {
            val intent = Intent(context, IndependentScriptService::class.java)
            intent.action = ACTION_START_FOREGROUND
            context.startService(intent)
        }
        fun stopForeground(context: Context) {
            val intent = Intent(context, IndependentScriptService::class.java)
            intent.action = ACTION_STOP_FOREGROUND
            context.startService(intent)
        }
    }
}