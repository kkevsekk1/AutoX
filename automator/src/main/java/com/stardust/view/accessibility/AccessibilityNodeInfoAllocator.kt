package com.stardust.view.accessibility

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.stardust.automator.BuildConfig
import java.util.Arrays

/**
 * Created by Stardust on 2017/3/22.
 */

open class AccessibilityNodeInfoAllocator {

    private val mAccessibilityNodeInfoList = HashMap<AccessibilityNodeInfo, String?>()

    open fun getChild(parent: AccessibilityNodeInfo, i: Int): AccessibilityNodeInfo? {
        return add(parent.getChild(i))
    }

    open fun getChild(parent: AccessibilityNodeInfoCompat, i: Int): AccessibilityNodeInfoCompat {
        val compat = parent.getChild(i)
        add(compat.unwrap() as AccessibilityNodeInfo)
        return compat
    }

    open fun getParent(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        return add(n.parent)
    }


    open fun getParent(n: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat {
        val compat = n.parent
        add(compat.unwrap() as AccessibilityNodeInfo)
        return compat
    }

    open fun findAccessibilityNodeInfosByText(
        root: AccessibilityNodeInfo,
        text: String
    ): List<AccessibilityNodeInfo> {
        val list = root.findAccessibilityNodeInfosByText(text)
        addAll(list)
        return list
    }


    open fun findAccessibilityNodeInfosByText(
        root: AccessibilityNodeInfoCompat,
        text: String
    ): List<AccessibilityNodeInfoCompat> {
        val list = root.findAccessibilityNodeInfosByText(text)
        addAll(list)
        return list
    }

    open fun findAccessibilityNodeInfosByViewId(
        root: AccessibilityNodeInfo,
        id: String
    ): List<AccessibilityNodeInfo> {
        val list = root.findAccessibilityNodeInfosByViewId(id)
        addAll(list)
        return list
    }


    open fun findAccessibilityNodeInfosByViewId(
        root: AccessibilityNodeInfoCompat,
        id: String
    ): List<AccessibilityNodeInfoCompat> {
        val list = root.findAccessibilityNodeInfosByViewId(id)
        addAll(list)
        return list
    }

    open fun recycle(nodeInfo: AccessibilityNodeInfo) {
        mAccessibilityNodeInfoList.remove(nodeInfo)
    }

    open fun recycle(nodeInfo: AccessibilityNodeInfoCompat) {
        recycle(nodeInfo.unwrap() as AccessibilityNodeInfo)
    }

    open fun recycleAll(): Int {
        return mAccessibilityNodeInfoList.size
    }

    fun add(nodeInfo: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        val stackTrace = if (DEBUG) Arrays.toString(Thread.currentThread().stackTrace) else null
        if (nodeInfo != null)
            mAccessibilityNodeInfoList[nodeInfo] = stackTrace
        return nodeInfo
    }

    private fun addAll(nodeInfos: Collection<*>) {
        val stackTrace = if (DEBUG) Arrays.toString(Thread.currentThread().stackTrace) else null
        for (nodeInfo in nodeInfos) {
            if (nodeInfo is AccessibilityNodeInfo) {
                mAccessibilityNodeInfoList[nodeInfo] = stackTrace
            } else if (nodeInfo is AccessibilityNodeInfoCompat) {
                mAccessibilityNodeInfoList[nodeInfo.unwrap() as AccessibilityNodeInfo] = stackTrace
            }
        }
    }

    private class NoOpAllocator : AccessibilityNodeInfoAllocator() {

        override fun getParent(n: AccessibilityNodeInfoCompat): AccessibilityNodeInfoCompat {
            return n.parent
        }

        override fun getParent(n: AccessibilityNodeInfo): AccessibilityNodeInfo? {
            return n.parent
        }

        override fun getChild(
            parent: AccessibilityNodeInfoCompat,
            i: Int
        ): AccessibilityNodeInfoCompat {
            return parent.getChild(i)
        }

        override fun getChild(parent: AccessibilityNodeInfo, i: Int): AccessibilityNodeInfo? {
            return parent.getChild(i)
        }

        override fun findAccessibilityNodeInfosByViewId(
            root: AccessibilityNodeInfoCompat,
            id: String
        ): List<AccessibilityNodeInfoCompat> {
            return root.findAccessibilityNodeInfosByViewId(id)
        }


        override fun findAccessibilityNodeInfosByViewId(
            root: AccessibilityNodeInfo,
            id: String
        ): List<AccessibilityNodeInfo> {
            return root.findAccessibilityNodeInfosByViewId(id)
        }

        override fun findAccessibilityNodeInfosByText(
            root: AccessibilityNodeInfo,
            text: String
        ): List<AccessibilityNodeInfo> {
            return root.findAccessibilityNodeInfosByText(text)
        }

        override fun findAccessibilityNodeInfosByText(
            root: AccessibilityNodeInfoCompat,
            text: String
        ): List<AccessibilityNodeInfoCompat> {
            return root.findAccessibilityNodeInfosByText(text)
        }

        override fun recycle(nodeInfo: AccessibilityNodeInfo) {
            super.recycle(nodeInfo)
        }

        override fun recycle(nodeInfo: AccessibilityNodeInfoCompat) {
            super.recycle(nodeInfo)
        }

        override fun recycleAll(): Int {
            return -1
        }
    }

    companion object {

        val NONE: AccessibilityNodeInfoAllocator = NoOpAllocator()

        private val TAG = "AccessibilityAllocator"
        private val DEBUG = BuildConfig.DEBUG

        val global = AccessibilityNodeInfoAllocator()
    }
}
