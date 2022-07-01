package com.stardust.autojs.core.http;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Stardust on 2018/4/11.
 */

public class MutableOkHttp extends OkHttpClient {

    private int mMaxRetries = 3;
    private long mTimeout = 30 * 1000;

    // okhttp3.Interceptor
    public Interceptor mRetryInterceptor = MutableOkHttp.this::retryResponse;

    @NonNull
    private Response retryResponse(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        int tryCount = 0;
        Response response = chain.proceed(request);
        while (!response.isSuccessful() && tryCount < getMaxRetries()) {
            tryCount++;
            response.close();
            response = chain.proceed(request);
        }
        return response;
    };

    public OkHttpClient mOkHttpClient = new OkHttpClient.Builder().readTimeout(getTimeout(), TimeUnit.MILLISECONDS).writeTimeout(getTimeout(), TimeUnit.MILLISECONDS).connectTimeout(getTimeout(), TimeUnit.MILLISECONDS).addInterceptor(this.mRetryInterceptor).build();

    public OkHttpClient client() {
        return mOkHttpClient;
    }

    public OkHttpClient newClient(OkHttpClient.Builder builder) {
        builder.readTimeout(getTimeout(), TimeUnit.MILLISECONDS).writeTimeout(getTimeout(), TimeUnit.MILLISECONDS).connectTimeout(getTimeout(), TimeUnit.MILLISECONDS);
        return builder.build();
    }

    public int getMaxRetries() {
        return mMaxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        mMaxRetries = maxRetries;
    }

    public long getTimeout() {
        return mTimeout;
    }


    public void setTimeout(long timeout) {
        mTimeout = timeout;
        muteClient();
    }

    public synchronized void muteClient(Builder builder) {
        mOkHttpClient = newClient(builder);
    }

    protected synchronized void muteClient() {
        mOkHttpClient = newClient(mOkHttpClient.newBuilder());
    }
}
