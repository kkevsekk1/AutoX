package com.baidu.paddle.lite.demo.ocr

import android.graphics.Rect
import kotlin.math.abs

data class OcrResult(
    @JvmField
    var confidence: Float = 0f,
    @JvmField
    var preprocessTime: Float = 0f,
    @JvmField
    var inferenceTime: Float = 0f,
    @JvmField
    var words: String? = null,
    @JvmField
    var bounds: Rect? = null,
) : Comparable<OcrResult> {

    override fun compareTo(other: OcrResult): Int {
        val deviation = (bounds!!.height() / 2).coerceAtLeast(other.bounds!!.height() / 2)
        return if (abs((bounds!!.top + bounds!!.bottom) / 2 - (other.bounds!!.top + other.bounds!!.bottom) / 2) < deviation) {
            bounds!!.left - other.bounds!!.left
        } else {
            bounds!!.bottom - other.bounds!!.bottom
        }
    }

}