package com.aiselp.autox.api.ui.component

import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render

object TextField : VueNativeComponent {
    override val tag: String = "TextField"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val value = element.props["value"] as? String
        val onValueChange = element.getEvent("onValueChange")
        val enabled = element.props["enabled"] as? Boolean
        val readOnly = element.props["readOnly"] as? Boolean
        val label = element.findTemplate("label")
        val isError = element.props["isError"] as? Boolean
        val singleLine = element.props["singleLine"] as? Boolean
        val maxLines = element.props["maxLines"] as? Int
        val minLines = element.props["minLines"] as? Int
        TextField(
            value = value ?: "",
            onValueChange = { onValueChange?.invoke(it) },
            modifier = modifier,
            enabled = enabled ?: true,
            readOnly = readOnly ?: false,
            label = label?.let { { it.Render() } },
            isError = isError ?: false,
            singleLine = singleLine ?: false,
            maxLines = maxLines ?: if (singleLine == true) 1 else Int.MAX_VALUE,
            minLines = minLines ?: 1,
        )
    }
}