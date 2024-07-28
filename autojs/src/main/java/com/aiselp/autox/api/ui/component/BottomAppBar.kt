package com.aiselp.autox.api.ui.component

import androidx.compose.material3.BottomAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render
import com.aiselp.autox.api.ui.RenderRow

object BottomAppBar : VueNativeComponent {
    override val tag: String = "BottomAppBar"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val actions = element.findTemplate("actions")
        val floatingActionButton = element.findTemplate("floatingActionButton")
        BottomAppBar(
            actions = {
                if (actions != null) {
                    RenderRow(element = actions)
                }
            },
            modifier = modifier,
            floatingActionButton = { floatingActionButton?.Render() }
        )
    }
}