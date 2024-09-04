package com.stardust.autojs.core.ui.dialog

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Looper
import android.view.WindowManager
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.stardust.autojs.runtime.ScriptBridges
import com.stardust.autojs.runtime.ScriptRuntime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Created by Stardust on 2017/5/8.
 */
open class BlockedMaterialDialog protected constructor(builder: MaterialDialog.Builder?) :
    MaterialDialog(builder) {
    override fun show() {
        if (!isActivityContext(context)) {
            val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            window!!.setType(type)
        }
        super.show()
    }

    private fun isActivityContext(context: Context?): Boolean {
        if (context == null) return false
        if (context is Activity) {
            return !context.isFinishing
        }
        if (context is ContextWrapper) {
            return isActivityContext(context.baseContext)
        }
        return false
    }


    class Builder(context: Context, runtime: ScriptRuntime, private val mCallback: Any?) :
        MaterialDialog.Builder(context) {
        private var result = CompletableDeferred<Any?>()
        private val mScriptBridges: ScriptBridges = runtime.bridges


        fun input(
            hint: CharSequence?,
            prefill: CharSequence?,
            allowEmptyInput: Boolean
        ): MaterialDialog.Builder {
            super.input(hint, prefill, allowEmptyInput) { _, input ->
                setAndNotify(input.toString())
            }
            cancelListener { setAndNotify(null) }
            return this
        }

        private fun setAndNotify(r: Any?) {
            if (result.isCompleted) return
            result.complete(r)
            if (mCallback != null) {
                mScriptBridges.callFunction(mCallback, null, arrayOf(r))
            }
        }

        fun alert(): Builder {
            dismissListener { setAndNotify(null) }
            onAny { _, _ -> setAndNotify(null) }
            return this
        }

        fun confirm(): Builder {
            dismissListener { setAndNotify(false) }
            onAny { _, which: DialogAction ->
                if (which == DialogAction.POSITIVE) {
                    setAndNotify(true)
                } else {
                    setAndNotify(false)
                }
            }
            return this
        }

        fun itemsCallback(): MaterialDialog.Builder {
            dismissListener { setAndNotify(-1) }
            super.itemsCallback { _, _, position, _ ->
                setAndNotify(position)
            }
            return this
        }

        fun itemsCallbackMultiChoice(selectedIndices: Array<Int?>?): MaterialDialog.Builder {
            dismissListener { setAndNotify(IntArray(0)) }
            super.itemsCallbackMultiChoice(selectedIndices) { _, which, _ ->
                setAndNotify(which.toIntArray())
                true
            }
            return this
        }

        fun itemsCallbackSingleChoice(selectedIndex: Int): MaterialDialog.Builder {
            dismissListener { setAndNotify(-1) }
            super.itemsCallbackSingleChoice(selectedIndex) { _, _, which, _ ->
                setAndNotify(which)
                true
            }
            return this
        }


        @OptIn(DelicateCoroutinesApi::class)
        fun showAndGet(): Any? {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                super.show()
                return null
            }
            GlobalScope.launch(Dispatchers.Main) { show() }
            return runBlocking { result.await() }
        }

        override fun build(): MaterialDialog {
            return BlockedMaterialDialog(this)
        }
    }
}
