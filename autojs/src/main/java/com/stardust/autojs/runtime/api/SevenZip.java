package com.stardust.autojs.runtime.api;

import android.content.Context;

import com.stardust.autojs.runtime.exception.ScriptException;
import com.stardust.pio.PFiles;

public class SevenZip {
    private Context mContext;

    public SevenZip(Context context) {
        mContext = context;
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
        String cmdStr = "7z";
        if (PFiles.isFile(srcPath)) {
            cmdStr = "7z a -y" + typeOption + " -ms " + destFilePath + " " + srcPath;
        } else if (PFiles.isDir(srcPath)) {
            cmdStr = "7z a -y" + typeOption + " -ms -r " + destFilePath + " " + srcPath;
        }
        try {
            cmd(cmdStr);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }

    public void X(String filePath0, String dirPath1) {
        String cmdStr = "7z x -y -aos " + filePath0;
        if (PFiles.isFile(filePath0)) {
            if (PFiles.isDir(dirPath1)) {
                cmdStr = "7z x -y -aos -o" + dirPath1 + " " + filePath0;
            } else {
                cmdStr = "7z x -y -aos " + filePath0;
            }
        }
        try {
            cmd(cmdStr);
        } catch (Exception e) {
            throw new ScriptException(e);
        }
    }
}
