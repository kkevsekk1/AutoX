package com.aiselp.autox.api.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.ComposeTextNode

object Text : VueNativeComponent {
    override val tag: String = "text"
    @Composable
    override fun Render(modifier: Modifier, element: ComposeElement, content: @Composable () -> Unit) {
        if (element is ComposeTextNode){
            androidx.compose.material3.Text(modifier = modifier, text = element.text)
        }
    }
}