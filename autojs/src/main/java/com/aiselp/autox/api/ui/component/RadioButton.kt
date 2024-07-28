package com.aiselp.autox.api.ui.component

import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object RadioButton : VueNativeComponent {
    override val tag: String = "RadioButton"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val selected = element.props["selected"] as? Boolean
        val enabled = element.props["enabled"] as? Boolean
        RadioButton(
            selected = selected ?: false,
            onClick = { element.getEvent("onClick")?.invoke() },
            modifier = modifier,
            enabled = enabled ?: true
        )
    }

}