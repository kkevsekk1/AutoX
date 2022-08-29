package com.stardust.autojs.runtime.api

import android.content.Context
import com.baidu.paddle.lite.demo.ocr.OcrResult
import com.baidu.paddle.lite.demo.ocr.Predictor
import com.stardust.app.GlobalAppContext.get
import com.stardust.autojs.core.image.ImageWrapper

class Paddle {

    private val predictor = Predictor()
    private val availableProcessors = Runtime.getRuntime().availableProcessors()

    private fun initOcr(context: Context, cpuThreadNum: Int, useSlim: Boolean) {
        predictor.initOcr(context, cpuThreadNum, useSlim)
    }

    private fun initOcr(context: Context, myModelPath: String): Boolean {
        return predictor.init(context, myModelPath)
    }

    @JvmOverloads
    fun ocr(
        image: ImageWrapper,
        cpuThreadNum: Int = availableProcessors,
        useSlim: Boolean = true
    ): List<OcrResult> {
        val bitmap = image.bitmap
        if (bitmap == null || bitmap.isRecycled) {
            return emptyList()
        }
        if (!predictor.isLoaded()) {
            initOcr(get(), cpuThreadNum, useSlim)
        }
        return predictor.runOcr(bitmap, cpuThreadNum)
    }

    fun ocr(
        image: ImageWrapper,
        cpuThreadNum: Int,
        myModelPath: String
    ): List<OcrResult> {

        val bitmap = image.bitmap
        if (bitmap == null || bitmap.isRecycled) {
            return emptyList()
        }
        if (!predictor.isLoaded()) {
            initOcr(get(), myModelPath)
        }
        return predictor.runOcr(bitmap, cpuThreadNum)
    }

    fun ocr(image: ImageWrapper, useSlim: Boolean): List<OcrResult> {
        return ocr(image, availableProcessors, useSlim)
    }

    fun ocr(image: ImageWrapper, myModelPath: String): List<OcrResult> {
        return ocr(image, availableProcessors, myModelPath)
    }

}