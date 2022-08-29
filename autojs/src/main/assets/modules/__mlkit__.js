module.exports = function (runtime, global) {
    let mlkitApi = new com.stardust.autojs.runtime.api.MlKit();
    let mlkit = {}

    mlkit.ocr = function (image, language) {
        if (!language) language = ""
        return global.util.java.toJsArray(mlkitApi.ocr(image, language))
    }

    mlkit.ocrText = function (image, language) {
        return mlkit.ocr(image, language).map(e => e.words)
    }

    return mlkit
}