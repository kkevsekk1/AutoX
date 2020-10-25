package com.stardust.autojs.core.console;

import android.content.Context;
import android.content.ContextWrapper;

import androidx.annotation.Nullable;

import android.content.res.ColorStateList;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.stardust.app.GlobalAppContext;
import com.stardust.enhancedfloaty.FloatyService;
import com.stardust.enhancedfloaty.ResizableExpandableFloaty;
import com.stardust.enhancedfloaty.ResizableExpandableFloatyWindow;
import com.stardust.util.ScreenMetrics;
import com.stardust.util.ViewUtil;
import com.stardust.autojs.R;
import com.stardust.util.ViewUtils;

/**
 * Created by Stardust on 2017/4/20.
 */

public class ConsoleFloaty extends ResizableExpandableFloaty.AbstractResizableExpandableFloaty {

    private ContextWrapper mContextWrapper;
    private View mResizer, mMoveCursor;
    private TextView mTitleView;
    private ImageView mCloaseView, mMinimizeView, mResizeView;
    private ConsoleImpl mConsole;
    private CharSequence mTitle;
    private int mContentSize = -1;
    private int mTitleColor = 0xfe14efb1;
    private double scale;

    private View mExpandedView;

    public ConsoleFloaty(ConsoleImpl console) {
        mConsole = console;
        setShouldRequestFocusWhenExpand(false);
        setInitialX(100);
        setInitialY(1000);
        setCollapsedViewUnpressedAlpha(1.0f);
    }

    @Override
    public int getInitialWidth() {
        return WindowManager.LayoutParams.WRAP_CONTENT; //ScreenMetrics.getDeviceScreenWidth() * 2 / 3;
    }

    @Override
    public int getInitialHeight() {
        return WindowManager.LayoutParams.WRAP_CONTENT;//ScreenMetrics.getDeviceScreenHeight() / 3;
    }

    @Override
    public View inflateCollapsedView(FloatyService service, final ResizableExpandableFloatyWindow window) {
        ensureContextWrapper(service);
        return View.inflate(mContextWrapper, R.layout.floating_window_collapse, null);
    }

    private void ensureContextWrapper(Context context) {
        if (mContextWrapper == null) {
            mContextWrapper = new ContextThemeWrapper(context, R.style.ConsoleTheme);
        }
    }

    @Override
    public View inflateExpandedView(FloatyService service, ResizableExpandableFloatyWindow window) {
        ensureContextWrapper(service);
        View view = View.inflate(mContextWrapper, R.layout.floating_console_expand, null);
        setListeners(view, window);
        setUpConsole(view, window);
        setInitialMeasure(view);
        mExpandedView = view;
        return view;
    }

    public View getExpandedView() {
        return mExpandedView;
    }

    private void setInitialMeasure(final View view) {
        view.post(() -> ViewUtil.setViewMeasure(view, ScreenMetrics.getDeviceScreenWidth() * 2 / 3,
                ScreenMetrics.getDeviceScreenHeight() / 3));
    }

    private void initConsoleTitle(View view) {
        mTitleView = view.findViewById(R.id.title);
        mCloaseView = view.findViewById(R.id.close);
        mMinimizeView = view.findViewById(R.id.minimize);
        mResizeView = view.findViewById(R.id.move_or_resize);
        if (mTitle != null) {
            scale = getBorderContentScale();
            resetTiteText();
            resetImageView(mCloaseView);
            resetImageView(mMinimizeView);
            resetImageView(mResizeView);
            mTitleView.setText(mTitle);
        }
    }

    private void setListeners(final View view, final ResizableExpandableFloatyWindow window) {
        setWindowOperationIconListeners(view, window);
    }

    private void setUpConsole(View view, ResizableExpandableFloatyWindow window) {
        ConsoleView consoleView = view.findViewById(R.id.console);
        consoleView.setConsole(mConsole);
        consoleView.setWindow(window);
        initConsoleTitle(view);
    }

    private void setWindowOperationIconListeners(View view, final ResizableExpandableFloatyWindow window) {
        view.findViewById(R.id.close).setOnClickListener(v -> window.close());
        view.findViewById(R.id.move_or_resize).setOnClickListener(v -> {
            if (mMoveCursor.getVisibility() == View.VISIBLE) {
                mMoveCursor.setVisibility(View.GONE);
                mResizer.setVisibility(View.GONE);
            } else {
                mMoveCursor.setVisibility(View.VISIBLE);
                mResizer.setVisibility(View.VISIBLE);
            }
        });
        view.findViewById(R.id.minimize).setOnClickListener(v -> window.collapse());
    }



    @Nullable
    @Override
    public View getResizerView(View expandedView) {
        mResizer = expandedView.findViewById(R.id.resizer);
        return mResizer;
    }

    @Nullable
    @Override
    public View getMoveCursorView(View expandedView) {
        mMoveCursor = expandedView.findViewById(R.id.move_cursor);
        return mMoveCursor;
    }


    public void setTitle(final CharSequence title, int color, int size) {
        mTitle = title;
        mTitleColor = color;
        mContentSize = ViewUtils.dpToPx(GlobalAppContext.get(), size);
        mTitleColor = color;
        if (mTitleView != null) {
            if(!TextUtils.isEmpty(title)){
                mTitleView.post(() -> mTitleView.setText(title));
            }
            resetTiteText();
            resetImageView(mCloaseView);
            resetImageView(mMinimizeView);
            resetImageView(mResizeView);
        }
    }

    private void resetTiteText() {
        if (mContentSize > 0) {
            LinearLayout linearLayout = (LinearLayout) mTitleView.getParent();
            ViewGroup.LayoutParams layoutParams = linearLayout.getLayoutParams();
            layoutParams.height = mContentSize;
            linearLayout.post(() -> linearLayout.setLayoutParams(layoutParams));

            float titileSize = (float) (mContentSize * scale);
            mTitleView.post(() -> mTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titileSize));

        }
        if (mTitleColor != 0xfe14efb1) {
            mTitleView.setTextColor(mTitleColor);
        }
    }

    private void resetImageView(ImageView view) {
        if (mContentSize > 0) {
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            layoutParams.height = mContentSize;
            layoutParams.width = mContentSize;
            view.setPadding((int) (view.getPaddingLeft() * scale), (int) (view.getPaddingTop() * scale), (int) (view.getPaddingRight() * scale), (int) (view.getPaddingBottom() * scale));
            view.post(() -> {
                view.setLayoutParams(layoutParams);
            });
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mTitleColor != 0xfe14efb1) {
            view.setImageTintList(createColorStateList(mTitleColor));
        }

    }

    private static ColorStateList createColorStateList(int color) {
        int[] colors = new int[]{color, color};
        int[][] states = new int[2][];
        states[0] = new int[]{android.R.attr.state_checked};
        states[1] = new int[]{-android.R.attr.state_checked};
        ColorStateList colorList = new ColorStateList(states, colors);
        return colorList;
    }

    private double getBorderContentScale() {
        LinearLayout linearLayout = (LinearLayout) mTitleView.getParent();
        ViewGroup.LayoutParams layoutParams = linearLayout.getLayoutParams();
        int borderHeight = layoutParams.height;
        float textSize = mTitleView.getTextSize();
        return (textSize * 1.0) / (borderHeight * 1.0);
    }

}
