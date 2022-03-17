

module.exports = function(__runtime__, scope){
    scope.newInjectableWebClient = function(){
        return new com.stardust.autojs.core.web.InjectableWebClient(org.mozilla.javascript.Context.getCurrentContext(), scope);
    }

    scope.newInjectableWebView = function(activity){
        return new com.stardust.autojs.core.web.InjectableWebView(scope.activity, org.mozilla.javascript.Context.getCurrentContext(), scope);
    }

    return {
        newWebSocket: function (url, options) {
            options = options || {};
            return new com.stardust.autojs.core.web.WebXSocket(http.__okhttp__, url, runtime, options.eventThread == 'this');
        },
        ByteString: Packages.okio.ByteString
    };
}


