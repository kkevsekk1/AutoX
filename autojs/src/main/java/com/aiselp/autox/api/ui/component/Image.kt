package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
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
        val alpha = parseFloat(element.props["alpha"]) ?: DefaultAlpha
        val alignment = parseAlignment(element.props["alignment"] as? String) ?: Alignment.Center
        val contentDescription = element.props["contentDescription"] as? String
        val model = if (src is String) {
            parseDrawable(src)?.let {
                painterResource(it)
            } ?: src
        } else src
        when (model) {
            is ImageVector -> Image(
                modifier = modifier,
                imageVector = model,
                alpha = alpha,
                alignment = alignment,
                contentDescription = contentDescription,
            )

            is Painter -> Image(
                modifier = modifier,
                painter = model,
                alpha = alpha,
                alignment = alignment,
                contentDescription = contentDescription,
            )

            else -> AsyncImage(
                model = model,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = parseContentScale(element.props["contentScale"] as? String),
                alpha = alpha,
                alignment = alignment,
            )
        }
    }

}