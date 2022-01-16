package com.baidu.paddle.lite.demo.ocr;

import android.graphics.Rect;

import java.util.Collections;
import java.util.List;

public class OcrResult {
    public boolean success;
    public String text;
    public List<OCrWord> words;
    public float timeRequired;

    public static OcrResult buildFailResult() {
        OcrResult ocrResult = new OcrResult();
        ocrResult.success = false;
        ocrResult.text = "";
        ocrResult.words = Collections.emptyList();
        ocrResult.timeRequired = 0;
        return ocrResult;
    }

    public static class OCrWord {

        public String text;
        public Rect bounds;
        public float confidences;

        public OCrWord(String text, Rect rect, float confidences) {
            this.text = text;
            this.bounds = rect;
            this.confidences = confidences;
        }
    }
}
