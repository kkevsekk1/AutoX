package com.stardust.util;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    @NonNull
    public static byte[] md5Bytes(String message) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(message.getBytes());
            return md5.digest();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NonNull
    public static String md5(String message) {
        byte[] bytes = md5Bytes(message);
        StringBuilder hexString = new StringBuilder(32);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();


    }
}
