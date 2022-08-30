package com.baidu.paddle.lite.demo.ocr

import android.graphics.Rect
import kotlin.math.abs

data class OcrResult(
    @JvmField
    val confidence: Float = 0f,
    @JvmField
    val preprocessTime: Float = 0f,
    @JvmField
    val inferenceTime: Float = 0f,
    @JvmField
    val text: String = "",
    @JvmField
    val bounds: Rect? = null,
) : Comparable<OcrResult> {

    @Deprecated("use text", ReplaceWith("text"))
    @JvmField
    val words: String = text

    @Deprecated("use text", ReplaceWith("text"))
    fun getWords() = text

    @Deprecated("use confidence", ReplaceWith("confidence"))
    fun getConfidence() = confidence

    @Deprecated("use preprocessTime", ReplaceWith("preprocessTime"))
    fun getPreprocessTime() = preprocessTime

    @Deprecated("use inferenceTime", ReplaceWith("inferenceTime"))
    fun getInferenceTime() = inferenceTime

    @Deprecated("use bounds", ReplaceWith("bounds"))
    fun getBounds() = bounds

    override fun compareTo(other: OcrResult): Int {
        val deviation = (bounds!!.height() / 2).coerceAtLeast(other.bounds!!.height() / 2)
        return if (abs((bounds.top + bounds.bottom) / 2 - (other.bounds.top + other.bounds.bottom) / 2) < deviation) {
            bounds.left - other.bounds.left
        } else {
            bounds.bottom - other.bounds.bottom
        }
    }

}