package com.aiselp.autox.api.ui.component

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object Switch : VueNativeComponent {
    override val tag: String = "Switch"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val checked = element.props["checked"] as? Boolean
        val onCheckedChange = element.getEvent("onCheckedChange")
        val enabled = element.props["enabled"] as? Boolean
        Switch(
            modifier = modifier,
            checked = checked ?: true,
            onCheckedChange = onCheckedChange?.let { { onCheckedChange.invoke(it) } },
            enabled = enabled ?: true
        )
    }

}