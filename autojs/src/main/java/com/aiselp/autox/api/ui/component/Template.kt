package com.aiselp.autox.api.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object Template : VueNativeComponent {
    override val tag: String = "template"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        content()
    }

}