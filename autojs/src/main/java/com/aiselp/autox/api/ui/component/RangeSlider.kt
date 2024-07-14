package com.aiselp.autox.api.ui.component

import androidx.compose.material3.RangeSlider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement

object RangeSlider : VueNativeComponent {
    override val tag: String = "RangeSlider"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val min = parseFloat(element.props["min"])
        val max = parseFloat(element.props["max"])
        val valueRange = if (max != null && min != null) min..max else 0f..1f
        val minValue = parseFloat(element.props["minValue"]) ?: min ?: 0f
        val maxValue = parseFloat(element.props["maxValue"]) ?: max ?: 1f
        RangeSlider(
            modifier = modifier,
            value = minValue..maxValue,
            valueRange = valueRange,
            onValueChange = {
                element.getEvent("onValueChange")?.invoke(it.start, it.endInclusive)
            },
            onValueChangeFinished = { element.getEvent("onValueChangeFinished")?.invoke() }
        )
    }

}