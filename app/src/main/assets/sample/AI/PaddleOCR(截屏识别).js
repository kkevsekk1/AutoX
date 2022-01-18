// 识别图片中的文字，只返回字符串列表。
if(!requestScreenCapture()){
    toastLog("请求截图失败");
    exit();
}
const cpuThreadNum = 4;
const stringList = paddle.ocrText(captureScreen(), cpuThreadNum);
stringList.forEach(item => {
    toastLog(item);
})
