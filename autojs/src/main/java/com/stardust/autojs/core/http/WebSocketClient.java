package com.stardust.autojs.core.http;

import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebSocketClient extends WebSocketListener {
        private final WebSocket mWebSocket;
        public WebSocketClient(Map<String,Object> clientOptions){
            Request.Builder  bx=  new Request.Builder();

            OkHttpClient client = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();
            Request request = new Request.Builder().url("ws://10.17.145.23:9988").build();

            this.mWebSocket = client.newWebSocket(request,this);
        }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        System.out.println("连接打开");
        webSocket.send("发送了一条数据");
        webSocket.send("{\"FID\":\"003\",\"SUB\":\"OFEX.BTCPERP.Depth\"}");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        System.out.println("接收到消息：" + text);
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        System.out.println("这个可以不管，这个接收到是byte类型的");
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        System.out.println("连接关闭中");
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        System.out.println("连接关闭");
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        System.out.println("结束时，重连可以在这儿发起");
        t.printStackTrace();;
    }




}
