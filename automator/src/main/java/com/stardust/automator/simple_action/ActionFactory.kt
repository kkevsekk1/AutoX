package com.stardust.automator.simple_action

import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.stardust.automator.UiObject

/**
 * Created by Stardust on 2017/1/27.
 */

object ActionFactory {

    private val searchUpAction = mapOf<Int, Any?>(
        AccessibilityNodeInfo.ACTION_CLICK to null,
        AccessibilityNodeInfo.ACTION_LONG_CLICK to null,
        AccessibilityNodeInfo.ACTION_SELECT to null,
        AccessibilityNodeInfo.ACTION_FOCUS to null,
        AccessibilityNodeInfo.ACTION_SELECT to null,
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD to null,
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD to null,
    )

    fun createActionWithTextFilter(action: Int, text: String, index: Int): SimpleAction {
        return if (searchUpAction.containsKey(action))
            SearchUpTargetAction(action, FilterAction.TextFilter(text, index))
        else
            DepthFirstSearchTargetAction(action, FilterAction.TextFilter(text, index))
    }

    fun createActionWithBoundsFilter(action: Int, rect: Rect): SimpleAction {
        return if (searchUpAction.containsKey(action))
            SearchUpTargetAction(action, FilterAction.BoundsFilter(rect))
        else
            DepthFirstSearchTargetAction(action, FilterAction.BoundsFilter(rect))
    }

    fun createActionWithEditableFilter(action: Int, index: Int, text: String): SimpleAction {
        return object : SearchTargetAction(action, EditableFilter(index)) {

            override fun performAction(node: UiObject): Boolean {
                val args = Bundle()
                if (action == AccessibilityNodeInfo.ACTION_SET_TEXT) {
                    args.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        text
                    )
                } else {
                    args.putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        node.text() + text
                    )
                }
                return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }
    }

    fun createScrollMaxAction(action: Int): SimpleAction {
        return ScrollMaxAction(action)
    }

    fun createScrollAction(action: Int, i: Int): SimpleAction {
        return ScrollAction(action, i)
    }

    fun createActionWithIdFilter(action: Int, id: String): SimpleAction {
        return FilterAction.SimpleFilterAction(action, FilterAction.IdFilter(id))
    }
}
