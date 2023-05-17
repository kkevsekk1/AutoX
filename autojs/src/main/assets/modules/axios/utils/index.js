
(function() {
    importClass(java.io.File);
    importClass(java.io.FileOutputStream);
    importClass(java.io.FileInputStream);
    const ThreadPool = require('./ThreadPool.js');

    const {
        setReadonlyAttribute,
        copyInputStream,
    } = require("../browser-libs/utils.js");
    const {
        Blob,
    } = require("../browser-libs/index.js");

    function saveBlobToFile(blob, path) {
        let file = new File(files.path(path));
        return ThreadPool.run(()=>{
           let is = blob._file.renameTo(file);
           if (!is){
               const inp = new FileInputStream(blob._file)
               const out = new FileOutputStream(file)
               try {
                   copyInputStream(inp,out,64*1024);
                   blob._file.delete();
               }finally{
                   inp.close();
                   out.close();
               }
           }
        }).then(()=>{
            setReadonlyAttribute(blob, '_file', file, false);
        })
    }

    function openFile(path) {
        let file = new File(files.path(path));
        if (!file.isFile()) {
            throw new Error('文件不存在')
        }
        let blob = Object.create(Blob.prototype);
        setReadonlyAttribute(blob, '_file', file, false);
        blob.size = file.length();
        blob.name = file.getName();
        let ext = files.getExtension(blob.name);
        blob.lastModified = file.lastModified();
        blob.type = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) || "application/octet-stream";
        return blob;
    }

    module.exports = {
        copyInputStream,
        saveBlobToFile,
        openFile,
        ThreadPool,
    }
})()