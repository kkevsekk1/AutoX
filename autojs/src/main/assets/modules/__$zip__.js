
module.exports = function (runtime, global) {
    let ZipFile = Packages.net.lingala.zip4j.core.ZipFile;
    let File = java.io.File;
    let ArrayList = java.util.ArrayList;
    let bridges = require("__bridges__");
    let $files = global.$files;
    let Zip4jConstants = Packages.net.lingala.zip4j.util.Zip4jConstants;

    function $zip() {

    }

    $zip.open = function (file) {
        let zip = new Zip(file);
        return zip;
    }

    $zip.zipFile = function (file, dest, options) {
        let zip = new Zip(dest);
        zip._zip.createZipFile(new File($files.path(file)), Zip.buildZipParameters(options));
        return zip;
    }

    $zip.zipDir = function (dir, dest, options) {
        let zip = new Zip(dest);
        zip._zip.createZipFileFromFolder($files.path(dir), Zip.buildZipParameters(options), false, -1);
        return zip;
    }

    $zip.zipFiles = function (fileList, dest, options) {
        let list = new ArrayList();
        fileList.forEach(file => {
            list.add(new File($files.path(file)));
        });
        let zip = new Zip(dest);
        zip._zip.createZipFile(list, Zip.buildZipParameters(options));
        return zip;
    }

    $zip.unzip = function (zipFile, dest, options) {
        let zip = new Zip(zipFile);
        if (options && typeof (options.password) != 'undefined') {
            zip.setPassword(options.password);
        }
        zip.extractAll(dest, options);
    }

    function Zip(path) {
        this._path = $files.path(path);
        this._zip = new ZipFile(this._path);
    }

    Zip.buildZipParameters = function (options) {
        let parameters = new Packages.net.lingala.zip4j.model.ZipParameters();
        if (!options) {
            return parameters;
        }
        options = Object.assign({}, options);
        if (typeof (options.password) == 'string') {
            options.password = new java.lang.String(options.password).toCharArray();
        }
        if (typeof (options.compressionMethod) == 'string') {
            options.compressionMethod = Zip.parseConstanst(options.compressionMethod);
        }
        if (typeof (options.encryptionMethod) == 'string') {
            options.encryptionMethod = Zip.parseConstanst(options.encryptionMethod);
        }
        if (typeof (options.aesKeyStrength) == 'string') {
            options.aesKeyStrength = Zip.parseConstanst(options.aesKeyStrength);
        }
        if(options.password !== undefined) {
            options.encryptFiles = true;
            options.encryptionMethod = options.encryptionMethod || Zip4jConstants.ENC_METHOD_AES;
            options.aesKeyStrength = options.aesKeyStrength || Zip4jConstants.AES_STRENGTH_256;
        }
        for (let key in options) {
            if (options.hasOwnProperty(key)) {
                parameters[key] = options[key];
            }
        }
        return parameters;
    }

    Zip.parseConstanst = function (name) {
        return Zip4jConstants[name.toUpperCase()];
    }

    Zip.buildUnzipParameters = function (options) {
        let parameters = new Packages.net.lingala.zip4j.model.UnzipParameters();
        if (!options || !options.ignoreAttribute) {
            return parameters;
        }
        if (!Array.isArray(options.ignoreAttribute)) {
            throw new TypeError();
        }
        options.ignoreAttribute.forEach(i => {
            if (i === 'dateTime') {
                parameters.ignoreDateTimeAttributes = true;
            } else {
                let normName = i.substring(0, 1).toUpperCase() + i.substring(1);
                parameters['ignore' + normName + 'FileAttribute'] = true;
            }
        });
        return parameters;
    }

    Zip.prototype.addFile = function (file, options) {
        this._zip.addFile(new File($files.path(file)), Zip.buildZipParameters(options));
    }

    Zip.prototype.addFiles = function (fileList, options) {
        let list = new ArrayList();
        fileList.forEach(file => {
            list.add(new File($files.path(file)));
        })
        this._zip.addFiles(list, Zip.buildZipParameters(options));
    }

    Zip.prototype.addFolder = function (file, options) {
        this._zip.addFolder(new File($files.path(file)), Zip.buildZipParameters(options));
    }

    Zip.prototype.extractAll = function (dest, options) {
        this._zip.extractAll($files.path(dest), Zip.buildUnzipParameters(options));
    }

    Zip.prototype.extractFile = function (file, dest, options, newFileName) {
        newFileName = typeof (newFileName) == 'undefined' ? null : newFileName;
        this._zip.extractFile(file, $files.path(dest), Zip.buildUnzipParameters(options), newFileName);
    }

    Zip.prototype.setPassword = function (password) {
        this._zip.setPassword(password);
    }

    Zip.prototype.getFileHeader = function (file) {
        return this._zip.getFileHeader(file);
    }

    Zip.prototype.getFileHeaders = function () {
        return bridges.toArray(this._zip.getFileHeaders());
    }

    Zip.prototype.isEncrypted = function () {
        return this._zip.isEncrypted();
    }

    Zip.prototype.removeFile = function (file) {
        this._zip.removeFile(file);
    }

    Zip.prototype.isValidZipFile = function () {
        return this._zip.isValidZipFile();
    }

    Zip.prototype.getPath = function () {
        return this._path;
    }

    return $zip;
}