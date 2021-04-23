package com.stardust.autojs.apkbuilder;


import com.stardust.autojs.apkbuilder.util.StreamUtils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import zhao.arsceditor.ResDecoder.ARSCDecoder;
import zhao.arsceditor.ResDecoder.AXMLDecoder;

/**
 * Created by Stardust on 2017/7/1.
 */

public class ApkBuilder {


    private File mOutApkFile;
    private ApkPackager mApkPackager;
    private ManifestEditor mManifestEditor;
    private String mWorkspacePath;
    private String mArscPackageName;

    public ApkBuilder(InputStream apkInputStream, File outApkFile, String workspacePath) {
        mOutApkFile = outApkFile;
        mWorkspacePath = workspacePath;
        mApkPackager = new ApkPackager(apkInputStream, mWorkspacePath);
    }

    public ApkBuilder(File inFile, File outFile, String workspacePath) throws FileNotFoundException {
        this(new FileInputStream(inFile), outFile, workspacePath);
    }

    public ApkBuilder prepare() throws IOException {
        new File(mWorkspacePath).mkdirs();
        mApkPackager.unzip();
        return this;
    }

    private File getManifestFile() {
        return new File(mWorkspacePath, "AndroidManifest.xml");
    }

    public ManifestEditor editManifest() throws FileNotFoundException {
        mManifestEditor = new ManifestEditor(new FileInputStream(getManifestFile()));
        return mManifestEditor;
    }

    public ApkBuilder setArscPackageName(String packageName) throws IOException {
        mArscPackageName = packageName;
        return this;
    }


    public ApkBuilder replaceFile(String relativePath, String newFilePath) throws IOException {
        StreamUtils.write(new FileInputStream(newFilePath),
                new FileOutputStream(new File(mWorkspacePath, relativePath)));
        return this;
    }

    public ApkBuilder build() throws IOException {
        if (mManifestEditor != null) {
            mManifestEditor.writeTo(new FileOutputStream(getManifestFile()));
        }
        if (mArscPackageName != null) {
            buildArsc();
        }
        return this;
    }

    private void buildArsc() throws IOException {
        File oldArsc = new File(mWorkspacePath, "resources.arsc");
        File newArsc = new File(mWorkspacePath, "resources.arsc.new");
        ARSCDecoder decoder = new ARSCDecoder(new BufferedInputStream(new FileInputStream(oldArsc)), null, false);
        FileOutputStream fos = new FileOutputStream(newArsc);
        decoder.CloneArsc(fos, mArscPackageName, true);
        oldArsc.delete();
        newArsc.renameTo(oldArsc);
    }

    public ApkBuilder sign() throws Exception {
        mApkPackager.repackage(mOutApkFile.getPath());
        return this;
    }

    public ApkBuilder cleanWorkspace() {
        delete(new File(mWorkspacePath));
        return this;
    }

    private void delete(File file) {
        if (file.isFile()) {
            file.delete();
            return;
        }
        for (File child : file.listFiles()) {
            delete(child);
        }
        file.delete();
    }

}
