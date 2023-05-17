(function(){
let {setReadonlyAttribute} = require("./utils.js");


let Event = function(type) {
    setReadonlyAttribute(this, 'bubbles', false);
    setReadonlyAttribute(this, 'eventPhase', 0);
    //是否可取消
    setReadonlyAttribute(this, 'cancelable', false)
    this.cancelBubble = false;
    setReadonlyAttribute(this, 'defaultPrevented', false)
    //节点
    setReadonlyAttribute(this, 'target', null)
    setReadonlyAttribute(this, 'currentTarget', null)
    setReadonlyAttribute(this, 'type', type)
    //时间截
    Object.defineProperty(this, 'timeStamp', {
        get: function() {
            return Date.now() - Event._loadTimeStamp;
        }
    })
    setReadonlyAttribute(this, 'isTrusted', false)
}
Event.prototype[Symbol.toStringTag] = 'Event';
Event._loadTimeStamp = Date.now();
Event.prototype.preventDefault = function() {}
Event.prototype.stopPropagation = function() {}
Event.prototype.stopImmediatePropagation = function() {

}

module.exports = Event
})()