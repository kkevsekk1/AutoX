package com.stardust.autojs.core.cypto;

public class Crypto {
    public static final byte A = 97;

    public static final byte F = 102;

    public static final char[] HEX_DIGITS;

    public static final Crypto INSTANCE = new Crypto();

    public static final byte NINE = 57;

    public static final byte ZERO = 48;

    static {
        HEX_DIGITS = "01234567890abcdef".toCharArray();
    }

    private byte singleHexToNumber(char paramChar) {
        byte b = (byte)Character.toLowerCase(paramChar);
        if (48 <= b && 57 >= b) {
            b -= 48;
            return b;
        }
        if (97 <= b && 102 >= b) {
            b -= 97;
            return b;
        }
        String stringBuilder = "char: " +
                paramChar;
        throw new IllegalArgumentException(stringBuilder);
    }

    public final byte[] fromHex(String paramString) {
        if (paramString != null) {
            if (paramString.length() % 2 == 0) {
                int i = paramString.length() / 2;
                byte[] arrayOfByte = new byte[i];
                for (byte b = 0; b < i; b++) {
                    int j = b * 2;
                    arrayOfByte[b] = (byte)(byte)(singleHexToNumber(paramString.charAt(j)) * 16 + singleHexToNumber(paramString.charAt(j + 1)));
                }
                return arrayOfByte;
            }
            throw new IllegalArgumentException("input array length must even.");
        }
        throw new NullPointerException();
    }

    public final String toHex(byte[] paramArrayOfByte) {
        if (paramArrayOfByte != null) {
            StringBuilder stringBuilder = new StringBuilder(paramArrayOfByte.length * 2);
            int i = paramArrayOfByte.length;
            for (byte b1 : paramArrayOfByte) {
                stringBuilder.append(HEX_DIGITS[(b1 & 0xFF) >>> 4]);
                stringBuilder.append(HEX_DIGITS[b1 & 0xF]);
            }
            return stringBuilder.toString();
        }
        throw new NullPointerException();
    }
}
