package com.aiselp.autox.api.ui

import androidx.compose.material3.ColorScheme
import com.aiselp.autox.api.ui.component.parseColor
import com.aiselp.autox.ui.material3.theme.backgroundDark
import com.aiselp.autox.ui.material3.theme.backgroundLight
import com.aiselp.autox.ui.material3.theme.errorContainerDark
import com.aiselp.autox.ui.material3.theme.errorContainerLight
import com.aiselp.autox.ui.material3.theme.errorDark
import com.aiselp.autox.ui.material3.theme.errorLight
import com.aiselp.autox.ui.material3.theme.inverseOnSurfaceDark
import com.aiselp.autox.ui.material3.theme.inverseOnSurfaceLight
import com.aiselp.autox.ui.material3.theme.inversePrimaryDark
import com.aiselp.autox.ui.material3.theme.inversePrimaryLight
import com.aiselp.autox.ui.material3.theme.inverseSurfaceDark
import com.aiselp.autox.ui.material3.theme.inverseSurfaceLight
import com.aiselp.autox.ui.material3.theme.onBackgroundDark
import com.aiselp.autox.ui.material3.theme.onBackgroundLight
import com.aiselp.autox.ui.material3.theme.onErrorContainerDark
import com.aiselp.autox.ui.material3.theme.onErrorContainerLight
import com.aiselp.autox.ui.material3.theme.onErrorDark
import com.aiselp.autox.ui.material3.theme.onErrorLight
import com.aiselp.autox.ui.material3.theme.onPrimaryContainerDark
import com.aiselp.autox.ui.material3.theme.onPrimaryContainerLight
import com.aiselp.autox.ui.material3.theme.onPrimaryDark
import com.aiselp.autox.ui.material3.theme.onPrimaryLight
import com.aiselp.autox.ui.material3.theme.onSecondaryContainerDark
import com.aiselp.autox.ui.material3.theme.onSecondaryContainerLight
import com.aiselp.autox.ui.material3.theme.onSecondaryDark
import com.aiselp.autox.ui.material3.theme.onSecondaryLight
import com.aiselp.autox.ui.material3.theme.onSurfaceDark
import com.aiselp.autox.ui.material3.theme.onSurfaceLight
import com.aiselp.autox.ui.material3.theme.onSurfaceVariantDark
import com.aiselp.autox.ui.material3.theme.onSurfaceVariantLight
import com.aiselp.autox.ui.material3.theme.onTertiaryContainerDark
import com.aiselp.autox.ui.material3.theme.onTertiaryContainerLight
import com.aiselp.autox.ui.material3.theme.onTertiaryDark
import com.aiselp.autox.ui.material3.theme.onTertiaryLight
import com.aiselp.autox.ui.material3.theme.outlineDark
import com.aiselp.autox.ui.material3.theme.outlineLight
import com.aiselp.autox.ui.material3.theme.outlineVariantDark
import com.aiselp.autox.ui.material3.theme.outlineVariantLight
import com.aiselp.autox.ui.material3.theme.primaryContainerDark
import com.aiselp.autox.ui.material3.theme.primaryContainerLight
import com.aiselp.autox.ui.material3.theme.primaryDark
import com.aiselp.autox.ui.material3.theme.primaryLight
import com.aiselp.autox.ui.material3.theme.scrimDark
import com.aiselp.autox.ui.material3.theme.scrimLight
import com.aiselp.autox.ui.material3.theme.secondaryContainerDark
import com.aiselp.autox.ui.material3.theme.secondaryContainerLight
import com.aiselp.autox.ui.material3.theme.secondaryDark
import com.aiselp.autox.ui.material3.theme.secondaryLight
import com.aiselp.autox.ui.material3.theme.surfaceBrightDark
import com.aiselp.autox.ui.material3.theme.surfaceBrightLight
import com.aiselp.autox.ui.material3.theme.surfaceContainerDark
import com.aiselp.autox.ui.material3.theme.surfaceContainerHighDark
import com.aiselp.autox.ui.material3.theme.surfaceContainerHighLight
import com.aiselp.autox.ui.material3.theme.surfaceContainerHighestDark
import com.aiselp.autox.ui.material3.theme.surfaceContainerHighestLight
import com.aiselp.autox.ui.material3.theme.surfaceContainerLight
import com.aiselp.autox.ui.material3.theme.surfaceContainerLowDark
import com.aiselp.autox.ui.material3.theme.surfaceContainerLowLight
import com.aiselp.autox.ui.material3.theme.surfaceContainerLowestDark
import com.aiselp.autox.ui.material3.theme.surfaceContainerLowestLight
import com.aiselp.autox.ui.material3.theme.surfaceDark
import com.aiselp.autox.ui.material3.theme.surfaceDimDark
import com.aiselp.autox.ui.material3.theme.surfaceDimLight
import com.aiselp.autox.ui.material3.theme.surfaceLight
import com.aiselp.autox.ui.material3.theme.surfaceVariantDark
import com.aiselp.autox.ui.material3.theme.surfaceVariantLight
import com.aiselp.autox.ui.material3.theme.tertiaryContainerDark
import com.aiselp.autox.ui.material3.theme.tertiaryContainerLight
import com.aiselp.autox.ui.material3.theme.tertiaryDark
import com.aiselp.autox.ui.material3.theme.tertiaryLight
import com.caoccao.javet.annotations.V8Function

object JsTheme {
    @V8Function
    fun darkColorScheme(ops: Map<String, Any?>): ColorScheme {
        return androidx.compose.material3.darkColorScheme(
            primary = parseColor(ops["primary"]) ?: primaryDark,
            onPrimary = parseColor(ops["onPrimary"]) ?: onPrimaryDark,
            primaryContainer = parseColor(ops["primaryContainer"]) ?: primaryContainerDark,
            onPrimaryContainer = parseColor(ops["onPrimaryContainer"])
                ?: onPrimaryContainerDark,
            inversePrimary = parseColor(ops[ops["inversePrimary"]]) ?: inversePrimaryDark,
            secondary = parseColor(ops["secondary"]) ?: secondaryDark,
            onSecondary = parseColor(ops["onSecondary"]) ?: onSecondaryDark,
            secondaryContainer = parseColor(ops["secondaryContainer"]) ?: secondaryContainerDark,
            onSecondaryContainer = parseColor(ops["onSecondaryContainer"])
                ?: onSecondaryContainerDark,
            tertiary = parseColor(ops["tertiary"]) ?: tertiaryDark,
            onTertiary = parseColor(ops["onTertiary"]) ?: onTertiaryDark,
            tertiaryContainer = parseColor(ops["tertiaryContainer"]) ?: tertiaryContainerDark,
            onTertiaryContainer = parseColor(ops["onTertiaryContainer"]) ?: onTertiaryContainerDark,
            background = parseColor(ops["background"]) ?: backgroundDark,
            onBackground = parseColor(ops["onBackground"]) ?: onBackgroundDark,
            surface = parseColor(ops["surface"]) ?: surfaceDark,
            onSurface = parseColor(ops["onSurface"]) ?: onSurfaceDark,
            surfaceVariant = parseColor(ops["surfaceVariant"]) ?: surfaceVariantDark,
            onSurfaceVariant = parseColor(ops["onSurfaceVariant"]) ?: onSurfaceVariantDark,
            surfaceTint = parseColor(ops["surfaceTint"]) ?: parseColor(ops["primary"])
            ?: primaryDark,
            inverseSurface = parseColor(ops["inverseSurface"]) ?: inverseSurfaceDark,
            inverseOnSurface = parseColor(ops["inverseOnSurface"]) ?: inverseOnSurfaceDark,
            error = parseColor(ops["error"]) ?: errorDark,
            onError = parseColor(ops["onError"]) ?: onErrorDark,
            errorContainer = parseColor(ops["errorContainer"]) ?: errorContainerDark,
            onErrorContainer = parseColor(ops["onErrorContainer"]) ?: onErrorContainerDark,
            outline = parseColor(ops["outline"]) ?: outlineDark,
            outlineVariant = parseColor(ops["outlineVariant"]) ?: outlineVariantDark,
            scrim = parseColor(ops["scrim"]) ?: scrimDark,
            surfaceBright = parseColor(ops["surfaceBright"]) ?: surfaceBrightDark,
            surfaceContainer = parseColor(ops["surfaceContainer"]) ?: surfaceContainerDark,
            surfaceContainerHigh = parseColor(ops["surfaceContainerHigh"])
                ?: surfaceContainerHighDark,
            surfaceContainerHighest = parseColor(ops["surfaceContainerHighest"])
                ?: surfaceContainerHighestDark,
            surfaceContainerLow = parseColor(ops["surfaceContainerLow"]) ?: surfaceContainerLowDark,
            surfaceContainerLowest = parseColor(ops["surfaceContainerLowest"])
                ?: surfaceContainerLowestDark,
            surfaceDim = parseColor(ops["surfaceDim"]) ?: surfaceDimDark,
        )
    }

    @V8Function
    fun lightColorScheme(ops: Map<String, Any?>): ColorScheme {
        return androidx.compose.material3.lightColorScheme(
            primary = parseColor(ops["primary"]) ?: primaryLight,
            onPrimary = parseColor(ops["onPrimary"]) ?: onPrimaryLight,
            primaryContainer = parseColor(ops["primaryContainer"]) ?: primaryContainerLight,
            onPrimaryContainer = parseColor(ops["onPrimaryContainer"])
                ?: onPrimaryContainerLight,
            inversePrimary = parseColor(ops[ops["inversePrimary"]]) ?: inversePrimaryLight,
            secondary = parseColor(ops["secondary"]) ?: secondaryLight,
            onSecondary = parseColor(ops["onSecondary"]) ?: onSecondaryLight,
            secondaryContainer = parseColor(ops["secondaryContainer"]) ?: secondaryContainerLight,
            onSecondaryContainer = parseColor(ops["onSecondaryContainer"])
                ?: onSecondaryContainerLight,
            tertiary = parseColor(ops["tertiary"]) ?: tertiaryLight,
            onTertiary = parseColor(ops["onTertiary"]) ?: onTertiaryLight,
            tertiaryContainer = parseColor(ops["tertiaryContainer"]) ?: tertiaryContainerLight,
            onTertiaryContainer = parseColor(ops["onTertiaryContainer"])
                ?: onTertiaryContainerLight,
            background = parseColor(ops["background"]) ?: backgroundLight,
            onBackground = parseColor(ops["onBackground"]) ?: onBackgroundLight,
            surface = parseColor(ops["surface"]) ?: surfaceLight,
            onSurface = parseColor(ops["onSurface"]) ?: onSurfaceLight,
            surfaceVariant = parseColor(ops["surfaceVariant"]) ?: surfaceVariantLight,
            onSurfaceVariant = parseColor(ops["onSurfaceVariant"]) ?: onSurfaceVariantLight,
            surfaceTint = parseColor(ops["surfaceTint"]) ?: parseColor(ops["primary"])
            ?: primaryLight,
            inverseSurface = parseColor(ops["inverseSurface"]) ?: inverseSurfaceLight,
            inverseOnSurface = parseColor(ops["inverseOnSurface"]) ?: inverseOnSurfaceLight,
            error = parseColor(ops["error"]) ?: errorLight,
            onError = parseColor(ops["onError"]) ?: onErrorLight,
            errorContainer = parseColor(ops["errorContainer"]) ?: errorContainerLight,
            onErrorContainer = parseColor(ops["onErrorContainer"]) ?: onErrorContainerLight,
            outline = parseColor(ops["outline"]) ?: outlineLight,
            outlineVariant = parseColor(ops["outlineVariant"]) ?: outlineVariantLight,
            scrim = parseColor(ops["scrim"]) ?: scrimLight,
            surfaceBright = parseColor(ops["surfaceBright"]) ?: surfaceBrightLight,
            surfaceContainer = parseColor(ops["surfaceContainer"]) ?: surfaceContainerLight,
            surfaceContainerHigh = parseColor(ops["surfaceContainerHigh"])
                ?: surfaceContainerHighLight,
            surfaceContainerHighest = parseColor(ops["surfaceContainerHighest"])
                ?: surfaceContainerHighestLight,
            surfaceContainerLow = parseColor(ops["surfaceContainerLow"])
                ?: surfaceContainerLowLight,
            surfaceContainerLowest = parseColor(ops["surfaceContainerLowest"])
                ?: surfaceContainerLowestLight,
            surfaceDim = parseColor(ops["surfaceDim"]) ?: surfaceDimLight,
        )
    }
}