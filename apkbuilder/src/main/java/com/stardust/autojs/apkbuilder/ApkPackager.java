package com.stardust.autojs.apkbuilder;

import android.text.TextUtils;

import com.stardust.autojs.apkbuilder.util.StreamUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import pxb.android.tinysign.TinySign;

/**
 * Created by Stardust on 2017/10/23.
 */

public class ApkPackager {

    private InputStream mApkInputStream;
    private String mWorkspacePath;

    public ApkPackager(InputStream apkInputStream, String workspacePath) {
        mApkInputStream = apkInputStream;
        mWorkspacePath = workspacePath;
    }

    public ApkPackager(String apkPath, String workspacePath) throws FileNotFoundException {
        mApkInputStream = new FileInputStream(apkPath);
        mWorkspacePath = workspacePath;
    }

    public void unzip() throws IOException {
        ZipInputStream zis = new ZipInputStream(mApkInputStream);
        for (ZipEntry e = zis.getNextEntry(); e != null; e = zis.getNextEntry()) {
            String name = e.getName();
            if (!e.isDirectory() && !TextUtils.isEmpty(name)) {
                File file = new File(mWorkspacePath, name);
                System.out.println(file);
                file.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(file);
                StreamUtils.write(zis, fos);
                fos.close();
            }
        }
        zis.close();
    }

    public void repackage(String newApkPath) throws Exception {
        FileOutputStream fos = new FileOutputStream(newApkPath);
        TinySign.sign(new File(mWorkspacePath), fos);
        fos.close();
    }

    public void cleanWorkspace() {

    }

}
