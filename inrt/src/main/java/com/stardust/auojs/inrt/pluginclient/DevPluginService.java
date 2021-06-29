package com.stardust.auojs.inrt.pluginclient;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.stardust.app.GlobalAppContext;
import com.stardust.auojs.inrt.BuildConfig;
import com.stardust.auojs.inrt.Pref;
import com.stardust.util.MapBuilder;


import java.io.File;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.subjects.PublishSubject;
import okhttp3.OkHttpClient;

/**
 * Created by Stardust on 2017/5/11.
 */

public class DevPluginService {

    private static final int CLIENT_VERSION = 2;
    private static final String LOG_TAG = "DevPluginService";
    private static final String TYPE_HELLO = "hello";
    private static final String TYPE_BYTES_COMMAND = "bytes_command";
    private static final long HANDSHAKE_TIMEOUT = 10 * 1000;
    private static String tmpMessageId = "";
    private static String tmpHost;
    private boolean debug = false;

    public static class State {

        public static final int DISCONNECTED = 0;
        public static final int CONNECTING = 1;
        public static final int CONNECTED = 2;

        private final int mState;
        private final Throwable mException;

        public State(int state, Throwable exception) {
            mState = state;
            mException = exception;
        }

        public State(int state) {
            this(state, null);
        }

        public int getState() {
            return mState;
        }

        public Throwable getException() {
            return mException;
        }
    }

    private static final int PORT = 9317;
    private static DevPluginService sInstance;
    private final PublishSubject<State> mConnectionState = PublishSubject.create();
    private final DevPluginResponseHandler mResponseHandler;
    private final HashMap<String, JsonWebSocket.Bytes> mBytes = new HashMap<>();
    private final HashMap<String, JsonObject> mRequiredBytesCommands = new HashMap<>();
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private volatile JsonWebSocket mSocket =new JsonWebSocket();

    public static DevPluginService getInstance() {
        if (sInstance == null) {
            sInstance = new DevPluginService();
        }
        return sInstance;
    }


    public DevPluginService() {
        File cache = new File(GlobalAppContext.get().getCacheDir(), "remote_project");
        mResponseHandler = new DevPluginResponseHandler(cache);
    }

    @AnyThread
    public boolean isConnected() {
        return mSocket != null && !mSocket.isClosed();
    }

    @AnyThread
    public boolean isDisconnected() {
        return mSocket == null || mSocket.isClosed();
    }

    @AnyThread
    public void disconnectIfNeeded() {
        if (isDisconnected())
            return;
        disconnect();
    }

    @AnyThread
    public void disconnect() {
        mSocket.close();
        mSocket = null;
    }

    public Observable<State> connectionState() {
        return mConnectionState;
    }

    public void connectionOnNext(String msg) {
        mConnectionState.onNext(new State(State.DISCONNECTED, new SocketTimeoutException(msg)));
    }

    @AnyThread
    public Observable<JsonWebSocket> connectToServer(String host, String params) {
        int port = PORT;
        String ip = host;
        int i = host.lastIndexOf(':');
        if (i > 0 && i < host.length() - 1) {
            port = Integer.parseInt(host.substring(i + 1));
            ip = host.substring(0, i);
        }
        mConnectionState.onNext(new State(State.CONNECTING));
        return socket(ip, port, params)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onSocketError);
    }

    @AnyThread
    private Observable<JsonWebSocket> socket(String ip, int port, String params) {
        OkHttpClient client = new OkHttpClient.Builder()
                .pingInterval(2, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build();
        String url = ip + ":" + port;
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            url = "ws://" + url;
        }
        url = url + "?" + params;
        if(null==mSocket){
            mSocket = new JsonWebSocket();
        }
        mSocket.createConnect(client,url);
        subscribeMessage(mSocket);
        return Observable.just(mSocket);
    }

    @SuppressLint("CheckResult")
    private void subscribeMessage(JsonWebSocket socket) {
        socket.data()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(() -> mConnectionState.onNext(new State(State.DISCONNECTED)))
                .subscribe(data -> onSocketData(socket, data), this::onSocketError);
        socket.bytes()
                .doOnComplete(() -> mConnectionState.onNext(new State(State.DISCONNECTED)))
                .subscribe(data -> onSocketData(socket, data), this::onSocketError);
    }

    @MainThread
    private void onSocketError(Throwable e) {
        e.printStackTrace();
        if (mSocket != null) {
            mConnectionState.onNext(new State(State.DISCONNECTED, e));
            mSocket.close();
            mSocket = null;
        }
    }

    @MainThread
    private void onSocketData(JsonWebSocket jsonWebSocket, JsonElement element) {
        Log.d("-------", "runScript: ");
        if (!element.isJsonObject()) {
            Log.w(LOG_TAG, "onSocketData: not json object: " + element);
            return;
        }
        try {
            JsonObject obj = element.getAsJsonObject();
            String tmp = obj.get("message_id").getAsString();
            if (tmpMessageId.equals(tmp)) {
                Log.w(LOG_TAG, "重复消息id=>" + tmp);
                return;
            }
            tmpMessageId = tmp;
            JsonElement typeElement = obj.get("type");
            if (typeElement == null || !typeElement.isJsonPrimitive()) {
                return;
            }
            String type = typeElement.getAsString();
            if (type.equals(TYPE_HELLO)) {
                onServerHello(jsonWebSocket, obj);
                return;
            }
            if (TYPE_BYTES_COMMAND.equals(type)) {
                String md5 = obj.get("md5").getAsString();
                JsonWebSocket.Bytes bytes = mBytes.remove(md5);
                if (bytes != null) {
                    handleBytes(obj, bytes);
                } else {
                    mRequiredBytesCommands.put(md5, obj);
                }
                return;
            }
            mResponseHandler.handle(obj);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @SuppressLint("CheckResult")
    private void handleBytes(JsonObject obj, JsonWebSocket.Bytes bytes) {
        mResponseHandler.handleBytes(obj, bytes)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(dir -> {
                    obj.get("data").getAsJsonObject().add("dir", new JsonPrimitive(dir.getPath()));
                    mResponseHandler.handle(obj);
                });

    }

    @WorkerThread
    private void onSocketData(JsonWebSocket JsonWebSocket, JsonWebSocket.Bytes bytes) {
        JsonObject command = mRequiredBytesCommands.remove(bytes.md5);
        if (command != null) {
            handleBytes(command, bytes);
        } else {
            mBytes.put(bytes.md5, bytes);
        }
    }

    @WorkerThread
    public void sayHelloToServer(int usercode) {
        if (!isConnected())
            return;
        writeMap(mSocket, TYPE_HELLO, new MapBuilder<String, Object>()
                .put("device_name", Build.BRAND + " " + Build.MODEL)
                .put("usercode", usercode)
                .put("client_version", CLIENT_VERSION)
                .put("app_version", BuildConfig.VERSION_NAME)
                .put("app_version_code", BuildConfig.VERSION_CODE)
                .build());

    }

    @MainThread
    private void onServerHello(JsonWebSocket jsonWebSocket, JsonObject message) {
        Log.i(LOG_TAG, "onServerHello: " + message);
        String msg = null;
        try {
            msg = message.get("data").getAsString();
        } catch (Exception e) {
        }
        if ("连接中...".equals(msg)) {
            sayHelloToServer(Integer.parseInt(Pref.getCode("-1")));
        }
        try {
            if (!message.get("debug").isJsonNull()) {
                boolean debug = message.get("debug").getAsBoolean();
                this.debug = debug;
            }
        } catch (Exception e) {
        }
        mSocket = jsonWebSocket;
        mConnectionState.onNext(new State(State.CONNECTED, new SocketTimeoutException(msg)));
    }

    @AnyThread
    private static boolean write(JsonWebSocket socket, String type, JsonObject data) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.add("data", data);
        return socket.write(json);
    }

    @AnyThread
    private static boolean writePair(JsonWebSocket socket, String type, Pair<String, String> pair) {
        JsonObject data = new JsonObject();
        data.addProperty(pair.first, pair.second);
        return write(socket, type, data);
    }

    @AnyThread
    private static boolean writeMap(JsonWebSocket socket, String type, Map<String, ?> map) {
        JsonObject data = new JsonObject();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                data.addProperty(entry.getKey(), (String) value);
            } else if (value instanceof Character) {
                data.addProperty(entry.getKey(), (Character) value);
            } else if (value instanceof Number) {
                data.addProperty(entry.getKey(), (Number) value);
            } else if (value instanceof Boolean) {
                data.addProperty(entry.getKey(), (Boolean) value);
            } else if (value instanceof JsonElement) {
                data.add(entry.getKey(), (JsonElement) value);
            } else {
                throw new IllegalArgumentException("cannot put value " + value + " into json");
            }
        }

        return write(socket, type, data);
    }


    @SuppressLint("CheckResult")
    @AnyThread
    public void log(String log) {
        if (!isConnected()) {
            return;
        }
        if (debug) {
            writePair(mSocket, "log", new Pair<>("log", log));
        }
    }
}
