package com.stardust.autojs.runtime.api;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.baidu.paddle.lite.demo.ocr.OcrResult;
import com.baidu.paddle.lite.demo.ocr.Predictor;
import com.stardust.app.GlobalAppContext;
import com.stardust.autojs.core.image.ImageWrapper;

import java.util.Collections;
import java.util.List;

public class Paddle {
    private Predictor mPredictor = new Predictor();

    private final int availableProcessors = Runtime.getRuntime().availableProcessors();

    public void initOcr(Context context, int cpuThreadNum, boolean useSlim) {
        mPredictor.initOcr(context, cpuThreadNum, useSlim);
    }

    public boolean initOcr(Context context, String myModelPath) {
        return mPredictor.init(context, myModelPath);
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum, boolean useSlim) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        if (!mPredictor.isLoaded()) {
            initOcr(GlobalAppContext.get(), cpuThreadNum, useSlim);
        }
        return mPredictor.runOcr(bitmap, cpuThreadNum, true);
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum, String myModelPath) {
        if (image == null) {
            return Collections.emptyList();
        }
        Bitmap bitmap = image.getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            return Collections.emptyList();
        }
        if (!mPredictor.isLoaded()) {
            initOcr(GlobalAppContext.get(), myModelPath);
        }
        return mPredictor.runOcr(bitmap, cpuThreadNum, true);
    }

    public List<OcrResult> ocr(ImageWrapper image, boolean useSlim) {
        return ocr(image, availableProcessors, useSlim);
    }

    public List<OcrResult> ocr(ImageWrapper image, String myModelPath) {
        return ocr(image, availableProcessors, myModelPath);
    }

    public List<OcrResult> ocr(ImageWrapper image, int cpuThreadNum) {
        return ocr(image, cpuThreadNum, true);
    }

    public List<OcrResult> ocr(ImageWrapper image) {
        return ocr(image, availableProcessors, true);
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum, boolean useSlim) {
        List<OcrResult> wordsResult = ocr(image, cpuThreadNum, useSlim);
        return sortResult(wordsResult);
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum, String myModelPath) {
        List<OcrResult> wordsResult = ocr(image, cpuThreadNum, myModelPath);
        return sortResult(wordsResult);
    }

    public String[] ocrText(ImageWrapper image, String myModelPath) {
        List<OcrResult> wordsResult = ocr(image, myModelPath);
        return sortResult(wordsResult);
    }

    public String[] ocrText(ImageWrapper image, boolean useSlim) {
        List<OcrResult> wordsResult = ocr(image, useSlim);
        return sortResult(wordsResult);
    }

    public String[] ocrText(ImageWrapper image, int cpuThreadNum) {
        return ocrText(image, cpuThreadNum, true);
    }

    public String[] ocrText(ImageWrapper image) {
        return ocrText(image, availableProcessors, true);
    }

    public void release() {
        mPredictor.releaseOcr();
    }

    private String[] sortResult(List<OcrResult> src) {
        String[] outputResult = new String[src.size()];
        for (int i = 0; i < src.size(); i++) {
            outputResult[i] = src.get(i).getWords();
            Log.i("outputResult", outputResult[i].toString()); // show LOG in Logcat panel
        }
        return outputResult;
    }

}
