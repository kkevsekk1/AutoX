package com.aiselp.autox.api.ui.component

import androidx.compose.material3.AssistChip
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.ComposeTextNode
import com.aiselp.autox.api.ui.Render

object Chip : VueNativeComponent {
    override val tag: String = "Chip"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val type = element.props["type"] as? String
        val style = element.props["style"] as? String
        val enabled = (element.props["enabled"] as? Boolean) ?: true
        val selected = (element.props["selected"] as? Boolean) ?: false
        val onClick = element.getEvent("onClick")
        val label = element.findTemplate("label") ?: run {
            ComposeTextNode((element.props["label"] as? String) ?: "")
        }
        val leadingIcon = element.findTemplate("leadingIcon") ?: run {
            if (element.props["leadingIcon"] != null) {
                ComposeElement(Icon.tag).apply {
                    props["src"] = element.props["leadingIcon"]
                }
            } else null
        }
        val trailingIcon = element.findTemplate("trailingIcon") ?: run {
            if (element.props["trailingIcon"] != null) {
                ComposeElement(Icon.tag).apply {
                    props["src"] = element.props["trailingIcon"]
                }
            } else null
        }
        val icon = element.findTemplate("'icon'") ?: run {
            if (element.props["icon"] != null) {
                ComposeElement(Icon.tag).apply {
                    props["src"] = element.props["icon"]
                }
            } else null
        }
        if (style == "elevated") {
            when (type) {
                "assist" -> ElevatedAssistChip(
                    onClick = { onClick?.invoke() },
                    label = { label.Render() },
                    modifier = modifier,
                    leadingIcon = leadingIcon?.let { { it.Render() } },
                    trailingIcon = trailingIcon?.let { { it.Render() } },
                    enabled = enabled,
                )

                "filter" -> ElevatedFilterChip(
                    selected = selected,
                    onClick = { onClick?.invoke() },
                    label = { label.Render() },
                    modifier = modifier,
                    leadingIcon = leadingIcon?.let { { it.Render() } },
                    trailingIcon = trailingIcon?.let { { it.Render() } },
                    enabled = enabled,
                )

                "suggestion" -> ElevatedSuggestionChip(
                    onClick = { onClick?.invoke() },
                    label = { label.Render() },
                    modifier = modifier,
                    icon = icon?.let { { it.Render() } },
                    enabled = enabled,
                )
            }
            return
        }
        when (type) {
            "assist" -> AssistChip(
                onClick = { onClick?.invoke() },
                label = { label.Render() },
                modifier = modifier,
                leadingIcon = leadingIcon?.let { { it.Render() } },
                trailingIcon = trailingIcon?.let { { it.Render() } },
                enabled = enabled,
            )

            "filter" -> FilterChip(
                selected = selected,
                onClick = { onClick?.invoke() },
                label = { label.Render() },
                modifier = modifier,
                leadingIcon = leadingIcon?.let { { it.Render() } },
                trailingIcon = trailingIcon?.let { { it.Render() } },
                enabled = enabled,
            )

            "input" -> kotlin.run {
                val avatar = element.findTemplate("avatar") ?: run {
                    if (element.props["avatar"] != null) {
                        ComposeElement(Icon.tag).apply {
                            props["src"] = element.props["avatar"]
                        }
                    } else null
                }
                InputChip(
                    selected = selected,
                    onClick = { onClick?.invoke() },
                    label = { label.Render() },
                    modifier = modifier,
                    leadingIcon = leadingIcon?.let { { it.Render() } },
                    avatar = avatar?.let { { it.Render() } },
                    trailingIcon = trailingIcon?.let { { it.Render() } },
                    enabled = enabled,
                )
            }

            "suggestion" -> SuggestionChip(
                onClick = { onClick?.invoke() },
                label = { label.Render() },
                modifier = modifier,
                enabled = enabled,
                icon = icon?.let { { it.Render() } }
            )
        }
    }

}