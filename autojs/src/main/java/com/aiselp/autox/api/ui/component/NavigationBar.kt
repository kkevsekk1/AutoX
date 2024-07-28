package com.aiselp.autox.api.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render

object NavigationBar : VueNativeComponent {
    override val tag: String = "NavigationBar"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val tonalElevation = parseFloat(element.props["tonalElevation"])
        val containerColor =
            parseColor(element.props["containerColor"]) ?: NavigationBarDefaults.containerColor
        val contentColor =
            parseColor(element.props["contentColor"]) ?: MaterialTheme.colorScheme.contentColorFor(
                containerColor
            )
        NavigationBar(
            modifier = modifier,
            contentColor = contentColor,
            containerColor = containerColor,
            tonalElevation = tonalElevation?.dp ?: NavigationBarDefaults.Elevation
        ) {
            element.children.forEach {
                it.update
                val selected = it.props["selected"] as? Boolean
                val label = it.findTemplate("label")
                val icon2 = it.props["icon"]
                val icon = it.findTemplate("icon")
                val onClick = it.getEvent("onClick")
                NavigationBarItem(
                    enabled = it.props["enabled"] as? Boolean ?: true,
                    modifier = it.modifier,
                    selected = selected ?: false,
                    onClick = { onClick?.invoke() },
                    icon = {
                        if (icon != null) {
                            icon.Render()
                        } else if (icon2 != null) {
                            val composeElement = ComposeElement("Icon")
                            composeElement.props["src"] = icon2
                            composeElement.Render()
                        }
                    },
                    label = {
                        if (label != null) {
                            label.Render()
                        } else it.children.forEach { r ->
                            r.Render()
                        }
                    },
                )
            }
        }
    }
}