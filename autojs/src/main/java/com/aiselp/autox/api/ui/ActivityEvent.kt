package com.aiselp.autox.api.ui

import com.aiselp.autox.engine.EventLoopQueue

enum class ActivityEvent(val value: String) {
    ON_CREATE("onCreate"),
    ON_START("onStart"),
    ON_RESUME("onResume"),
    ON_PAUSE("onPause"),
    ON_STOP("onStop"),
    ON_DESTROY("onDestroy"),
    ON_ACTIVITY_RESULT("onActivityResult"),
    ON_BACK_PRESSED("onBackPressed"),
    ON_NEW_INTENT("onNewIntent"),
    ON_RECREATE("onRecreate"),
    ON_SAVE_INSTANCE_STATE("onSaveInstanceState"),
    ON_CONFIGURATION_CHANGED("onConfigurationChanged"),
}

open class ActivityEventDelegate(private val v8Callback: EventLoopQueue.V8Callback?) {
    open fun emit(event: ActivityEvent, vararg args: Any?) {
        v8Callback?.invoke(event.value, *args)
    }
}