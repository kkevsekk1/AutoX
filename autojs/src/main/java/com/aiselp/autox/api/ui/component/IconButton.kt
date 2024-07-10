package com.aiselp.autox.api.ui.component

import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object IconButton : VueNativeComponent {
    override val tag: String = "IconButton"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val onClick = element.getEvent("onClick")
        val enabled = element.props["enabled"] as? Boolean
        IconButton(
            onClick = { onClick?.invoke() },
            modifier = modifier,
            enabled = enabled ?: true,
        ) {
            content()
        }
    }
}