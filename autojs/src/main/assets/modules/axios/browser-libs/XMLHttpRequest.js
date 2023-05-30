(function () {
    importClass(java.io.InputStream);
    importClass(java.io.FileOutputStream);
    let RequestBody = Packages.okhttp3.RequestBody;
    let OkHttpClient = Packages.okhttp3.OkHttpClient;
    let Request = Packages.okhttp3.Request;
    let HttpUrl = Packages.okhttp3.HttpUrl;
    let MediaType = Packages.okhttp3.MediaType;
    let Headers = Packages.okhttp3.Headers;

    const stream = require("stream");
    let EventTarget = require("./EventTarget.js");
    let Event = require("./Event.js");
    let Blob = require("./Blob.js");
    let FormData = require("./FormData.js");
    let {
        setReadonlyAttribute,
        copyInputStream,
        AsyncThreadLock,
    } = require("./utils.js");

    let atl = new AsyncThreadLock();

    ///////XMLHttpRequest对象
    let XMLHttpRequest = function () {
        this.responseType = 'text';
        setReadonlyAttribute(this, 'upload', {})
        setReadonlyAttribute(this, 'readyState', 0);
        setReadonlyAttribute(this, 'status', 0);
        setReadonlyAttribute(this, 'statusText', '')
        setReadonlyAttribute(this, 'response', null)
        setReadonlyAttribute(this, 'responseText', '')
        setReadonlyAttribute(this, 'responseXML', null)
        setReadonlyAttribute(this, 'responseURL', '')
        setReadonlyAttribute(this, '_requestData', {
            headers: new Headers.Builder()
        }, false)

        EventTarget.call(this)
    }
    XMLHttpRequest.prototype[Symbol.toStringTag] = 'XMLHttpRequest';

    setReadonlyAttribute(XMLHttpRequest, '_parserResBody', {
        text: function (xhr, body) {
            let contentType = body.contentType();
            let charset = (contentType && contentType.charset()) || 'utf-8';
            let str = String(new java.lang.String(body.bytes(), charset));
            setReadonlyAttribute(xhr, 'responseText', str);
            return str;
        },
        json: function (xhr, body) {
            let str = this['text'](xhr, body);
            let json = JSON.parser(str);
            return json;
        },
        blob: function (xhr, body) {
            let blob = Blob._createBlobCache();
            let out = new FileOutputStream(blob._file);
            let inp = this.inputstream(xhr, body)
            try {
                copyInputStream(inp, out)
            } finally {
                inp.close();
                out.close();
            }
            let contentType = body.contentType();
            blob.size = blob._file.length();
            blob.type = contentType.type() + '/' + contentType.subtype();
            return blob;
        },
        stream: function (xhr, body) {
            const inputstream = this.inputstream(xhr, body)
            return stream.fromInputStream(inputstream);
        },
        inputstream: function (xhr, body) {
            return body.byteStream();
        },
        parser: function (type, xhr, body) {
            //log('resType:', type)
            const fn = this[type.toLowerCase()] || this['text'];
            xhr._setReadyState(3);
            const data = fn.call(this, xhr, body);
            setReadonlyAttribute(xhr, 'response', data);
            xhr._setReadyState(4);
            xhr.dispatchEvent(new Event('load'));
            xhr.dispatchEvent(new Event('loadend'));
        }
    }, false)
    XMLHttpRequest.prototype = Object.create(EventTarget.prototype);
    XMLHttpRequest.prototype.constructor = XMLHttpRequest;
    XMLHttpRequest._okHttpClient = new OkHttpClient.Builder()
        .followRedirects(true).build();
    XMLHttpRequest.prototype.open = function (method, url, ac, user, password) {
        Object.assign(this._requestData, {
            method,
            url: HttpUrl.parse(url).newBuilder(),
            ac,
        });
        if (user && password) {
            this._requestData.url.username(user).password(password)
        }
        this._setReadyState(1);
        //log(arguments)
    }
    XMLHttpRequest.prototype.send = function (body) {
        atl.addTask();
        const xhr = this;
        const {
            url,
            method,
            ac,
            headers,
        } = this._requestData
        const responseType = xhr.responseType;

        const builder = new Request.Builder();

        builder.headers(headers.build());

        let reqBody = parserReqBody(xhr, body);

        let request = builder.url(url.build()).method(method, reqBody).build();
        let call = XMLHttpRequest._okHttpClient.newCall(request);
        setReadonlyAttribute(xhr, '_call', call, false);
        call.enqueue({
            onFailure(call, e) {
                xhr._setReadyState(4);
                setReadonlyAttribute(xhr, 'statusText', e.message)
                xhr.dispatchEvent(new Event('error'))
                xhr.dispatchEvent(new Event('loadend'))
                atl.removeTask()
            },
            onResponse(call, res) {
                setReadonlyAttribute(xhr, 'status', res.code());
                setReadonlyAttribute(xhr, 'statusText', res.message());
                setReadonlyAttribute(xhr, 'responseURL', res.request().url().toString())
                setReadonlyAttribute(xhr, '_resHeaders', res.headers(), false)
                xhr._setReadyState(2);
                try {
                    XMLHttpRequest._parserResBody
                        .parser(xhr.responseType, xhr, res.body());

                } catch (e) {
                    res.close();
                    xhr._setReadyState(4);
                    xhr.dispatchEvent(new Event('error'))
                    xhr.dispatchEvent(new Event('loadend'))
                }
                atl.removeTask();
            }
        })
    }
    XMLHttpRequest.prototype.setRequestHeader = function (h, v) {
        this._requestData.headers.add(h, v)
    }
    XMLHttpRequest.prototype._setReadyState = function (i) {
        setReadonlyAttribute(this, 'readyState', i);
        this.dispatchEvent(new Event('readyStateChange'))
        this.dispatchEvent(new Event('readystatechange'))
    }
    XMLHttpRequest.prototype.getResponseHeader = function (name) {
        const _resHeaders = this._resHeaders;
        if (!_resHeaders) return null;
        const list = _resHeaders.values(name)
        let jsList = [];
        for (let val of list) {
            jsList.push(val)
        }
        if (jsList.length > 0) {
            return jsList.join(', ')
        } else return null;
    }
    XMLHttpRequest.prototype.getAllResponseHeaders = function () {
        const _resHeaders = this._resHeaders;
        if (!_resHeaders) return null;
        let jsList = [];
        for (let name of _resHeaders.names()) {
            for (let val of _resHeaders.values(name)) {
                jsList.push(name + ': ' + val);
            }
        }
        return jsList.join('\r\n')
    }
    XMLHttpRequest.prototype.abort = function () {
        this._call && this._call.cancel();
    }
    XMLHttpRequest.prototype.overrideMimeType = function () { }

    function parserReqBody(xhr, body) {
        let type = xhr._requestData.headers.get('content-type');
        const method = xhr._requestData.method.toLowerCase();
        const url = xhr._requestData.url;
        //需要请求体的http方法
        const dataMethod = ['post', 'put', 'patch', 'delete']
        if (type === 'application/x-www-form-urlencoded') type = null;
        if (dataMethod.some(val => val === method)) {
            if (body instanceof RequestBody) return body;
            if (body instanceof FormData) {
                //表单对象
                return body._toRequseBody();
            } else if (body instanceof Blob) {
                //Blob对象
                return RequestBody.create(MediaType.parse(type || body.type), body._file)
            } else if (body instanceof InputStream) {
                //java输入流
                return RequestBody.create(MediaType.parse(type || 'application/octet-stream'), body);
            } else if (typeof body === 'string') {
                //字符串
                return RequestBody.create(MediaType.parse(type || 'text/plain'), body);
            } else if (body) {
                //json
                return RequestBody.create(MediaType.parse('application/json'), JSON.stringify(body));
            }
            return RequestBody.create(MediaType.parse('text/plain'), '');
        } else {
            if (body instanceof FormData) {
                for (let [name, val] of body) {
                    if (typeof val === 'string') {
                        url.addQueryParameter(name, val);
                    }
                }
            }
            return null;
        }
    }

    module.exports = XMLHttpRequest;
})()