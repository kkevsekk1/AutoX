package com.stardust.autojs.runtime.api;

import android.webkit.JavascriptInterface;

import com.stardust.autojs.runtime.ScriptRuntime;
import com.stardust.pio.PFileInterface;
import com.stardust.pio.PFiles;
import com.stardust.pio.UncheckedIOException;
import com.stardust.util.Func1;

import java.io.File;
import java.io.IOException;

/**
 * Created by Stardust on 2018/1/23.
 */

public class Files {

    private final ScriptRuntime mRuntime;

    public Files(ScriptRuntime runtime) {
        mRuntime = runtime;
    }

    // FIXME: 2018/10/16 is not correct in sub-directory?
    public String path(String relativePath) {
        if (mRuntime == null) {
            return relativePath.startsWith(File.separator) ? relativePath : "/" + relativePath;
        } else {
            String cwd = cwd();
            if (cwd == null || relativePath == null || relativePath.startsWith("/"))
                return relativePath;
            File f = new File(cwd);
            String[] paths = relativePath.split("/");
            for (String path : paths) {
                if (path.equals("."))
                    continue;
                if (path.equals("..")) {
                    f = f.getParentFile();
                    continue;
                }
                f = new File(f, path);
            }
            String path = f.getPath();
            return relativePath.endsWith(File.separator) ? path + "/" : path;
        }
    }

    public String cwd() {
        return mRuntime.engines.myEngine().cwd();
    }

    public String readAssets(String path, String encoding) {
        try {
            return PFiles.read(mRuntime.getUiHandler().getContext().getAssets().open(path), encoding);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String readAssets(String path) {
        return readAssets(path, "UTF-8");
    }

    @JavascriptInterface
    public PFileInterface open(String path, String mode, String encoding, int bufferSize) {
        return PFiles.open(path(path), mode, encoding, bufferSize);
    }

    @JavascriptInterface
    public Object open(String path, String mode, String encoding) {
        return PFiles.open(path(path), mode, encoding);
    }

    @JavascriptInterface
    public Object open(String path, String mode) {
        return PFiles.open(path(path), mode);
    }

    @JavascriptInterface
    public Object open(String path) {
        return PFiles.open(path(path));
    }

    @JavascriptInterface
    public boolean create(String path) {
        return PFiles.create(path(path));
    }

    @JavascriptInterface
    public boolean createIfNotExists(String path) {
        return PFiles.createIfNotExists(path(path));
    }

    @JavascriptInterface
    public boolean createWithDirs(String path) {
        return PFiles.createWithDirs(path(path));
    }

    @JavascriptInterface
    public boolean exists(String path) {
        return PFiles.exists(path(path));
    }

    @JavascriptInterface
    public boolean ensureDir(String path) {
        return PFiles.ensureDir(path(path));
    }

    @JavascriptInterface
    public String read(String path, String encoding) {
        return PFiles.read(path(path), encoding);
    }


    @JavascriptInterface
    public String read(String path) {
        return PFiles.read(path(path));
    }

    @JavascriptInterface
    public byte[] readBytes(String path) {
        return PFiles.readBytes(path(path));
    }

    @JavascriptInterface
    public void write(String path, String text) {
        PFiles.write(path(path), text);
    }

    @JavascriptInterface
    public void write(String path, String text, String encoding) {
        PFiles.write(path(path), text, encoding);
    }

    @JavascriptInterface
    public void append(String path, String text) {
        PFiles.append(path(path), text);
    }

    @JavascriptInterface
    public void append(String path, String text, String encoding) {
        PFiles.append(path(path), text, encoding);
    }

    @JavascriptInterface
    public void appendBytes(String path, byte[] bytes) {
        PFiles.appendBytes(path(path), bytes);
    }

    @JavascriptInterface
    public void writeBytes(String path, byte[] bytes) {
        PFiles.writeBytes(path(path), bytes);
    }

    @JavascriptInterface
    public boolean copy(String pathFrom, String pathTo) {
        return PFiles.copy(path(pathFrom), path(pathTo));
    }

    @JavascriptInterface
    public boolean renameWithoutExtension(String path, String newName) {
        return PFiles.renameWithoutExtension(path(path), newName);
    }

    @JavascriptInterface
    public boolean rename(String path, String newName) {
        return PFiles.rename(path(path), newName);
    }

    @JavascriptInterface
    public boolean move(String path, String newPath) {
        return PFiles.move(path(path), newPath);
    }

    @JavascriptInterface
    public String getExtension(String fileName) {
        return PFiles.getExtension(fileName);
    }

    @JavascriptInterface
    public String getName(String filePath) {
        return PFiles.getName(filePath);
    }

    @JavascriptInterface
    public String getNameWithoutExtension(String filePath) {
        return PFiles.getNameWithoutExtension(filePath);
    }

    @JavascriptInterface
    public boolean remove(String path) {
        return PFiles.remove(path(path));
    }

    @JavascriptInterface
    public boolean removeDir(String path) {
        return PFiles.removeDir(path(path));
    }

    @JavascriptInterface
    public String getSdcardPath() {
        return PFiles.getSdcardPath();
    }

    @JavascriptInterface
    public String[] listDir(String path) {
        return PFiles.listDir(path(path));
    }

    @JavascriptInterface
    public String[] listDir(String path, Func1<String, Boolean> filter) {
        return PFiles.listDir(path(path), filter);
    }

    @JavascriptInterface
    public boolean isFile(String path) {
        return PFiles.isFile(path(path));
    }

    @JavascriptInterface
    public boolean isDir(String path) {
        return PFiles.isDir(path(path));
    }

    @JavascriptInterface
    public boolean isEmptyDir(String path) {
        return PFiles.isEmptyDir(path(path));
    }

    @JavascriptInterface
    public static String join(String parent, String... child) {
        return PFiles.join(parent, child);
    }

    @JavascriptInterface
    public String getHumanReadableSize(long bytes) {
        return PFiles.getHumanReadableSize(bytes);
    }

    @JavascriptInterface
    public String getSimplifiedPath(String path) {
        return PFiles.getSimplifiedPath(path);
    }

}
