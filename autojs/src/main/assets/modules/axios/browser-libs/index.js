(function() {
    let XMLHttpRequest = require("./XMLHttpRequest.js");
    let EventTarget = require("./EventTarget.js");
    let Event = require("./Event.js");
    let FormData = require("./FormData.js");
    let Blob = require("./Blob.js");

    let window = {}
    let _Object = Object.create(Object)
    _Object.getOwnPropertyDescriptors = function(obj) {
        var descriptors = {};
        Object.getOwnPropertyNames(obj).forEach(function(key) {
            descriptors[key] = Object.getOwnPropertyDescriptor(obj, key);
        });
        return descriptors;
    }

    let _setTimeout = (fn, time) => setTimeout(fn, time || 0)
    module.exports = {
        Object:_Object,
        window,
        FormData,
        XMLHttpRequest,
        Blob,
        EventTarget,
        Event,
        setTimeout:_setTimeout,
    }
})()