package com.baidu.paddle.lite.demo.ocr;

import android.graphics.Rect;
import android.os.Parcelable;

public class OcrResult implements Comparable<OcrResult> {
    public float confidence;
    public float preprocessTime;
    public float inferenceTime;
    public String words;
    public Rect bounds;
    public RectLocation location;


    @Override
    public int compareTo(OcrResult s) {
        int deviation = Math.max(this.location.height / 2, s.location.height / 2);
        if (Math.abs((this.bounds.top + this.bounds.bottom) / 2 - (s.bounds.top + s.bounds.bottom) / 2) < deviation) {
            return this.bounds.left - s.bounds.left;
        } else {
            return this.bounds.bottom - s.bounds.bottom;
        }
    }


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
