let img = images.read("./0.jpg")
// 新增：自定义模型路径(必须是绝对路径), files.path() 将相对路径转为绝对路径
let myModelPath = files.path("./models");
let start = new Date()
// 识别图片中的文字，返回完整识别信息（兼容百度OCR格式）。
let result = paddle.ocr(img, myModelPath)
log('OCR识别耗时：' + (new Date() - start) + 'ms')
toastLog("完整识别信息: " + JSON.stringify(result))
start = new Date()
// 识别图片中的文字，只返回文本识别信息（字符串列表）。当前版本可能存在文字顺序错乱的问题 建议先使用detect后自行排序
const stringList = paddle.ocrText(img, myModelPath)
log('OCR纯文本识别耗时：' + (new Date() - start) + 'ms')
toastLog("文本识别信息: " + JSON.stringify(stringList))

// 回收图片
img.recycle()
// 释放native内存，非必要，供万一出现内存泄露时使用
// paddle.release()

