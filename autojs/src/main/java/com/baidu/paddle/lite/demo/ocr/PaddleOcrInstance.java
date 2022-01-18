package com.baidu.paddle.lite.demo.ocr;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.stardust.app.GlobalAppContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class PaddleOcrInstance implements IOcrInstance<Predictor> {

    private Predictor mPredictor;

    public PaddleOcrInstance() {
        this.mPredictor = new Predictor();
    }

    @Override
    public Predictor getInstance() {
        return this.mPredictor;
    }

    @Override
    public void init() {
        if (mPredictor != null) {
            mPredictor.init(GlobalAppContext.get());
        } else {
            Log.e("mPredictor.init", "mPredictor == null");
        }
    }

    @Override
    public List<OcrResult> ocr(Bitmap bitmap, int cpuThreadNum) {
        List<OcrResult> words_result = Collections.emptyList();
        if (mPredictor != null && bitmap != null && !bitmap.isRecycled()) {
            mPredictor.setInputImage(bitmap);
            List<OcrResultModel> OcrResultModelList = mPredictor.ocr(bitmap, cpuThreadNum);
            words_result = transformData(OcrResultModelList);
        } else {
            words_result = Collections.emptyList();
        }
        return words_result;
    }

    @Override
    public void end() {
        if (mPredictor != null) {
            mPredictor.release();
        }
    }

    public List<OcrResult> transformData(List<OcrResultModel> OcrResultModelList) {
        if (OcrResultModelList == null) {
            return Collections.emptyList();
        }
        List<OcrResult> words_result = new ArrayList<>();
        for (OcrResultModel model : OcrResultModelList) {
            List<Point> pointList = model.getPoints();
            if (pointList.isEmpty()) {
                continue;
            }
            Point firstPoint = pointList.get(0);
            int left = firstPoint.x;
            int top = firstPoint.y;
            int right = firstPoint.x;
            int bottom = firstPoint.y;
            for (Point p : pointList) {
                if (p.x < left) {
                    left = p.x;
                }
                if (p.x > right) {
                    right = p.x;
                }
                if (p.y < top) {
                    top = p.y;
                }
                if (p.y > bottom) {
                    bottom = p.y;
                }
            }
            OcrResult ocrResult = new OcrResult();
            ocrResult.confidence = model.getConfidence();
            ocrResult.words = model.getLabel().trim().replace("\r", "");
            ocrResult.location = new int[]{left, top, Math.abs(right - left), Math.abs(bottom - top)};
            ocrResult.bounds = new Rect(left, top, right, bottom);
            words_result.add(ocrResult);
        }
        return words_result;
    }
}
