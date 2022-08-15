package com.baidu.paddle.lite.demo.ocr

import android.graphics.Point
import java.util.ArrayList

data class OcrResultModel(
    val points: MutableList<Point> = mutableListOf(),
    val wordIndex: MutableList<Int> = mutableListOf(),
    var label: String? = null,
    var confidence: Float = 0f
) {

    fun addPoints(x: Int, y: Int) {
        val point = Point(x, y)
        points.add(point)
    }

    fun addWordIndex(index: Int) {
        wordIndex.add(index)
    }

}