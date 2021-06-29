package com.stardust.auojs.inrt.pluginclient;


import android.text.TextUtils;
import android.util.Log;

import com.dhh.websocket.Config;
import com.dhh.websocket.RxWebSocket;
import com.dhh.websocket.WebSocketSubscriber;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
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
    private final JsonParser mJsonParser = new JsonParser();
    private final PublishSubject<JsonElement> mJsonElementPublishSubject = PublishSubject.create();
    private final PublishSubject<Bytes> mBytesPublishSubject = PublishSubject.create();
    private volatile String url;
    private volatile boolean opened =false;
    private int index =0;

    public JsonWebSocket() {
    }
    public void createConnect(OkHttpClient client, String url){
        this.url = url;
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
                        opened =true;
                        Log.d(LOG_TAG, "----链接打开----");
                        String json = "{\"data\":\"连接中...\",\"type\":\"hello\",\"message_id\":\"1615128788594" + new Random().nextInt(1000) + "\"}";
                        dispatchJson(json);
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
                        opened=false;
                        Log.d(LOG_TAG, "---------重连-------------");
                        DevPluginService.getInstance().connectionOnNext("正在重连"+index);
                        index++;
                    }

                    @Override
                    protected void onClose() {
                        opened=false;
                        Log.d(LOG_TAG, "onClose:");
                        DevPluginService.getInstance().connectionOnNext("已关闭");
                        JsonWebSocket.this.url=null;
                    }

                    @Override
                    public void onError(Throwable e) {
                        opened=false;
                        Log.e(LOG_TAG, "---------链接错误-------------");
                        DevPluginService.getInstance().connectionOnNext("链接错误"+e.getMessage());
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
        if (!isClosed()) {
            RxWebSocket.send(url, json);
            return true;
        } else {
            return false;
        }
    }

    public void close() {
        if (TextUtils.isEmpty(url)) {
            Disposable disposable = RxWebSocket.get(url).subscribe();
            if (disposable != null && !disposable.isDisposed()) {
                disposable.dispose();
            }
            if (TextUtils.isEmpty(url)) {
                return;
            }
            url = null;
        }
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
        return !opened;
    }
}


