package com.aiselp.autojs.component.monaco

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiselp.autojs.component.monaco.web.EditorChromeClient
import com.aiselp.autojs.component.monaco.web.EditorWebViewClient
import com.aiselp.autojs.component.monaco.web.JsBridge
import com.google.accompanist.web.WebContent
import com.google.accompanist.web.WebView
import com.google.accompanist.web.WebViewState


@RequiresApi(Build.VERSION_CODES.M)
class EditorActivity : ComponentActivity() {
    companion object {
        @JvmStatic
        fun start(context: Context) {
            context.startActivity(Intent(context, EditorActivity::class.java))
        }
    }

    var editorView: WebView? = null
    var jsBridge: JsBridge? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true)
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        setContent {
            Main()
        }
    }
    @Composable
    fun Main(){
        MaterialTheme() {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.padding(0.dp)) {
                    EditorWebView()
                }
            }
        }
    }
    @Composable
    fun EditorWebView() {
        val webViewState =
            WebViewState(WebContent.Url("file:///android_asset/monaco-editor/dist/index.html"))
        val chromeClient = EditorChromeClient()
        val webViewClient = EditorWebViewClient()
        WebView(
            state = webViewState,
            chromeClient = chromeClient,
            client = webViewClient,
            onCreated = {
                editorView = it;
                jsBridge = JsBridge(it);
                val settings = it.settings
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.useWideViewPort = true
                settings.allowFileAccessFromFileURLs = true
                settings.allowUniversalAccessFromFileURLs = true
                settings.loadWithOverviewMode = true
                it.requestFocus()
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        editorView?.destroy()
    }
}