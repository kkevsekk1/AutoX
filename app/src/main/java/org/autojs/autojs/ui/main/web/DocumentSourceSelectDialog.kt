package org.autojs.autojs.ui.main.web

import android.webkit.WebView
import com.afollestad.materialdialogs.MaterialDialog

class DocumentSourceSelectDialog(private val webView: WebView) {
    private val documentSources = DocumentSource.values()
    private var select: DocumentSource? = null
    private val dialogBuilder = MaterialDialog.Builder(webView.context)
        .title("选择文档源")
        .items(documentSources.map { it.sourceName })
        .itemsCallback { _, _, position, _ ->
            select = documentSources[position]
        }
        .dismissListener { _ -> switchDocument() }

    init {
        val name = EditorAppManager.getSaveStatus(webView.context)
            .getString(EditorAppManager.DocumentSourceKEY, DocumentSource.DOC_V1_LOCAL.name)!!
        val documentSource = DocumentSource.valueOf(name)
        val i = documentSources.lastIndexOf(documentSource)
        dialogBuilder.itemsCallbackSingleChoice(if (i == -1) 0 else i) { _, _, position, _ ->
            select = documentSources[position]
            true
        }
    }

    private fun switchDocument() {
        val documentSource = select ?: return
        EditorAppManager.switchDocument(webView, documentSource)
    }

    fun show(): MaterialDialog = dialogBuilder.show()
}