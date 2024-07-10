package com.aiselp.autox.api.ui.component

import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.toColorInt


fun parseContentScale(value: String?): ContentScale {
    return when (value) {
        "FillWidth" -> ContentScale.FillWidth
        "FillHeight" -> ContentScale.FillHeight
        "FillBounds" -> ContentScale.FillBounds
        "Crop" -> ContentScale.Crop
        "Inside" -> ContentScale.Inside
        "Fit" -> ContentScale.Fit
        "None" -> ContentScale.None
        else -> ContentScale.Fit
    }
}

fun parseAlignment(value: String?): Alignment {
    return when (value) {
        "TopStart" -> Alignment.TopStart
        "TopCenter" -> Alignment.TopCenter
        "TopEnd" -> Alignment.TopEnd
        "CenterStart" -> Alignment.CenterStart
        "Center" -> Alignment.Center
        "CenterEnd" -> Alignment.CenterEnd
        "BottomStart" -> Alignment.BottomStart
        "BottomCenter" -> Alignment.BottomCenter
        "BottomEnd" -> Alignment.BottomEnd
        else -> Alignment.Center
    }
}

fun parseFloat(value: Any?): Float? {
    return when (value) {
        null -> null
        is Float -> value
        is Double -> value.toFloat()
        is Long -> value.toFloat()
        is Int -> value.toFloat()
        is String -> value.toFloatOrNull()
        else -> null
    }
}
fun parseColor(value: Any?): Color? {
    return when (value) {
        null -> null
        is Int -> Color(value)
        is Long -> Color(value)
        is String -> Color(value.toColorInt())
        else -> null
    }
}