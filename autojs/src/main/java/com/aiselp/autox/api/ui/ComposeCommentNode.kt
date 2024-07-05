package com.aiselp.autox.api.ui

import androidx.compose.ui.Modifier

class ComposeCommentNode : ComposeNode {
    override var id: Int = 0
    override var modifier: Modifier = Modifier
    override var parentNode: ComposeElement? = null
    override val props = mutableMapOf<String, Any?>()
}