package com.aiselp.autox.api.ui

data class ScriptActivityBuilder(
    val element: ComposeElement,
    val activityEventDelegate: ActivityEventDelegate? = null
)
