package com.aiselp.autox.api.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.core.graphics.toColorInt
import com.stardust.autojs.R


fun parseContentScale(value: String?): ContentScale {
    return when (value) {
        "fill_width" -> ContentScale.FillWidth
        "fill_height" -> ContentScale.FillHeight
        "fill_bounds" -> ContentScale.FillBounds
        "crop" -> ContentScale.Crop
        "inside" -> ContentScale.Inside
        "fit" -> ContentScale.Fit
        "none" -> ContentScale.None
        else -> ContentScale.Fit
    }
}

fun parseAlignment(value: String?): Alignment {
    return when (value) {
        "top_start" -> Alignment.TopStart
        "top_center" -> Alignment.TopCenter
        "top_end" -> Alignment.TopEnd
        "center_start" -> Alignment.CenterStart
        "center" -> Alignment.Center
        "center_end" -> Alignment.CenterEnd
        "bottom_start" -> Alignment.BottomStart
        "bottom_center" -> Alignment.BottomCenter
        "bottom_end" -> Alignment.BottomEnd
        else -> Alignment.Center
    }
}

fun parseVerticalAlignment(value: String?): Alignment.Vertical {
    return when (value) {
        "top" -> Alignment.Top
        "center" -> Alignment.CenterVertically
        "bottom" -> Alignment.Bottom
        else -> Alignment.Top
    }
}
fun parseHorizontalAlignment(value: String?): Alignment.Horizontal {
    return when (value) {
        "start" -> Alignment.Start
        "center" -> Alignment.CenterHorizontally
        "end" -> Alignment.End
        else -> Alignment.Start
    }
}
fun parseVerticalArrangement(value: String?): Arrangement.Vertical {
    return when (value) {
        "start" -> Arrangement.Top
        "center" -> Arrangement.Center
        "end" -> Arrangement.Bottom
        "space_between" -> Arrangement.SpaceBetween
        "space_around" -> Arrangement.SpaceAround
        "space_evenly" -> Arrangement.SpaceEvenly
        else -> Arrangement.Top
    }
}
fun parseHorizontalArrangement(value: String?): Arrangement.Horizontal {
    return when (value) {
        "start" -> Arrangement.Start
        "center" -> Arrangement.Center
        "end" -> Arrangement.End
        "space_between" -> Arrangement.SpaceBetween
        "space_around" -> Arrangement.SpaceAround
        "space_evenly" -> Arrangement.SpaceEvenly
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