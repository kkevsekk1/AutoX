package com.aiselp.autojs.component.monaco.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightThemeColors = lightColors(
    primary = Color(0xFF009688),
    error = Color(0xFFCF6679),
)
@Composable
fun LightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = LightThemeColors,
        content = content
    )
}