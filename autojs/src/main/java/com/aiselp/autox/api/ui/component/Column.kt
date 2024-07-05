package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object Column : VueNativeComponent {
    override val tag: String = "column"

    @Composable
    override fun Render(modifier: Modifier, element: ComposeElement, content: @Composable () -> Unit) {
        Column(modifier = modifier) {
            content()
        }
    }
}