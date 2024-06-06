package com.stardust.util;

import java.lang.reflect.Array;

/**
 * Created by Stardust on 2017/5/8.
 */

public class ArrayUtils {


    public static Integer[] box(int[] array) {
        Integer[] box = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            box[i] = array[i];
        }
        return box;
    }


    public static int[] unbox(Integer[] array) {
        int[] unbox = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            unbox[i] = array[i];
        }
        return unbox;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T[] a1, T[] a2) {
        T[] a = (T[]) Array.newInstance(a1.getClass().getComponentType(), a1.length + a2.length);
        System.arraycopy(a1, 0, a, 0, a1.length);
        System.arraycopy(a2, 0, a, a1.length, a2.length);
        return a;
    }
}
