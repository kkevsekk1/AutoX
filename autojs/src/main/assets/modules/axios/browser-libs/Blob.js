(function() {
    let {
        setReadonlyAttribute
    } = require("./utils.js");
    //console.error('Blob对象')
    const cacheDir = new java.io.File(context.getExternalCacheDir(), 'blobCache');
    files.removeDir(cacheDir.toString())
    cacheDir.mkdirs();
    //////Blob对象
    /*
      该对象每个实例对应一个文件
    */
    const Blob = function() {
        this.type = '';
        this.size = 0;
    }
    Blob.prototype[Symbol.toStringTag] = 'Blob';
    
    Object.defineProperty(Blob, '_fileIndex', {
        value: 0,
        writable: true,
        configurable: true,
        enumerable: false,
    })
    setReadonlyAttribute(Blob, '_createBlobCache', function() {
        const blob = Object.create(Blob.prototype);
        const file = new java.io.File(cacheDir, '' + (Blob._fileIndex++) + '.blob');
        setReadonlyAttribute(blob, '_file', file, false);
        return blob;
    }, false)
    Blob.prototype.slice = function(start, end, contentType) {

    }
    Blob.prototype.arrayBuffer = function() {

    }
    module.exports = Blob;
})()