package org.autojs.autojs.tool

fun <T : Any?> MutableList<T>.addIfNotExist(element: T): Boolean {
    if (!this.contains(element)) {
        this.add(element)
        return true
    }
    return false
}

fun <T : Any?> MutableList<T>.addAllIfNotExist(elements: Collection<T>): Int {
    var num = 0
    for (element in elements) {
        if (!this.contains(element)) {
            this.add(element)
            num++
        }
    }
    return num
}