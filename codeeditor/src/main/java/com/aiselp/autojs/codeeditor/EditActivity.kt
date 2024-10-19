package com.aiselp.autojs.codeeditor

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import com.aiselp.autojs.codeeditor.web.EditorAppManager
import com.aiselp.autox.ui.material3.theme.AppTheme
import java.io.File


class EditActivity : AppCompatActivity() {
    private lateinit var editorAppManager: EditorAppManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorAppManager = EditorAppManager(this)
        editorAppManager.openedFile = intent.getStringExtra(EXTRA_PATH)

        setContent {
            val rootView = LocalView.current
            LaunchedEffect(Unit) {
                setKeyboardEvent(rootView)
            }
            AppTheme {
                editorAppManager.loadDialog.Dialog()
                Box(modifier = Modifier.fillMaxSize()) {
                    AndroidView(factory = {
                        editorAppManager.webView
                    })
                }
            }
        }

        onBackPressedDispatcher.addCallback {
            moveTaskToBack(false)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "EditActivity onDestroy")
        super.onDestroy()
        editorAppManager.destroy()
    }

    private fun setKeyboardEvent(rootView: View) {
        val rootHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val metrics = windowManager.currentWindowMetrics
            metrics.bounds.bottom
        } else {
            windowManager.defaultDisplay.height
        }
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect().also {
                rootView.getWindowVisibleDisplayFrame(it)
            }
            val resultBottom = rect.bottom
            if (rootHeight - resultBottom > 200) {
                editorAppManager.onKeyboardDidShow()
            } else {
                editorAppManager.onKeyboardDidHide()
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val path = intent.getStringExtra(EXTRA_PATH)
        if (path != null) {
            editorAppManager.openFile(path)
        }
    }

    companion object {
        private const val EXTRA_PATH = "path";
        const val TAG = "EditActivity"
        fun editFile(context: Context, path: File) {
            val intent = Intent(context, EditActivity::class.java)
                .setFlags(FLAG_ACTIVITY_NEW_TASK)
                .putExtra(EXTRA_PATH, path.path)
            context.startActivity(intent)
        }
    }
}