package com.stardust.autojs.execution

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.stardust.autojs.ScriptEngineService
import com.stardust.autojs.core.eventloop.EventEmitter
import com.stardust.autojs.core.eventloop.SimpleEvent
import com.stardust.autojs.engine.JavaScriptEngine
import com.stardust.autojs.engine.LoopBasedJavaScriptEngine
import com.stardust.autojs.engine.LoopBasedJavaScriptEngine.ExecuteCallback
import com.stardust.autojs.engine.ScriptEngine
import com.stardust.autojs.engine.ScriptEngineManager
import com.stardust.autojs.execution.ExecutionConfig.CREATOR.tag
import com.stardust.autojs.execution.ScriptExecution.AbstractScriptExecution
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.autojs.script.ScriptSource
import org.mozilla.javascript.ContinuationPending

/**
 * Created by Stardust on 2017/2/5.
 */
class ScriptExecuteActivity : AppCompatActivity() {
    private var mResult: Any? = null
    private lateinit var mScriptEngine: ScriptEngine<*>
    private var mExecutionListener: ScriptExecutionListener? = null
    private var mScriptSource: ScriptSource? = null
    private lateinit var mScriptExecution: ActivityScriptExecution
    private lateinit var mRuntime: ScriptRuntime
    lateinit var eventEmitter: EventEmitter
        private set

    // FIXME: 2018/3/16 如果Activity被回收则得不到改进
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val executionId = intent.getIntExtra(EXTRA_EXECUTION_ID, ScriptExecution.NO_ID)
        if (executionId == ScriptExecution.NO_ID) {
            super.finish()
            return
        }
        val execution = ScriptEngineService.instance?.getScriptExecution(executionId)
        if (execution == null || execution !is ActivityScriptExecution) {
            Toast.makeText(this, "脚本环境异常", Toast.LENGTH_SHORT).show()
            super.finish()
            return
        }
        mScriptExecution = execution
        mScriptSource = mScriptExecution.source
        mScriptEngine = mScriptExecution.createEngine()
        mExecutionListener = mScriptExecution.listener
        mRuntime = (mScriptEngine as JavaScriptEngine).runtime
        eventEmitter = EventEmitter(mRuntime.bridges)
        runScript()
        emit("create", savedInstanceState)
    }

    private fun runScript() {
        try {
            prepare()
            doExecution()
        } catch (pending: ContinuationPending) {
            pending.printStackTrace()
        } catch (e: Exception) {
            onException(e)
        }
    }

    private fun onException(e: Throwable) {
        mExecutionListener!!.onException(mScriptExecution, e)
        super.finish()
    }

    private fun doExecution() {
        mScriptEngine.setTag(ScriptEngine.TAG_SOURCE, mScriptSource)
        mExecutionListener!!.onStart(mScriptExecution)
        (mScriptEngine as LoopBasedJavaScriptEngine?)!!.execute(
            mScriptSource,
            object : ExecuteCallback {
                override fun onResult(r: Any) {
                    mResult = r
                }

                override fun onException(e: Exception) {
                    this@ScriptExecuteActivity.onException(e)
                }
            }
        )
    }

    private fun prepare() {
        mScriptEngine.put("activity", this)
        mScriptEngine.setTag("activity", this)
        mScriptEngine.setTag(ScriptEngine.TAG_ENV_PATH, mScriptExecution.config.path)
        mScriptEngine.setTag(
            ScriptEngine.TAG_WORKING_DIRECTORY,
            mScriptExecution.config.workingDirectory
        )
        mScriptEngine.init()
    }

    override fun finish() {
        if (mExecutionListener == null) {
            super.finish()
            return
        }
        val exception = mScriptEngine.uncaughtException
        if (exception != null) {
            onException(exception)
        } else {
            mExecutionListener!!.onSuccess(mScriptExecution, mResult)
        }
        super.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(LOG_TAG, "onDestroy")
        if (::mScriptEngine.isInitialized) {
            mScriptEngine.put("activity", null)
            mScriptEngine.setTag("activity", null)
            mScriptEngine.destroy()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(EXTRA_EXECUTION_ID, mScriptExecution.id)
        emit("save_instance_state", outState)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        onBackPressedDispatcher.onBackPressed()
        val event = SimpleEvent()
        emit("back_pressed", event)
        if (!event.consumed) {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        emit("pause")
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        emit("resume")
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        emit("restore_instance_state", savedInstanceState)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val e = SimpleEvent()
        emit("key_down", keyCode, event, e)
        return e.consumed || super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        val e = SimpleEvent()
        emit("generic_motion_event", event, e)
        return super.onGenericMotionEvent(event)
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        emit("activity_result", requestCode, resultCode, data)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        emit("create_options_menu", menu)
        return menu.size() > 0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val e = SimpleEvent()
        emit("options_item_selected", e, item)
        return e.consumed || super.onOptionsItemSelected(item)
    }

    fun emit(event: String?, vararg args: Any?) {
        try {
            eventEmitter.emit(event, *args)
        } catch (e: Exception) {
            mRuntime.exit(e)
        }
    }

    class ActivityScriptExecution internal constructor(
        private val mScriptEngineManager: ScriptEngineManager,
        task: ScriptExecutionTask?
    ) : AbstractScriptExecution(task) {
        private var mScriptEngine: ScriptEngine<*>? = null
        fun createEngine(): ScriptEngine<*> {
            if (mScriptEngine != null) {
                mScriptEngine!!.forceStop()
            }
            mScriptEngine = mScriptEngineManager.createEngineOfSourceOrThrow(source, id)
            mScriptEngine!!.setTag(tag, config)
            return mScriptEngine!!
        }

        override fun getEngine(): ScriptEngine<*>? {
            return mScriptEngine
        }
    }

    companion object {
        private const val LOG_TAG = "ScriptExecuteActivity"
        private val EXTRA_EXECUTION_ID = ScriptExecuteActivity::class.java.name + ".execution_id"

        @JvmStatic
        fun execute(
            context: Context,
            manager: ScriptEngineManager,
            task: ScriptExecutionTask
        ): ActivityScriptExecution {
            val execution = ActivityScriptExecution(manager, task)
            val i = Intent(context, ScriptExecuteActivity::class.java)
                .putExtra(EXTRA_EXECUTION_ID, execution.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(task.config.intentFlags)
            context.startActivity(i)
            return execution
        }
    }
}
