package com.aiselp.autox.api.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.DefaultAlpha
import coil.compose.AsyncImage
import com.aiselp.autox.api.ui.ComposeElement

object Image : VueNativeComponent {
    override val tag: String = "Image"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val src = element.props["src"]
        val contentDescription = element.props["contentDescription"] as? String
        val model = when (src) {
            is String -> parseDrawable(src) ?: src
            else -> src
        }

        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = parseContentScale(element.props["contentScale"] as? String),
            alignment = parseAlignment(element.props["alignment"] as? String),
            alpha = parseFloat(element.props["alpha"]) ?: DefaultAlpha,
        )
    }

}