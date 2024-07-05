// 新建一个WebSocket
// 指定web socket的事件回调在当前线程（好处是没有多线程问题要处理，坏处是不能阻塞当前线程，包括死循环）
// 不加后面的参数则回调在IO线程
let ws = web.newWebSocket("wss://demo.piesocket.com/v3/channel_1?notify_self", {
    eventThread: 'this'
});
console.show();
// 监听他的各种事件
ws.on("open", (res, ws) => {
    log("WebSocket已连接");
}).on("failure", (err, res, ws) => {
    log("WebSocket连接失败");
    console.error(err);
}).on("closing", (code, reason, ws) => {
    log("WebSocket关闭中");
}).on("text", (text, ws) => {
    console.info("收到文本消息: ", text);
}).on("binary", (bytes, ws) => {
    console.info("收到二进制消息:");
    console.info("hex: ", bytes.hex());
    console.info("base64: ", bytes.base64());
    console.info("md5: ", bytes.md5());
    console.info("size: ", bytes.size());
    console.info("bytes: ", bytes.toByteArray());
}).on("closed", (code, reason, ws) => {
    log("WebSocket已关闭: code = %d, reason = %s", code, reason);
});

// 发送文本消息
log("发送消息: Hello, WebSocket!");
ws.send("h");
setTimeout(() => {
    // 两秒后发送二进制消息
   log("发送二进制消息: 5piO5aSp5L2g6IO96ICDMTAw5YiG44CC");
   ws.send(web.ByteString.decodeBase64("5piO5aSp5L2g6IO96ICDMTAw5YiG44CC"));
}, 2000);
setTimeout(() => {
    // 8秒后断开WebSocket
    log("断开WebSocket");
    // 1000表示正常关闭
    ws.close(1000, null);
}, 8000);
setTimeout(() => {
    log("退出程序");
}, 12000)
