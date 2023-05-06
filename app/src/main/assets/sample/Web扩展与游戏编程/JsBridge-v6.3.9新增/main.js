"ui";

ui.layout(`
    <vertical>
        <webview id="web" h="*"/>
    </vertical>`)

ui.web.loadUrl("file://" + files.path("./网页.html"))
/*
    注意：在web与安卓端传递的数据只能是字符串，其他数据需自行使用JSON序列化
    在调用callHandler时传入了回调函数，但web端没有调用则会造成内存泄露。
    jsBridge自动注入依赖于webViewClient，如设置了自定义webViewClient则需要在合适的时机（页面加载完成后）调用webview.injectionJsBridge()手动注入
*/
//注册一个监听函数
ui.web.jsBridge.registerHandler("test", (data, callBack) => {
    toastLog("web调用安卓,data:" + data)
    setTimeout(() => {
        //回调web
        callBack("1155")
    }, 2000)
})
//定时器中等待web加载完成
setTimeout(() => {
    ui.web.jsBridge.callHandler('jsTest', '数据', (data) => {
        toastLog('web回调,data:' + data)
    })
}, 1000)