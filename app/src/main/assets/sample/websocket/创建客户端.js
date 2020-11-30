importPackage(Packages["okhttp3"]);
var client = new OkHttpClient.Builder().retryOnConnectionFailure(true).build();

var request = new Request.Builder().url("ws://192.168.31.164:9317").build();
client.dispatcher().cancelAll();//清理一次
myListener = {
    onOpen: function (webSocket, response) {
        print("onOpen");
        var json = {};
        json.type="hello";
        json.data= {device_name:"模拟设备",client_version:123,app_version:123,app_version_code:"233"};
        var hello=JSON.stringify(json);
        webSocket.send(hello);
    },
    onMessage: function (webSocket, msg) {
        print("msg");
        print(msg);
    },
    onClosing: function (webSocket, code, reason) {
        print("正在关闭");
    },
    onClosed: function (webSocket, code, reason) {
        print("关闭");
    },
    onFailure: function (webSocket, t, response) {
        print("错误");
        print( t);
    }
}
var webSocket= client.newWebSocket(request, new WebSocketListener(myListener));

setInterval(() => {

}, 1000);

