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

public class OCR {

    private Predictor mPredictor = new Predictor();

    public synchronized boolean init(boolean useSlim) {
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

    public void release() {
        mPredictor.releaseModel();
    }

    public List<OcrResult> img(ImageWrapper image, int cpuThreadNum, boolean useSlim) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        init(useSlim);
        return mPredictor.ocr(bitmap, cpuThreadNum);
    }

    public List<OcrResult> img(ImageWrapper image, int cpuThreadNum) {
        return img(image, cpuThreadNum, true);
    }

    public List<OcrResult> img(ImageWrapper image) {
        return img(image, 4, true);
    }

    public String[] text(ImageWrapper image, int cpuThreadNum, boolean useSlim) {
        List<OcrResult> words_result = img(image, cpuThreadNum, useSlim);
        String[] outputResult = new String[words_result.size()];
        for (int i = 0; i < words_result.size(); i++) {
            outputResult[i] = words_result.get(i).words;
            Log.i("outputResult", outputResult[i].toString()); // show LOG in Logcat panel
        }
        return outputResult;
    }

    public String[] text(ImageWrapper image, int cpuThreadNum) {
        return text(image, cpuThreadNum, true);
    }

    public String[] text(ImageWrapper image) {
        return text(image, 4, true);
    }

}



