package com.aiselp.autox.api.ui.component

import android.text.format.DateFormat.is24HourFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aiselp.autox.api.ui.ComposeElement

object TimePicker : VueNativeComponent {

    override val tag: String = "TimePicker"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val initialTime = (element.props["initialTime"] as? String)?.split(":")
        val is24Hour = element.props["is24Hour"] as? Boolean
        val state = rememberTimePickerState(
            initialHour = initialTime?.getOrNull(0)?.toInt() ?: 0,
            initialMinute = initialTime?.getOrNull(1)?.toInt() ?: 0,
            is24Hour = is24Hour ?: is24HourFormat(LocalContext.current)
        )
        LaunchedEffect(Unit) {
            element.getEvent("onRender")?.invoke(state)
        }
        TimePicker(
            state = state,
            modifier = modifier
        )
    }
}