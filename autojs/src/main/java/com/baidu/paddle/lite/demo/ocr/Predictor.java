package com.baidu.paddle.lite.demo.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import static android.graphics.Color.*;


public class Predictor {
    private static final String TAG = Predictor.class.getSimpleName();
    public boolean isLoaded = false;
    public boolean useSlim = true;
    protected OCRPredictorNative mPaddlePredictorNative;
    public int warmupIterNum = 1;
    public int inferIterNum = 1;
    public int cpuThreadNum = 4;
    public String cpuPowerMode = "LITE_POWER_HIGH";
    public String modelPath = "";
    public String modelName = "";
    protected OCRPredictorNative paddlePredictor = null;
    protected float inferenceTime = 0;
    // Only for object detection
    protected Vector<String> wordLabels = new Vector<String>();
    protected String inputColorFormat = "BGR";
    protected long[] inputShape = new long[]{1, 3, 960};
    protected float[] inputMean = new float[]{0.485f, 0.456f, 0.406f};
    protected float[] inputStd = new float[]{0.229f, 0.224f, 0.225f};
    protected float scoreThreshold = 0.1f;
    protected Bitmap inputImage = null;
    protected Bitmap outputImage = null;
    protected volatile String outputResult = "";
    protected float preprocessTime = 0;
    protected float postprocessTime = 0;


    public Predictor() {
    }

    public boolean init(Context appCtx, String modelPath, String labelPath) {
        if (!isLoaded) {
            loadModel(appCtx, modelPath, cpuThreadNum, cpuPowerMode);
            loadLabel(appCtx, labelPath);
            isLoaded = true;
        }
        Log.i(TAG, "PaddleOCR isLoaded: " + isLoaded);
        return isLoaded;
    }


    public boolean init(Context appCtx, String modelPath, String labelPath, int cpuThreadNum, String cpuPowerMode,
                        String inputColorFormat,
                        long[] inputShape, float[] inputMean,
                        float[] inputStd, float scoreThreshold) {
        if (inputShape.length != 3) {
            Log.e(TAG, "Size of input shape should be: 3");
            return false;
        }
        if (inputMean.length != inputShape[1]) {
            Log.e(TAG, "Size of input mean should be: " + Long.toString(inputShape[1]));
            return false;
        }
        if (inputStd.length != inputShape[1]) {
            Log.e(TAG, "Size of input std should be: " + Long.toString(inputShape[1]));
            return false;
        }
        if (inputShape[0] != 1) {
            Log.e(TAG, "Only one batch is supported in the image classification demo, you can use any batch size in " +
                    "your Apps!");
            return false;
        }
        if (inputShape[1] != 1 && inputShape[1] != 3) {
            Log.e(TAG, "Only one/three channels are supported in the image classification demo, you can use any " +
                    "channel size in your Apps!");
            return false;
        }
        if (!inputColorFormat.equalsIgnoreCase("BGR")) {
            Log.e(TAG, "Only  BGR color format is supported.");
            return false;
        }
        boolean isLoaded = init(appCtx, modelPath, labelPath);
        if (!isLoaded) {
            return false;
        }
        this.inputColorFormat = inputColorFormat;
        this.inputShape = inputShape;
        this.inputMean = inputMean;
        this.inputStd = inputStd;
        this.scoreThreshold = scoreThreshold;
        return true;
    }

    protected boolean loadModel(Context appCtx, String modelPath, int cpuThreadNum, String cpuPowerMode) {
        // Release model if exists
        releaseModel();

        // Load model
        if (modelPath.isEmpty()) {
            Log.i(TAG, "modelPath.isEmpty() ");
            return false;
        }
        String realPath = modelPath;
        if (!modelPath.substring(0, 1).equals("/")) {
            // Read model files from custom path if the first character of mode path is '/'
            // otherwise copy model to cache from assets
            realPath = appCtx.getCacheDir() + "/" + modelPath;
            Log.i(TAG, "realPath.isEmpty() " + realPath);
            Utils.copyDirectoryFromAssets(appCtx, modelPath, realPath);
        }
        if (realPath.isEmpty()) {
            Log.i(TAG, "realPath.isEmpty() ");
            realPath = appCtx.getCacheDir() + "/" + modelPath;
            Utils.copyDirectoryFromAssets(appCtx, modelPath, realPath);
            return false;
        }

        OCRPredictorNative.Config config = new OCRPredictorNative.Config();
        config.cpuThreadNum = cpuThreadNum;
        config.detModelFilename = realPath + File.separator + "ch_ppocr_mobile_v2.0_det_opt.nb";
        config.recModelFilename = realPath + File.separator + "ch_ppocr_mobile_v2.0_rec_opt.nb";
        config.clsModelFilename = realPath + File.separator + "ch_ppocr_mobile_v2.0_cls_opt.nb";
        Log.i("Predictor", "model path" + config.detModelFilename + " ; " + config.recModelFilename + ";" + config.clsModelFilename);
        config.cpuPower = cpuPowerMode;
        paddlePredictor = new OCRPredictorNative(config);

        this.cpuThreadNum = cpuThreadNum;
        this.cpuPowerMode = cpuPowerMode;
        this.modelPath = realPath;
        this.modelName = realPath.substring(realPath.lastIndexOf("/") + 1);
        this.mPaddlePredictorNative = new OCRPredictorNative(config);
        Log.i(TAG, "realPath " + realPath);
        return true;
    }

    public void release() {
        if (this.mPaddlePredictorNative != null) {
            this.mPaddlePredictorNative.destroy();
            this.mPaddlePredictorNative = null;
        }

        this.isLoaded = false;
    }

    public void releaseModel() {
        if (paddlePredictor != null) {
            paddlePredictor.destroy();
            paddlePredictor = null;
        }
        isLoaded = false;
        cpuThreadNum = 4;
        cpuPowerMode = "LITE_POWER_HIGH";
        modelPath = "";
        modelName = "";
    }

    protected boolean loadLabel(Context appCtx, String labelPath) {
        wordLabels.clear();
        wordLabels.add("black");
        // Load word labels from file
        try {
            InputStream assetsInputStream = appCtx.getAssets().open(labelPath);
            int available = assetsInputStream.available();
            byte[] lines = new byte[available];
            assetsInputStream.read(lines);
            assetsInputStream.close();
            String words = new String(lines);
            String[] contents = words.split("\n");
            for (String content : contents) {
                wordLabels.add(content);
            }
            Log.i(TAG, "Word label size: " + wordLabels.size());
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        return true;
    }

    public boolean isLoaded() {
        return this.mPaddlePredictorNative != null && this.isLoaded;
    }

    public String modelPath() {
        return modelPath;
    }

    public String modelName() {
        return modelName;
    }

    public int cpuThreadNum() {
        return cpuThreadNum;
    }

    public String cpuPowerMode() {
        return cpuPowerMode;
    }

    public float inferenceTime() {
        return inferenceTime;
    }

    public Bitmap inputImage() {
        return inputImage;
    }

    public Bitmap outputImage() {
        return outputImage;
    }

    public String outputResult() {
        return outputResult;
    }

    public float preprocessTime() {
        return preprocessTime;
    }

    public float postprocessTime() {
        return postprocessTime;
    }

    public void setInputImage(Bitmap image) {
        if (image == null) {
            return;
        }
        this.inputImage = image.copy(Bitmap.Config.ARGB_8888, true);
    }

    private ArrayList<OcrResultModel> postprocess(ArrayList<OcrResultModel> results) {
        for (OcrResultModel r : results) {
            StringBuffer word = new StringBuffer();
            for (int index : r.getWordIndex()) {
                if (index >= 0 && index < wordLabels.size()) {
                    word.append(wordLabels.get(index));
                } else {
                    Log.e(TAG, "Word index is not in label list:" + index);
                    word.append("×");
                }
            }
            r.setLabel(word.toString());
        }
        return results;
    }

    public boolean runModel() {
        if (inputImage == null || !isLoaded()) {
            return false;
        }

        // Pre-process image, and feed input tensor with pre-processed data

        Bitmap scaleImage = Utils.resizeWithStep(inputImage, Long.valueOf(inputShape[2]).intValue(), 32);

        Date start = new Date();
        int channels = (int) inputShape[1];
        int width = scaleImage.getWidth();
        int height = scaleImage.getHeight();
        float[] inputData = new float[channels * width * height];
        if (channels == 3) {
            int[] channelIdx = null;
            if (inputColorFormat.equalsIgnoreCase("RGB")) {
                channelIdx = new int[]{0, 1, 2};
            } else if (inputColorFormat.equalsIgnoreCase("BGR")) {
                channelIdx = new int[]{2, 1, 0};
            } else {
                Log.i(TAG, "Unknown color format " + inputColorFormat + ", only RGB and BGR color format is " +
                        "supported!");
                return false;
            }

            int[] channelStride = new int[]{width * height, width * height * 2};
            int[] pixels = new int[width * height];
            scaleImage.getPixels(pixels, 0, scaleImage.getWidth(), 0, 0, scaleImage.getWidth(), scaleImage.getHeight());
            for (int i = 0; i < pixels.length; i++) {
                int color = pixels[i];
                float[] rgb = new float[]{(float) red(color) / 255.0f, (float) green(color) / 255.0f,
                        (float) blue(color) / 255.0f};
                inputData[i] = (rgb[channelIdx[0]] - inputMean[0]) / inputStd[0];
                inputData[i + channelStride[0]] = (rgb[channelIdx[1]] - inputMean[1]) / inputStd[1];
                inputData[i + channelStride[1]] = (rgb[channelIdx[2]] - inputMean[2]) / inputStd[2];
            }
        } else if (channels == 1) {
            int[] pixels = new int[width * height];
            scaleImage.getPixels(pixels, 0, scaleImage.getWidth(), 0, 0, scaleImage.getWidth(), scaleImage.getHeight());
            for (int i = 0; i < pixels.length; i++) {
                int color = pixels[i];
                float gray = (float) (red(color) + green(color) + blue(color)) / 3.0f / 255.0f;
                inputData[i] = (gray - inputMean[0]) / inputStd[0];
            }
        } else {
            Log.i(TAG, "Unsupported channel size " + Integer.toString(channels) + ",  only channel 1 and 3 is " +
                    "supported!");
            return false;
        }
        float[] pixels = inputData;
        Log.i(TAG, "pixels " + pixels[0] + " " + pixels[1] + " " + pixels[2] + " " + pixels[3]
                + " " + pixels[pixels.length / 2] + " " + pixels[pixels.length / 2 + 1] + " " + pixels[pixels.length - 2] + " " + pixels[pixels.length - 1]);
        Date end = new Date();
        preprocessTime = (float) (end.getTime() - start.getTime());

        // Warm up
        for (int i = 0; i < warmupIterNum; i++) {
            paddlePredictor.runImage(inputData, width, height, channels, inputImage);
        }
        warmupIterNum = 0; // do not need warm
        // Run inference
        start = new Date();
        ArrayList<OcrResultModel> results = paddlePredictor.runImage(inputData, width, height, channels, inputImage);
        end = new Date();
        inferenceTime = (end.getTime() - start.getTime()) / (float) inferIterNum;

        results = postprocess(results);
        Log.i(TAG, "[stat] Preprocess Time: " + preprocessTime
                + " ; Inference Time: " + inferenceTime + " ;Box Size " + results.size());
        drawResults(results);

        return true;
    }

    private void drawResults(ArrayList<OcrResultModel> results) {
        StringBuffer outputResultSb = new StringBuffer("");
        for (int i = 0; i < results.size(); i++) {
            OcrResultModel result = results.get(i);
            StringBuilder sb = new StringBuilder("");
            sb.append(result.getLabel());
            sb.append(" ").append(result.getConfidence());
            sb.append("; Points: ");
            for (Point p : result.getPoints()) {
                sb.append("(").append(p.x).append(",").append(p.y).append(") ");
            }
            Log.i(TAG, sb.toString()); // show LOG in Logcat panel
            outputResultSb.append(i + 1).append(": ").append(result.getLabel()).append("\n");
        }
        outputResult = outputResultSb.toString();
        outputImage = inputImage;
        Canvas canvas = new Canvas(outputImage);
        Paint paintFillAlpha = new Paint();
        paintFillAlpha.setStyle(Paint.Style.FILL);
        paintFillAlpha.setColor(Color.parseColor("#3B85F5"));
        paintFillAlpha.setAlpha(50);

        Paint paint = new Paint();
        paint.setColor(Color.parseColor("#3B85F5"));
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);

        for (OcrResultModel result : results) {
            Path path = new Path();
            List<Point> points = result.getPoints();
            path.moveTo(points.get(0).x, points.get(0).y);
            for (int i = points.size() - 1; i >= 0; i--) {
                Point p = points.get(i);
                path.lineTo(p.x, p.y);
            }
            canvas.drawPath(path, paint);
            canvas.drawPath(path, paintFillAlpha);
        }
    }

    public boolean init(Context appCtx, boolean useSlim) {
        if (!this.isLoaded || (this.useSlim != useSlim)) {
            loadLabel(appCtx, "labels/ppocr_keys_v1.txt");
            if (useSlim) {
                loadModel(appCtx, "models/ocr_v2_for_cpu(slim)", 4, "LITE_POWER_HIGH");
            } else {
                loadModel(appCtx, "models/ocr_v2_for_cpu", 4, "LITE_POWER_HIGH");
            }
        }
        this.isLoaded = true;
        this.useSlim = useSlim;
        Log.i(TAG, "isLoaded: " + this.isLoaded);
        return this.isLoaded;
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
            ocrResult.preprocessTime = preprocessTime;
            ocrResult.inferenceTime = inferenceTime;
            ocrResult.confidence = model.getConfidence();
            ocrResult.words = model.getLabel().trim().replace("\r", "");
            ocrResult.location = new OcrResult.RectLocation(left, top, Math.abs(right - left), Math.abs(bottom - top));
            ocrResult.bounds = new Rect(left, top, right, bottom);
            words_result.add(ocrResult);
        }
        return words_result;
    }

    public List<OcrResult> ocr(Bitmap inputImage, int cpuThreadNum) {
        this.cpuThreadNum = cpuThreadNum;
        if (inputImage == null) {
            return Collections.emptyList();
        }
        // Pre-process image, and feed input tensor with pre-processed data
        Bitmap scaleImage = Utils.resizeWithStep(inputImage, Long.valueOf(inputShape[2]).intValue(), 32);
        Date start = new Date();
        int channels = (int) inputShape[1];
        int width = scaleImage.getWidth();
        int height = scaleImage.getHeight();
        float[] inputData = new float[channels * width * height];
        if (channels == 3) {
            int[] channelIdx = null;
            if (inputColorFormat.equalsIgnoreCase("RGB")) {
                channelIdx = new int[]{0, 1, 2};
            } else if (inputColorFormat.equalsIgnoreCase("BGR")) {
                channelIdx = new int[]{2, 1, 0};
            } else {
                Log.i(TAG, "Unknown color format " + inputColorFormat + ", only RGB and BGR color format is " +
                        "supported!");
                return Collections.emptyList();
            }
            int[] channelStride = new int[]{width * height, width * height * 2};
            int[] pixels = new int[width * height];
            scaleImage.getPixels(pixels, 0, scaleImage.getWidth(), 0, 0, scaleImage.getWidth(), scaleImage.getHeight());
            for (int i = 0; i < pixels.length; i++) {
                int color = pixels[i];
                float[] rgb = new float[]{(float) red(color) / 255.0f, (float) green(color) / 255.0f,
                        (float) blue(color) / 255.0f};
                inputData[i] = (rgb[channelIdx[0]] - inputMean[0]) / inputStd[0];
                inputData[i + channelStride[0]] = (rgb[channelIdx[1]] - inputMean[1]) / inputStd[1];
                inputData[i + channelStride[1]] = (rgb[channelIdx[2]] - inputMean[2]) / inputStd[2];
            }
        } else if (channels == 1) {
            int[] pixels = new int[width * height];
            scaleImage.getPixels(pixels, 0, scaleImage.getWidth(), 0, 0, scaleImage.getWidth(), scaleImage.getHeight());
            for (int i = 0; i < pixels.length; i++) {
                int color = pixels[i];
                float gray = (float) (red(color) + green(color) + blue(color)) / 3.0f / 255.0f;
                inputData[i] = (gray - inputMean[0]) / inputStd[0];
            }
        } else {
            Log.i(TAG, "Unsupported channel size " + Integer.toString(channels) + ",  only channel 1 and 3 is " +
                    "supported!");
            return Collections.emptyList();
        }
        float[] pixels = inputData;
        Log.i(TAG, "pixels " + pixels[0] + " " + pixels[1] + " " + pixels[2] + " " + pixels[3]
                + " " + pixels[pixels.length / 2] + " " + pixels[pixels.length / 2 + 1] + " " + pixels[pixels.length - 2] + " " + pixels[pixels.length - 1]);
        Date end = new Date();
        preprocessTime = (float) (end.getTime() - start.getTime());

        // Warm up
        for (int i = 0; i < warmupIterNum; i++) {
            paddlePredictor.runImage(inputData, width, height, channels, inputImage);
        }
        warmupIterNum = 0; // do not need warm
        // Run inference
        start = new Date();
        ArrayList<OcrResultModel> results = paddlePredictor.runImage(inputData, width, height, channels, inputImage);
        end = new Date();
        inferenceTime = (float) (end.getTime() - start.getTime());
        results = postprocess(results);
        return transformData(results);
    }

}
