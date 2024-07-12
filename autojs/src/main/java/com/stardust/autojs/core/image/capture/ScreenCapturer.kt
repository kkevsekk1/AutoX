package com.stardust.autojs.core.image.capture

import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.stardust.autojs.core.image.ImageWrapper
import com.stardust.util.ScreenMetrics
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by Stardust on 2017/5/17.
 * Improvedd by TonyJiangWJ(https://github.com/TonyJiangWJ).
 * From [TonyJiangWJ/Auto.js](https://github.com/TonyJiangWJ/Auto.js)
 */
class ScreenCapturer(
    private val mediaProjection: MediaProjection,
    orientation: Int = 0,
    private val screenDensity: Int = ScreenMetrics.getDeviceScreenDensity(),
    mHandler: Handler = Handler(Looper.getMainLooper())
) {
    private var mVirtualDisplay: VirtualDisplay
    private var mImageReader: ImageReader

    private val mCachedImage = AtomicReference<Image?>()
    private val mCachedImageWrapper = AtomicReference<ImageWrapper?>()

    @Volatile
    var available = true

    private var mDetectedOrientation = 0


    init {
        val screenHeight = ScreenMetrics.getOrientationAwareScreenHeight(orientation)
        val screenWidth = ScreenMetrics.getOrientationAwareScreenWidth(orientation)
        mImageReader = createImageReader(screenWidth, screenHeight)
        mediaProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                available = false
                release()
            }
        }, mHandler)
        mVirtualDisplay = createVirtualDisplay(screenWidth, screenHeight, screenDensity)
    }

    private fun createImageReader(width: Int, height: Int): ImageReader {
        return ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
    }

    private fun createVirtualDisplay(width: Int, height: Int, screenDensity: Int): VirtualDisplay {
        return mediaProjection.createVirtualDisplay(
            LOG_TAG,
            width, height, screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.surface, null, null
        )
    }

    fun setOrientation(orientation: Int, context: Context) {
        mDetectedOrientation = context.resources.configuration.orientation
        refreshVirtualDisplay(if (orientation == ORIENTATION_AUTO) mDetectedOrientation else orientation)
    }

    private fun refreshVirtualDisplay(orientation: Int) = synchronized(this) {
        mImageReader.close()
        mCachedImage.set(null)
        mCachedImageWrapper.set(null)
        val screenHeight = ScreenMetrics.getOrientationAwareScreenHeight(orientation)
        val screenWidth = ScreenMetrics.getOrientationAwareScreenWidth(orientation)
        mImageReader = createImageReader(screenWidth, screenHeight)
        mVirtualDisplay.surface = mImageReader.surface
        mVirtualDisplay.resize(screenWidth, screenHeight, screenDensity)
    }

    fun capture(): Image? = synchronized(this) {
        if (!available) throw Exception("ScreenCapturer is not available")
        val newImage = mImageReader.acquireLatestImage()
        if (newImage != null) {
            mCachedImage.getAndSet(newImage)?.close()
        }
        return newImage
    }

    suspend fun captureImageWrapper(): ImageWrapper = coroutineScope {
        var image = capture()
        val imageWrapper = mCachedImageWrapper.get()
        if (image == null && imageWrapper != null) {
            Log.i(LOG_TAG, "Using cached image")
            return@coroutineScope imageWrapper.clone()
        }
        //在缓存图像均不可用的情况下等待2秒取得截图，否则抛出错误
        val newImage = image ?: runCatching {
            withTimeout(2000) {
                while (image == null) {
                    delay(200)
                    image = capture()
                }
                yield()
                return@withTimeout image!!
            }
        }.getOrElse {
            it.printStackTrace()
            available = false
            throw Exception("ScreenCapturer timeout")
        }
        val newImageWrapper = ImageWrapper.ofImage(newImage)
        mCachedImageWrapper.set(newImageWrapper)
        return@coroutineScope newImageWrapper.clone() ?: throw Exception("Not available yet ImageWrapper")
    }

    fun release() = synchronized(this) {
        available = false
        mVirtualDisplay.release()
        mImageReader.close()
        mCachedImage.getAndSet(null)?.close()
    }

    @Throws(Throwable::class)
    protected fun finalize() {
        release()
    }

    companion object {
        @JvmStatic
        val ORIENTATION_AUTO = Configuration.ORIENTATION_UNDEFINED

        @JvmStatic
        val ORIENTATION_LANDSCAPE = Configuration.ORIENTATION_LANDSCAPE

        @JvmStatic
        val ORIENTATION_PORTRAIT = Configuration.ORIENTATION_PORTRAIT
        private const val LOG_TAG = "ScreenCapturer"
    }
}