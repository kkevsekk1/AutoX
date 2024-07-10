package com.aiselp.autox.api.ui.component

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object Fab : VueNativeComponent {
    override val tag: String = "Fab"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val onClick = element.getEvent("onClick")
        val size = element.props["size"] as? String
        when (size) {
            "small" -> SmallFloatingActionButton(
                modifier = modifier,
                onClick = { onClick?.invoke() },
                content = content
            )

            "large" -> LargeFloatingActionButton(
                modifier = modifier,
                onClick = { onClick?.invoke() },
                content = content
            )

            else -> FloatingActionButton(
                modifier = modifier,
                onClick = { onClick?.invoke() },
                content = content
            )
        }
    }

}