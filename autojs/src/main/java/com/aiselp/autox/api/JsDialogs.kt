package com.aiselp.autox.api

import android.content.Context
import androidx.compose.ui.window.SecureFlagPolicy
import com.aiselp.autox.activity.AppDialogActivity
import com.aiselp.autox.api.ui.ComposeElement
import com.aiselp.autox.engine.EventLoopQueue
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.reference.V8ValueFunction
import com.caoccao.javet.values.reference.V8ValueObject
import kotlinx.coroutines.CoroutineScope

class JsDialogs(
    val eventLoopQueue: EventLoopQueue,
    val context: Context,
    val scope: CoroutineScope
) : NativeApi {
    override val moduleId: String = ID
    override fun install(v8Runtime: V8Runtime, global: V8ValueObject): NativeApi.BindingMode {
        return NativeApi.BindingMode.PROXY
    }

    override fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
    }

    @V8Function
    fun showDialog(
        element: ComposeElement,
        listener: V8ValueObject?
    ): AppDialogActivity.AppDialogBuilder {
        val securePolicy = listener?.getString("securePolicy")
        val builder = object : AppDialogActivity.AppDialogBuilder(element, scope) {
            val dismissListener = listener?.get<V8ValueFunction>("onDismiss")?.let {
                eventLoopQueue.createV8Callback(it)
            }
            override val dismissOnBackPress: Boolean =
                listener?.getBoolean("dismissOnBackPress") ?: super.dismissOnBackPress
            override val dismissOnClickOutside: Boolean =
                listener?.getBoolean("dismissOnClickOutside") ?: super.dismissOnClickOutside
            override val securePolicy: SecureFlagPolicy = when (securePolicy) {
                "SecureOn" -> SecureFlagPolicy.SecureOn
                "SecureOff" -> SecureFlagPolicy.SecureOff
                else -> super.securePolicy
            }

            override fun onDismiss() {
                dismissListener?.invoke()
                dismissListener?.close()
            }
        }

        AppDialogActivity.showDialog(context, builder, scope)
        return builder
    }

    companion object {
        const val ID = "dialogs"
    }
}