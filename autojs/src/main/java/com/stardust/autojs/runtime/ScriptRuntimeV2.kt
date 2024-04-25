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
import com.stardust.util.Supplier
import com.stardust.util.UiHandler
import org.mozilla.javascript.RhinoException
import java.io.BufferedReader
import java.io.PrintWriter
import java.io.StringReader
import java.io.StringWriter

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
//        ObjectWatcher.default.watch(this, engines.myEngine().toString() + "::" + TAG)
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
        @JvmStatic
        fun getStackTrace(e: Throwable, printJavaStackTrace: Boolean): String? {
            val message = e.message
            val scriptTrace = StringBuilder(if (message == null) "" else message + "\n")
            if (e is RhinoException) {
                scriptTrace.append(e.details()).append("\n")
                for (element in e.scriptStack) {
                    element.renderV8Style(scriptTrace)
                    scriptTrace.append("\n")
                }
                if (printJavaStackTrace) {
                    scriptTrace.append("- - - - - - - - - - -\n")
                } else {
                    return scriptTrace.toString()
                }
            }
            val stringWriter = StringWriter()
            PrintWriter(stringWriter).use { e.printStackTrace(it) }
            val bufferedReader = BufferedReader(StringReader(stringWriter.toString()))
            var line: String?
            while (bufferedReader.readLine().also { line = it } != null) {
                scriptTrace.append("\n").append(line)
            }
            return scriptTrace.toString()
        }
    }
}


