
module.exports = function (__runtime__, scope) {
    var threads = Object.create(__runtime__.threads);

    threads.runAsync = function (fn) {
        return new Promise(function (resolve, reject) {
            __runtime__.threads.runTaskForThreadPool(function() {
                try {
                    const result = fn();
                    setImmediate(resolve, result)
                } catch (e) {
                    setImmediate(reject, e)
                }
            })
        })
    }
    scope.sync = function (func, lock) {
        lock = lock || null;
        return new org.mozilla.javascript.Synchronizer(func, lock);
    }

    global.Promise.prototype.wait = function () {
        var disposable = threads.disposable();
        this.then(result => {
            disposable.setAndNotify({ result: result });
        }).catch(error => {
            disposable.setAndNotify({ error: error });
        });
        var r = disposable.blockedGet();
        if (r.error) {
            throw r.error;
        }
        return r.result;
    }

    return threads;
}