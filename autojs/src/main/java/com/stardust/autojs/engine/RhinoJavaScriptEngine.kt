package com.stardust.autojs.engine

import android.util.Log
import android.view.View
import com.stardust.autojs.core.ui.ViewExtras
import com.stardust.autojs.engine.module.AssetAndUrlModuleSourceProvider
import com.stardust.autojs.engine.module.ScopeRequire
import com.stardust.autojs.execution.ExecutionConfig
import com.stardust.autojs.project.ScriptConfig
import com.stardust.autojs.rhino.AndroidContextFactory
import com.stardust.autojs.rhino.AutoJsContext
import com.stardust.autojs.rhino.RhinoAndroidHelper
import com.stardust.autojs.rhino.TopLevelScope
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.script.JavaScriptSource
import com.stardust.pio.UncheckedIOException
import org.mozilla.javascript.Context
import org.mozilla.javascript.Script
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by Stardust on 2017/4/2.
 */

open class RhinoJavaScriptEngine(private val mAndroidContext: android.content.Context) :
    JavaScriptEngine() {

    private val wrapFactory = WrapFactory()
    val context: Context = enterContext()
    private val mScriptable: TopLevelScope = createScope(this.context)
    lateinit var thread: Thread
        private set

    private val initScript: Script by lazy<Script> {
        try {
            val reader = InputStreamReader(mAndroidContext.assets.open("init.js"))
            val script = context.compileReader(reader, SOURCE_NAME_INIT, 1, null)
            script
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    val scriptable: Scriptable
        get() = mScriptable

    init {

    }

    override fun put(name: String, value: Any?) {
        ScriptableObject.putProperty(mScriptable, name, Context.javaToJS(value, mScriptable))
    }

    override fun setRuntime(runtime: ScriptRuntime) {
        super.setRuntime(runtime)
        runtime.bridges.setup(this)
        runtime.topLevelScope = mScriptable
    }

    public override fun doExecution(source: JavaScriptSource): Any? {
        val reader = source.nonNullScriptReader
        try {
            val script = context.compileReader(reader, source.toString(), 1, null)
            return if (hasFeature(ScriptConfig.FEATURE_CONTINUATION)) {
                context.executeScriptWithContinuations(script, mScriptable)
            } else {
                script.exec(context, mScriptable)
            }
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }

    }

    fun hasFeature(feature: String): Boolean {
        val config = getTag(ExecutionConfig.tag) as ExecutionConfig?
        return config != null && config.scriptConfig.hasFeature(feature)
    }


    override fun forceStop() {
        Log.d(LOG_TAG, "forceStop: interrupt Thread: $thread")
        thread.interrupt()
    }


    @Synchronized
    override fun destroy() {
        super.destroy()
        Log.d(LOG_TAG, "on destroy")
        Context.exit()
    }

    override fun init() {
        thread = Thread.currentThread()
        ScriptableObject.putProperty(mScriptable, "__engine__", this)
        initRequireBuilder(context, mScriptable)
        try {
            context.executeScriptWithContinuations(initScript, mScriptable)
        } catch (e: IllegalArgumentException) {
            if ("Script argument was not a script or was not created by interpreted mode " == e.message) {
                initScript.exec(context, mScriptable)
            } else {
                throw e
            }
        }
    }

    private fun initRequireBuilder(context: Context, scope: Scriptable) {
        val provider = AssetAndUrlModuleSourceProvider(
            mAndroidContext,
            listOf(
                AssetAndUrlModuleSourceProvider.MODULE_DIR,
                AssetAndUrlModuleSourceProvider.NPM_MODULE_DIR
            )
        )
        val require = ScopeRequire(
            context, scope, SoftCachingModuleScriptProvider(provider),
            null, null, false
        )
        require.install(scope)
    }

    private fun createScope(context: Context): TopLevelScope {
        val topLevelScope = TopLevelScope()
        topLevelScope.initStandardObjects(context, false)
        return topLevelScope
    }

    fun enterContext(): Context {
        val context = RhinoAndroidHelper(mAndroidContext).enterContext()
        setupContext(context)
        return context
    }

    fun setupContext(context: Context) {
        context.wrapFactory = wrapFactory
        (context as? AutoJsContext)?.let {
            it.rhinoJavaScriptEngine = this
        }
    }


    private inner class WrapFactory : AndroidContextFactory.WrapFactory() {
        override fun wrapAsJavaObject(
            cx: Context?,
            scope: Scriptable,
            javaObject: Any?,
            staticType: Class<*>?
        ): Scriptable? {
            //Log.d(LOG_TAG, "wrapAsJavaObject: java = " + javaObject + ", result = " + result + ", scope = " + scope);
            return if (javaObject is View) {
                ViewExtras.getNativeView(scope, javaObject, staticType, runtime)
            } else {
                super.wrapAsJavaObject(cx, scope, javaObject, staticType)
            }
        }

    }

    companion object {
        const val SOURCE_NAME_INIT = "<init>"
        private const val LOG_TAG = "RhinoJavaScriptEngine"

    }


}
