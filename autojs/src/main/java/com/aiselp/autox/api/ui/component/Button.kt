package com.aiselp.autox.api.ui.component

import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.RenderRow

object Button : VueNativeComponent {
    override val tag: String = "Button"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val onClick = element.getEvent("onClick")
        val enabled = element.props["enabled"] as? Boolean
        val type = element.props["type"] as? String
        when (type) {
            "text" -> TextButton(
                onClick = { onClick?.invoke() },
                modifier = modifier,
                enabled = element.props["enabled"] as? Boolean ?: true
            ) {
                element.children.forEach {
                    RenderRow(element = it)
                }
            }

            "elevated" -> ElevatedButton(
                onClick = { onClick?.invoke() },
                modifier = modifier,
                enabled = enabled ?: true
            ) {
                element.children.forEach {
                    RenderRow(element = it)
                }
            }

            "outlined" -> OutlinedButton(
                onClick = { onClick?.invoke() },
                modifier = modifier,
                enabled = enabled ?: true
            ) {
                element.children.forEach {
                    RenderRow(element = it)
                }
            }

            "tonal" -> FilledTonalButton(
                onClick = { onClick?.invoke() },
                modifier = modifier,
                enabled = enabled ?: true
            ) {
                element.children.forEach {
                    RenderRow(element = it)
                }
            }

            else -> Button(
                onClick = { onClick?.invoke() },
                modifier = modifier,
                enabled = enabled ?: true
            ) {
                element.children.forEach {
                    RenderRow(element = it)
                }
            }
        }

    }
}