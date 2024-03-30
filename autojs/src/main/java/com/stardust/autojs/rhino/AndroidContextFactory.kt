package com.stardust.autojs.rhino

import android.os.Looper
import android.util.Log
import com.stardust.autojs.runtime.ScriptBridges
import com.stardust.autojs.runtime.exception.ScriptInterruptedException
import com.stardust.automator.UiObject
import com.stardust.automator.UiObjectCollection
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Stardust on 2017/4/5.
 */
open class AndroidContextFactory(private val cacheDirectory: File) : ContextFactory() {
    companion object {
        const val LOG_TAG = "ContextFactory"
        val bridges = ScriptBridges()
    }

    private val mContextCount = AtomicInteger()
    private val wrapFactory = WrapFactory()

    init {
        initApplicationClassLoader(createClassLoader(AndroidContextFactory::class.java.classLoader!!))
    }

    /**
     * Create a ClassLoader which is able to deal with bytecode
     *
     * @param parent the parent of the create classloader
     * @return a new ClassLoader
     */
    final override fun createClassLoader(parent: ClassLoader): AndroidClassLoader {
        return AndroidClassLoader(parent, cacheDirectory)
    }

    override fun observeInstructionCount(cx: Context, instructionCount: Int) {
        if (Thread.currentThread().isInterrupted && Looper.myLooper() != Looper.getMainLooper()) {
            throw ScriptInterruptedException()
        }
    }

    override fun makeContext(): Context {
        val cx: Context = AutoJsContext(this)
        setupContext(cx)
        return cx
    }

    private fun setupContext(context: Context) {
        context.instructionObserverThreshold = 10000
        context.optimizationLevel = -1
        context.languageVersion = Context.VERSION_ES6
        context.locale = Locale.getDefault()
        context.wrapFactory = wrapFactory
    }

    override fun onContextCreated(cx: Context) {
        super.onContextCreated(cx)
        val i = mContextCount.incrementAndGet()
        Log.d(LOG_TAG, "onContextCreated: count = $i")
    }

    override fun onContextReleased(cx: Context) {
        super.onContextReleased(cx)
        val i = mContextCount.decrementAndGet()
        Log.d(LOG_TAG, "onContextReleased: count = $i")
    }

    open class WrapFactory : org.mozilla.javascript.WrapFactory() {
        override fun wrap(cx: Context, scope: Scriptable, obj: Any?, staticType: Class<*>?): Any? {
            return when (obj) {
                is UiObject -> UiObjectProxy(obj)
                is String -> bridges.toString(obj)
                is UiObjectCollection -> bridges.asArray(obj)
                else -> super.wrap(cx, scope, obj, staticType)
            }
        }
    }
}