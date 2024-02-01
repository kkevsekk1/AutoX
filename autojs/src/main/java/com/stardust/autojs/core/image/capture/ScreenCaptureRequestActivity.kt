package com.stardust.autojs.core.image.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.stardust.app.OnActivityResultDelegate
import com.stardust.autojs.core.image.capture.ScreenCaptureRequester.ActivityScreenCaptureRequester
import com.stardust.util.IntentExtras
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Created by Stardust on 2017/5/22.
 */
class ScreenCaptureRequestActivity : Activity() {
    interface Callback {
        fun onResult(data: Intent?)
    }

    private val mOnActivityResultDelegateMediator = OnActivityResultDelegate.Mediator()
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

        MainScope().launch {
            val screenCaptureRequester = ActivityScreenCaptureRequester(
                mOnActivityResultDelegateMediator,
                this@ScreenCaptureRequestActivity
            )
            val intent = try {
                screenCaptureRequester.request()
            } catch (e: Exception) {
                null
            }
            mCallback?.onResult(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        IntentExtras.fromIdAndRelease(extraId)
        mCallback = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        mOnActivityResultDelegateMediator.onActivityResult(requestCode, resultCode, data)
        IntentExtras.fromIdAndRelease(extraId)
        finish()
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