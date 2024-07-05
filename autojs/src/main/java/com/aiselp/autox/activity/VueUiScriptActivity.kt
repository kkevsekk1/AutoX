package com.aiselp.autox.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.aiselp.autox.api.ui.ActivityEvent
import com.aiselp.autox.api.ui.ActivityEventDelegate
import com.aiselp.autox.api.ui.ScriptActivityBuilder
import com.aiselp.autox.api.ui.render

class VueUiScriptActivity : AppCompatActivity() {
    private var activityEventDelegate: ActivityEventDelegate? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getIntExtra(TAG, 0)
        val builder = builderList[id] ?: return
        activityEventDelegate = builder.activityEventDelegate
        setContent {
            BackHandler {
                activityEventDelegate?.emit(ActivityEvent.ON_BACK_PRESSED)
                finish()
            }
            render(builder.element)
        }
        activityEventDelegate?.emit(ActivityEvent.ON_CREATE)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        activityEventDelegate?.emit(ActivityEvent.ON_NEW_INTENT, intent)
    }
    override fun onPause() {
        super.onPause()
        activityEventDelegate?.emit(ActivityEvent.ON_PAUSE)
    }

    override fun onStop() {
        super.onStop()
        activityEventDelegate?.emit(ActivityEvent.ON_STOP)
    }
    override fun onStart() {
        super.onStart()
        activityEventDelegate?.emit(ActivityEvent.ON_START)
    }

    override fun onResume() {
        super.onResume()
        activityEventDelegate?.emit(ActivityEvent.ON_RESUME)
    }

    override fun onRestart() {
        super.onRestart()
    }

    override fun onDestroy() {
        super.onDestroy()
        activityEventDelegate?.emit(ActivityEvent.ON_DESTROY)
    }

    companion object {
        private const val TAG = "VueUiScriptActivity"
        private val builderList = mutableMapOf<Int, ScriptActivityBuilder>()
        private var id = 0
        fun startActivity(context: Context, element: ScriptActivityBuilder) {
            builderList[id] = element
            val intent = Intent(context, VueUiScriptActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .putExtra(TAG, id++)
            context.startActivity(intent)
        }
    }
}