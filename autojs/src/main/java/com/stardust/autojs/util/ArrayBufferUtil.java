package com.stardust.autojs.util;

import org.mozilla.javascript.typedarrays.NativeArrayBuffer;
import org.mozilla.javascript.typedarrays.NativeUint8Array;

public class ArrayBufferUtil {
    public static byte[] getBytes(NativeArrayBuffer arrayBuffer) {
        return arrayBuffer.getBuffer();
    }
    public static NativeArrayBuffer fromBytes(byte[] byteArray) {
        NativeArrayBuffer buffer = new NativeArrayBuffer(byteArray.length);
        System.arraycopy(byteArray, 0, buffer.getBuffer(), 0, byteArray.length);
        return buffer;
    }
}
