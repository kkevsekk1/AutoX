package com.stardust.autojs.apkbuilder.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Stardust on 2017/10/23.
 */

public class StreamUtils {


    public static void write(InputStream inputStream, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) > 0) {
            out.write(buffer, 0, len);
        }
    }

    public static byte[] readAsBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, len);
        }
        return outputStream.toByteArray();
    }
}
