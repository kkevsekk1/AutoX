package com.stardust.autojs.runtime.api;

import android.graphics.Bitmap;
import android.os.Looper;
import android.util.Log;

import com.baidu.paddle.lite.demo.ocr.OcrResult;
import com.baidu.paddle.lite.demo.ocr.Predictor;
import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.core.image.ImageWrapper;

import java.util.Collections;
import java.util.List;

public class Paddle {

    private Predictor mPredictor = new Predictor();

    public synchronized boolean initOcr(boolean useSlim) {
        if (!mPredictor.isLoaded) {
            if (Looper.getMainLooper() == Looper.myLooper()) {
                new Thread(() -> {
                    mPredictor.init(GlobalAppContext.get(), useSlim);
                }).start();
            } else {
                mPredictor.init(GlobalAppContext.get(), useSlim);
            }
        }
        return mPredictor.isLoaded;
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum, boolean useSlim) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        initOcr(useSlim);
        return mPredictor.ocr(bitmap, cpuThreadNum);
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum) {
        return ocr(image, cpuThreadNum, true);
    }

    public List<OcrResult> ocr(ImageWrapper image) {
        return ocr(image, 4, true);
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum, boolean useSlim) {
        List<OcrResult> words_result = ocr(image, cpuThreadNum, useSlim);
        String[] outputResult = new String[words_result.size()];
        for (int i = 0; i < words_result.size(); i++) {
            outputResult[i] = words_result.get(i).words;
            Log.i("outputResult", outputResult[i].toString()); // show LOG in Logcat panel
        }
        return outputResult;
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum) {
        return ocrText(image, cpuThreadNum, true);
    }

    public String[] ocrText(ImageWrapper image) {
        return ocrText(image, 4, true);
    }

    public static Paddle getInstance() {
        final Paddle INSTANCE = new Paddle();
        return INSTANCE;
    }
}



