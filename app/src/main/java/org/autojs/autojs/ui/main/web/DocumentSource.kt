package org.autojs.autojs.ui.main.web

enum class DocumentSource(
    val sourceName: String,
    val uri: String,
    val isLocal: Boolean = false,
    val openUri: String? = null
) {
    DOC_V1("在线文档v1", "http://doc.autoxjs.com/"),
    DOC_V1_LOCAL("本地文档v1", "docs/v1", true, DOC_V1.uri),
    DOC_V2_LOCAL("本地文档v2", "docs/v2", true)
}