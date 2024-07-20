package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render

object FloatingActionButton : VueNativeComponent {
    override val tag: String = "FloatingActionButton"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val onClick = element.getEvent("onClick")
        val size = element.props["size"] as? String
        val icon = element.findTemplate("icon")?: element.props["icon"]

        when (size) {
            "large" -> LargeFloatingActionButton(
                modifier = modifier,
                onClick = { onClick?.invoke() }
            ) {
                if (icon != null) {
                    ComposeElement(Icon.tag).apply {
                        props["src"] = icon
                        this.modifier =
                            this.modifier.size(FloatingActionButtonDefaults.LargeIconSize)
                    }.Render()
                } else content()
            }

            "small" -> SmallFloatingActionButton(
                modifier = modifier,
                onClick = { onClick?.invoke() }
            ) {
                if (icon != null) {
                    ComposeElement(Icon.tag).apply {
                        props["src"] = icon
                    }.Render()
                } else content()
            }

            else -> FloatingActionButton(
                modifier = modifier,
                onClick = { onClick?.invoke() }
            ) {
                if (icon != null) {
                    ComposeElement(Icon.tag).apply {
                        props["src"] = icon
                    }.Render()
                } else content()
            }
        }
    }

}