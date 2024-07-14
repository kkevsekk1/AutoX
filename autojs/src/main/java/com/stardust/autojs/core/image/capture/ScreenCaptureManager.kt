package com.stardust.autojs.core.image.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import com.stardust.app.OnActivityResultDelegate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
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
        val result = if (context is OnActivityResultDelegate.DelegateHost && context is Activity) {
            ScreenCaptureRequester.ActivityScreenCaptureRequester(
                context.onActivityResultDelegateMediator,
                context
            ).request()
        } else {
            coroutineScope {
                val result = CompletableDeferred<Intent>()
                ScreenCaptureRequestActivity.request(
                    context,
                    object : ScreenCaptureRequestActivity.Callback {
                        override fun onResult(data: Intent?) {
                            if (data != null) {
                                result.complete(data)
                            } else {
                                result.cancel(CancellationException("data is null"))
                            }
                        }
                    }
                )
                result.await()
            }
        }
        context.startService(Intent(context, CaptureForegroundService::class.java))
        delay(50)
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
