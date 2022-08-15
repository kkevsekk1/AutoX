package com.baidu.paddle.lite.demo.ocr

import android.graphics.Rect
import kotlin.math.abs

data class OcrResult(
    var confidence: Float = 0f,
    var preprocessTime: Float = 0f,
    var inferenceTime: Float = 0f,
    var words: String? = null,
    var bounds: Rect? = null,
    var location: RectLocation? = null
) : Comparable<OcrResult> {

    override fun compareTo(other: OcrResult): Int {
        val deviation = (location!!.height / 2).coerceAtLeast(other.location!!.height / 2)
        return if (abs((bounds!!.top + bounds!!.bottom) / 2 - (other.bounds!!.top + other.bounds!!.bottom) / 2) < deviation) {
            bounds!!.left - other.bounds!!.left
        } else {
            bounds!!.bottom - other.bounds!!.bottom
        }
    }

}

data class RectLocation(
    var left: Int,
    var top: Int,
    var width: Int,
    var height: Int
)