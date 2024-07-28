package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.RenderColumn
import com.aiselp.autox.engine.EventLoopQueue

object Card : VueNativeComponent {
    override val tag: String = "card"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val onClick = element.props["onClick"] as? EventLoopQueue.V8Callback
        val enabled = element.props["enabled"] as? Boolean
        val borderWidth = parseFloat(element.props["borderWidth"])?.dp
        val borderColor = parseColor(element.props["borderColor"])
        val border = if (borderWidth != null && borderColor != null) {
            BorderStroke(borderWidth, borderColor)
        } else null
        val type = element.props["type"] as? String
        val elevation = when (type) {
            "elevated" -> CardDefaults.elevatedCardElevation()
            "outlined" -> CardDefaults.outlinedCardElevation()
            else -> CardDefaults.cardElevation()
        }
        val shape = when (type) {
            "elevated" -> CardDefaults.elevatedShape
            "outlined" -> CardDefaults.outlinedShape
            else -> CardDefaults.shape
        }
        val colors = when (type) {
            "elevated" -> CardDefaults.elevatedCardColors()
            "outlined" -> CardDefaults.outlinedCardColors()
            else -> CardDefaults.cardColors()
        }
        if (onClick != null) {
            Card(
                modifier = modifier,
                elevation = elevation,
                shape = shape,
                colors = colors,
                border = border,
                enabled = enabled ?: true,
                onClick = { onClick.invoke() }
            ) {
                element.children.forEach {
                    RenderColumn(element = it)
                }
            }

        } else {
            Card(
                modifier = modifier,
                elevation = elevation,
                colors = colors,
                border = border,
                shape = shape,
            ) {
                element.children.forEach {
                    RenderColumn(element = it)
                }
            }
        }
    }
}