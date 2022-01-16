package com.stardust.autojs.runtime.api;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.util.Log;
import com.baidu.paddle.lite.demo.ocr.OcrResultModel;
import com.stardust.autojs.annotation.ScriptInterface;
import com.stardust.autojs.core.image.ImageWrapper;
import com.baidu.paddle.lite.demo.ocr.OcrHelper;
import com.baidu.paddle.lite.demo.ocr.OcrResult;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Paddle {

    private final long tomeOut = 6000;
    private boolean isInitialized;

    static class Ref<T> {
        public T value;

        public Ref(T value) {
            this.value = value;
        }
    }

    @ScriptInterface
    public boolean init() {
        Ref<Boolean> isSuccess = new Ref<>(false);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        OcrHelper.getInstance().initIfNeeded(() -> {
            countDownLatch.countDown();
            isSuccess.value = true;
        });
        try {
            countDownLatch.await(tomeOut, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Log.e("InterruptedException", e.getMessage());
        }
        return isSuccess.value;
    }

    @ScriptInterface
    public OcrResult ocrImage(ImageWrapper image, int cpuThreadNum) {
        if (image == null) {
            return OcrResult.buildFailResult();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return OcrResult.buildFailResult();
        }
        init();
        return OcrHelper.getInstance().getOcrInstance().ocrImage(bitmap, cpuThreadNum);
    }

    @ScriptInterface
    public String[] ocrText(ImageWrapper image, int cpuThreadNum) {
        OcrResult results =  ocrImage(image, cpuThreadNum);
        String[] outputResult = new String[results.words.size()];
        for (int i = 0; i < results.words.size(); i++) {
            outputResult[i] = results.words.get(i).text;
            Log.i("outputResult", outputResult[i].toString()); // show LOG in Logcat panel
         }
        return outputResult;
    }

    @ScriptInterface
    public boolean end() {
        OcrHelper.getInstance().end();
        return true;
    }
}



