
module.exports = function (runtime, scope) {
    const rtConsole = runtime.console;
    const extension = runtime.consoleExtension
    let consoleEmitter = null;
    let globalConsoleEmitter = null;

    const console = {
        extension,
        get emitter() {
            if (consoleEmitter) return consoleEmitter;
            consoleEmitter = new (require('events').EventEmitter)();
            extension.registerConsoleListener(function (log, level, levelString) {
                consoleEmitter.emit('println', log, level, levelString)
                consoleEmitter.emit(levelString, log, level)
            })
            return consoleEmitter;
        },
        get globalEmitter() {
            if (globalConsoleEmitter) return globalConsoleEmitter;
            globalConsoleEmitter = new (require('events').EventEmitter)();
            extension.registerGlobalConsoleListener(function (log, level, levelString) {
                globalConsoleEmitter.emit('println', log, level, levelString)
                globalConsoleEmitter.emit(levelString, log, level)
            })
            return globalConsoleEmitter;
        }
    };

    console.assert = function (value, message) {
        message = message || "";
        rtConsole.assertTrue(value, message);
    }

    console.rawInput = rtConsole.rawInput.bind(rtConsole);

    console.input = function () {
        return eval(console.rawInput.apply(console, arguments) + '');
    };

    console.log = function () {
        rtConsole.log(util.format.apply(util, arguments));
    }

    console.verbose = function () {
        rtConsole.verbose(util.format.apply(util, arguments));
    }

    console.print = function () {
        rtConsole.print(android.util.Log.DEBUG, util.format.apply(util, arguments));
    }

    console.info = function () {
        rtConsole.info(util.format.apply(util, arguments));
    }

    console.warn = function () {
        rtConsole.warn(util.format.apply(util, arguments));
    }

    console.error = function () {
        rtConsole.error(util.format.apply(util, arguments));
    }

    var timers = {}, ascu = android.os.SystemClock.uptimeMillis;
    console.time = console.time || function (label) {
        label = label || "default";
        timers[label] = ascu();
    }

    console.timeEnd = console.timeEnd || function (label) {
        label = label || "default";
        var result = ascu() - timers[label];
        delete timers[label];
        console.log(label + ": " + result + "ms");
    }

    console.trace = console.trace || function captureStack(message) {
        var k = {};
        Error.captureStackTrace(k, captureStack);
        console.log(util.format.apply(util, arguments) + "\n" + k.stack);
    };

    let s = null;
    console.setGlobalLogConfig = function (config) {
        console.warn(`setGlobalLogConfig is deprecated, replaced with console.globalEmitter`)
        const { file } = config;
        if (s) s.dispose()
        s = extension.registerGlobalConsoleListener(function (log, level, levelString) {
            try { files.append(file, log + '\n') } catch (e) { }
        })
    }


    console.show = rtConsole.show.bind(rtConsole);
    console.hide = rtConsole.hide.bind(rtConsole);
    console.clear = rtConsole.clear.bind(rtConsole);
    console.setSize = rtConsole.setSize.bind(rtConsole);
    console.setPosition = rtConsole.setPosition.bind(rtConsole);
    console.setTitle = rtConsole.setTitle.bind(rtConsole);
    console.setBackgroud = rtConsole.setBackground.bind(rtConsole);
    console.setBackground = rtConsole.setBackground.bind(rtConsole);
    console.setCanInput = rtConsole.setCanInput.bind(rtConsole);
    console.setLogSize = rtConsole.setLogSize.bind(rtConsole);
    console.setMaxLines = rtConsole.setMaxLines.bind(rtConsole);

    scope.print = console.print.bind(console);
    scope.log = console.log.bind(console);
    scope.err = console.error.bind(console);
    scope.openConsole = console.show.bind(console);
    scope.clearConsole = console.clear.bind(console);

    return console;
}
