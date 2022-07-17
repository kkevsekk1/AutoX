package org.autojs.autojs.ui.widget

import android.view.View
import android.view.ViewGroup

fun View.fillMaxSize(): View {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )
    return this
}