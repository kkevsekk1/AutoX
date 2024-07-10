package com.aiselp.autox.api.ui.component

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object Checkbox : VueNativeComponent {
    override val tag: String = "Checkbox"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val checked = element.props["checked"] as? Boolean
        val onCheckedChange = element.getEvent("onCheckedChange")
        val enabled = element.props["enabled"] as? Boolean
        Checkbox(
            checked = checked ?: false,
            onCheckedChange = onCheckedChange?.let { { onCheckedChange.invoke(it) } },
            modifier = modifier,
            enabled = enabled ?: true
        )
    }
}