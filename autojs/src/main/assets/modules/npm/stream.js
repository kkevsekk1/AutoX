
const stream = require("stream-browserify");
importClass(java.io.OutputStream);
importClass(java.io.InputStream);
importClass(java.io.FileOutputStream);
importClass(java.io.FileInputStream);

const ioLock = {
    lock: threads.lock(),
    ioThread: null,
    tasks: 0,
    addTask() {
        this.lock.lock();
        if (this.ioThread == null) {
            this.ioThread = threads.start(function() {
                const id = setInterval(() => {
                    this.lock.lock();
                    if (this.tasks == 0) {
                        clearInterval(id);
                        this.ioThread = null;
                    }
                    this.lock.unlock();
                }, 200)
            }.bind(this))
        }
        this.tasks++;
        this.lock.unlock();
    },
    removeTask() {
        this.lock.lock();
        this.tasks--;
        this.lock.unlock();
    }

}

function addTask(callback) {
    return ioLock.ioThread.setImmediate(callback)
}

stream.fromInputStream = function(inp, options) {
    if (!(inp instanceof InputStream)) throw TypeError('需要InputStream流');
    ioLock.addTask();
    options = options || {};
    options.highWaterMark = options.highWaterMark || (1024 * 64);
    options.autoDestroy = true;
    options.emitClose = true;
    options.destroy = function(e, cb) {
        this._inp.close();
        ioLock.removeTask();
        return cb(e)
    }
    options.read = function(size) {
        addTask(() => {
            let buffer = Buffer.alloc(size);
            const bytes = buffer.getBytes()
            try {
                const i = this._inp.read(bytes);
                if (buffer.length !== i) {
                    buffer = Buffer.from(buffer.buffer, 0, i);
                    setImmediate(() => {
                        this.push(buffer);
                        this.push(null)
                    })
                    return
                }
                return setImmediate(() => this.push(buffer))
            } catch (e) {
                return this.destroy(e)
            }
        })
    }
    const readable = new stream.Readable(options)
    readable._inp = inp;
    return readable;
}

stream.fromOutputStream = function(out, options) {
    if (!(out instanceof OutputStream)) throw TypeError('需要OutputStream流');
    ioLock.addTask();
    options = options || {};
    options.highWaterMark = options.highWaterMark || (1024 * 64);
    options.autoDestroy = true;
    options.emitClose = true;
    options.destroy = function(e, cb) {
        this._out.close();
        ioLock.removeTask()
        return cb(e)
    }
    options.final = function(callback) {
        addTask(() => {
            this._out.flush()
            return callback();
        })
    }
    options.write = function(chunk, encoding, callback) {
        addTask(() => {
            try {
                const buffer = (chunk instanceof Buffer) ? chunk : Buffer.from(chunk, encoding);
                const bytes = buffer.getBytes();
                this._out.write(bytes, 0, buffer.length);
                return setImmediate(callback);
            } catch (e) {
                return callback(e)
            }
        })
    }
    const writable = new stream.Writable(options)
    writable._out = out;
    return writable;
}
stream.createFileReadStream = function(path, bufferSize) {
    return stream.fromInputStream(new FileInputStream(
        files.path(path)), {
        highWaterMark: bufferSize || (256 * 1024)
    })
}
stream.createFileWriteStream = function(path, bufferSize) {
    return stream.fromOutputStream(new FileOutputStream(
        files.path(path)), {
        highWaterMark: bufferSize || (256 * 1024)
    })
}
module.exports = stream;