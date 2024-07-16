package com.aiselp.autox.api.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aiselp.autox.api.ui.ComposeElement

object Dialog : VueNativeComponent {

    override val tag: String = "Dialog"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val properties = DialogProperties(
            dismissOnBackPress = element.props["dismissOnBackPress"] as? Boolean ?: true,
            dismissOnClickOutside = element.props["dismissOnClickOutside"] as? Boolean ?: true,
            usePlatformDefaultWidth = element.props["usePlatformDefaultWidth"] as? Boolean ?: true,
            decorFitsSystemWindows = element.props["decorFitsSystemWindows"] as? Boolean ?: true
        )
        Dialog(
            onDismissRequest = { element.getEvent("onDismissRequest")?.invoke() },
            properties = properties
        ) {
            content()
        }
    }

}