package com.stardust.autojs.core.image.capture

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.stardust.util.IntentExtras
import kotlinx.coroutines.delay

/**
 * Created by Stardust on 2017/5/22.
 */
class ScreenCaptureRequestActivity : AppCompatActivity() {
    fun interface Callback {
        fun onResult(data: Intent?)
    }

    private var mCallback: Callback? = null
    private var extraId = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val extras = IntentExtras.fromIntentAndRelease(intent)
        if (extras == null) {
            finish()
            return
        }
        extraId = extras.id
        mCallback = extras.get("callback")
        if (mCallback == null) {
            finish()
            return
        }

        setContent {
            val requester = rememberLauncherForActivityResult(ScreenCaptureRequester()) {
                mCallback?.onResult(it)
                finish()
            }
            val context = LocalContext.current
            LaunchedEffect(key1 = Unit) {
                delay(10)
                requester.launch(context)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        IntentExtras.fromIdAndRelease(extraId)
        mCallback = null
    }

    class ScreenCaptureRequester : ActivityResultContract<Context, Intent?>() {
        override fun createIntent(context: Context, input: Context): Intent {
            return (input.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager).createScreenCaptureIntent()
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Intent? {
            return intent
        }
    }

    companion object {
        fun request(context: Context, callback: Callback) {
            val intent = Intent(context, ScreenCaptureRequestActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            IntentExtras.newExtras()
                .put("callback", callback)
                .putInIntent(intent)
            context.startActivity(intent)
        }
    }
}