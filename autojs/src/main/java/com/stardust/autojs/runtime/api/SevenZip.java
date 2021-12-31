package com.stardust.autojs.runtime.api;

import android.content.Context;
import android.widget.Toast;

import com.stardust.autojs.runtime.ScriptRuntime;
import com.stardust.autojs.runtime.exception.ScriptException;
import com.stardust.autojs.runtime.exception.ScriptInterruptedException;
import com.stardust.pio.PFiles;
import com.stardust.view.accessibility.AccessibilityNotificationObserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SevenZip {
    private Context mContext;
    private ScriptRuntime mRuntime;

    public SevenZip(Context context, ScriptRuntime scriptRuntime) {
        mContext = context;
        mRuntime = scriptRuntime;
    }

    static {
        System.loadLibrary("zips");
    }

    public static native int cmd(String cmdStr);

    public void A(String type, String destFilePath, String srcPath) {
        String typeOption = "";
        if (!type.trim().isEmpty()) {
            typeOption = " -t" + type.trim();
        }
        String cmdStr = "7za";
        if (PFiles.isFile(srcPath)) {
            cmdStr = "7za a -y" + typeOption + " -ms " + destFilePath + " " + srcPath;
        } else if (PFiles.isDir(srcPath)) {
            cmdStr = "7za a -y" + typeOption + " -ms -r " + destFilePath + " " + srcPath;
        }
        try {
            cmd(cmdStr);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    public void X(String filePath0, String dirPath1) {
        String cmdStr = "7za x -y -aos " + filePath0;
        if (PFiles.isFile(filePath0)) {
            if (PFiles.isDir(dirPath1)) {
                cmdStr = "7za x -y -aos -o" + dirPath1 + " " + filePath0;
            } else {
                cmdStr = "7za x -y -aos " + filePath0;
            }
        }
        try {
            cmd(cmdStr);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }
}
