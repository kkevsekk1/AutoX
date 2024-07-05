package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object Box: VueNativeComponent {
    override val tag: String = "box"

    @Composable
    override fun Render(modifier: Modifier, element: ComposeElement, content: @Composable () -> Unit) {
        Box(modifier = modifier) {
            content()
        }
    }
}