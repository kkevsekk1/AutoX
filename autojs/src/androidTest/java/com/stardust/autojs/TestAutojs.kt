package com.stardust.autojs

import android.app.Application
import android.content.Context

class TestAutojs(application: Application) : AutoJs(application) {

    override fun ensureAccessibilityServiceEnabled() {

    }

    override fun waitForAccessibilityServiceEnabled() {

    }

    companion object {
        fun init(context: Context){
            instance = TestAutojs(context.applicationContext as Application)
        }
    }
}