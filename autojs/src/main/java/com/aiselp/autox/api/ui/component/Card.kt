package com.aiselp.autox.api.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.engine.EventLoopQueue

object Card : VueNativeComponent {
    override val tag: String = "card"

    @Composable
    override fun Render(modifier: Modifier, element: ComposeElement, content: @Composable () -> Unit) {
        val onClick = element.props["onClick"] as? EventLoopQueue.V8Callback
        androidx.compose.material3.Card(modifier = modifier, onClick = { onClick?.invoke() }) {
            content()
        }
    }
}