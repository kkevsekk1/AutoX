package org.autojs.autojs.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = Color(0xFF009688),
    primaryVariant = Teal200,
    secondary = Color(0xFF03a9f4),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    error = Color(0xFFCF6679),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

private val LightColorPalette = lightColors(
    primary = Color(0xFF009688),
    primaryVariant = Teal200,
    secondary = Color(0xFF03a9f4),

//     Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun AutoXJsTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    val darkExtendedColors = ExtendedColors(
        onBackgroundVariant = Color.Gray,
        switchUncheckedThumbColor = Color(0xFFA9A9A9) ,
        divider = Color(0xff262626)
    )
    val lightExtendedColors = ExtendedColors(
        onBackgroundVariant = Color(0xFF8D8D8D),
        switchUncheckedThumbColor = MaterialTheme.colors.surface,
        divider = Color(0xFFF2F3F5)
    )
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) darkExtendedColors else lightExtendedColors) {
        MaterialTheme(
            colors = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }

}

@Immutable
data class ExtendedColors(
    val onBackgroundVariant: Color,
    val switchUncheckedThumbColor: Color,
    val divider: Color
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        onBackgroundVariant = Color.Unspecified,
        switchUncheckedThumbColor = Color.Unspecified,
        divider = Color.Unspecified
    )
}

object AutoXJsTheme {
    val colors: ExtendedColors
        @Composable
        get() = LocalExtendedColors.current
}