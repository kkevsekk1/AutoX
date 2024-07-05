package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object Row : VueNativeComponent {
    override val tag: String = "row"

    @Composable
    override fun Render(modifier: Modifier, element: ComposeElement, content: @Composable () -> Unit) {
        Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
            content()
        }
    }
}