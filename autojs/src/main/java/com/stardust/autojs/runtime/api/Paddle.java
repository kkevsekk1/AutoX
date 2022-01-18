package com.stardust.autojs.runtime.api;

import android.graphics.Bitmap;
import android.util.Log;

import com.baidu.paddle.lite.demo.ocr.OcrHelper;
import com.baidu.paddle.lite.demo.ocr.OcrResult;
import com.stardust.autojs.core.image.ImageWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Paddle {

    static class Ref<T> {
        public T value;
        public Ref(T value) {
            this.value = value;
        }
    }

    public boolean init() {
        Ref<Boolean> isSuccess = new Ref<>(false);
        OcrHelper.getInstance().initIfNeeded(() -> {
            isSuccess.value = true;
        });
        return isSuccess.value;
    }

    public boolean end() {
        OcrHelper.getInstance().end();
        return true;
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        init();
        return OcrHelper.getInstance().getOcrInstance().ocr(bitmap, cpuThreadNum);
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum) {
        List<OcrResult> words_result =  ocr(image, cpuThreadNum);
        String[] outputResult = new String[words_result.size()];
        for (int i = 0; i < words_result.size(); i++) {
            outputResult[i] = words_result.get(i).words;
            Log.i("outputResult", outputResult[i].toString()); // show LOG in Logcat panel
         }
        return outputResult;
    }
}



