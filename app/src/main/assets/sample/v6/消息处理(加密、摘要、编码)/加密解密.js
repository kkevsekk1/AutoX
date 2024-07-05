let message = "未加密字符串";
log("明文: ", message);
// 密钥，由于AES等算法要求是16位的倍数，我们这里用一个16位的密钥
let key = new $crypto.Key("password12345678");
log("密钥: ", key);
// AES加密
let aes = $crypto.encrypt(message, key, "AES/ECB/PKCS5padding");
log("AES加密后二进制数据: ", aes);
log("AES解密: ", $crypto.decrypt(aes, key, "AES/ECB/PKCS5padding", {output: 'string'}));

// RSA加密
// 生成RSA密钥
let keyPair = $crypto.generateKeyPair("RSA");
log("密钥对: ", keyPair);
// 使用私钥加密
let rsa = $crypto.encrypt(message, keyPair.privateKey, "RSA/ECB/PKCS1padding");
log("RSA私钥加密后二进制数据: ", rsa);
// 使用公钥解密
log("RSA公钥解密: ", $crypto.decrypt(rsa, keyPair.publicKey, "RSA/ECB/PKCS1padding", {output: 'string'}));