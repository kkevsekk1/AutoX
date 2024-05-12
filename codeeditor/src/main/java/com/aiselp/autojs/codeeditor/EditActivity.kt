package com.aiselp.autojs.codeeditor

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.aiselp.autojs.codeeditor.web.EditorAppManager
import java.io.File

@RequiresApi(Build.VERSION_CODES.M)
class EditActivity : AppCompatActivity() {
    private lateinit var editorAppManager: EditorAppManager
    private lateinit var contextFrameLayout: FrameLayout
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorAppManager = EditorAppManager(this)
        contextFrameLayout = FrameLayout(this)
        contextFrameLayout.addView(editorAppManager.webView)
        setContentView(contextFrameLayout)
        setKeyboardEvent()
        editorAppManager.openedFile = intent.getStringExtra(EXTRA_PATH)
    }

    override fun onDestroy() {
        Log.i(TAG, "EditActivity onDestroy")
        super.onDestroy()
        editorAppManager.destroy()
    }

    private fun setKeyboardEvent() {
        val rootView = contextFrameLayout.rootView
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val currentHeight = rootView.height
            val resultBottom = r.bottom
            if (currentHeight - resultBottom > 200) {
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

    @Deprecated("Deprecated in Java", ReplaceWith("moveTaskToBack(false)"))
    override fun onBackPressed() {
//        editorAppManager.onBackButton()
        moveTaskToBack(false)
        super.onBackPressed()
    }

    companion object {
        private const val EXTRA_PATH = "path";
        const val TAG = "EditActivity"
        fun editFile(context: Context, path: File) {
            val intent = Intent(context, EditActivity::class.java)
                .putExtra(EXTRA_PATH, path.path)
            context.startActivity(intent)
        }
    }
}