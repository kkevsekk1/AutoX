package com.baidu.paddle.lite.demo.ocr;

import android.graphics.Bitmap;

public interface IOcrInstance<T> {
    T getInstance();

    void init();

    OcrResult ocrImage(Bitmap bitmap, int cpuThreadNum);

    void end();
}
