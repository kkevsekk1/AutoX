package com.stardust.autojs.runtime.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Looper
import android.util.Base64
import android.view.Gravity
import com.stardust.autojs.annotation.ScriptVariable
import com.stardust.autojs.core.image.ColorFinder
import com.stardust.autojs.core.image.ImageWrapper
import com.stardust.autojs.core.image.TemplateMatching
import com.stardust.autojs.core.image.capture.ScreenCaptureRequester
import com.stardust.autojs.core.opencv.Mat
import com.stardust.autojs.core.opencv.OpenCVHelper
import com.stardust.autojs.core.ui.inflater.util.Drawables
import com.stardust.autojs.runtime.ScriptRuntime
import com.stardust.pio.UncheckedIOException
import com.stardust.util.ScreenMetrics
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Created by Stardust on 2017/5/20.
 */
class Images(
    private val mContext: Context,
    private val mScriptRuntime: ScriptRuntime,
    private val mScreenCaptureRequester: ScreenCaptureRequester
) {
    private val mScreenMetrics: ScreenMetrics = mScriptRuntime.screenMetrics

    @Volatile
    private var mOpenCvInitialized = false

    @ScriptVariable
    val colorFinder: ColorFinder = ColorFinder(mScreenMetrics)

    fun requestScreenCapture(orientation: Int): Boolean = runBlocking {
        return@runBlocking runCatching {
            mScreenCaptureRequester.requestScreenCapture(
                mContext, orientation
            )
            captureScreen()
        }.isSuccess
    }
    fun stopScreenCapturer(){
        mScreenCaptureRequester.recycle()
    }

    @Synchronized
    fun captureScreen(): ImageWrapper {
        val screenCapture = mScreenCaptureRequester.screenCapture
        checkNotNull(screenCapture) { SecurityException("No screen capture permission") }
        return runBlocking {
            screenCapture.captureImageWrapper()
        }
    }

    fun captureScreen(path: String): Boolean {
        val rpath = mScriptRuntime.files.path(path)
        val image = captureScreen()
        image.saveTo(rpath)
        return true
    }

    fun copy(image: ImageWrapper): ImageWrapper {
        return image.clone()
    }

    @Throws(IOException::class)
    fun save(image: ImageWrapper, path: String?, format: String, quality: Int): Boolean {
        val compressFormat = parseImageFormat(format)
            ?: throw IllegalArgumentException("unknown format $format")
        val bitmap = image.bitmap
        val outputStream = FileOutputStream(mScriptRuntime.files.path(path))
        return outputStream.use { out ->
            val compress = bitmap.compress(compressFormat, quality, out)
            out.flush()
            compress
        }
    }

    fun rotate(img: ImageWrapper, x: Float, y: Float, degree: Float): ImageWrapper {
        val matrix = Matrix()
        matrix.postRotate(degree, x, y)
        return ImageWrapper.ofBitmap(
            Bitmap.createBitmap(
                img.bitmap,
                0,
                0,
                img.width,
                img.height,
                matrix,
                true
            )
        )
    }

    fun clip(img: ImageWrapper, x: Int, y: Int, w: Int, h: Int): ImageWrapper? {
        return ImageWrapper.ofBitmap(Bitmap.createBitmap(img.bitmap, x, y, w, h))
    }

    fun read(path: String): ImageWrapper? {
        val bitmap = BitmapFactory.decodeFile(mScriptRuntime.files.path(path))
        return ImageWrapper.ofBitmap(bitmap)
    }

    fun fromBase64(data: String): ImageWrapper? {
        return ImageWrapper.ofBitmap(Drawables.loadBase64Data(data))
    }

    fun toBase64(wrapper: ImageWrapper, format: String, quality: Int): String {
        return Base64.encodeToString(toBytes(wrapper, format, quality), Base64.NO_WRAP)
    }

    fun toBytes(wrapper: ImageWrapper, format: String, quality: Int): ByteArray {
        val compressFormat = parseImageFormat(format)
            ?: throw IllegalArgumentException("unknown format $format")
        val bitmap = wrapper.bitmap
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(compressFormat, quality, outputStream)
        return outputStream.toByteArray()
    }

    fun fromBytes(bytes: ByteArray): ImageWrapper {
        return ImageWrapper.ofBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
    }

    private fun parseImageFormat(format: String): CompressFormat? {
        when (format) {
            "png" -> return CompressFormat.PNG
            "jpeg", "jpg" -> return CompressFormat.JPEG
            "webp" -> return CompressFormat.WEBP
        }
        return null
    }

    fun load(src: String): ImageWrapper? {
        return try {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            ImageWrapper.ofBitmap(bitmap)
        } catch (e: IOException) {
            null
        }
    }

    fun releaseScreenCapturer() {
        //mScreenCapturer?.release()
    }

    @JvmOverloads
    fun findImage(
        image: ImageWrapper?,
        template: ImageWrapper?,
        threshold: Float = 0.9f,
        rect: Rect? = null
    ): Point? {
        return findImage(image, template, 0.7f, threshold, rect, TemplateMatching.MAX_LEVEL_AUTO)
    }

    fun findImage(
        image: ImageWrapper?,
        template: ImageWrapper?,
        weakThreshold: Float,
        threshold: Float,
        rect: Rect?,
        maxLevel: Int
    ): Point? {
        initOpenCvIfNeeded()
        if (image == null) throw NullPointerException("image = null")
        if (template == null) throw NullPointerException("template = null")
        var src = image.mat
        if (rect != null) {
            src = Mat(src, rect)
        }
        val point = TemplateMatching.fastTemplateMatching(
            src, template.mat, TemplateMatching.MATCHING_METHOD_DEFAULT,
            weakThreshold, threshold, maxLevel
        )
        if (point != null) {
            if (rect != null) {
                point.x += rect.x.toDouble()
                point.y += rect.y.toDouble()
            }
            point.x = mScreenMetrics.scaleX(point.x.toInt()).toDouble()
            point.y = mScreenMetrics.scaleX(point.y.toInt()).toDouble()
        }
        if (src !== image.mat) {
            OpenCVHelper.release(src)
        }
        return point
    }

    fun matchTemplate(
        image: ImageWrapper?,
        template: ImageWrapper?,
        weakThreshold: Float,
        threshold: Float,
        rect: Rect?,
        maxLevel: Int,
        limit: Int
    ): List<TemplateMatching.Match> {
        initOpenCvIfNeeded()
        if (image == null) throw NullPointerException("image = null")
        if (template == null) throw NullPointerException("template = null")
        var src = image.mat
        if (rect != null) {
            src = Mat(src, rect)
        }
        val result = TemplateMatching.fastTemplateMatching(
            src, template.mat, Imgproc.TM_CCOEFF_NORMED,
            weakThreshold, threshold, maxLevel, limit
        )
        for (match in result) {
            val point = match.point
            if (rect != null) {
                point.x += rect.x.toDouble()
                point.y += rect.y.toDouble()
            }
            point.x = mScreenMetrics.scaleX(point.x.toInt()).toDouble()
            point.y = mScreenMetrics.scaleX(point.y.toInt()).toDouble()
        }
        if (src !== image.mat) {
            OpenCVHelper.release(src)
        }
        return result
    }

    fun newMat(): Mat {
        return Mat()
    }

    fun newMat(mat: Mat?, roi: Rect?): Mat {
        return Mat(mat, roi)
    }

    fun initOpenCvIfNeeded() {
        if (mOpenCvInitialized || OpenCVHelper.isInitialized()) {
            return
        }
        val currentActivity = mScriptRuntime.app.currentActivity
        val context = currentActivity ?: mContext
        mScriptRuntime.console.info("opencv initializing")
        if (Looper.myLooper() == Looper.getMainLooper()) {
            OpenCVHelper.initIfNeeded(context) {
                mOpenCvInitialized = true
                mScriptRuntime.console.info("opencv initialized")
            }
        } else {
            runBlocking {
                val result = Job()
                launch {
                    OpenCVHelper.initIfNeeded(context) {
                        result.complete()
                    }
                }
                result.join()
                mOpenCvInitialized = true
                mScriptRuntime.console.info("opencv initialized")
            }
        }
    }


    fun pixel(image: ImageWrapper?, x: Int, y: Int): Int {
        if (image == null) {
            throw NullPointerException("image = null")
        }
        return image.pixel(x, y)
    }

    fun concat(img1: ImageWrapper, img2: ImageWrapper, direction: Int): ImageWrapper {
        var img1 = img1
        var img2 = img2
        require(
            listOf(
                Gravity.LEFT,
                Gravity.RIGHT,
                Gravity.TOP,
                Gravity.BOTTOM
            ).contains(direction)
        ) { "unknown direction $direction" }
        val width: Int
        val height: Int
        if (direction == Gravity.LEFT || direction == Gravity.TOP) {
            val tmp = img1
            img1 = img2
            img2 = tmp
        }
        if (direction == Gravity.LEFT || direction == Gravity.RIGHT) {
            width = img1.width + img2.width
            height = Math.max(img1.height, img2.height)
        } else {
            width = Math.max(img1.width, img2.height)
            height = img1.height + img2.height
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()
        if (direction == Gravity.LEFT || direction == Gravity.RIGHT) {
            canvas.drawBitmap(img1.bitmap, 0f, ((height - img1.height) / 2).toFloat(), paint)
            canvas.drawBitmap(
                img2.bitmap,
                img1.width.toFloat(),
                ((height - img2.height) / 2).toFloat(),
                paint
            )
        } else {
            canvas.drawBitmap(img1.bitmap, ((width - img1.width) / 2).toFloat(), 0f, paint)
            canvas.drawBitmap(
                img2.bitmap,
                ((width - img2.width) / 2).toFloat(),
                img1.height.toFloat(),
                paint
            )
        }
        return ImageWrapper.ofBitmap(bitmap)
    }

    fun saveBitmap(bitmap: Bitmap, path: String?) {
        try {
            bitmap.compress(CompressFormat.PNG, 100, FileOutputStream(path))
        } catch (e: FileNotFoundException) {
            throw UncheckedIOException(e)
        }
    }

    fun scaleBitmap(origin: Bitmap?, newWidth: Int, newHeight: Int): Bitmap? {
        if (origin == null) {
            return null
        }
        val height = origin.height
        val width = origin.width
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        return Bitmap.createBitmap(origin, 0, 0, width, height, matrix, false)
    }

}
