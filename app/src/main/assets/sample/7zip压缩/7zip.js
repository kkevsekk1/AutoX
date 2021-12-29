//压缩文件路径(必须是完整路径)
var filePath = "/sdcard/脚本.7z";
//目录路径(必须是完整路径)
var dirPath = "/sdcard/脚本";
//压缩类型
//支持的压缩类型包括：zip 7z bz2 bzip2 tbz2 tbz gz gzip tgz tar wim swm xz txz。
var type = "7z";
//压缩
runtime.zips.A(type，filePath, dirPath);
toastLog("压缩成功！")

//解压(若文件已存在则跳过)
//runtime.zips.X(filePath, dirPath);

//自定义命令:7z加密压缩
//var password = "password"
//runtime.zips.cmd("7za a -y -ms -t7z -p" + password + " "  + filePath + " -r" + dirPath);
