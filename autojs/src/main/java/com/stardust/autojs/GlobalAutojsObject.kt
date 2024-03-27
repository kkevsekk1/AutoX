package com.stardust.autojs

import android.app.Application
import com.stardust.autojs.core.console.GlobalConsole
import com.stardust.autojs.runtime.api.AppUtils

class GlobalAutojsObject private constructor(application: Application) : AutoJs(application) {
    var accessibilityServiceEntry: AccessibilityServiceEntry? = null
    override fun ensureAccessibilityServiceEnabled() {
        TODO("Not yet implemented")
    }

    override fun waitForAccessibilityServiceEnabled() {
        TODO("Not yet implemented")
    }

    interface AccessibilityServiceEntry {
        fun ensureAccessibilityServiceEnabled()
        fun waitForAccessibilityServiceEnabled()
    }
    class Builder(var application: Application? = null){
        var appUtils: AppUtils? = null
        var globalConsole: GlobalConsole? = null
        var accessibilityServiceEntry: AccessibilityServiceEntry? = null

    }
}