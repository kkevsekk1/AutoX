package com.aiselp.autox.api.ui.component

import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object Button : VueNativeComponent {
    override val tag: String = "Button"

    @Composable
    override fun Render(modifier: Modifier, element: ComposeElement, content: @Composable () -> Unit) {
        val onClick = element.getEvent("onClick")
        Button(
            onClick = { onClick?.invoke() },
            modifier = modifier,
            enabled = element.props["enabled"] as? Boolean ?: true
        ) {
            content()
        }
    }
}