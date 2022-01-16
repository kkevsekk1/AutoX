"ui";
ui.layout(
    <vertical>
        <horizontal bg="#c7edcc" gravity="center" h="auto">
            <button text="远程网页" id="loadHtmlBtn" style="Widget.AppCompat.Button.Colored" w="auto" />
            <button text="本地游戏" id="loadLocalHtmlBtn" style="Widget.AppCompat.Button.Colored" w="auto" />
            <button text="控制台" id="consoleBtn" style="Widget.AppCompat.Button.Colored" w="auto" />
        </horizontal>
        <vertical h="*" w="*">
            <webview id="webView" layout_below="title" w="*" h="*" />
        </vertical>
    </vertical>
);

function callJavaScript(webViewWidget, script, callback) {
    try {
        console.assert(webViewWidget != null, "webView控件为空");
        //console.log(script.toString())
        webViewWidget.evaluateJavascript("javascript:" + script, new JavaAdapter(android.webkit.ValueCallback, {
            onReceiveValue: (val) => {
                if (callback) {
                    callback(val);
                }
            }
        }));
    } catch (e) {
        console.error("执行JavaScript失败");
        console.trace(e);
    }
}

function AutoX() {
    let getAutoXFrame = () => {
        let bridgeFrame = document.getElementById("AutoXFrame");
        if (!bridgeFrame) {
            bridgeFrame = document.createElement('iframe');
            bridgeFrame.id = "AutoXFrame";
            bridgeFrame.style = "display: none";
            document.body.append(bridgeFrame);
        }
        return bridgeFrame;
    };
    const h5Callbackers = {};
    let h5CallbackIndex = 1;
    let setCallback = (callback) => {
        let callId = h5CallbackIndex++;
        h5Callbackers[callId] = {
            "callback": callback
        };
        return callId;
    };
    let getCallback = (callId) => {
        let callback = h5Callbackers[callId];
        if (callback) {
            delete h5Callbackers[callId];
        }
        return callback;
    };

    function invoke(cmd, params, callback) {
        let callId = null;
        try {
            let paramsStr = JSON.stringify(params);
            let AutoXFrame = getAutoXFrame();
            callId = setCallback(callback);
            AutoXFrame.src = "jsbridge://" + cmd + "/" + callId + "/" + encodeURIComponent(paramsStr);
        } catch (e) {
            if (callId) {
                getCallback(callId);
            }
            console.trace(e);
        }
    };
    let callback = (data) => {
        let callId = data.callId;
        let params = data.params;
        let callbackFun = getCallback(callId);
        if (callbackFun) {
            callbackFun.callback(params);
        }
    };
    return {
        invoke: invoke,
        callback: callback
    };
};
function bridgeHandler_handle(cmd, params) {
    console.log('bridgeHandler处理 cmd=%s, params=%s', cmd, JSON.stringify(params));
    let fun = this[cmd];
    if (!fun) {
        throw new Error("cmd= " + cmd + " 没有定义实现");
    }
    let ret = fun(params)
    return ret;
}
function mFunction(params) {
    toastLog(params.toString());
    device.vibrate(120);
    return files.isDir('/storage/emulated/0/Download')//'toast提示成功';
}
function webViewExpand_init(webViewWidget) {
    webViewWidget.webViewClient = new JavaAdapter(android.webkit.WebViewClient, {
        onPageFinished: (webView, curUrl) => {
            try {
                // 注入 AutoX
                callJavaScript(webView, AutoX.toString() + ";var auto0 = AutoX();auto0.invoke('mFunction','This is AutoX!',(data) => {console.log('接收到callback1:' + JSON.stringify(data));});", null);
            } catch (e) {
                console.trace(e)
            }
        },
        shouldOverrideUrlLoading: (webView, request) => {
            let url = '';
            try {
                url = (request.a && request.a.a) || (request.url);
                if (url instanceof android.net.Uri) {
                    url = url.toString();
                }
                if (url.indexOf("jsbridge://") == 0) {
                    let uris = url.split("/");
                    let cmd = uris[2];
                    let callId = uris[3];
                    let params = java.net.URLDecoder.decode(uris[4], "UTF-8");
                    console.log('AutoX处理JavaScript调用请求: callId=%s, cmd=%s, params=%s', callId, cmd, params);
                    let result = null;
                    try {
                        result = bridgeHandler_handle(cmd, JSON.parse(params));
                    } catch (e) {
                        console.trace(e);
                        result = {
                            message: e.message
                        };
                    }
                    result = result || {};
                    webView.loadUrl("javascript:auto0.callback({'callId':" + callId + ", 'params': " + JSON.stringify(result) + "});");
                } else if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://") || url.startsWith("ws://") || url.startsWith("wss://")) {
                    webView.loadUrl(url);
                } else {
                }
                return true;
            } catch (e) {
                if (e.javaException instanceof android.content.ActivityNotFoundException) {
                    webView.loadUrl(url);
                } else {
                    toastLog('无法打开URL: ' + url);
                }
                console.trace(e);
            }
        },
        onReceivedError: (webView, webResourceRequest, webResourceError) => {
            let url = webResourceRequest.getUrl();
            let errorCode = webResourceError.getErrorCode();
            let description = webResourceError.getDescription();
            console.trace(errorCode + " " + description + " " + url);
        }
    });
    webViewWidget.webChromeClient = new JavaAdapter(android.webkit.WebChromeClient, {
        onConsoleMessage: (msg) => {
            console.log("[%s:%s]: %s", msg.sourceId(), msg.lineNumber(), msg.message());
        }
    });
}
webViewExpand_init(ui.webView)
let path = "file:" + files.path("game.html");
ui.webView.loadUrl(path);

ui.loadHtmlBtn.on("click", () => {
    webViewExpand_init(ui.webView);
    ui.webView.loadUrl("https://www.baidu.com/");
});
ui.consoleBtn.on("click", () => {
    app.startActivity("console");
});
ui.loadLocalHtmlBtn.on('click', () => {
    webViewExpand_init(ui.webView);
    let path = "file:" + files.path("game.html");
    ui.webView.loadUrl(path);
});
