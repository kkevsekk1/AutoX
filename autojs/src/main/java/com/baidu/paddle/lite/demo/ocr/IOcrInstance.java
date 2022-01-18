package com.baidu.paddle.lite.demo.ocr;

import android.graphics.Bitmap;

import java.util.List;

public interface IOcrInstance<T> {
    T getInstance();

    void init();

    List<OcrResult> ocr(Bitmap bitmap, int cpuThreadNum);

    void end();
}
