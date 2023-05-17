(function(){
let FormBody = Packages.okhttp3.FormBody;
let MultipartBody = Packages.okhttp3.MultipartBody;
let RequestBody = Packages.okhttp3.RequestBody;
let MediaType = Packages.okhttp3.MediaType;
let Blob = require('./Blob.js');
let {
    setReadonlyAttribute
} = require("./utils.js");

//console.error('FormData')
//////FormData对象
let FormData = function() {
    this.enctype = 'application/x-www-form-urlencoded'
    setReadonlyAttribute(this, '_data', {}, false)
}
FormData.prototype[Symbol.toStringTag] = 'FormData';
FormData.prototype.get = function(key) {
    const data = this._data[key];
    return data ? data[0].value : null;
}
FormData.prototype.getAll = function(key) {
    return (this._data[key] || []).map(v => v.value)
}
FormData.prototype.delete = function(key) {
    delete this._data[key]
}
FormData.prototype.has = function(key) {
    return !!this._data[key]
}
FormData.prototype.set = function(key, value, filename) {
    this._data[key] = [];
    this.append(key, value, filename)
}
FormData.prototype.append = function(key, value, filename) {
    let values = this._data[key] || [];
    if (typeof value == 'string') {
        values.push({
            type: 'string',
            value
        })
    } else if (value instanceof Blob) {
        values.push({
            type: 'blob',
            filename,
            value,
        })
    } else {
        throw new Error('不支持的value:' + String(value))
    }
    this._data[key] = values
}
FormData.prototype.keys = function*() {
    let data = this._data;
    for (let key of Object.keys(data)) {
        for (let val of data[key]) {
            yield key;
        }
    }
}
FormData.prototype.values = function*() {
    let data = this._data;
    for (let key of Object.keys(data)) {
        for (let val of data[key]) {
            yield val.value;
        }
    }
}
FormData.prototype.entries = function*() {
    let data = this._data;
    for (let key of Object.keys(data)) {
        for (let val of data[key]) {
            yield [key, val.value];
        }
    }
}
FormData.prototype[Symbol.iterator] = FormData.prototype.entries;
FormData.prototype._toRequseBody = function() {
    if (this.enctype === 'multipart/form-data') {
        let builder = new MultipartBody.Builder();
        builder.setType(MultipartBody.FORM);
        for (let [name, val] of vs(this)) {
            if (val.type === 'string') {
                builder.addFormDataPart(name, val.value)
            } else if (val.type === 'blob') {
                let blob = val.value;
                let filename = val.filename || blob.name || null;
                builder.addFormDataPart(name,filename,
                    RequestBody.create(MediaType.parse(blob.type), blob._file))
            }
        }
        return builder.build();
    } else if (this.enctype === 'text/plain') {
        let text = '';
        for (let [name, val] of vs(this)) {
            if (val.type === 'string') {
                text += (name + '=' + val.value + '\r\n')
            }
        }
        return RequestBody.create(MediaType.parse(this.enctype), text);
    } else {
        let builder = new FormBody.Builder();
        for (let [name, val] of vs(this)) {
            if (val.type === 'string') {
                builder.add(name, val.value)
            }
        }
        return builder.build();
    }
}

function* vs(form) {
    let data = form._data;
    for (let key of Object.keys(data)) {
        for (let val of data[key]) {
            yield [key, val];
        }
    }
}
module.exports = FormData;
})()