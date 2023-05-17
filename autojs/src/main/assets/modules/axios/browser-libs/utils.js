(function() {
    function setReadonlyAttribute(obj, att, value, enumerable) {
        enumerable = (enumerable === undefined) ? true : enumerable
        Object.defineProperty(obj, att, {
            value,
            writable: false,
            configurable: true,
            enumerable,
        })
    }

    function copyInputStream(inputstream, outputstream) {
        let buffer = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, 1024 * 16);
        let length = 0;
        while ((length = inputstream.read(buffer)) > 0) {
            outputstream.write(buffer, 0, length);
        }
        outputstream.flush();
    }
    //此对象用于确保在多线程执行异步任务时主线程不会因空闲而停止
    function AsyncThreadLock() {
        this.lock = threads.lock();
        this.tasks = 0;
        this.intervalId = null;
    }
    setReadonlyAttribute(AsyncThreadLock, 'teeFn', function() {
        this.intervalId = setInterval(() => {
            this.lock.lock();
            if (this.tasks === 0) {
                clearInterval(this.intervalId)
                this.intervalId = null;
            }
            this.lock.unlock();
        }, 200)
    }, false)

    AsyncThreadLock.prototype.addTask = function() {
        this.lock.lock();
        this.tasks++;
        if (this.intervalId === null) {
            AsyncThreadLock.teeFn.call(this)
        }
        this.lock.unlock();
    }
    AsyncThreadLock.prototype.removeTask = function() {
        this.lock.lock();
        this.tasks--;
        this.lock.unlock();
    }

    module.exports = {
        setReadonlyAttribute,
        copyInputStream,
        AsyncThreadLock,
    }
})()