package com.stardust.autojs.util

interface ObjectWatcher {
    fun watch(watchedObject: Any, description: String)

    companion object {
        val default: ObjectWatcher = try {
            Class.forName("com.aiselp.debug.ObjectWatcher").getDeclaredConstructor().newInstance()
                as ObjectWatcher
        } catch (e: Throwable) {
            object : ObjectWatcher {
                override fun watch(watchedObject: Any, description: String) {}
            }
        }
    }
}
