package com.stardust.autojs

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.stardust.app.SimpleActivityLifecycleCallbacks
import com.stardust.autojs.core.accessibility.AccessibilityBridge
import com.stardust.autojs.core.activity.ActivityInfoProvider
import com.stardust.autojs.core.console.ConsoleImpl
import com.stardust.autojs.core.console.GlobalConsole
import com.stardust.autojs.core.image.capture.ScreenCaptureManager
import com.stardust.autojs.core.image.capture.ScreenCaptureRequester
import com.stardust.autojs.core.record.accessibility.AccessibilityActionRecorder
import com.stardust.autojs.core.util.Shell
import com.stardust.autojs.engine.LoopBasedJavaScriptEngine
import com.stardust.autojs.engine.RootAutomatorEngine
import com.stardust.autojs.engine.ScriptEngineManager
import com.stardust.autojs.rhino.AndroidContextFactory
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.runtime.ScriptRuntimeV2
import com.stardust.autojs.runtime.accessibility.AccessibilityConfig
import com.stardust.autojs.runtime.api.AbstractShell
import com.stardust.autojs.runtime.api.AppUtils
import com.stardust.autojs.script.AutoFileSource
import com.stardust.autojs.script.JavaScriptSource
import com.stardust.util.ResourceMonitor
import com.stardust.util.ResourceMonitor.UnclosedResourceDetectedException
import com.stardust.util.ResourceMonitor.UnclosedResourceException
import com.stardust.util.ScreenMetrics
import com.stardust.util.Supplier
import com.stardust.util.UiHandler
import com.stardust.view.accessibility.AccessibilityNotificationObserver
import com.stardust.view.accessibility.AccessibilityService
import com.stardust.view.accessibility.AccessibilityService.Companion.addDelegate
import com.stardust.view.accessibility.LayoutInspector
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.WrappedException
import java.io.File

/**
 * Created by Stardust on 2017/11/29.
 */
abstract class AutoJs protected constructor(protected val application: Application) {
    private val mContext: Context = application.applicationContext
    private val accessibilityActionRecorder = AccessibilityActionRecorder()
    private val mNotificationObserver: AccessibilityNotificationObserver =
        AccessibilityNotificationObserver(mContext)
    private val mScreenCaptureRequester: ScreenCaptureRequester = ScreenCaptureManager()

    val layoutInspector: LayoutInspector = LayoutInspector(mContext)
    val scriptEngineManager: ScriptEngineManager = ScriptEngineManager(mContext)
    val uiHandler: UiHandler = UiHandler(mContext)
    val appUtils: AppUtils by lazy { createAppUtils(mContext) }
    val infoProvider: ActivityInfoProvider = ActivityInfoProvider(mContext)
    val scriptEngineService: ScriptEngineService
    val globalConsole: GlobalConsole by lazy { createGlobalConsole() }

    init {
        scriptEngineService = buildScriptEngineService()
        ScriptEngineService.instance = scriptEngineService
        init()
    }

    protected open fun createAppUtils(context: Context): AppUtils {
        return AppUtils(mContext)
    }

    protected open fun createGlobalConsole(): GlobalConsole {
        return GlobalConsole(uiHandler)
    }

    protected fun init() {
        addAccessibilityServiceDelegates()
        registerActivityLifecycleCallbacks()
        ResourceMonitor.setExceptionCreator { resource: ResourceMonitor.Resource? ->
            val exception: Exception =
                if (org.mozilla.javascript.Context.getCurrentContext() != null) {
                    WrappedException(UnclosedResourceException(resource))
                } else {
                    UnclosedResourceException(resource)
                }
            exception.fillInStackTrace()
            exception
        }
        //        ResourceMonitor.setUnclosedResourceDetectedHandler(detectedException -> mGlobalConsole.error(detectedException));
        ResourceMonitor.setUnclosedResourceDetectedHandler { data: UnclosedResourceDetectedException? ->
            globalConsole.error(
                data
            )
        }
        /*      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mContext.startForegroundService(new Intent(mContext, CaptureForegroundService.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }*/
    }

    abstract fun ensureAccessibilityServiceEnabled()
    protected fun buildScriptEngineService(): ScriptEngineService {
        initScriptEngineManager()
        return ScriptEngineServiceBuilder()
            .uiHandler(uiHandler)
            .globalConsole(globalConsole)
            .engineManger(scriptEngineManager)
            .build()
    }

    protected open fun initScriptEngineManager() {
        scriptEngineManager.registerEngine(JavaScriptSource.ENGINE) {
            val engine = LoopBasedJavaScriptEngine(mContext)
            engine.runtime = createRuntime()
            engine
        }
        initContextFactory()
        scriptEngineManager.registerEngine(AutoFileSource.ENGINE) { RootAutomatorEngine(mContext) }
    }

    protected fun initContextFactory() {
        ContextFactory.initGlobal(AndroidContextFactory(File(mContext.cacheDir, "classes")))
    }

    protected open fun createRuntime(): ScriptRuntime {
        return ScriptRuntimeV2.Builder().also {
            it.console = ConsoleImpl(uiHandler, globalConsole)
            it.screenCaptureRequester = mScreenCaptureRequester
            it.accessibilityBridge = AccessibilityBridgeImpl(uiHandler)
            it.uiHandler = uiHandler
            it.appUtils = appUtils
            it.engineService = scriptEngineService
            it.shellSupplier = Supplier<AbstractShell> { Shell(mContext, true) }
        }.build()
    }

    protected fun registerActivityLifecycleCallbacks() {
        application.registerActivityLifecycleCallbacks(object : SimpleActivityLifecycleCallbacks() {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                ScreenMetrics.initIfNeeded(activity)
                appUtils.currentActivity = activity
            }

            override fun onActivityPaused(activity: Activity) {
                appUtils.currentActivity = null
            }

            override fun onActivityResumed(activity: Activity) {
                appUtils.currentActivity = activity
            }
        })
    }

    private fun addAccessibilityServiceDelegates() {
        addDelegate(100, infoProvider)
        addDelegate(200, mNotificationObserver)
        addDelegate(300, accessibilityActionRecorder)
    }

    abstract fun waitForAccessibilityServiceEnabled()
    protected open fun createAccessibilityConfig(): AccessibilityConfig? {
        return AccessibilityConfig()
    }

    private inner class AccessibilityBridgeImpl(uiHandler: UiHandler) :
        AccessibilityBridge(mContext, createAccessibilityConfig(), uiHandler) {
        override fun ensureServiceEnabled() {
            ensureAccessibilityServiceEnabled()
        }

        override fun waitForServiceEnabled() {
            waitForAccessibilityServiceEnabled()
        }

        override fun getService(): AccessibilityService? {
            return AccessibilityService.instance
        }

        override fun getInfoProvider(): ActivityInfoProvider {
            return this@AutoJs.infoProvider
        }


        override fun getNotificationObserver(): AccessibilityNotificationObserver {
            return mNotificationObserver
        }
    }
}
