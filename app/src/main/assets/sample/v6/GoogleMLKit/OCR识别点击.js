//requestScreenCapture()
var mainActivity = "org.autojs.autojs.ui.main.MainActivity"
if (currentActivity != mainActivity) {
    app.startActivity({
        packageName: "org.autojs.autoxjs.v6",
        className: mainActivity,
    });
    waitForActivity(mainActivity)
}
requestScreenCapture()
sleep(1000)
let img = captureScreen()
let start = new Date()
let result = gmlkit.ocr(img, "zh")
toastLog('OCR识别耗时：' + (new Date() - start) + 'ms')
let managerBtn = result.find(3, e => e.text == "管理")
if (managerBtn) click(managerBtn.bounds)
sleep(500)
let homeBtn = result.find(3, e => e.text == "主页")
if (homeBtn) click(homeBtn.bounds)
sleep(500)
let docBtn = result.find(3, e => e.text == "文档")
if (docBtn) press(docBtn.bounds, 500)
// 回收图片
img.recycle()