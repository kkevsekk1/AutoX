package com.aiselp.autox.api.ui.component

import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.RenderColumn

object DropdownMenu : VueNativeComponent {
    override val tag: String = "DropdownMenu"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val expanded = element.props["expanded"] as? Boolean
        val onDismissRequest = element.getEvent("onDismissRequest")
        DropdownMenu(
            modifier = modifier,
            expanded = expanded ?: false,
            onDismissRequest = { onDismissRequest?.invoke() }
        ) {
            element.children.forEach {
                RenderColumn(element = it)
            }
        }
    }

}