package com.baidu.paddle.lite.demo.ocr;

import android.graphics.Rect;

public class OcrResult {
    public float confidence;
    public String words;
    public Rect bounds;
    public int[] location;
}
