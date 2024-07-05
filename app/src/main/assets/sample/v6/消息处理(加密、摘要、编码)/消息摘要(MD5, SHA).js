
// 字符串消息摘要
let message = "Hello, Autox.js";
// 输出各种消息摘要算法结果的hex值
log("字符串: ", message);
log("MD5: ", $crypto.digest(message, "MD5"));
log("SHA1: ", $crypto.digest(message, "SHA-1"));
log("SHA256: ", $crypto.digest(message, "SHA-256"));

// 输出各种消息摘要算法结果的base64值
log("MD5 [base64]: ", $crypto.digest(message, "MD5", {output: 'base64'}));
log("SHA1 [base64]: ", $crypto.digest(message, "SHA-1", {output: 'base64'}));
log("SHA256 [base64]: ", $crypto.digest(message, "SHA-256", {output: 'base64'}));


// 文件消息摘要
let file = "/sdcard/脚本/_test_for_message_digest.js"
// 写入文件内容，提供为后续计算MD5等
$files.write(file, "Test!");
log("文件: ", file);
log("MD5: ", $crypto.digest(file, "MD5", {input: 'file'}));
log("SHA1: ", $crypto.digest(file, "SHA-1", {input: 'file'}));
log("SHA256: ", $crypto.digest(file, "SHA-256", {input: 'file'}));