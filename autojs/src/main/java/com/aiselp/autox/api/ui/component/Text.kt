package com.aiselp.autox.api.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.ComposeTextNode

object Text : VueNativeComponent {
    override val tag: String = "text"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        var text = element.props["text"] as? String
        val color = parseColor(element.props["color"])
        val maxLines = element.props["maxLines"] as? Int
        val minLines = element.props["minLines"] as? Int
        val fontSize = parseFloat(element.props["fontSize"])
        val fontStyle = when (element.props["fontStyle"] as? String) {
            "italic" -> FontStyle.Italic
            "normal" -> FontStyle.Normal
            else -> null
        }
        val textDecoration = when (element.props["textDecoration"] as? String) {
            "underline" -> TextDecoration.Underline
            "lineThrough" -> TextDecoration.LineThrough
            "none" -> TextDecoration.None
            else -> null
        }
        val fontWeight = parseFloat(element.props["fontWeight"])
        if (element is ComposeTextNode) {
            if (element.text.isNotEmpty()) text = element.text
        }
        Text(
            modifier = modifier,
            fontSize = fontSize?.sp ?: TextUnit.Unspecified,
            fontStyle = fontStyle,
            textDecoration = textDecoration,
            fontWeight = fontWeight?.let { FontWeight(it.toInt()) },
            color = color ?: Color.Unspecified,
            maxLines = maxLines ?: Int.MAX_VALUE,
            minLines = minLines ?: 1,
            text = text ?: ""
        )

    }
}