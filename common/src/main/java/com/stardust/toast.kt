package com.stardust

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import android.widget.Toast
import java.lang.ref.WeakReference

private var toast: WeakReference<Toast>? = null

fun toast(context: Context, @StringRes resId: Int, isLongToast: Boolean = false) {
    toast(context, context.getString(resId), isLongToast)
}

fun toast(context: Context, text: String?, isLongToast: Boolean = false) {
    Log.d("toast: ", "toast is null: ${toast?.get() == null}")
    toast?.get()?.cancel()//
    toast = WeakReference(
        Toast.makeText(
            context.applicationContext,
            text,
            if (isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        )
    )
    toast?.get()?.show()
}