package com.aiselp.debug

import leakcanary.AppWatcher.objectWatcher

class ObjectWatcher : com.stardust.autojs.util.ObjectWatcher {
    override fun watch(watchedObject: Any, description: String) {
        objectWatcher.expectWeaklyReachable(watchedObject, description)
    }
}