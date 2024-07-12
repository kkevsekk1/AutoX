package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.toColorInt
import com.stardust.autojs.R


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

fun parseVerticalAlignment(value: String?): Alignment.Vertical {
    return when (value) {
        "Top" -> Alignment.Top
        "Center" -> Alignment.CenterVertically
        "Bottom" -> Alignment.Bottom
        else -> Alignment.Top
    }
}
fun parseHorizontalAlignment(value: String?): Alignment.Horizontal {
    return when (value) {
        "Start" -> Alignment.Start
        "Center" -> Alignment.CenterHorizontally
        "End" -> Alignment.End
        else -> Alignment.Start
    }
}
fun parseVerticalArrangement(value: String?): Arrangement.Vertical {
    return when (value) {
        "Start" -> Arrangement.Top
        "Center" -> Arrangement.Center
        "End" -> Arrangement.Bottom
        "SpaceBetween" -> Arrangement.SpaceBetween
        "SpaceAround" -> Arrangement.SpaceAround
        "SpaceEvenly" -> Arrangement.SpaceEvenly
        else -> Arrangement.Top
    }
}
fun parseHorizontalArrangement(value: String?): Arrangement.Horizontal {
    return when (value) {
        "Start" -> Arrangement.Start
        "Center" -> Arrangement.Center
        "End" -> Arrangement.End
        "SpaceBetween" -> Arrangement.SpaceBetween
        "SpaceAround" -> Arrangement.SpaceAround
        "SpaceEvenly" -> Arrangement.SpaceEvenly
        else -> Arrangement.Start
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

fun parseDrawable(src: String): Int? {
    return if (src.startsWith("@drawable/")) {
        try {
            R.drawable::class.java
                .getField(src.removePrefix("@drawable/"))
                .getInt(null)
        } catch (e: Exception) {
            null
        }
    } else null
}