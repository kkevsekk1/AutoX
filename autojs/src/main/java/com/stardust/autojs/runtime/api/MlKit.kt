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
import java.util.concurrent.CountDownLatch

class MlKit {

    companion object {
        const val TAG = "mlkit"
    }

    fun ocr(imageWrapper: ImageWrapper, language: String): List<OcrResult> {
        val languageOption = when (language) {
            "zh" -> ChineseTextRecognizerOptions.Builder().build()
            "sa" -> DevanagariTextRecognizerOptions.Builder().build()
            "ja" -> JapaneseTextRecognizerOptions.Builder().build()
            "ko" -> KoreanTextRecognizerOptions.Builder().build()
            else -> TextRecognizerOptions.Builder().build()
        }
        val textRecognizer =
            TextRecognition.getClient(languageOption)
        val ocrResults = mutableListOf<OcrResult>()
        val controller = CountDownLatch(1)

        textRecognizer.process(InputImage.fromBitmap(imageWrapper.bitmap, 0))
            .addOnSuccessListener { result ->
                Log.d(TAG, "ocr: success")
                mapToOcrResults(result, ocrResults)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
            .addOnCompleteListener {
                controller.countDown()
            }

        controller.await()
        return ocrResults.toList()
    }

    private fun mapToOcrResults(
        result: Text,
        ocrResults: MutableList<OcrResult>
    ) {
        for (block in result.textBlocks) {
            ocrResults.add(
                OcrResult(
                    confidence = 1.0f,
                    words = block.text,
                    bounds = block.boundingBox
                )
            )
            for (line in block.lines) {
                ocrResults.add(
                    OcrResult(
                        confidence = line.confidence,
                        words = line.text,
                        bounds = line.boundingBox
                    )
                )
                for (element in line.elements) {
                    ocrResults.add(
                        OcrResult(
                            confidence = element.confidence,
                            words = element.text,
                            bounds = element.boundingBox
                        )
                    )
                }
            }
        }
    }

}
