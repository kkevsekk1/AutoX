package org.autojs.autojs

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import android.webkit.WebView
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import com.flurry.android.FlurryAgent
import com.stardust.app.GlobalAppContext
import com.stardust.autojs.servicecomponents.ScriptServiceConnection
import com.stardust.autojs.util.ProcessUtils
import com.stardust.theme.ThemeColor
import com.tencent.bugly.Bugly
import com.tencent.bugly.crashreport.CrashReport
import org.autojs.autojs.autojs.AutoJs
import org.autojs.autojs.autojs.key.GlobalKeyObserver
import org.autojs.autojs.external.receiver.DynamicBroadcastReceivers
import org.autojs.autojs.theme.ThemeColorManagerCompat
import org.autojs.autojs.timing.TimedTaskManager
import org.autojs.autojs.timing.TimedTaskScheduler
import org.autojs.autojs.tool.CrashHandler
import org.autojs.autojs.ui.error.ErrorReportActivity
import org.autojs.autoxjs.BuildConfig
import org.autojs.autoxjs.R
import java.lang.ref.WeakReference

/**
 * Created by Stardust on 2017/1/27.
 */

class App : MultiDexApplication(), Configuration.Provider {
    lateinit var dynamicBroadcastReceivers: DynamicBroadcastReceivers
        private set

    override fun onCreate() {
        super.onCreate()
        GlobalAppContext.set(
            this, com.stardust.app.BuildConfig.generate(BuildConfig::class.java)
        )
        instance = WeakReference(this)
        setUpStaticsTool()
        setUpDebugEnvironment()
        init()
    }

    private fun setUpStaticsTool() {
        if (BuildConfig.DEBUG)
            return
        FlurryAgent.Builder()
            .withLogEnabled(BuildConfig.DEBUG)
            .build(this, "D42MH48ZN4PJC5TKNYZD")
    }

    private fun setUpDebugEnvironment() {
        Bugly.isDev = false
        val crashHandler = CrashHandler(ErrorReportActivity::class.java)

        val strategy = CrashReport.UserStrategy(applicationContext)
        strategy.setCrashHandleCallback(crashHandler)

        CrashReport.initCrashReport(applicationContext, BUGLY_APP_ID, false, strategy)

        crashHandler.setBuglyHandler(Thread.getDefaultUncaughtExceptionHandler())
        Thread.setDefaultUncaughtExceptionHandler(crashHandler)
    }

    private fun init() {
        ThemeColorManagerCompat.init(
            this,
            ThemeColor(
                ContextCompat.getColor(this, R.color.colorPrimary),
                ContextCompat.getColor(this, R.color.colorPrimaryDark),
                ContextCompat.getColor(this, R.color.colorAccent)
            )
        )
        if (ProcessUtils.isScriptProcess(this)) {
            AutoJs.initInstance(this)
            if (Pref.isRunningVolumeControlEnabled()) {
                GlobalKeyObserver.init()
            }
            TimedTaskScheduler.init(this)
            initDynamicBroadcastReceivers()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WebView.setDataDirectorySuffix(getString(R.string.text_script_process_name))
            };
            ScriptServiceConnection.start(this)
        }
        Log.i(
            TAG, "Pid: ${Process.myPid()}, isScriptProcess: ${ProcessUtils.isScriptProcess(this)}"
        )

    }


    @SuppressLint("CheckResult")
    private fun initDynamicBroadcastReceivers() {
        dynamicBroadcastReceivers = DynamicBroadcastReceivers(this)
        val localActions = ArrayList<String>()
        val actions = ArrayList<String>()
        TimedTaskManager.allIntentTasks
            .filter { task -> task.action != null }
            .doOnComplete {
                if (localActions.isNotEmpty()) {
                    dynamicBroadcastReceivers.register(localActions, true)
                }
                if (actions.isNotEmpty()) {
                    dynamicBroadcastReceivers.register(actions, false)
                }
                @Suppress("DEPRECATION")
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(
                    Intent(
                        DynamicBroadcastReceivers.ACTION_STARTUP
                    )
                )
            }
            .subscribe({
                if (it.isLocal) {
                    it.action?.let { it1 -> localActions.add(it1) }
                } else {
                    it.action?.let { it1 -> actions.add(it1) }
                }
            }, { it.printStackTrace() })


    }

    companion object {
        private const val TAG = "App"
        private const val BUGLY_APP_ID = "19b3607b53"

        private lateinit var instance: WeakReference<App>

        val app: App
            get() = instance.get()!!
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    }

}
