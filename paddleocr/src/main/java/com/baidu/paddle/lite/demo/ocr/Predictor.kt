package com.baidu.paddle.lite.demo.ocr

import android.content.Context
import android.graphics.*
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import java.io.File
import java.util.*
import kotlin.math.abs

open class Predictor {
    private var isLoaded = false
    var useSlim = true
    var warmupIterNum = 1
    var inferIterNum = 1
    var cpuThreadNum = 1
    var cpuPowerMode = "LITE_POWER_HIGH"
    var modelPath = ""
    var modelName = ""
    var inferenceTime = 0f

    @Volatile
    var outputResult = ""
    var preprocessTime = 0f
    var initSuccess = false

    protected var mPaddlePredictorNative: OCRPredictorNative? = null
    protected var paddlePredictor: OCRPredictorNative? = null

    // Only for object detection
    protected var wordLabels = Vector<String>()
    protected var inputColorFormat = "BGR"
    protected var inputShape = longArrayOf(1, 3, 960)
    protected var inputMean = floatArrayOf(0.485f, 0.456f, 0.406f)
    protected var inputStd = floatArrayOf(0.229f, 0.224f, 0.225f)
    protected var scoreThreshold = 0.1f
    var inputImage: Bitmap? = null
        set(value) {
            field = value?.copy(Bitmap.Config.ARGB_8888, true)
        }
    var outputImage: Bitmap? = null

    protected var postProcessTime = 0f
    private var retryTime = 1

    fun init(
        appCtx: Context,
        modelPath: String
    ): Boolean {
        return init(appCtx, modelPath, "labels/ppocr_keys_v1.txt")
    }

    fun init(
        appCtx: Context,
        modelPath: String,
        labelPath: String? = "labels/ppocr_keys_v1.txt"
    ): Boolean {
        if (!isLoaded) {
            loadModel(appCtx, modelPath, cpuThreadNum, cpuPowerMode)
            loadLabel(appCtx, labelPath)
            isLoaded = true
        }
        Log.i(TAG, "PaddleOCR modelLoaded: $isLoaded")
        return isLoaded
    }

    fun init(appCtx: Context, useSlim: Boolean): Boolean {
        if (!isLoaded || this.useSlim != useSlim) {
            loadLabel(appCtx, "labels/ppocr_keys_v1.txt")
            if (useSlim) {
                loadModel(appCtx, "models/ocr_v2_for_cpu(slim)", 4, "LITE_POWER_HIGH")
            } else {
                loadModel(appCtx, "models/ocr_v2_for_cpu", 4, "LITE_POWER_HIGH")
            }
        }
        isLoaded = true
        this.useSlim = useSlim
        Log.i(TAG, "isLoaded: $isLoaded")
        return isLoaded
    }

    fun init(
        appCtx: Context,
        modelPath: String,
        labelPath: String?,
        cpuThreadNum: Int,
        cpuPowerMode: String?,
        inputColorFormat: String,
        inputShape: LongArray,
        inputMean: FloatArray,
        inputStd: FloatArray,
        scoreThreshold: Float
    ): Boolean {
        if (inputShape.size != 3) {
            Log.e(TAG, "Size of input shape should be: 3")
            return false
        }
        if (inputMean.size.toLong() != inputShape[1]) {
            Log.e(
                TAG, "Size of input mean should be: " + java.lang.Long.toString(
                    inputShape[1]
                )
            )
            return false
        }
        if (inputStd.size.toLong() != inputShape[1]) {
            Log.e(
                TAG, "Size of input std should be: " + java.lang.Long.toString(
                    inputShape[1]
                )
            )
            return false
        }
        if (inputShape[0] != 1L) {
            Log.e(
                TAG,
                "Only one batch is supported in the image classification demo, you can use any batch size in " +
                        "your Apps!"
            )
            return false
        }
        if (inputShape[1] != 1L && inputShape[1] != 3L) {
            Log.e(
                TAG,
                "Only one/three channels are supported in the image classification demo, you can use any " +
                        "channel size in your Apps!"
            )
            return false
        }
        if (!inputColorFormat.equals("BGR", ignoreCase = true)) {
            Log.e(TAG, "Only  BGR color format is supported.")
            return false
        }
        val modelLoaded = init(appCtx, modelPath, labelPath)
        if (!modelLoaded) {
            return false
        }
        this.inputColorFormat = inputColorFormat
        this.inputShape = inputShape
        this.inputMean = inputMean
        this.inputStd = inputStd
        this.scoreThreshold = scoreThreshold
        return true
    }

    protected fun loadModel(
        appCtx: Context,
        modelPath: String,
        cpuThreadNum: Int,
        cpuPowerMode: String
    ): Boolean {
        // Release model if exists
        releaseModel()

        // Load model
        if (modelPath.isEmpty()) {
            Log.i(TAG, "modelPath.isEmpty() ")
            return false
        }
        var realPath = modelPath
        if (modelPath.substring(0, 1) != "/") {
            // Read model files from custom path if the first character of mode path is '/'
            // otherwise copy model to cache from assets
            realPath = appCtx.cacheDir.toString() + "/" + modelPath
            Log.i(TAG, "realPath.isEmpty() $realPath")
            Utils.copyDirectoryFromAssets(appCtx, modelPath, realPath)
        }
        if (realPath.isEmpty()) {
            Log.i(TAG, "realPath.isEmpty() ")
            return false
        }
        val config = OCRPredictorNative.Config()
        config.cpuThreadNum = cpuThreadNum
        config.detModelFilename = realPath + File.separator + "ch_ppocr_mobile_v2.0_det_opt.nb"
        config.recModelFilename = realPath + File.separator + "ch_ppocr_mobile_v2.0_rec_opt.nb"
        config.clsModelFilename = realPath + File.separator + "ch_ppocr_mobile_v2.0_cls_opt.nb"
        Log.i(
            "Predictor",
            "model path" + config.detModelFilename + " ; " + config.recModelFilename + ";" + config.clsModelFilename
        )
        config.cpuPower = cpuPowerMode
        paddlePredictor = OCRPredictorNative(config)
        this.cpuThreadNum = cpuThreadNum
        this.cpuPowerMode = cpuPowerMode
        this.modelPath = realPath
        modelName = realPath.substring(realPath.lastIndexOf("/") + 1)
        mPaddlePredictorNative = OCRPredictorNative(config)
        Log.i(TAG, "realPath $realPath")
        return true
    }

    @JavascriptInterface
    fun release() {
        if (mPaddlePredictorNative != null) {
            mPaddlePredictorNative!!.destroy()
            mPaddlePredictorNative = null
        }
        isLoaded = false
    }

    fun releaseModel() {
        if (paddlePredictor != null) {
            paddlePredictor!!.destroy()
            paddlePredictor = null
        }
        isLoaded = false
        cpuThreadNum = 4
        cpuPowerMode = "LITE_POWER_HIGH"
        modelPath = ""
        modelName = ""
    }

    protected fun loadLabel(appCtx: Context, labelPath: String?): Boolean {
        wordLabels.clear()
        wordLabels.add("black")
        // Load word labels from file
        try {
            val assetsInputStream = appCtx.assets.open(labelPath!!)
            val available = assetsInputStream.available()
            val lines = ByteArray(available)
            assetsInputStream.read(lines)
            assetsInputStream.close()
            val words = String(lines)
            val contents = words.split("\n").toTypedArray()
            for (content in contents) {
                wordLabels.add(content)
            }
            Log.i(TAG, "Word label size: " + wordLabels.size)
        } catch (e: Exception) {
            Log.e(TAG, e.message!!)
            return false
        }
        return true
    }

    fun isModelLoaded(): Boolean {
        return mPaddlePredictorNative != null && isLoaded
    }

    fun postProcessTime(): Float {
        return postProcessTime
    }

    private fun postProcess(results: ArrayList<OcrResultModel>): ArrayList<OcrResultModel> {
        for (r in results) {
            val word = StringBuffer()
            for (index in r.wordIndex) {
                if (index >= 0 && index < wordLabels.size) {
                    word.append(wordLabels[index])
                } else {
                    Log.e(TAG, "Word index is not in label list:$index")
                    word.append("×")
                }
            }
            r.label = word.toString()
        }
        return results
    }

    fun runModel(): Boolean {
        if (inputImage == null || !isModelLoaded()) {
            return false
        }

        // Pre-process image, and feed input tensor with pre-processed data
        val scaleImage = Utils.resizeWithStep(
            inputImage!!, java.lang.Long.valueOf(
                inputShape[2]
            ).toInt(), 32
        )
        var start = Date()
        val channels = inputShape[1].toInt()
        val width = scaleImage.width
        val height = scaleImage.height
        val inputData = FloatArray(channels * width * height)
        when (channels) {
            3 -> {
                val channelIdx: IntArray = if (inputColorFormat.equals("RGB", ignoreCase = true)) {
                    intArrayOf(0, 1, 2)
                } else if (inputColorFormat.equals("BGR", ignoreCase = true)) {
                    intArrayOf(2, 1, 0)
                } else {
                    Log.i(
                        TAG,
                        "Unknown color format " + inputColorFormat + ", only RGB and BGR color format is " +
                                "supported!"
                    )
                    return false
                }
                val channelStride = intArrayOf(width * height, width * height * 2)
                val pixels = IntArray(width * height)
                scaleImage.getPixels(
                    pixels,
                    0,
                    scaleImage.width,
                    0,
                    0,
                    scaleImage.width,
                    scaleImage.height
                )
                for (i in pixels.indices) {
                    val color = pixels[i]
                    val rgb = floatArrayOf(
                        Color.red(color).toFloat() / 255.0f, Color.green(color).toFloat() / 255.0f,
                        Color.blue(color).toFloat() / 255.0f
                    )
                    inputData[i] = (rgb[channelIdx[0]] - inputMean[0]) / inputStd[0]
                    inputData[i + channelStride[0]] =
                        (rgb[channelIdx[1]] - inputMean[1]) / inputStd[1]
                    inputData[i + channelStride[1]] =
                        (rgb[channelIdx[2]] - inputMean[2]) / inputStd[2]
                }
            }
            1 -> {
                val pixels = IntArray(width * height)
                scaleImage.getPixels(
                    pixels,
                    0,
                    scaleImage.width,
                    0,
                    0,
                    scaleImage.width,
                    scaleImage.height
                )
                for (i in pixels.indices) {
                    val color = pixels[i]
                    val gray =
                        (Color.red(color) + Color.green(color) + Color.blue(color)).toFloat() / 3.0f / 255.0f
                    inputData[i] = (gray - inputMean[0]) / inputStd[0]
                }
            }
            else -> {
                Log.i(
                    TAG,
                    "Unsupported channel size " + Integer.toString(channels) + ",  only channel 1 and 3 is " +
                            "supported!"
                )
                return false
            }
        }
        Log.i(
            TAG,
            "pixels " + inputData[0] + " " + inputData[1] + " " + inputData[2] + " " + inputData[3]
                    + " " + inputData[inputData.size / 2] + " " + inputData[inputData.size / 2 + 1] + " " + inputData[inputData.size - 2] + " " + inputData[inputData.size - 1]
        )
        var end = Date()
        preprocessTime = (end.time - start.time).toFloat()

        // Warm up
        for (i in 0 until warmupIterNum) {
            paddlePredictor!!.runImage(inputData, width, height, channels, inputImage)
        }
        warmupIterNum = 0 // do not need warm
        // Run inference
        start = Date()
        var results = paddlePredictor!!.runImage(inputData, width, height, channels, inputImage)
        end = Date()
        inferenceTime = (end.time - start.time) / inferIterNum.toFloat()
        results = postProcess(results)
        Log.i(
            TAG, "[stat] Preprocess Time: " + preprocessTime
                    + " ; Inference Time: " + inferenceTime + " ;Box Size " + results.size
        )
        drawResults(results)
        return true
    }

    fun isLoaded(): Boolean {
        return paddlePredictor != null && isLoaded
    }

    private fun drawResults(results: ArrayList<OcrResultModel>) {
        val outputResultSb = StringBuffer("")
        for (i in results.indices) {
            val result = results[i]
            val sb = StringBuilder("")
            sb.append(result.label)
            sb.append(" ").append(result.confidence)
            sb.append("; Points: ")
            for (p in result.points) {
                sb.append("(").append(p.x).append(",").append(p.y).append(") ")
            }
            Log.i(TAG, sb.toString()) // show LOG in Logcat panel
            outputResultSb.append(i + 1).append(": ").append(result.label).append("\n")
        }
        outputResult = outputResultSb.toString()
        outputImage = inputImage
        val canvas = Canvas(outputImage!!)
        val paintFillAlpha = Paint()
        paintFillAlpha.style = Paint.Style.FILL
        paintFillAlpha.color = Color.parseColor("#3B85F5")
        paintFillAlpha.alpha = 50
        val paint = Paint()
        paint.color = Color.parseColor("#3B85F5")
        paint.strokeWidth = 5f
        paint.style = Paint.Style.STROKE
        for (result in results) {
            val path = Path()
            val points = result.points
            path.moveTo(points[0].x.toFloat(), points[0].y.toFloat())
            for (i in points.indices.reversed()) {
                val p = points[i]
                path.lineTo(p.x.toFloat(), p.y.toFloat())
            }
            canvas.drawPath(path, paint)
            canvas.drawPath(path, paintFillAlpha)
        }
    }

    @JavascriptInterface
    fun initOcr(appCtx: Context, cpuThreadNum: Int, useSlim: Boolean) {
        loadLabel(appCtx, "labels/ppocr_keys_v1.txt")
        if (!isLoaded() || this.useSlim != useSlim) {
            if (useSlim) {
                loadModel(appCtx, "models/ocr_v2_for_cpu(slim)", cpuThreadNum, "LITE_POWER_HIGH")
            } else {
                loadModel(appCtx, "models/ocr_v2_for_cpu", cpuThreadNum, "LITE_POWER_HIGH")
            }
        }
        this.useSlim = useSlim
        Log.i(TAG, "initSuccess: " + initSuccess)
    }

    @JavascriptInterface
    fun initOcr(appCtx: Context, cpuThreadNum: Int, myModelPath: String): Boolean {
        var retryTime = 0
        while (retryTime < 3 && (!isLoaded || !checkInitSuccess())) {
            Utils.copyDirectoryFromAssets(
                appCtx,
                "models/ocr_v2_for_cpu(slim)",
                appCtx.getExternalFilesDir(null as String?)!!
                    .parent!! + File.separator + "models/ocr_v2_for_cpu"
            )
            loadLabel(appCtx, "labels/ppocr_keys_v1.txt")
            if (!isLoaded()) {
                loadModel(appCtx, myModelPath, cpuThreadNum, "LITE_POWER_HIGH")
            }
            isLoaded = true
            ++retryTime
        }
        Log.i(TAG, "第" + retryTime + "次初始化校验是否成功: " + initSuccess)
        return initSuccess
    }

    //    public synchronized List<OcrResult> runOcr(Bitmap inputImage0, int cpuThreadNum) {
    //        AtomicReference<List<OcrResult>> resultList = new AtomicReference<>(Collections.emptyList());
    //        if (Looper.getMainLooper() == Looper.myLooper()) {
    //            new Thread(() -> {
    //                resultList.set(ocr(inputImage0, cpuThreadNum));
    //            }).start();
    //        } else {
    //            resultList.set(ocr(inputImage0, cpuThreadNum));
    //        }
    //        return resultList.get();
    //    }
    fun transformData(OcrResultModelList: List<OcrResultModel>?): List<OcrResult> {
        if (OcrResultModelList == null) {
            return emptyList()
        }
        val wordsResult: MutableList<OcrResult> = ArrayList()
        for (model in OcrResultModelList) {
            val pointList = model.points
            if (pointList.isEmpty()) {
                continue
            }
            val firstPoint = pointList[0]
            var left = firstPoint.x
            var top = firstPoint.y
            var right = firstPoint.x
            var bottom = firstPoint.y
            for (p in pointList) {
                if (p.x < left) {
                    left = p.x
                }
                if (p.x > right) {
                    right = p.x
                }
                if (p.y < top) {
                    top = p.y
                }
                if (p.y > bottom) {
                    bottom = p.y
                }
            }
            val ocrResult = OcrResult(
                preprocessTime=preprocessTime,
                inferenceTime = inferenceTime,
                confidence = model.confidence,
                text = model.label!!.trim { it <= ' ' }.replace("\r", ""),
                bounds = Rect(left, top, right, bottom)
            )
            wordsResult.add(ocrResult)
        }
        wordsResult.sort()
        return wordsResult
    }

    @JavascriptInterface
    fun checkInitSuccess(): Boolean {
        if (initSuccess) return true
        val checkImgBase64 =
            "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"
        val checkingBitmap = BitmapFactory.decodeByteArray(
            Base64.decode(
                checkImgBase64,
                Base64.DEFAULT
            ), 0, Base64.decode(checkImgBase64, Base64.DEFAULT).size
        )
        val checkingResults = runOcr(checkingBitmap, 4)
        val sb = StringBuilder()
        for ((_, _, _, words) in checkingResults) {
            sb.append(words)
        }
        initSuccess = sb.toString().contains("测") || sb.toString().contains("试")
        Log.i(TAG, "第" + retryTime + "次校验\t" + "结果: " + sb.toString() + "\t是否成功: " + initSuccess)
        return initSuccess || retryTime++ >= 5
    }

    @JavascriptInterface
    fun runOcr(inputImage: Bitmap?, cpuThreadNum: Int): List<OcrResult> {
        var resultList: ArrayList<OcrResultModel>
        this.cpuThreadNum = cpuThreadNum
        if (inputImage == null) {
            return emptyList()
        }
        // Pre-process image, and feed input tensor with pre-processed data
        val scaleImage = Utils.resizeWithStep(
            inputImage, java.lang.Long.valueOf(
                inputShape[2]
            ).toInt(), 32
        )
        var start = Date()
        val channels = inputShape[1].toInt()
        val width = scaleImage.width
        val height = scaleImage.height
        val inputData = FloatArray(channels * width * height)
        when (channels) {
            3 -> {
                val channelIdx: IntArray = if (inputColorFormat.equals("RGB", ignoreCase = true)) {
                    intArrayOf(0, 1, 2)
                } else if (inputColorFormat.equals("BGR", ignoreCase = true)) {
                    intArrayOf(2, 1, 0)
                } else {
                    Log.i(
                        TAG,
                        "Unknown color format " + inputColorFormat + ", only RGB and BGR color format is " +
                                "supported!"
                    )
                    return emptyList()
                }
                val channelStride = intArrayOf(width * height, width * height * 2)
                val pixels = IntArray(width * height)
                scaleImage.getPixels(
                    pixels,
                    0,
                    scaleImage.width,
                    0,
                    0,
                    scaleImage.width,
                    scaleImage.height
                )
                for (i in pixels.indices) {
                    val color = pixels[i]
                    val rgb = floatArrayOf(
                        Color.red(color).toFloat() / 255.0f, Color.green(color).toFloat() / 255.0f,
                        Color.blue(color).toFloat() / 255.0f
                    )
                    inputData[i] = (rgb[channelIdx[0]] - inputMean[0]) / inputStd[0]
                    inputData[i + channelStride[0]] =
                        (rgb[channelIdx[1]] - inputMean[1]) / inputStd[1]
                    inputData[i + channelStride[1]] =
                        (rgb[channelIdx[2]] - inputMean[2]) / inputStd[2]
                }
            }
            1 -> {
                val pixels = IntArray(width * height)
                scaleImage.getPixels(
                    pixels,
                    0,
                    scaleImage.width,
                    0,
                    0,
                    scaleImage.width,
                    scaleImage.height
                )
                for (i in pixels.indices) {
                    val color = pixels[i]
                    val gray =
                        (Color.red(color) + Color.green(color) + Color.blue(color)).toFloat() / 3.0f / 255.0f
                    inputData[i] = (gray - inputMean[0]) / inputStd[0]
                }
            }
            else -> {
                Log.i(
                    TAG,
                    "Unsupported channel size " + Integer.toString(channels) + ",  only channel 1 and 3 is " +
                            "supported!"
                )
                return emptyList()
            }
        }
        Log.i(
            TAG,
            "pixels " + inputData[0] + " " + inputData[1] + " " + inputData[2] + " " + inputData[3]
                    + " " + inputData[inputData.size / 2] + " " + inputData[inputData.size / 2 + 1] + " " + inputData[inputData.size - 2] + " " + inputData[inputData.size - 1]
        )
        var end = Date()
        preprocessTime = (end.time - start.time).toFloat()

        // Warm up
        for (i in 0 until warmupIterNum) {
            paddlePredictor!!.runImage(inputData, width, height, channels, inputImage)
        }
        warmupIterNum = 0 // do not need warm
        // Run inference
        start = Date()
        resultList = paddlePredictor!!.runImage(inputData, width, height, channels, inputImage)
        end = Date()
        inferenceTime = (end.time - start.time).toFloat()
        resultList = postProcess(resultList)
        return transformData(resultList)
    }

    @JavascriptInterface
    fun ocr(
        appCtx: Context,
        inputImage: Bitmap?,
        cpuThreadNum: Int,
        useSlim: Boolean
    ): List<OcrResult> {
        while (paddlePredictor == null || !checkInitSuccess()) {
            initOcr(appCtx, cpuThreadNum, useSlim)
        }
        return runOcr(inputImage, cpuThreadNum)
    }

    @JavascriptInterface
    fun ocrText(
        appCtx: Context,
        bitmap: Bitmap?,
        cpuThreadNum: Int,
        useSlim: Boolean
    ): Array<String?> {
        val wordsResult = ocr(appCtx, bitmap, cpuThreadNum, useSlim)
        val outputResult = arrayOfNulls<String>(wordsResult.size)
        for (i in wordsResult.indices) {
            outputResult[i] = wordsResult[i].text
            Log.i("outputResult", outputResult[i]!!) // show LOG in Logcat panel
        }
        return outputResult
    }

    @JavascriptInterface
    fun releaseOcr(): Boolean {
        releaseModel()
        initSuccess = false
        return true
    }

    companion object {
        private val TAG = Predictor::class.java.simpleName
    }
}