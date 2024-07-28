package com.aiselp.autox.api.ui.component

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aiselp.autox.api.ui.ComposeElement

object ProgressIndicator : VueNativeComponent {
    override val tag: String = "ProgressIndicator"

    @Composable
    override fun Render(
        modifier: Modifier,
        element: ComposeElement,
        content: @Composable () -> Unit
    ) {
        val type = element.props["type"] as? String
        val color = parseColor(element.props["color"])
        val strokeWidth = parseFloat(element.props["strokeWidth"])?.dp
        val trackColor = parseColor(element.props["trackColor"])
        val progress = parseFloat(element.props["progress"])
        val indeterminate = element.props["indeterminate"] as? Boolean
        if (type == "circular") {
            if (indeterminate == true) {
                CircularProgressIndicator(
                    modifier = modifier,
                    color ?: ProgressIndicatorDefaults.circularColor,
                    trackColor = trackColor ?: ProgressIndicatorDefaults.circularTrackColor,
                    strokeWidth = strokeWidth ?: ProgressIndicatorDefaults.CircularStrokeWidth,
                )
            } else {
                CircularProgressIndicator(
                    progress = { progress ?: 0f },
                    modifier = modifier,
                    color ?: ProgressIndicatorDefaults.circularColor,
                    trackColor = trackColor ?: ProgressIndicatorDefaults.circularTrackColor,
                    strokeWidth = strokeWidth ?: ProgressIndicatorDefaults.CircularStrokeWidth,
                )
            }
        }
        if (type == "linear") {
            if (indeterminate == true) {
                LinearProgressIndicator(
                    modifier = modifier,
                    color = color ?: ProgressIndicatorDefaults.linearColor,
                    trackColor = trackColor ?: ProgressIndicatorDefaults.linearTrackColor,
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress ?: 0f },
                    modifier = modifier,
                    color = color ?: ProgressIndicatorDefaults.linearColor,
                    trackColor = trackColor ?: ProgressIndicatorDefaults.linearTrackColor,
                )
            }
        }
    }
}