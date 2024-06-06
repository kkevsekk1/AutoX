package com.stardust.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Stardust on 2017/1/24.
 */

public class ViewUtil {

    // FIXME: 2018/1/23 not working in some devices (https://github.com/hyb1996/Auto.js/issues/268)
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static void setViewMeasure(View view, int width, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }
}
