package com.stardust.autojs.core.web;

import androidx.annotation.NonNull;

import com.stardust.autojs.core.eventloop.EventEmitter;
import com.stardust.autojs.runtime.ScriptBridges;
import com.stardust.autojs.runtime.ScriptRuntime;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public final class WebXSocket extends EventEmitter implements WebSocket {
    public WebSocketMessageListener mListener;

    public WebSocket mWebSocket;

    public WebXSocket(OkHttpClient okHttpClient, String url, ScriptRuntime scriptRuntime, boolean z) {
        super(scriptRuntime.bridges,z ?scriptRuntime.timers.getTimerForCurrentThread():null);
        if(okHttpClient == null || url == null){
            throw new NullPointerException();
        }
        this.mListener = new WebSocketMessageListener(this);
        this.mWebSocket = okHttpClient.newWebSocket(new Request.Builder().url(url).build(),this.mListener);
    }

    public WebXSocket(ScriptBridges bridges) {
        super(bridges);
    }

    public void cancel() {
        this.mWebSocket.cancel();
    }

    public boolean close(int paramInt, String paramString) {
        return this.mWebSocket.close(paramInt, paramString);
    }

    public long queueSize() {
        return this.mWebSocket.queueSize();
    }

    public Request request() {
        return this.mWebSocket.request();
    }

    public boolean send(@NonNull String paramString) {
        return this.mWebSocket.send(paramString);
    }

    public boolean send(@NonNull ByteString paramByteString) {
        return this.mWebSocket.send(paramByteString);
    }

    public final class WebSocketMessageListener extends WebSocketListener {
        public final WebXSocket webXSocket;

        public WebSocketMessageListener(WebXSocket webXSocket) {
            this.webXSocket = webXSocket;
        }

        public void onClosed(WebSocket webSocket, int param1Int, String param1String) {
            this.webXSocket.emit("closed",param1Int,param1String,this.webXSocket);
        }

        public void onClosing(WebSocket param1WebSocket, int param1Int, String param1String) {
            this.webXSocket.emit("closing",param1Int,param1String,this.webXSocket);
        }

        public void onFailure(WebSocket param1WebSocket, Throwable param1Throwable, Response param1Response) {
            this.webXSocket.emit("failure",param1Throwable,param1Response,this.webXSocket);
        }

        public void onMessage(WebSocket param1WebSocket, String param1String) {
            if(param1String!=null){
                this.webXSocket.emit("text", param1String, this.webXSocket);
            }else{
                throw new NullPointerException();
            }
        }

        public void onMessage(WebSocket param1WebSocket, ByteString param1ByteString) {
            if(param1ByteString !=null){
                this.webXSocket.emit("binary", param1ByteString, this.webXSocket);
            }else{
                throw new NullPointerException();
            }
        }

        public void onOpen(WebSocket param1WebSocket, Response param1Response) {
            if (param1Response != null) {
                this.webXSocket.emit("open", param1Response, this.webXSocket);
            }else{
                throw new NullPointerException();
            }
        }
    }
}
