package com.stardust.autojs

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Process
import android.util.Log
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
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
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

    fun initAutojs() {

    }


    companion object {
        private const val TAG = "ScriptService"
    }
}