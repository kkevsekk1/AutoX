// 识别图片中的文字，返回完整识别信息。
const img = images.read("./0.jpg");
const cpuThreadNum = 8;
const result = paddle.ocr(img, cpuThreadNum);
toastLog(JSON.stringify(result));

