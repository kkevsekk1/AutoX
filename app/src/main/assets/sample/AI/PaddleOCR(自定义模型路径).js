let img = images.read("./0.jpg")
let cpuThreadNum = 4
// 新增：自定义模型路径(必须是完整路径)
// 该路径下必须有以下三个文件： ch_ppocr_mobile_v2.0_cls_opt.nb ch_ppocr_mobile_v2.0_det_opt.nb ch_ppocr_mobile_v2.0_rec_opt.nb，否则就会启用默认模型。
let myModelPath = "/sdcard/Android/data/org.autojs.autojs/models/ocr_v2_for_cpu";
let start = new Date()
// 识别图片中的文字，返回完整识别信息（兼容百度OCR格式）。
let result = paddle.ocr(img, cpuThreadNum, myModelPath)
log('OCR识别耗时：' + (new Date() - start) + 'ms')
toastLog("完整识别信息: " + JSON.stringify(result))
start = new Date()
// 识别图片中的文字，只返回文本识别信息（字符串列表）。当前版本可能存在文字顺序错乱的问题 建议先使用detect后自行排序
const stringList = paddle.ocrText(img, cpuThreadNum, myModelPath)
log('OCR纯文本识别耗时：' + (new Date() - start) + 'ms')
toastLog("文本识别信息: " + JSON.stringify(stringList))

// 回收图片
img.recycle()
// 释放native内存，非必要，供万一出现内存泄露时使用
// paddle.release()

