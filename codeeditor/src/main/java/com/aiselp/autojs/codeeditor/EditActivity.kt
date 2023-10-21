package com.aiselp.autojs.codeeditor

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.aiselp.autojs.codeeditor.web.EditorAppManager

@RequiresApi(Build.VERSION_CODES.M)
class EditActivity: AppCompatActivity() {
    private lateinit var editorAppManager: EditorAppManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editorAppManager = EditorAppManager(this)
        setContentView(editorAppManager.webView)
    }

    override fun onDestroy() {
        super.onDestroy()
        editorAppManager.destroy()
    }
}