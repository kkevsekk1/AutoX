package com.aiselp.autox.api.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import coil.compose.rememberAsyncImagePainter
import com.aiselp.autox.api.ui.ComposeElement

object Icon : VueNativeComponent {
    override val tag: String = "Icon"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val contentDescription = element.props["contentDescription"] as? String
        val src = element.props["src"]
        val tint = parseColor(element.props["tint"]) ?: LocalContentColor.current
        when (src) {
            is ImageVector -> Icon(
                modifier = modifier,
                imageVector = src,
                contentDescription = contentDescription,
                tint = tint
            )

            is Painter -> Icon(
                modifier = modifier,
                painter = src,
                contentDescription = contentDescription,
                tint = tint
            )

            is String -> run {
                Icon(
                    modifier = modifier,
                    painter = rememberAsyncImagePainter(model = src),
                    contentDescription = contentDescription,
                    tint = tint
                )
            }
        }

    }
}