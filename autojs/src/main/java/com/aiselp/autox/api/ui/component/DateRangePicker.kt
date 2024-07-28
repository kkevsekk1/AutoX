package com.aiselp.autox.api.ui.component

import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.api.ui.Render

object DateRangePicker : VueNativeComponent {
    override val tag: String = "DateRangePicker"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val yearRange = element.props["yearRange"]?.let {
            if (it is List<*>) {
                val min = it.getOrNull(0) as? Int
                val max = it.getOrNull(1) as? Int
                if (min != null && max != null) {
                    min..max
                } else null
            } else null
        }
        val state = rememberDateRangePickerState(
            yearRange = yearRange ?: DatePickerDefaults.YearRange
        )
        state.selectedEndDateMillis
        LaunchedEffect(Unit) {
            element.getEvent("onRender")?.invoke(state)
        }
        val showModeToggle = element.props["showModeToggle"] as? Boolean ?: true

        val title = element.findTemplate("title") ?: run {
            element.props["title"]?.let {
                val el = ComposeElement("text")
                el.props["text"] = element.props["title"]
                el
            }
        }
        if (title != null) {
            DateRangePicker(
                state = state,
                modifier = modifier,
                showModeToggle = showModeToggle,
                title = { title.Render() }
            )
        } else {
            DateRangePicker(
                state = state,
                modifier = modifier,
                showModeToggle = showModeToggle,
            )
        }
    }

}