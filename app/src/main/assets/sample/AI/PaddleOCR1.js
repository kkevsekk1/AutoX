// 识别图片中的文字，只返回字符串列表。
const img = images.read("./0.jpg");
const cpuThreadNum = 4;
toastLog('开始识别');
const stringList = paddle.ocrText(img, cpuThreadNum);
stringList.forEach(item => {
    toastLog(item);
})

