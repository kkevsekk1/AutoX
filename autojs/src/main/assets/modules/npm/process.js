const {
    EventEmitter
} = require("events");
const es = require("event-stream");

const process = new EventEmitter();

//events.on('exit', process.emit.bind(process, 'exit'))

process.stdout = es.map(function(data, callback) {
    console.log(data);
    return callback(null, data)
})
/*
process.stdout.end = function() {};
process.stdout.destroy = function(){};
*/
process.stderr = es.map(function(data, callback) {
    console.error(data);
    return callback(null, data)
})

process.nextTick = setImmediate;
process.env = {}
//log(process);

module.exports = process