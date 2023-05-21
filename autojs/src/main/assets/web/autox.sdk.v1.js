(function() {
    if (window.$autox) {
        return
    }
    const java = window.AutoxJavaBridge;
    window.$autox = {};
    const callBacks = {};
    let nextId = 1;
    //调用java方法
    $autox.callHandler = function(event, data, callBack) {
        const callBackId = nextId++;
        const reqData = {
            id:-1,
            event,
            data,
            callBackId: null,
        };
        if (typeof callBack === 'function') {
            callBacks[callBackId] = callBack;
            reqData.callBackId = callBackId;
        }
        java.callHandle(JSON.stringify(reqData));
    }
    $autox._requestHandlers = {};
    $autox.registerHandler = function(channel, handler) {
        $autox._requestHandlers[channel || ''] = handler;
        return this;
    }
    $autox.removeHandler = function(channel){
        $autox._requestHandlers[channel || ''] = null;
        return this;
    }
    //接收来自java的调用
    $autox._onCallBack = function(id) {
        const {data,callBackId} = JSON.parse(java.getCallBackData(id));
        let callBack = callBacks[callBackId];
        delete callBacks[callBackId];
        return callBack(data);
    }
    $autox._onCallHandler = function(id) {
        const {
            data,
            event,
            callBackId
        } = JSON.parse(java.getCallHandlerData(id));
        const handler = $autox._requestHandlers[event];
        if (typeof handler !== 'function') {
            throw new Error(`handler:${event} 不存在`)
        }
        return handler(data, callBackId ? (data) => {
            java.callBack(callBackId,data)
        } : null)
    }
    document.dispatchEvent(new Event('AutoxJsBridgeReady'))
})();
