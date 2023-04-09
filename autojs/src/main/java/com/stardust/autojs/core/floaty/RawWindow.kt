package com.stardust.autojs.core.floaty

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.stardust.autojs.R
import com.stardust.enhancedfloaty.FloatyService
import com.stardust.enhancedfloaty.FloatyWindow
import com.stardust.enhancedfloaty.util.WindowTypeCompat


class RawWindow(rawFloaty: RawFloaty, context: Context) : FloatyWindow() {
    interface RawFloaty {
        fun inflateWindowView(context: Context, parent: ViewGroup): View
    }

    private val mRawFloaty: RawFloaty = rawFloaty
    private var mContentView: View

    init {
        //使悬浮窗在初始化阶段创建好view和LayoutParams
        val windowView = View.inflate(context, R.layout.raw_window, null) as ViewGroup
        mContentView = mRawFloaty.inflateWindowView(context, windowView)
        val mWindowLayoutParams = createWindowLayoutParams()
        super.setWindowView(windowView)
        super.setWindowManager(context.getSystemService(FloatyService.WINDOW_SERVICE) as WindowManager)
        super.setWindowLayoutParams(mWindowLayoutParams)
        super.setWindowBridge(super.onCreateWindowBridge(mWindowLayoutParams))
        println(super.getWindowManager())
    }


    override fun onCreateView(floatyService: FloatyService?): View? {
        return super.getWindowView()
    }

    fun getContentView(): View {
        return mContentView
    }

    private fun createWindowLayoutParams(): WindowManager.LayoutParams {
        var flags = (WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_FULLSCREEN
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        flags = flags or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowTypeCompat.getWindowType(),
            flags,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        return layoutParams
    }

    override fun onCreateWindowLayoutParams(): WindowManager.LayoutParams? {
        return super.getWindowLayoutParams()
    }

    fun disableWindowFocus() {
        val windowLayoutParams: WindowManager.LayoutParams = windowLayoutParams
        windowLayoutParams.flags =
            windowLayoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        updateWindowLayoutParams(windowLayoutParams)
    }

    fun requestWindowFocus() {
        val windowLayoutParams: WindowManager.LayoutParams = windowLayoutParams
        windowLayoutParams.flags =
            windowLayoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        updateWindowLayoutParams(windowLayoutParams)
        super.getWindowView().requestLayout()
    }

    fun setTouchable(touchable: Boolean) {
        val windowLayoutParams: WindowManager.LayoutParams = windowLayoutParams
        if (touchable) {
            windowLayoutParams.flags =
                windowLayoutParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            windowLayoutParams.flags =
                windowLayoutParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        updateWindowLayoutParams(windowLayoutParams)
    }
}