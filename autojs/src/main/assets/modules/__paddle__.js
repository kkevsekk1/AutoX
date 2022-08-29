module.exports = function (runtime, global) {
    let paddleApi = new com.stardust.autojs.runtime.api.Paddle();
    let paddle = {}

    paddle.ocr = function () {
        if (!arguments[0]) return []
        let result
        switch (arguments.length) {
            case 1:
                result = paddleApi.ocr(arguments[0])
                break
            case 2:
                result = paddleApi.ocr(arguments[0], arguments[1])
                break
            case 3:
                result = paddleApi.ocr(arguments[0], arguments[1], arguments[2])
        }
        return global.util.java.toJsArray(result)
    }

    paddle.ocrText = function (image, param2, param3) {
        if (!arguments[0]) return []
        let result
        switch (arguments.length) {
            case 1:
                result = paddle.ocr(arguments[0])
                break
            case 2:
                result = paddle.ocr(arguments[0], arguments[1])
                break
            case 3:
                result = paddle.ocr(arguments[0], arguments[1], arguments[2])
        }
        return result.map(e => e.words)
    }

    return paddle
}