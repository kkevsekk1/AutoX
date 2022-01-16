package com.baidu.paddle.lite.demo.ocr;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import com.stardust.app.GlobalAppContext;

import java.util.ArrayList;
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
        }else{
            Log.e("mPredictor.init", "mPredictor == null");
        }
    }

    @Override
    public OcrResult ocrImage(Bitmap bitmap, int cpuThreadNum) {
        OcrResult ocrResult;
        if (mPredictor != null && bitmap != null && !bitmap.isRecycled()) {
            mPredictor.setInputImage(bitmap);
            List<OcrResultModel> OcrResultModelList = mPredictor.ocrImage(bitmap,cpuThreadNum);
            ocrResult = transformData(OcrResultModelList);
            ocrResult.timeRequired = mPredictor.inferenceTime();
        } else {
            ocrResult = OcrResult.buildFailResult();
        }
        return ocrResult;
    }

    @Override
    public void end() {
        if (mPredictor != null) {
            mPredictor.release();
        }
    }

    public OcrResult transformData(List<OcrResultModel> OcrResultModelList) {
        if (OcrResultModelList == null) {
            return OcrResult.buildFailResult();
        }
        OcrResult ocrResult = new OcrResult();
        List<OcrResult.OCrWord> wordList = new ArrayList<>();
        StringBuilder textSb = new StringBuilder();
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
            textSb.append(model.getLabel());
            Rect rect = new Rect(left, top, right, bottom);
            OcrResult.OCrWord oCrWord = new OcrResult.OCrWord(model.getLabel(), rect, model.getConfidence());
            wordList.add(oCrWord);
        }
        ocrResult.success = true;
        ocrResult.words = wordList;
        ocrResult.text = textSb.toString();
        return ocrResult;
    }
}
