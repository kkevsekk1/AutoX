package com.aiselp.autox.api.ui.component

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render

object DropdownMenuItem : VueNativeComponent {
    override val tag: String = "DropdownMenuItem"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val title = element.props["title"] as? String
        val text = element.findTemplate("text")
        val onClick = element.getEvent("onClick")
        val enabled = element.props["enabled"] as? Boolean
        val leadingIcon = element.findTemplate("leadingIcon")
        val trailingIcon = element.findTemplate("trailingIcon")
        DropdownMenuItem(
            modifier = modifier,
            text = {
                if (text != null) {
                    text.Render()
                } else if (title!=null) {
                    androidx.compose.material3.Text(text = title)
                }else content()
            },
            onClick = { onClick?.invoke() },
            enabled = enabled ?: true,
            leadingIcon = leadingIcon?.let { { it.Render() } },
            trailingIcon = trailingIcon?.let { { it.Render() } }
        )
    }
}