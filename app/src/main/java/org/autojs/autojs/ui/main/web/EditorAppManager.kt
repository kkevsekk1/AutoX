package org.autojs.autojs.ui.main.web

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import org.autojs.autojs.ui.widget.SwipeRefreshWebView
import org.autojs.autojs.ui.widget.fillMaxSize

class EditorAppManager : Fragment() {

    val swipeRefreshWebView by lazy {
        val context = requireContext()
        SwipeRefreshWebView(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return swipeRefreshWebView.apply {
            loadHomeDocument(this.webView)
            fillMaxSize()
        }
    }

    companion object {
        const val TAG = "EditorAppManager"
        const val DocumentSourceKEY = "DocumentSource"

        private var saveStatus: SharedPreferences? = null

        @Synchronized
        fun getSaveStatus(context: Context): SharedPreferences {
            if (saveStatus == null) {
                saveStatus = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
            }
            return saveStatus!!
        }

        fun loadHomeDocument(webView: WebView) {
            val saveStatus = getSaveStatus(webView.context)
            val name = saveStatus.getString(DocumentSourceKEY, DocumentSource.DOC_V1_LOCAL.name)
            switchDocument(
                webView, try {
                    DocumentSource.valueOf(name!!)
                } catch (e: Exception) {
                    DocumentSource.DOC_V1_LOCAL
                }
            )
        }

        fun switchDocument(webView: WebView, documentSource: DocumentSource) {
            if (documentSource.isLocal) {
                webView.webViewClient = WebViewClient(webView.context, documentSource.uri)
                webView.loadUrl("https://appassets.androidplatform.net")
            } else
                webView.loadUrl(documentSource.uri)
            getSaveStatus(webView.context).edit()
                .putString(DocumentSourceKEY, documentSource.name)
                .apply()
        }
    }
}