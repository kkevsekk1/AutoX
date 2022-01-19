package com.baidu.paddle.lite.demo.ocr;

import android.graphics.Rect;
import android.os.Parcelable;

public class OcrResult {
    public float confidence;
    public float preprocessTime;
    public float inferenceTime;
    public String words;
    public Rect bounds;
    public RectLocation location;

    public static class RectLocation {
        public int left;
        public int top;
        public int width;
        public int height;

        public RectLocation(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }
    }
}
