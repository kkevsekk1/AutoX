package com.baidu.paddle.lite.demo.ocr

import android.graphics.Bitmap
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.roundToInt

open class OCRPredictorNative(config: Config) {
    private var config: Config? = null
    private var nativePointer: Long = 0
    fun runImage(
        inputData: FloatArray,
        width: Int,
        height: Int,
        channels: Int,
        originalImage: Bitmap?
    ): ArrayList<OcrResultModel> {
        lock.lock()
        return try {
            Log.i(
                "OCRPredictorNative",
                "begin to run image " + inputData.size + " " + width + " " + height
            )
            val dims = floatArrayOf(1f, channels.toFloat(), height.toFloat(), width.toFloat())
            val rawResults = forward(nativePointer, inputData, dims, originalImage)
            postprocess(rawResults)
        } finally {
            lock.unlock()
        }
    }

    class Config {
        var cpuThreadNum = 0
        var cpuPower: String? = null
        var detModelFilename: String? = null
        var recModelFilename: String? = null
        var clsModelFilename: String? = null
    }

    fun destroy() {
        if (nativePointer != 0L) {
            release(nativePointer)
            nativePointer = 0
        }
    }

    protected external fun init(
        detModelPath: String?,
        recModelPath: String?,
        clsModelPath: String?,
        threadNum: Int,
        cpuMode: String?
    ): Long

    protected external fun forward(
        pointer: Long,
        buf: FloatArray?,
        ddims: FloatArray?,
        originalImage: Bitmap?
    ): FloatArray

    protected external fun release(pointer: Long)
    private fun postprocess(raw: FloatArray): ArrayList<OcrResultModel> {
        val results = ArrayList<OcrResultModel>()
        var begin = 0
        while (begin < raw.size) {
            val pointNum = raw[begin].roundToInt()
            val wordNum = raw[begin + 1].roundToInt()
            val model = parse(raw, begin + 2, pointNum, wordNum)
            begin += 2 + 1 + pointNum * 2 + wordNum
            results.add(model)
        }
        return results
    }

    private fun parse(raw: FloatArray, begin: Int, pointNum: Int, wordNum: Int): OcrResultModel {
        var current = begin
        val model = OcrResultModel()
        model.confidence = raw[current]
        current++
        for (i in 0 until pointNum) {
            model.addPoints(raw[current + i * 2].roundToInt(),
                raw[current + i * 2 + 1].roundToInt()
            )
        }
        current += pointNum * 2
        for (i in 0 until wordNum) {
            val index = raw[current + i].roundToInt()
            model.addWordIndex(index)
        }
        Log.i("OCRPredictorNative", "word finished $wordNum")
        return model
    }

    // 重写 finalize 确保对象被GC回收时native内存能被释放
    protected fun finalize() {
        destroy()
    }

    companion object {
        private val isSOLoaded = AtomicBoolean()
        private val lock = ReentrantLock()
        @Throws(RuntimeException::class)
        fun loadLibrary() {
            if (!isSOLoaded.get() && isSOLoaded.compareAndSet(false, true)) {
                try {
                    System.loadLibrary("Native")
                } catch (e: Throwable) {
                    throw RuntimeException(
                        "Load libNative.so failed, please check it exists in apk file.", e
                    )
                }
            }
        }
    }

    init {
        lock.lock()
        try {
            this.config = config
            loadLibrary()
            nativePointer = init(
                config.detModelFilename, config.recModelFilename, config.clsModelFilename,
                config.cpuThreadNum, config.cpuPower
            )
            Log.i("OCRPredictorNative", "load success $nativePointer")
        } finally {
            lock.unlock()
        }
    }
}