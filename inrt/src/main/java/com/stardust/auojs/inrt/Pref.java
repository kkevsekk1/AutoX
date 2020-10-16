package com.stardust.auojs.inrt;

import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;


import com.stardust.app.GlobalAppContext;

import java.io.File;

public class Pref {
    private static String  KEY_FIRST_USING = "key_first_using";
    private static SharedPreferences def() {
        return PreferenceManager.getDefaultSharedPreferences(GlobalAppContext.get());
    }

 public static boolean isFirstUsing() {
        boolean firstUsing =  def().getBoolean(KEY_FIRST_USING, true);
        if (firstUsing) {
            def().edit().putBoolean(KEY_FIRST_USING, false).apply();
        }
        return firstUsing;
    }

    private  static String getString(int res) {
        return GlobalAppContext.getString(res);
    }

    public static boolean shouldEnableAccessibilityServiceByRoot() {
        return  def().getBoolean(getString(R.string.key_enable_accessibility_service_by_root), false);
    }

    public static boolean shouldEnableAccessibilityService() {
        return  def().getBoolean(getString(R.string.key_enable_accessibility_service), false);
    }
    public static boolean shouldEnableFloatingWindow() {
        return  def().getBoolean(getString(R.string.key_enable_floating_window), false);
    }

    public static boolean  shouldStopAllScriptsWhenVolumeUp() {
        return  def().getBoolean(getString(R.string.key_use_volume_control_running), true);
    }

    public static String getScriptDirPath() {
         String dir ="/脚本/";
        return  new File(Environment.getExternalStorageDirectory(), dir).getPath();
    }


    public static void setCode( String value) {
        def().edit().putString("user_code",value).apply();
    }

    public static String getCode( String defValue)  {
        return  def().getString("user_code", defValue);
    }

    public static void setHost( String value) {
        def().edit().putString("user_host",value).apply();
    }
    public static String getHost( String defValue)  {
        return  def().getString("user_host", defValue);
    }

    public static void setImei( String value) {
        def().edit().putString("user_imei",value).apply();
    }
    public static String getImei( String defValue)  {
        return  def().getString("user_imei", defValue);
    }


    public static String getStatus(String defValue) {
        return  def().getString("user_status", defValue);
    }

    public static void setStatus(String defValue) {
        def().edit().putString("user_status",defValue).apply();
    }

}
