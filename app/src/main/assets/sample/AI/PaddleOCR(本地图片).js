// 识别图片中的文字，返回完整识别信息。
const img = images.read("./0.jpg");
const cpuThreadNum = 4;
// PaddleOCR 移动端提供了两种模型：ocr_v2_for_cpu与ocr_v2_for_cpu(slim)，此选项用于选择加载的模型,默认true使用v2的slim版(速度更快)，false使用v2的普通版(准确率更高）
var useSlim = true;
const result = paddle.ocr(img, cpuThreadNum, useSlim);
toastLog(JSON.stringify(result));
// 可以使用简化的调用命令，默认参数：cpuThreadNum = 4, useSlim = true;
// const result = paddle.ocr(img);

