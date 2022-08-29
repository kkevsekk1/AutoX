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
let result = mlkit.ocr(img,"zh")
toastLog('OCR识别耗时：' + (new Date() - start) + 'ms')
let managerBtn = result.find(e => e.words == "管理")
if (managerBtn) click(managerBtn.bounds)
sleep(500)
let homeBtn = result.find(e => e.words == "主页")
if (homeBtn) click(homeBtn.bounds)
sleep(500)
let docBtn = result.find(e => e.words == "文档")
press(docBtn.bounds, 500)
// 回收图片
img.recycle()