importClass(java.io.OutputStream);
importClass(java.io.InputStream);
importClass(java.io.FileOutputStream);
importClass(java.io.FileInputStream);
importClass(java.util.concurrent.CompletableFuture);

const stream = require("stream-browserify");

function addTask(fn, callback) {
    CompletableFuture.runAsync(function() {
        try {
            const data = fn();
            return setImmediate(callback, null, data)
        } catch (e) {
            //console.error(e)
            return setImmediate(callback, e)
        }
    })
}

stream.fromInputStream = function(inp, options) {
    if (!(inp instanceof InputStream)) throw TypeError('需要InputStream流');
    options = options || {};
    options.highWaterMark = options.highWaterMark || (1024 * 64);
    options.autoDestroy = true;
    options.emitClose = true;
    options.destroy = function(e, cb) {
        this._inp.close();
        runtime.loopers.removeAsyncTask(this._task)
        return cb(e)
    }
    options.read = function(size) {
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
                this.push(null)
            }
            return this.push(buffer);
        })
    }
    const readable = new stream.Readable(options)
    setReadonlyAttribute(readable, '_inp', inp, false)
    setReadonlyAttribute(readable, '_task', runtime.loopers.createAndAddAsyncTask('stream'), false)
    return readable;
}

stream.fromOutputStream = function(out, options) {
    if (!(out instanceof OutputStream)) throw TypeError('需要OutputStream流');
    options = options || {};
    options.highWaterMark = options.highWaterMark || (1024 * 64);
    options.autoDestroy = true;
    options.emitClose = true;
    options.destroy = function(e, cb) {
        this._out.close();
        runtime.loopers.removeAsyncTask(this._task)
        return cb(e)
    }
    options.final = function(callback) {
        addTask(() => {
            this._out.flush();
        }, (err) => {
            callback();
        })
    }
    options.write = function(chunk, encoding, callback) {
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
    setReadonlyAttribute(writable, '_out', out, false);
    setReadonlyAttribute(writable, '_task', runtime.loopers.createAndAddAsyncTask('stream'), false)
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