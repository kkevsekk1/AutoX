package com.stardust.autojs

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Process
import android.util.Log
import org.mozilla.javascript.ContextFactory

class IndependentScriptService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        Log.i(TAG, "Pid: ${Process.myPid()}")
        Log.i(TAG, ContextFactory.getGlobal().toString())
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onBind(intent: Intent?): IBinder? {
        Rs()
        return null
    }

    fun initAutojs() {

    }

    class Rs : Binder() {

    }

    companion object {
        private const val TAG = "ScriptService"
    }
}