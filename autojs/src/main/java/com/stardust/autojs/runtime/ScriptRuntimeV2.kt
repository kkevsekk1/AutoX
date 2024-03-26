package com.stardust.autojs.runtime

import com.stardust.autojs.ScriptEngineService
import com.stardust.autojs.core.accessibility.AccessibilityBridge
import com.stardust.autojs.core.image.capture.ScreenCaptureRequester
import com.stardust.autojs.core.looper.Loopers
import com.stardust.autojs.rhino.TopLevelScope
import com.stardust.autojs.runtime.api.AbstractShell
import com.stardust.autojs.runtime.api.AppUtils
import com.stardust.autojs.runtime.api.Console
import com.stardust.autojs.runtime.api.Events
import com.stardust.autojs.runtime.api.Sensors
import com.stardust.autojs.runtime.api.Threads
import com.stardust.autojs.runtime.api.Timers
import com.stardust.autojs.util.ObjectWatcher
import com.stardust.util.Supplier
import com.stardust.util.UiHandler

class ScriptRuntimeV2(builder: Builder) : ScriptRuntime(builder) {

    override fun init() {
        check(loopers == null) { "already initialized" }
        threads = Threads(this)
        timers = Timers(this)
        loopers = Loopers(this)
        events = Events(uiHandler.context, accessibilityBridge, this)
        mThread = Thread.currentThread()
        sensors = Sensors(uiHandler.context, this)
    }
    override fun getUiHandler(): UiHandler {
        return uiHandler
    }

    override fun getTopLevelScope(): TopLevelScope = mTopLevelScope
    override fun setTopLevelScope(topLevelScope: TopLevelScope) {
        check(mTopLevelScope == null) { "top level has already exists" }
        mTopLevelScope = topLevelScope
    }
    override fun onExit() {
        super.onExit()
        ObjectWatcher.default.watch(this, engines.myEngine().toString() + "::" + TAG)
    }

    class Builder {
        var uiHandler: UiHandler? = null
        var console: Console? = null
        var accessibilityBridge: AccessibilityBridge? = null
        var shellSupplier: Supplier<AbstractShell>? = null
        var screenCaptureRequester: ScreenCaptureRequester? = null
        var appUtils: AppUtils? = null
        var engineService: ScriptEngineService? = null
        fun build(): ScriptRuntimeV2 {
            return ScriptRuntimeV2(this)
        }
    }

    companion object {
        private const val TAG = "ScriptRuntimeV2"
    }
}


