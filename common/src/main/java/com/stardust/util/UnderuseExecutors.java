package com.stardust.util;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Stardust on 2017/5/2.
 */

public class UnderuseExecutors {

    private static final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    public static ExecutorService getExecutor() {
        return mExecutor;
    }
}
