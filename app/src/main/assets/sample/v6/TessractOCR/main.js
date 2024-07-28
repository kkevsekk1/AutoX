//此例子仅作为演示，无法运行，因为tessdata目录下没有训练数据，
//如需运行，可前往github下载完整例子：https://github.com/wilinz/autoxjs-tessocr
//导包
importClass(com.googlecode.tesseract.android.TessBaseAPI)
//新建OCR实例
var tessocr = new TessBaseAPI()
//请求截图权限
requestScreenCapture(false);
//3秒后开始
toastLog("3秒后截图")
sleep(3000)
toastLog("开始截图")
//截图 
var img = captureScreen();
//tessdata目录所在的文件夹，目录下放置训练数据
//训练数据下载地址：https://github.com/tesseract-ocr/tessdata/tree/4.0.0
var dataPath = files.path("./")
//初始化tessocr
//第二个参数是初始化的语言，是数据文件去掉扩展名后的文件名，多个语言用+连接
//训练数据文件夹必须命名为tessdata
//训练数据下载时是什么名字就是什么名字，不能改
var ok = tessocr.init(dataPath, "eng+chi_sim")
if (ok) {
    toastLog("初始化成功: " + tessocr.getInitLanguagesAsString())
} else {
    toastLog("初始化失败")
}
//设置图片
tessocr.setImage(img.getBitmap())
//打印文本结果
toastLog(tessocr.getUTF8Text())
//如需获取位置等其他结果请看文档