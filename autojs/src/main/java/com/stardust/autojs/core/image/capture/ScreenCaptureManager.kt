package com.stardust.autojs.core.image.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import com.stardust.app.OnActivityResultDelegate
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.CancellationException

class ScreenCaptureManager : ScreenCaptureRequester {
    @Volatile
    override var screenCapture: ScreenCapturer? = null
    private var mediaProjection: MediaProjection? = null

    override suspend fun requestScreenCapture(context: Context, orientation: Int) {
        if (screenCapture?.available == true) {
            screenCapture?.setOrientation(orientation, context)
            return
        }
        context.startService(Intent(context, CaptureForegroundService::class.java))
        val result = if (context is OnActivityResultDelegate.DelegateHost && context is Activity) {
            ScreenCaptureRequester.ActivityScreenCaptureRequester(
                context.onActivityResultDelegateMediator, context
            ).request()
        } else {
            val result = CompletableDeferred<Intent>()
            ScreenCaptureRequestActivity.request(context) { data ->
                if (data != null) {
                    result.complete(data)
                } else result.cancel(CancellationException("data is null"))
            }
            result.await()
        }
        mediaProjection =
            (context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).getMediaProjection(
                Activity.RESULT_OK,
                result
            )
        CaptureForegroundService.mediaProjection = mediaProjection
        screenCapture = ScreenCapturer(mediaProjection!!, orientation)
    }

    override fun recycle() {
        screenCapture?.release()
        screenCapture = null
        mediaProjection?.stop()
        mediaProjection = null
    }
}