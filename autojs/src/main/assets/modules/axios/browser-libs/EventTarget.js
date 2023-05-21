(function(){
let {
    setReadonlyAttribute
} = require("./utils.js");


let EventTarget = function() {
    setReadonlyAttribute(this, '_events', {}, false)
}
EventTarget.prototype[Symbol.toStringTag] = 'EventTarget';
EventTarget.prototype.addEventListener = function(type, r, options) {
    const fns = this._events[type] || [];
    const fn = (typeof r == 'function') ? r : r.handleEvent;
    if ((typeof type !== 'string') || (typeof fn !== 'function')) {
        throw new TypeError('参数错误');
    }
    fns.push({
        fn,
        options: options || {},
    })
    this._events[type] = fns;
}
EventTarget.prototype.removeEventListener = function(type, r, options) {
    const fns = this._events[type] || [];
    const fn = (typeof r == 'function') ? r : r.handleEvent;
    if ((typeof type !== 'string') || (typeof fn !== 'function')) {
        throw new TypeError('参数错误');
    }
    for (let i = 0; i < fns.length; i++) {
        if (fns[i].fn === fn) {
            fns.splice(i, 1)
            break;
        }
    }
}
EventTarget.prototype.dispatchEvent = function(event) {
    const type = event.type;
    const fns = this._events[type] || [];
    setReadonlyAttribute(event, 'currentTarget', this)
    setReadonlyAttribute(event, 'target', this)
    if (typeof this['on' + type] == 'function') {
        this['on' + type].call(this, event);
    }
    for (let oc of fns) {
        oc.fn.call(this, event)
    }
    setReadonlyAttribute(event, 'currentTarget', null)
    setReadonlyAttribute(event, 'target', null)
}

module.exports = EventTarget;
})()