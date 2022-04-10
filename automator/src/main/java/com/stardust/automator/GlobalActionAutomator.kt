package com.stardust.automator

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import android.view.ViewConfiguration

import com.stardust.concurrent.VolatileBox
import com.stardust.concurrent.VolatileDispose
import com.stardust.util.ScreenMetrics

/**
 * Created by Stardust on 2017/5/16.
 */

class GlobalActionAutomator(private val mHandler: Handler?, private val serviceProvider: () -> AccessibilityService) {

    private val service: AccessibilityService
        get() = serviceProvider()

    private var mScreenMetrics: ScreenMetrics? = null

    fun setScreenMetrics(screenMetrics: ScreenMetrics?) {
        mScreenMetrics = screenMetrics
    }

    fun back(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    fun home(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun powerDialog(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
    }

    private fun performGlobalAction(globalAction: Int): Boolean {
        return service.performGlobalAction(globalAction)
    }

    fun notifications(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
    }

    fun quickSettings(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
    }

    fun recents(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
    }

    /**
     * Action to take a screenshot
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun takeScreenshot(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
    }

    /**
     * Action to lock the screen
     */
    @RequiresApi(Build.VERSION_CODES.P)
    fun lockScreen(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
    }

    /**
     * Action to dismiss the notification shade
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun dismissNotificationShade(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
    }

    /**
     * Action to send the KEYCODE_HEADSETHOOK KeyEvent, which is used to answer/hang up
     * calls and play/stop media
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun keyCodeHeadsetHook(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_KEYCODE_HEADSETHOOK)
    }

    /**
     * Action to trigger the Accessibility Shortcut. This shortcut has a hardware trigger
     * and can be activated by holding down the two volume keys.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun accessibilityShortcut(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_SHORTCUT)
    }

    /**
     * Action to bring up the Accessibility Button’s chooser menu
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun accessibilityButtonChooser(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_BUTTON_CHOOSER)
    }

    /**
     * Action to trigger the Accessibility Button
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun accessibilityButton(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_BUTTON)
    }

    /**
     * Action to show Launcher’s all apps.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    fun accessibilityAllApps(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS)
    }

    /**
     * Action to trigger dpad up keyevent.
     */
//    @RequiresApi(Build.VERSION_CODES.Tiramisu)
    fun dpadUp(): Boolean {
//        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_UP)
        // TODO: 待适配Api Tiramisu
        return false
    }

    /**
     * Action to trigger dpad down keyevent.
     */
//    @RequiresApi(Build.VERSION_CODES.Tiramisu)
    fun dpadDown(): Boolean {
//        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_DOWN)
        // TODO: 待适配Api Tiramisu
        return false
    }

    /**
     * Action to trigger dpad right keyevent.
     */
//    @RequiresApi(Build.VERSION_CODES.Tiramisu)
    fun dpadRight(): Boolean {
//        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_RIGHT)
        // TODO: 待适配Api Tiramisu
        return false
    }

    /**
     * Action to trigger dpad left keyevent.
     */
//    @RequiresApi(Build.VERSION_CODES.Tiramisu)
    fun dpadLeft(): Boolean {
//        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_LEFT)
        // TODO: 待适配Api Tiramisu
        return false
    }

    /**
     * Action to trigger dpad center keyevent.
     */
//    @RequiresApi(Build.VERSION_CODES.Tiramisu)
    fun dpadCenter(): Boolean {
//        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_DPAD_CENTER)
        // TODO: 待适配Api Tiramisu
        return false
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun splitScreen(): Boolean {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun gesture(start: Long, duration: Long, vararg points: IntArray): Boolean {
        val path = pointsToPath(points)
        return gestures(GestureDescription.StrokeDescription(path, start, duration))
    }

    private fun pointsToPath(points: Array<out IntArray>): Path {
        val path = Path()
        path.moveTo(scaleX(points[0][0]).toFloat(), scaleY(points[0][1]).toFloat())
        for (i in 1 until points.size) {
            val point = points[i]
            path.lineTo(scaleX(point[0]).toFloat(), scaleY(point[1]).toFloat())
        }
        return path
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun gestureAsync(start: Long, duration: Long, vararg points: IntArray) {
        val path = pointsToPath(points)
        gesturesAsync(GestureDescription.StrokeDescription(path, start, duration))
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun gestures(vararg strokes: GestureDescription.StrokeDescription): Boolean {
        val builder = GestureDescription.Builder()
        for (stroke in strokes) {
            builder.addStroke(stroke)
        }
        val handler = mHandler
        return if (handler == null) {
            gesturesWithoutHandler(builder.build())
        } else {
            gesturesWithHandler(handler, builder.build())
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun gesturesWithHandler(handler: Handler, description: GestureDescription): Boolean {
        val result = VolatileDispose<Boolean>()
        service.dispatchGesture(description, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                result.setAndNotify(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                result.setAndNotify(false)
            }
        }, handler)
        return result.blockedGet()
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun gesturesWithoutHandler(description: GestureDescription): Boolean {
        prepareLooperIfNeeded()
        val result = VolatileBox(false)
        val handler = Looper.myLooper()?.let { Handler(it) }
        service.dispatchGesture(description, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription) {
                result.set(true)
                quitLoop()
            }

            override fun onCancelled(gestureDescription: GestureDescription) {
                result.set(false)
                quitLoop()
            }
        }, handler)
        Looper.loop()
        return result.get()
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun gesturesAsync(vararg strokes: GestureDescription.StrokeDescription) {
        val builder = GestureDescription.Builder()
        for (stroke in strokes) {
            builder.addStroke(stroke)
        }
        service.dispatchGesture(builder.build(), null, null)
    }

    private fun quitLoop() {
        val looper = Looper.myLooper()
        looper?.quit()
    }

    private fun prepareLooperIfNeeded() {
        if (Looper.myLooper() == null) {
            Looper.prepare()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun click(x: Int, y: Int): Boolean {
        return press(x, y, ViewConfiguration.getTapTimeout() + 50)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun press(x: Int, y: Int, delay: Int): Boolean {
        return gesture(0, delay.toLong(), intArrayOf(x, y))
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun longClick(x: Int, y: Int): Boolean {
        return gesture(0, (ViewConfiguration.getLongPressTimeout() + 200).toLong(), intArrayOf(x, y))
    }

    private fun scaleX(x: Int): Int {
        return mScreenMetrics?.scaleX(x) ?: x
    }

    private fun scaleY(y: Int): Int {
        return mScreenMetrics?.scaleX(y) ?: y
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, delay: Long): Boolean {
        return gesture(0, delay, intArrayOf(x1, y1), intArrayOf(x2, y2))
    }

}
