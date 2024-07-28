package com.stardust.autojs.engine

import androidx.annotation.CallSuper
import com.stardust.autojs.execution.ScriptExecution
import com.stardust.autojs.script.ScriptSource
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Stardust on 2017/4/2.
 *
 *
 *
 *
 * A ScriptEngine is created by [ScriptEngineManager.createEngine] ()}, and then can be
 * used to execute script with [ScriptEngine.execute] in the **same** thread.
 * When the execution finish successfully, the engine should be destroy in the thread that created it.
 *
 *
 * If you want to stop the engine in other threads, you should call [ScriptEngine.forceStop].
 */
interface ScriptEngine<S : ScriptSource?> {
    var id: Int
    val isDestroyed: Boolean
    val uncaughtException: Throwable?
    fun put(name: String, value: Any?)
    fun execute(scriptSource: S): Any?
    fun forceStop()
    fun destroy()
    fun setTag(key: String, value: Any?)
    fun getTag(key: String): Any?
    fun cwd(): String?
    fun uncaughtException(throwable: Throwable?)
    fun setOnDestroyListener(listener: OnDestroyListener)
    fun init()

    interface OnDestroyListener {
        fun onDestroy(engine: ScriptEngine<*>)
    }

    interface EngineEvent {
        fun emit(name: String, vararg args: Any?)
    }


    abstract class AbstractScriptEngine<S : ScriptSource?> : ScriptEngine<S> {
        private val mTags: MutableMap<String, Any> = ConcurrentHashMap()
        private var mOnDestroyListener: OnDestroyListener? = null

        @Volatile
        final override var isDestroyed = false
            private set
        private var mUncaughtException: Throwable? = null

        private val mId = AtomicInteger(ScriptExecution.NO_ID)
        override var id: Int
            get() = mId.get()
            set(value) {
                mId.compareAndSet(ScriptExecution.NO_ID, value)
            }

        override fun setTag(key: String, value: Any?) {
            if (value == null) {
                mTags.remove(key)
            } else {
                mTags[key] = value
            }
        }

        override fun getTag(key: String): Any? = mTags[key]


        @CallSuper
        override fun destroy() {
            mOnDestroyListener?.onDestroy(this)
            isDestroyed = true
        }

        override fun cwd(): String? {
            return getTag(TAG_WORKING_DIRECTORY) as String?
        }

        override fun setOnDestroyListener(listener: OnDestroyListener) {
            if (mOnDestroyListener != null) throw SecurityException("setOnDestroyListener can be called only once")
            mOnDestroyListener = listener
        }

        override fun uncaughtException(throwable: Throwable?) {
            mUncaughtException = throwable
            forceStop()
        }

        override val uncaughtException: Throwable?
            get() = mUncaughtException
    }

    companion object {
        const val TAG_ENV_PATH = "env_path"
        const val TAG_SOURCE = "source"
        const val TAG_WORKING_DIRECTORY = "execute_path"
    }
}
