package com.stardust.autojs.core.mlkit

import android.graphics.Rect

data class MlKitOcrResult(
    val level: Int = 0,
    val confidence: Float = -1f,
    val text: String = "",
    val recognizedLanguage: String = "",
    val bounds: Rect? = null,
    val children: List<MlKitOcrResult>? = null
) {

    fun find(predicate: (MlKitOcrResult) -> Boolean): MlKitOcrResult? {
        return recursiveFind(this, predicate)
    }

    fun find(level: Int, predicate: (MlKitOcrResult) -> Boolean): MlKitOcrResult? {
        return find { it.level == level && predicate(it) }
    }

    fun filter(predicate: (MlKitOcrResult) -> Boolean): List<MlKitOcrResult> {
        val filterList = mutableListOf<MlKitOcrResult>()
        recursiveFilter(filterList, this, false, predicate)
        return filterList.toList()
    }

    fun filter(level: Int, predicate: (MlKitOcrResult) -> Boolean): List<MlKitOcrResult> {
        return filter { it.level == level && predicate(it) }
    }

    fun toArray(level: Int): List<MlKitOcrResult> {
        val filterList = mutableListOf<MlKitOcrResult>()
        recursiveFilter(filterList, this, true) { it.level == level }
        return filterList.toList()
    }

    fun toArray(): List<MlKitOcrResult> {
        val filterList = mutableListOf<MlKitOcrResult>()
        recursiveFilter(filterList, this, true) { true }
        return filterList.toList()
    }

    companion object {
        private fun recursiveFind(
            mlKitOcrResult: MlKitOcrResult,
            predicate: (MlKitOcrResult) -> Boolean
        ): MlKitOcrResult? {
            if (predicate(mlKitOcrResult)) return mlKitOcrResult
            if (!mlKitOcrResult.children.isNullOrEmpty()) {
                for (child in mlKitOcrResult.children) {
                    recursiveFind(child, predicate)?.let { return it }
                }
            }
            return null
        }

        private fun recursiveFilter(
            filterList: MutableList<MlKitOcrResult>,
            mlKitOcrResult: MlKitOcrResult,
            childToNull: Boolean = false,
            predicate: (MlKitOcrResult) -> Boolean
        ) {
            if (predicate(mlKitOcrResult)) {
                filterList.add(if (childToNull) mlKitOcrResult.copy(children = null) else mlKitOcrResult)
            }
            if (!mlKitOcrResult.children.isNullOrEmpty()) {
                for (child in mlKitOcrResult.children) {
                    recursiveFilter(filterList, child, childToNull, predicate)
                }
            }
            return
        }
    }

}