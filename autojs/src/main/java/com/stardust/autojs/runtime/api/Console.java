package com.stardust.autojs.runtime.api;

import android.graphics.Color;

import androidx.annotation.Nullable;

import com.stardust.autojs.annotation.ScriptInterface;

/**
 * Created by Stardust on 2017/4/2.
 */

public interface Console {


    @ScriptInterface
    void verbose(@Nullable Object data, Object... options);

    @ScriptInterface
    void log(@Nullable Object data, Object... options);

    @ScriptInterface
    void print(int level, Object data, Object... options);

    @ScriptInterface
    void info(@Nullable Object data, Object... options);

    @ScriptInterface
    void warn(@Nullable Object data, Object... options);

    @ScriptInterface
    void error(@Nullable Object data, Object... options);

    @ScriptInterface
    void assertTrue(boolean value, @Nullable Object data, Object... options);

    @ScriptInterface
    void clear();

    @ScriptInterface
    void show(boolean isAutoHide);

    @ScriptInterface
    void show();

    @ScriptInterface
    void hide();

    boolean isAutoHide();

    String println(int level, CharSequence charSequence);

    /**
     *
     * @param title  标题文字
     * @param color  标题颜色 rgba
     * @param size  标题字号，自动根据字号调整标题高度
     */
    void setTitle(CharSequence title,String color,int size);
    /**
     *
     * @param color 背景色 rgba
     */
    void setBackgroud(@Nullable String color);

    /**
     * @param size 字号大小
     */
    void setLogSize(int size);

    /**
     * 设置是否可以输入
     * @param can
     */
    public void  setCanInput(boolean can);
}
