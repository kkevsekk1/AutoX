package com.stardust.autojs.project;

import com.google.gson.annotations.SerializedName;

/**
 * Created by Stardust on 2018/1/25.
 */

public class LaunchConfig {

    @SerializedName("hideLogs")
    private boolean mHideLogs = false;

    @SerializedName("stableMode")
    private boolean stableMode = false;

    @SerializedName("splashIcon")
    private String splashIcon;

    @SerializedName("splashText")
    private String splashText = "Powered by Autoxjs.com";

    public boolean shouldHideLogs() {
        return mHideLogs;
    }

    public void setHideLogs(boolean hideLogs) {
        mHideLogs = hideLogs;
    }

    public boolean isStableMode() {
        return stableMode;
    }

    public void setStableMode(boolean stableMode) {
        this.stableMode = stableMode;
    }

    public String getSplashIcon() {
        return splashIcon;
    }

    public void setSplashIcon(String splashIcon) {
        this.splashIcon = splashIcon;
    }

    public String getSplashText() {
        return splashText;
    }

    public void setSplashText(String splashText) {
        this.splashText = splashText;
    }
}
