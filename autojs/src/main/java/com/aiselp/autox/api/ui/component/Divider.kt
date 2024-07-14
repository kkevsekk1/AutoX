package com.aiselp.autox.api.ui.component

import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiselp.autox.api.ui.ComposeElement

object Divider : VueNativeComponent {
    override val tag: String = "Divider"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val thickness = parseFloat(element.props["thickness"])
        val color = parseColor(element.props["color"])
        val type = element.props["type"] as? String
        when (type) {
            "horizontal" -> HorizontalDivider(
                modifier = modifier,
                color = color ?: DividerDefaults.color,
                thickness = thickness?.dp ?: DividerDefaults.Thickness
            )

            "vertical" -> VerticalDivider(
                modifier = modifier,
                color = color ?: DividerDefaults.color,
                thickness = thickness?.dp ?: DividerDefaults.Thickness
            )
        }
    }

}