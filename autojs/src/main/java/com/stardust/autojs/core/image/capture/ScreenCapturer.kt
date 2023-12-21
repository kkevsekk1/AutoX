package com.stardust.autojs.core.image.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.view.OrientationEventListener
import com.stardust.util.ScreenMetrics
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by Stardust on 2017/5/17.
 * Improvedd by TonyJiangWJ(https://github.com/TonyJiangWJ).
 * From [TonyJiangWJ/Auto.js](https://github.com/TonyJiangWJ/Auto.js)
 */
class ScreenCapturer(
    private val mContext: Context,
    mData: Intent,
    orientation: Int = 0,
    private val screenDensity: Int = ScreenMetrics.getDeviceScreenDensity(),
    mHandler: Handler = Handler(Looper.getMainLooper())
) {
    private val mProjectionManager: MediaProjectionManager =
        mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var mMediaProjection: MediaProjection =
        mProjectionManager.getMediaProjection(Activity.RESULT_OK, mData)
    private var mVirtualDisplay: VirtualDisplay
    private var mImageReader: ImageReader

    private val mCachedImage = AtomicReference<Image?>()

    @Volatile
    var available = true

    private var mOrientation = -1
    private var mDetectedOrientation = 0


    init {
        mMediaProjection.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                available = false
                release()
            }
        }, mHandler)
        val screenHeight = ScreenMetrics.getOrientationAwareScreenHeight(orientation)
        val screenWidth = ScreenMetrics.getOrientationAwareScreenWidth(orientation)
        mImageReader = createImageReader(screenWidth, screenHeight)
        mVirtualDisplay = createVirtualDisplay(screenWidth, screenHeight, screenDensity)
        setOrientation(orientation)

    }

    private fun createImageReader(width: Int, height: Int): ImageReader {
        return ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 3)
    }

    private fun createVirtualDisplay(width: Int, height: Int, screenDensity: Int): VirtualDisplay {
        return mMediaProjection.createVirtualDisplay(
            LOG_TAG,
            width, height, screenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader.surface, null, null
        )
    }

    fun setOrientation(orientation: Int) {
        if (mOrientation == orientation) return
        mOrientation = orientation
        mDetectedOrientation = mContext.resources.configuration.orientation
        refreshVirtualDisplay(if (mOrientation == ORIENTATION_AUTO) mDetectedOrientation else mOrientation)
    }

    private fun refreshVirtualDisplay(orientation: Int) = synchronized(this) {
        mImageReader.close()
        mVirtualDisplay.release()
        val screenHeight = ScreenMetrics.getOrientationAwareScreenHeight(orientation)
        val screenWidth = ScreenMetrics.getOrientationAwareScreenWidth(orientation)
        mImageReader = createImageReader(screenWidth, screenHeight)
        mVirtualDisplay = createVirtualDisplay(screenWidth, screenHeight, screenDensity)
    }

    fun capture(): Image = synchronized(this) {
        val newImage = mImageReader.acquireLatestImage()

        val oldImage = mCachedImage.get()
        if (newImage == null) {
            if (oldImage != null) {
                return oldImage
            }
            Thread.sleep(100)
            return capture()
        }
        mCachedImage.set(newImage)
        oldImage?.close()
        return newImage
    }

    fun release() = synchronized(this){
        mMediaProjection.stop()
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