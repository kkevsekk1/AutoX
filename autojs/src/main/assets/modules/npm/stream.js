
const stream = require("stream-browserify");
importClass(java.io.OutputStream);
importClass(java.io.InputStream);
importClass(java.io.FileOutputStream);
importClass(java.io.FileInputStream);

const mainTherad = threads.currentThread();

const ioLock = {
    lock: threads.lock(),
    ioThread: null,
    tasks: 0,
    addTask() {
        this.lock.lock();
        this.tasks++;
        if (this.ioThread == null) {
            const dis = threads.disposable();
            this.ioThread = threads.start(function () {
                dis.setAndNotify(true);
                const id = setInterval(() => {
                    this.lock.lock();
                    if (this.tasks === 0) {
                        clearInterval(id);
                        this.ioThread = null;
                    }
                    this.lock.unlock();
                }, 200)
            }.bind(this));
            dis.blockedGet() //等待线程创建完成
        }
        this.lock.unlock();
    },
    removeTask() {
        this.lock.lock();
        this.tasks--;
        this.lock.unlock();
    }

}

function addTask(task, callback) {
    return ioLock.ioThread.setImmediate(function () {
        try {
            const data = task();
            return callback(null, data)
        } catch (e) {
            return callback(e)
        }
    })
}

stream.fromInputStream = function (inp, options) {
    if (!(inp instanceof InputStream)) throw TypeError('需要InputStream流');
    ioLock.addTask();
    options = options || {};
    options.highWaterMark = options.highWaterMark || (1024 * 64);
    options.autoDestroy = true;
    options.emitClose = true;
    options.destroy = function (e, cb) {
        this._inp.close();
        ioLock.removeTask();
        return cb(e)
    }
    options.read = function (size) {
        addTask(() => {
            const buffer = Buffer.alloc(size);
            const bytes = buffer.getBytes();
            const i = this._inp.read(bytes);
            if (buffer.length !== i) {
                if (i < 0) return null;
                return Buffer.from(buffer.buffer, 0, i);
            }
            return buffer
        }, (err, buffer) => {
            if (err) return this.destroy(err);
            if (buffer === null) {
                mainTherad.setImmediate(() => this.push(null));
            }
            return this.push(buffer);
        })
    }
    const readable = new stream.Readable(options)
    setReadonlyAttribute(readable, "_inp", inp, false)
    return readable;
}

stream.fromOutputStream = function (out, options) {
    if (!(out instanceof OutputStream)) throw TypeError('需要OutputStream流');
    ioLock.addTask();
    options = options || {};
    options.highWaterMark = options.highWaterMark || (1024 * 64);
    options.autoDestroy = true;
    options.emitClose = true;
    options.destroy = function (e, cb) {
        this._out.close();
        ioLock.removeTask()
        return cb(e)
    }
    options.final = function (callback) {
        addTask(() => {
            this._out.flush();
        }, (err) => {
            mainTherad.setImmediate(callback)
        })
    }
    options.write = function (chunk, encoding, callback) {
        addTask(() => {
            const buffer = (chunk instanceof Buffer) ? chunk : Buffer.from(chunk, encoding);
            const bytes = buffer.getBytes()
            this._out.write(bytes, 0, buffer.length);
        }, (err) => {
            if (err) return callback(err);
            return callback();
        })
    }
    const writable = new stream.Writable(options)
    setReadonlyAttribute(writable, "_out", out, false)
    return writable;
}
stream.createFileReadStream = function (path, bufferSize) {
    return stream.fromInputStream(new FileInputStream(
        files.path(path)), {
        highWaterMark: bufferSize || (256 * 1024)
    })
}
stream.createFileWriteStream = function (path, bufferSize) {
    return stream.fromOutputStream(new FileOutputStream(
        files.path(path)), {
        highWaterMark: bufferSize || (256 * 1024)
    })
}

function setReadonlyAttribute(obj, att, value, enumerable) {
    enumerable = (enumerable === undefined) ? true : enumerable
    Object.defineProperty(obj, att, {
        value,
        writable: false,
        configurable: true,
        enumerable,
    })
}
module.exports = stream