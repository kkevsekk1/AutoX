package com.stardust.autojs.core.floaty;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import com.stardust.autojs.R;
import com.stardust.enhancedfloaty.FloatyService;
import com.stardust.enhancedfloaty.FloatyWindow;
import com.stardust.enhancedfloaty.ResizableFloaty;
import com.stardust.enhancedfloaty.ResizableFloatyWindow;
import com.stardust.enhancedfloaty.WindowBridge;
import com.stardust.enhancedfloaty.gesture.DragGesture;
import com.stardust.enhancedfloaty.gesture.ResizeGesture;
import com.stardust.enhancedfloaty.util.WindowTypeCompat;
import com.stardust.lib.R.layout;

/**
 * Created by Stardust on 2017/12/5.
 */

public class BaseResizableFloatyWindow extends FloatyWindow {

    public interface ViewSupplier {
        View inflate(Context context, ViewGroup parent);
    }

    private View mView;
    private View mResizer;
    private View mMoveCursor;
    private final MyFloaty mFloaty;
    private View mCloseButton;
    private final int mOffset;


    public BaseResizableFloatyWindow(Context context, ViewSupplier viewSupplier) {
        this.mFloaty = new MyFloaty(context, viewSupplier);
        mOffset = context.getResources().getDimensionPixelSize(R.dimen.floaty_window_offset);
        create(context);
    }
    public void create(Context context){
        WindowManager.LayoutParams layoutParams = createWindowLayoutParams();
        super.setWindowLayoutParams(layoutParams);
        super.setWindowBridge(super.onCreateWindowBridge(layoutParams));

        ViewGroup windowView = (ViewGroup)View.inflate(context, layout.ef_floaty_container, (ViewGroup)null);
        this.mView = mFloaty.createView();
        this.mResizer = mFloaty.getResizerView(mView);
        this.mMoveCursor = mFloaty.getMoveCursorView(mView);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(-2, -2);
        windowView.addView(mView, params);
        windowView.setFocusableInTouchMode(true);
        mCloseButton = windowView.findViewById(R.id.close);
        super.setWindowView(windowView);
    }
    @SuppressLint("WrongConstant")
    private WindowManager.LayoutParams createWindowLayoutParams() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-2, -2, WindowTypeCompat.getPhoneWindowType(), 520, -3);
        layoutParams.gravity = 8388659;
        return layoutParams;
    }
    protected void onViewCreated(View view) {
        super.onViewCreated(view);
        this.initGesture();
    }
    @Override
    protected WindowManager.LayoutParams onCreateWindowLayoutParams(){
        return super.getWindowLayoutParams();
    }
    @Override
    protected WindowBridge onCreateWindowBridge(WindowManager.LayoutParams params) {
        return new WindowBridge.DefaultImpl(params, getWindowManager(), getWindowView()) {
            @Override
            public int getX() {
                return super.getX() + mOffset;
            }
            @Override
            public int getY() {
                return super.getY() + mOffset;
            }
            @Override
            public void updatePosition(int x, int y) {
                super.updatePosition(x - mOffset, y - mOffset);
            }
        };
    }
    public View getRootView() {
        return this.mView;
    }
    public void setOnCloseButtonClickListener(View.OnClickListener listener) {
        mCloseButton.setOnClickListener(listener);
    }
    private void initGesture() {
        if (this.mResizer != null) {
            ResizeGesture.enableResize(this.mResizer, this.mView, this.getWindowBridge());
        }
        if (this.mMoveCursor != null) {
            DragGesture gesture = new DragGesture(this.getWindowBridge(), this.mMoveCursor);
            gesture.setPressedAlpha(1.0F);
        }

    }
    public void setAdjustEnabled(boolean enabled) {
        if (!enabled) {
            mMoveCursor.setVisibility(View.GONE);
            mResizer.setVisibility(View.GONE);
            mCloseButton.setVisibility(View.GONE);
        } else {
            mMoveCursor.setVisibility(View.VISIBLE);
            mResizer.setVisibility(View.VISIBLE);
            mCloseButton.setVisibility(View.VISIBLE);
        }
    }
    public boolean isAdjustEnabled() {
        return mMoveCursor.getVisibility() == View.VISIBLE;
    }
    @Override
    protected View onCreateView(FloatyService floatyService) {
        return super.getWindowView();
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

    private static class MyFloaty implements ResizableFloaty {
        private final ViewSupplier mContentViewSupplier;
        private View mRootView;
        private final Context mContext;


        public MyFloaty(Context context, ViewSupplier supplier) {
            mContentViewSupplier = supplier;
            mContext = context;
        }
        public View createView(){
            mRootView = View.inflate(mContext, R.layout.floaty_window, null);
            FrameLayout container = mRootView.findViewById(R.id.container);
            View contentView = mContentViewSupplier.inflate(mContext, container);
            return mRootView;
        }
        @Override
        public View inflateView(FloatyService floatyService, ResizableFloatyWindow resizableFloatyWindow) {
            return mRootView;
        }

        @Nullable
        @Override
        public View getResizerView(View view) {
            return view.findViewById(R.id.resizer);
        }

        @Nullable
        @Override
        public View getMoveCursorView(View view) {
            return view.findViewById(R.id.move_cursor);
        }
    }
}
