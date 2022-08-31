package com.stardust.autojs.runtime.api

import android.util.Log
import com.baidu.paddle.lite.demo.ocr.OcrResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.stardust.autojs.core.image.ImageWrapper
import com.stardust.autojs.core.mlkit.GoogleMLKitOcrResult
import java.util.concurrent.CountDownLatch

class GoogleMLKit {

    companion object {
        const val TAG = "mlkit"

        private fun getLanguage(language: String) = when (language) {
            "zh" -> ChineseTextRecognizerOptions.Builder().build()
            "sa" -> DevanagariTextRecognizerOptions.Builder().build()
            "ja" -> JapaneseTextRecognizerOptions.Builder().build()
            "ko" -> KoreanTextRecognizerOptions.Builder().build()
            else -> TextRecognizerOptions.Builder().build()
        }

        private fun Text.mapToOcrResults(): GoogleMLKitOcrResult {
            val result = this
            val ocrResult = GoogleMLKitOcrResult(
                level = 0,
                text = result.text,
                children = result.textBlocks.map { block ->
                    GoogleMLKitOcrResult(
                        level = 1,
                        text = block.text,
                        language = block.recognizedLanguage,
                        bounds = block.boundingBox,
                        children = block.lines.map { line ->
                            GoogleMLKitOcrResult(
                                level = 2,
                                confidence = line.confidence,
                                text = line.text,
                                language = line.recognizedLanguage,
                                bounds = line.boundingBox,
                                children = line.elements.map { e ->
                                    GoogleMLKitOcrResult(
                                        level = 3,
                                        confidence = e.confidence,
                                        text = e.text,
                                        language = e.recognizedLanguage,
                                        bounds = e.boundingBox,
                                        children = null
                                    )
                                }
                            )
                        }
                    )
                }
            )
            return ocrResult
        }

    }

    fun ocr(imageWrapper: ImageWrapper, language: String): GoogleMLKitOcrResult? {
        val textRecognizer = TextRecognition.getClient(getLanguage(language))
        var ocrResults: GoogleMLKitOcrResult? = null
        val controller = CountDownLatch(1)

        textRecognizer.process(InputImage.fromBitmap(imageWrapper.bitmap, 0))
            .addOnSuccessListener { result ->
                Log.d(TAG, "ocr: success")
                ocrResults = result.mapToOcrResults()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
            .addOnCompleteListener {
                controller.countDown()
            }

        controller.await()
        return ocrResults
    }

    fun ocrText(imageWrapper: ImageWrapper, language: String): String {
        val textRecognizer =
            TextRecognition.getClient(getLanguage(language))
        var ocrResults = ""
        val controller = CountDownLatch(1)

        textRecognizer.process(InputImage.fromBitmap(imageWrapper.bitmap, 0))
            .addOnSuccessListener { result ->
                Log.d(TAG, "ocrText: success")
                ocrResults = result.text
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
            .addOnCompleteListener {
                controller.countDown()
            }

        controller.await()
        return ocrResults
    }

}
