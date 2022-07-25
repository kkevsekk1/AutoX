package org.autojs.autojs.ui.util

import android.content.Context
import android.content.res.Configuration

fun Context.isSystemNightMode(): Boolean {
    val configuration = this.resources.configuration
    return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}

