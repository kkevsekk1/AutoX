package com.aiselp.autox.api.ui.component

import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.ComposeTextNode
import com.aiselp.autox.api.ui.Render
import com.aiselp.autox.api.ui.RenderColumn

object Tab : VueNativeComponent {
    override val tag: String = "Tab"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val type = element.props["type"] as? String
        val enabled = element.props["enabled"] as? Boolean ?: true
        val selected = element.props["selected"] as? Boolean ?: false
        val onClick = element.getEvent("onClick")
        val selectedContentColor = parseColor(element.props["selectedContentColor"])
            ?: LocalContentColor.current
        val unselectedContentColor =
            parseColor(element.props["unselectedContentColor"]) ?: selectedContentColor
        val text = element.findTemplate("text") ?: kotlin.run {
            if (element.props["text"] != null) {
                ComposeTextNode(element.props["text"] as? String ?: "")
            } else null
        }
        val icon = element.findTemplate("icon") ?: kotlin.run {
            if (element.props["icon"] != null) {
                ComposeElement(Icon.tag).apply {
                    props["src"] = element.props["icon"]
                }
            } else null
        }
        when (type) {
            "leadingIcon" -> LeadingIconTab(
                selected = selected,
                onClick = { onClick?.invoke() },
                enabled = enabled,
                text = { text?.Render() },
                icon = { icon?.Render() },
                modifier = modifier,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
            )

            else -> run {
                if (text != null && icon != null) {
                    Tab(
                        selected = selected,
                        onClick = { onClick?.invoke() },
                        enabled = enabled,
                        text = { text.Render() },
                        icon = { icon.Render() },
                        modifier = modifier,
                        selectedContentColor = selectedContentColor,
                        unselectedContentColor = unselectedContentColor,
                    )
                } else Tab(
                    selected = selected,
                    onClick = { onClick?.invoke() },
                    enabled = enabled,
                    modifier = modifier,
                    selectedContentColor = selectedContentColor,
                    unselectedContentColor = unselectedContentColor,
                ) {
                    element.children.forEach {
                        RenderColumn(it)
                    }
                }
            }
        }
    }

}