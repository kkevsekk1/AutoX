package com.stardust.autojs.core.floaty;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.stardust.autojs.R;
import com.stardust.enhancedfloaty.FloatyService;
import com.stardust.enhancedfloaty.FloatyWindow;
import com.stardust.enhancedfloaty.util.WindowTypeCompat;

public class RawWindow extends FloatyWindow {
    public interface RawFloaty {
        View inflateWindowView(Context context, ViewGroup parent);
    }
    private RawFloaty mRawFloaty;
    private ViewGroup windowView;
    private WindowManager.LayoutParams mWindowLayoutParams;
    private View mContentView;
    public RawWindow(RawFloaty rawFloaty, Context context) {
        mRawFloaty = rawFloaty;
        create(context);
    }
    public void create(Context context){
        windowView = (ViewGroup) View.inflate(context, R.layout.raw_window, null);
        mContentView = mRawFloaty.inflateWindowView(context, windowView);
        createWindowLayoutParams();
        super.setWindowBridge(super.onCreateWindowBridge(mWindowLayoutParams));
    }
    @Override
    protected View onCreateView(FloatyService floatyService) {
        return windowView;
    }
    public View getContentView() {
        return mContentView;
    }
    private void createWindowLayoutParams(){
        int flags =
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowTypeCompat.getWindowType(),
                flags,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        mWindowLayoutParams = layoutParams;
    }
    @Override
    protected WindowManager.LayoutParams onCreateWindowLayoutParams() {
        return mWindowLayoutParams;
    }

    public void disableWindowFocus() {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        windowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        updateWindowLayoutParams(windowLayoutParams);
    }

    public void requestWindowFocus() {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        windowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        updateWindowLayoutParams(windowLayoutParams);
        getWindowView().requestLayout();
    }

    public void setTouchable(boolean touchable) {
        WindowManager.LayoutParams windowLayoutParams = getWindowLayoutParams();
        if (touchable) {
            windowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            windowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        updateWindowLayoutParams(windowLayoutParams);
    }

}
