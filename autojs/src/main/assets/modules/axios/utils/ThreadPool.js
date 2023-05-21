(function() {
    importClass(java.util.concurrent.CompletableFuture);
    importClass(java.lang.Thread);
    importClass(java.util.concurrent.Executors);
    let {
        AsyncThreadLock,
    } = require("../browser-libs/utils.js");
    
    let atl = new AsyncThreadLock();
    
    let printError = true;
    let th = threads.currentThread();
    //log(threads.currentThread().setTimeout);
    function setCallback(callback) {
        if (!printError) return callback;
        return function() {
            try {
                return callback.apply(this, arguments);
            } catch (e) {
                console.error(e);
                throw e;
            }
        }
    }

    function run(callback) {
        callback = setCallback(callback);
        let p = new MyPromise(CompletableFuture.supplyAsync(callback));
        atl.addTask();
        return Promise.resolve(p).finally(()=>{
            atl.removeTask()
        });
    }
    let MyPromise = function(cf) {
        this.cf = cf;
    }

    MyPromise.prototype.then = function(fn, fn2) {
        let t = this.cf;
        if (fn) {
            t = t.thenAccept(function() {
                th.setImmediate(() => {
                    setCallback(fn).apply(this, arguments)
                })
            })
        }
        if (fn2) {
            t = t.exceptionally(function() {
                th.setImmediate(() => {
                    setCallback(fn2).apply(this, arguments)
                })
            })
        }
        return new MyPromise(t);
    }


    let PrThread = function() {
        this.es = Executors.newSingleThreadExecutor();
    }
    PrThread.prototype.run = function(callback) {
        callback = setCallback(callback);
        const fut = CompletableFuture.supplyAsync(callback, this.es);
        atl.addTask();
        return Promise.resolve(new MyPromise(fut)).finally(()=>{
            atl.removeTask();
        });
    }
    PrThread.prototype.close = function() {
        this.es.shutdown()
    }

    function createThread() {
        return new PrThread();
    }

    function setPrintError(b) {
        printError = b;
    }
    exports.setPrintError = setPrintError;
    exports.run = run;
    exports.create_thread = createThread;
})()