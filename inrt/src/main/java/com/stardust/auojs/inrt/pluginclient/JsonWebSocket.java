package com.stardust.auojs.inrt.pluginclient;


import android.util.Log;

import com.dhh.websocket.Config;
import com.dhh.websocket.RxWebSocket;
import com.dhh.websocket.WebSocketSubscriber;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.subjects.PublishSubject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class JsonWebSocket {

    public static class Bytes {
        public final String md5;
        public final ByteString byteString;
        public final long timestamp;

        public Bytes(String md5, ByteString byteString) {
            this.md5 = md5;
            this.byteString = byteString;
            this.timestamp = System.currentTimeMillis();
        }
    }

    private static final String LOG_TAG = "JsonWebSocket";

    private WebSocket mWebSocket;
    private final JsonParser mJsonParser = new JsonParser();
    private final PublishSubject<JsonElement> mJsonElementPublishSubject = PublishSubject.create();
    private final PublishSubject<Bytes> mBytesPublishSubject = PublishSubject.create();
    private volatile boolean mClosed = false;

    public JsonWebSocket(OkHttpClient client, String url) {
        Config config = new Config.Builder()
                .setShowLog(true)           //show  log
                .setClient(client)   //if you want to set your okhttpClient
                .setShowLog(true, "inrt.connect")

                .setReconnectInterval(2, TimeUnit.SECONDS)  //set reconnect interval
                .build();
        RxWebSocket.setConfig(config);
        RxWebSocket.get(url)
                .subscribe(new WebSocketSubscriber() {
                    @Override
                    public void onOpen(@NonNull WebSocket webSocket) {
                        Log.d(LOG_TAG, "----链接打开----");
                        mWebSocket = webSocket;
                        DevPluginService.getInstance().connectionOnNext("连接成功");
                    }

                    @Override
                    public void onMessage(@NonNull String text) {
                        Log.d(LOG_TAG, "返回数据:" + text);
                        dispatchJson(text);
                    }

                    @Override
                    public void onMessage(@NonNull ByteString byteString) {
                        mBytesPublishSubject.onNext(new Bytes(byteString.md5().hex(), byteString));
                    }

                    @Override
                    protected void onReconnect() {
                        Log.d(LOG_TAG, "---------重连-------------");
                        DevPluginService.getInstance().connectionOnNext("正在重连...");
                    }

                    @Override
                    protected void onClose() {
                        Log.d(LOG_TAG, "onClose:");
                        DevPluginService.getInstance().connectionOnNext("已关闭");
                    }
                });

    }

    public Observable<JsonElement> data() {
        return mJsonElementPublishSubject;
    }

    public Observable<Bytes> bytes() {
        return mBytesPublishSubject;
    }

    public boolean write(JsonElement element) {
        String json = element.toString();
        Log.d(LOG_TAG, "write: length = " + json.length() + ", json = " + element);
        return mWebSocket.send(json);
    }

    public void close() {
        mJsonElementPublishSubject.onComplete();
        mClosed = true;
        mWebSocket.close(1000, "close");
    }

    private void close(Throwable e) {
        if (mClosed) {
            return;
        }
        mJsonElementPublishSubject.onError(e);
        mClosed = true;
        mWebSocket.close(1011, "remote exception: " + e.getMessage());
    }

    private void dispatchJson(String json) {
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.setLenient(true);
            JsonElement element = mJsonParser.parse(reader);
            mJsonElementPublishSubject.onNext(element);
        } catch (JsonParseException e) {
            e.printStackTrace();
        }

    }

    public boolean isClosed() {
        return mClosed;
    }
}


