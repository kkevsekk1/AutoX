package com.aiselp.autojs.codeeditor.web.annotation

@Target(AnnotationTarget.FUNCTION)
annotation class WebFunction(
    val name: String = "null"
)
