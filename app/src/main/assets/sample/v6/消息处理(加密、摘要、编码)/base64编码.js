let message = "autox.js";
log("明文: ", message);

let base64encode = $base64.encode(message);
log("Base64编码: ", base64encode);

let message2 = "YXV0b3guanM=";
log("明文2: ", message2);

let base64decode = $base64.decode(message2);
log("Base64解码: ", base64decode);