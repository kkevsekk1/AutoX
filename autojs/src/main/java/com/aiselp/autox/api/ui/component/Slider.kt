package com.aiselp.autox.api.ui.component

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object Slider : VueNativeComponent {
    override val tag: String = "Slider"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val enabled = element.props["enabled"] as? Boolean
        val value = parseFloat(element.props["value"])
        val steps = parseFloat(element.props["steps"])?.toInt()
        val onValueChangeFinished = element.getEvent("onValueChangeFinished")
        val onValueChange = element.getEvent("onValueChange")
        val min = parseFloat(element.props["min"])
        val max = parseFloat(element.props["max"])
        val valueRange = if (max != null && min != null) min..max else 0f..1f

        Slider(
            modifier = modifier,
            value = value ?: 0f,
            onValueChange = { onValueChange?.invoke(it) },
            enabled = enabled ?: true,
            onValueChangeFinished = { onValueChangeFinished?.invoke() },
            steps = steps ?: 0,
            valueRange = valueRange,
        )
    }

}