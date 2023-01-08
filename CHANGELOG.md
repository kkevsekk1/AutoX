# Change Log
autoxjs 整个项目的一些更新日志,双版本号为稳定版，单版本号为激进版本。

## [Unreleased](https://github.com/kkevsekk1/AutoX/compare/6.3.5...HEAD) 未发布

## [6.3.6] - 2023-1-8

@ 调整如下内容
1.将console打印Error对象时的行为，调整为与Chrome和Node.js等平台保持一致，打印堆栈信息。

[6.3.6]:https://github.com/kkevsekk1/AutoX/compare/6.3.5...6.3.6

下载地址： \
  http://autoxoss.autoxjs.com/autoxjs/6.3.6/app-v6-arm64-v8a-release-unsigned-signed.apk \
  http://autoxoss.autoxjs.com/autoxjs/6.3.6/app-v6-armeabi-v7a-release-unsigned-signed.apk \
  http://autoxoss.autoxjs.com/autoxjs/6.3.6/app-v6-universal-release-unsigned-signed.apk


## [6.3.5] - 2022-9-28

@wilinz  调整如下功能

1. 修复 PC端日志输出先后顺序有问题
2. 其他js等实例脚本问题

[6.3.5]:https://github.com/kkevsekk1/AutoX/compare/6.3.4...6.3.5

 下载地址： \
  http://autoxoss.autoxjs.com/autoxjs/6.3.5/app-v6-arm64-v8a-release-unsigned-signed.apk \
  http://autoxoss.autoxjs.com/autoxjs/6.3.5/app-v6-armeabi-v7a-release-unsigned-signed.apk \
  http://autoxoss.autoxjs.com/autoxjs/6.3.5/app-v6-universal-release-unsigned-signed.apk

## [6.3.4] - 2022-9-4

1. [更新paddle api并向后兼容](https://github.com/wilinz/AutoX/commit/5fae0643a563b69c7691f55ee3e3ec2771a656e3)
2. 添加Google ML kIT OCR API
3. [修复toast bug](https://github.com/wilinz/AutoX/commit/ea1e7663af7069a3750b64cd838d0148a7dae9b9)
4. [迁移到 kotlin build.gradle](https://github.com/wilinz/AutoX/commit/fc6d740afe1004bc4cd68dcb814afc52e5dba419)
5. 修复打包BUG

下载地址： \
  http://autoxoss.autoxjs.com/autoxjs/6.3.4/app-v6-arm64-v8a-release-unsigned-signed.apk \
  http://autoxoss.autoxjs.com/autoxjs/6.3.4/app-v6-armeabi-v7a-release-unsigned-signed.apk \
  http://autoxoss.autoxjs.com/autoxjs/6.3.4/app-v6-universal-release-unsigned-signed.apk 

[6.3.4]:https://github.com/kkevsekk1/AutoX/compare/6.2.3...6.3.4


## [6.3.3] - 2022-8-25

*  apk 使用cdn加速,地址格式：http://autoxoss.autoxjs.com/autoxjs/{版本号}/xxx.apk 
*  apk发布oss
*  根据changelog自动发布release note
*  增加版本之间的比较 点击版本号
*  workflow 不好使 跳跃了基本版本

下载地址： \
  http://autoxoss.autoxjs.com/autoxjs/6.3.3/app-v6-arm64-v8a-release-unsigned-signed.apk \
  http://autoxoss.autoxjs.com/autoxjs/6.3.3/app-v6-armeabi-v7a-release-unsigned-signed.apk \
  http://autoxoss.autoxjs.com/autoxjs/6.3.3/app-v6-universal-release-unsigned-signed.apk 

[6.3.3]:https://github.com/kkevsekk1/AutoX/compare/6.2.9...6.3.3


## [6.2.9] - 2022-8-22

@wilinz  调整如下功能


1. [打包的APK日志页面增加 重新运行，停止运行，清除日志 3个按钮](https://github.com/wilinz/AutoX/commit/32541253d870d6b752b9c436ca6676f59638655d)
2. [优化异步读取配置文件](https://github.com/wilinz/AutoX/commit/eca3c20ae32651fde526ef75f1cbd8c761999bd8)
3. [连接电脑增加断线重连和心跳检测（需要更新VSCODE Autox 插件到 1.109.0 以上才会启用）](https://github.com/wilinz/AutoX/commit/a703977fcfdeda32b0d40424f7f3933f0a274a3c)
4. [添加TessractOCR及例子](https://github.com/wilinz/AutoX/commit/1ab2345d01860b134b622d27cb3f8b7a9a14bbee)
5. [远程项目临时文件夹改回MD5命名](https://github.com/wilinz/AutoX/commit/a654bdb727fb14997f6c696a077a8c094d3175ab)
6. [修复 PFile.copyAssetDir() 空目录报错（打包后的APK闪退问题）](https://github.com/wilinz/AutoX/commit/0f6a7945729871fae160ad81d61c964ffb018e92) https://github.com/kkevsekk1/AutoX/issues/411
7. [修复打包Bug & 优化](https://github.com/wilinz/AutoX/commit/81703d29b775ee11cbbe6e05a0fea2f85560f943)

[6.2.9]:https://github.com/kkevsekk1/AutoX/compare/6.2.8...6.2.9

## [6.2.8] - 2022-8-20

@wilinz 调整如下功能
1. [修复 “运行项目” 与 “保存项目” bug](https://github.com/wilinz/AutoX/commit/683d81eae440b53fbbd4ce57bccad1fb24124dd8)
2. [修复打包完成之后某些ui样式失效 #397 ](https://github.com/wilinz/AutoX/commit/b118b11fd4d40bc477e0a35bf529ec0017b73964)
3. [修复示例代码"表格控件-内置图标查看器"打包后不能正常运行BUG](https://github.com/wilinz/AutoX/commit/d72ef148cc6c258e33570125f54c756f5dfa1a3f)
4. [修复打包的一些BUG（自定义签名BUG未修复，预计下版本修复）](https://github.com/wilinz/AutoX/commit/37762e5b4d9c6481d532094c1494c6960207b0a7)
5. [优化ocr](https://github.com/wilinz/AutoX/commit/2724484a065d70a747653e3f1b4960e11319e239)
6. [解决 Web注入 invoke 不指定回调方法就报错的bug](https://github.com/kkevsekk1/AutoX/commit/b7778cef2b5f0d0d875a4bf9c016092527668458)
@xxxxue 修复示例中的一些问题

[6.2.8]:https://github.com/kkevsekk1/AutoX/compare/6.2.7...6.2.8

## [6.2.7] - 2022-8-15

@wilinz   调整如下功能：

1. [修改优化OCR示例](https://github.com/wilinz/AutoX/commit/b07aa38770e3d4a832223625886939d094c98980)
2. [优化打包后的Apk自动使用root权限开启无障碍服务](https://github.com/wilinz/AutoX/commit/1ab37c9e8fd8e635cfa5464bd9f4af94248f0ba2)
3. [修复OCR BUG](https://github.com/wilinz/AutoX/commit/58f763f061f121c3eb47505a70eb8ee0fd111ebb)
4. [修复打包BUG，打包编辑框自动填充BUG](https://github.com/wilinz/AutoX/commit/8031a8e9cc0e4d70429e7f067f15876f1d789b7e)

[6.2.7]:https://github.com/kkevsekk1/AutoX/compare/6.2.6...6.2.7

## [6.2.6] - 2022-8-14

@wilinz 增加如下功能
1. [修复](https://github.com/wilinz/AutoX/commit/3d04d498d4c5ebdae60a41c02abd3dd43c374040) https://github.com/kkevsekk1/AutoX/issues/392 [root权限直接打开无障碍](https://github.com/wilinz/AutoX/commit/3d04d498d4c5ebdae60a41c02abd3dd43c374040)
2. [文档页面工具栏添加在浏览器打开按钮，侧滑菜单添加切换定时任务调度器按钮](https://github.com/wilinz/AutoX/commit/991de7fb687c561c0ad9064dae3ca192bb541c22) 
3. [修复定时任务无效问题](https://github.com/wilinz/AutoX/commit/362a1f31bc8df682f138d9d86c0d9229c8fb241f)
4. [修复打包BUG，以及打包后的App闪退的问题](https://github.com/wilinz/AutoX/commit/2a1e5c0edb2d70ce88a07cc5f5ba608c1e4e7b8c)
5. [修复app.autojs.versionName和app.autojs.versionCode问题](https://github.com/wilinz/AutoX/commit/af54fd82996941752d3dd5d9888fd1f4d8df7416)
6. [新增退出布局分析悬浮窗按钮](https://github.com/wilinz/AutoX/commit/8f5a5f886d8d1071ca6b6cb5a0d67c9fb7375fff)
7. [优化topAppBar弹出菜单, 确保点击后关闭菜单](https://github.com/wilinz/AutoX/commit/3fdd4a239b12e1c5518c5893ff9b8d65060a20d2)
8. 修复连接vscode URL解析BUG，增加扫码连接后记住扫码结果功能

[6.2.6]:https://github.com/kkevsekk1/AutoX/compare/6.2.5...6.2.6

## [6.2.5] -- 2022-8-13

[6.2.5]:https://github.com/kkevsekk1/AutoX/compare/6.2.3...6.2.5

@wilinz 增加如下功能

1. [调整UI避免文件操作按钮被挡住](https://github.com/wilinz/AutoX/commit/b65fd14d9ed01601affd9822dfbab5c54b94ee19)
2. [调整资源文件, 添加多语言支持](https://github.com/wilinz/AutoX/commit/b9e29c663288e1cd9458a73a0deb0a99b955c65a) by [Globalization Translator](https://github.com/wilinz/globalization-translator) 
3. [添加英文Readme](https://github.com/wilinz/AutoX/commit/7941357d0fbee713b45c3454cd27a2b3c9b657b4)
4. [修复打包后不显示 logo 和 底部splashText 的问题](https://github.com/wilinz/AutoX/commit/35e71046e98149d74bdeb150b6900e5edea61fab)
5. [优化文件操作，确保正常关闭文件流](https://github.com/wilinz/AutoX/commit/1bb4a1fceb13c3e87c6cc600be1afdcd560b056c)
6. [修复http模块response.close()错误](https://github.com/wilinz/AutoX/commit/3b2f58ff0ed10ee8243fbc8d7ccc4e0e47aa187e)
7. [优化打包，新增打包abi过滤，支持保存打包配置文件，可保存为项目。](https://github.com/wilinz/AutoX/commit/8b6776cff8b0fca4be4a52719b7d7d07c0a058f3) 
8. [修复打包的APK不支持armeabi-v7a的BUG，优化模板apk，与Autox.js共享二进制库和Assets, 以减少Autox.js Apk 大小](https://github.com/wilinz/AutoX/commit/8b6776cff8b0fca4be4a52719b7d7d07c0a058f3) 
9. [修改包名使Autox.js可以和Auto.js共存](https://github.com/wilinz/AutoX/commit/8b6776cff8b0fca4be4a52719b7d7d07c0a058f3)
10. [优化检查更新](https://github.com/wilinz/AutoX/commit/629e8d90317b12ac7109ea808689c8072dd8cd83)
11. [修复crypto模块BUG](https://github.com/kkevsekk1/AutoX/pull/391/commits/28913396430f0189a3cd0334382f44178bba55de)



## 5.7.6
优化以下特性：
*  回滚到 rhino-1.7.13

## 5.7.5
优化以下特性：
*   双指缩放代码编辑页
*   感谢 [heham](https://github.com/heham)  修复
*   进一步解决OCR内存泄漏问题
*   Background拼写问题



## 5.7.4
优化以下特性：
*   感谢 [syhyz](https://github.com/syhyz/)  ocr 结果排序问题
*   空指针问题

## 5.7.3
优化以下特性：
*  感谢 [heham](https://github.com/heham/)  修复屏幕宽带为0 的bug
*  感谢 [syhyz](https://github.com/syhyz/)  rhino-1.7.14  升级
*  修改一些文档,修复ocr内存泄露问题
*  新年已开始，欢迎反馈bug，最近一个月会大力支持新功能


## 5.7.2
优化以下特性：
*  感谢 [heham](https://github.com/heham/)  修复屏幕宽带为0 的bug
*  感谢 [syhyz](https://github.com/syhyz/)  rhino-1.7.14  升级
*  修改一些文档等
*  新年已开始，欢迎反馈bug，最近一个月会大力支持新功能


## 5.7.1
优化以下特性：
*  5.7.0默认发布为 64位版本，优化性能问题，需32位版本到github下载 5.7.0以前的版本。
*  感谢 [Aioure](https://github.com/Aioure)  升级如下功能：
*  修复64位版本中的一些bug
*  修复字体大小设置无法保存问题
*  其他一些bug
*  欢迎反馈64位版本的bug


## 5.7.0
优化以下特性：
*  感谢 [Aioure](https://github.com/Aioure)  升级如下功能：
*  5.7.0默认发布为 64位版本，优化性能问题，需32位版本到github下载。
*  ocr相关bug
*  其他一些bug
*  欢迎提交bug，欢迎pr本项目，欢迎pr文档，欢迎给贡献代码的开发者点赞！


## 5.6.4
优化以下特性：
*  版本检查问题
*  感谢 [Aioure](https://github.com/Aioure)  升级如下功能：
*  ocr一些调整和完善，更多模型
*  7zip一下完善，更多参数
*  欢迎提交bug，欢迎pr本项目，欢迎pr文档，欢迎给贡献代码的开发者点赞！

## 5.6.3
优化以下特性：
*  感谢 [Aioure](https://github.com/Aioure)  升级如下功能：
*  ocr一些调整和完善，更多模型
*  7zip一下完善，更多参数
*  欢迎提交bug，欢迎pr本项目，欢迎pr文档，欢迎给贡献代码的开发者点赞！


## 5.6.2
优化以下特性：
*  升级了一下[文档](https://github.com/kkevsekk1/kkevsekk1.github.io)项目一些路径问题，删除了本地文档
*  修改为在线文档，使用cdn加速github的文档
*  删除不必要的用户检测
*  [Aioure](https://github.com/Aioure)  升级如下功能：
*  ocr一些调整和完善
*  7zip一下完善
*  欢迎提交bug，欢迎pr本项目，欢迎pr文档，欢迎给贡献代码的开发者点赞！


### 5.6.1
优化以下特性：
*  手动检测更新无法显示问题
*  重新配置控件，升级androidx
*  修复定时任务月份bug
*  提示开启权限，后运行
*  qq群号问题
*  脚本目录修改bug
*  IP地址不合法bug
*  不兼容arm64 bug
*  [Aioure](https://github.com/Aioure)  升级如下功能：
*  编译兼容低版本的opencv
*  集成PaddleOCR
*  修复若干兼容问题
*  此版本有重大升级变化，欢迎测试提交bug



## 5.5.6
优化以下特性：
*  [Aioure](https://github.com/Aioure)  升级如下功能：
*  opencv 到4.5.5版，修复上一版兼容问题
*  最低版升级到21即android 5
*  打包后的dex进行拆分

### 5.5.5
优化以下特性：
*  感谢 [Aioure](https://github.com/Aioure) 更新如下功能，次版本可能存在bug，欢迎反馈
*  RhinoJS 升级版本到1.7.13版
*  更新 opencv 到4.2.0版
*  添加多媒体、Webview扩展与游戏编程两组示例等
*  修复一些闪退问题，此版本有重大升级变化

### 5.5.3
优化以下特性：
* 增加配置隐藏启动图标，只能通过adb等其他方式启动app
* 感谢 [Aioure](https://github.com/Aioure) 增加7zip 相关api，在打包app中应用
* 修复一些闪推问题



## 5.5.2
优化以下特性：
1.增加配置进行，音量上键是否停止脚本（需开启无障碍）
2.增加配置关闭启动页，
3.修复配置非ui模式，隐藏开启日志无效，ui模式下，应该从ui界面取去日志
4.修复空文件夹不能删除
5.升级androidx
6.修改配置项设置效果，无效项仅保留，
7.闪退问题，打包后app中，暂时屏蔽7zip

### 5.5.1
优化以下特性：
*  恢复默认保活功能，删除上通知栏功能
*  修复项目打包bug
*  感谢 [Aioure](https://github.com/Aioure) 增加7zip 压缩，解压功能
*  7zip的api文档需要进一步完善...


### 5.4.0
优化以下特性：
*  删除默认保活功能和上通知栏功能
*  按网友方法加入识别web元素功能，但是此api已经被标记过时
*  [falcolee](https://github.com/falcolee) 修复项目名的bug


## 5.3.2
优化以下特性：
* 剔除登录和用户模块
* 测试opencv 4 准备升级

## 5.3.1
优化以下特性：
*  新增无障碍服务描述配置
* [CcSimple](https://github.com/CcSimple)  优化currentPackage及currentActivity获取方式(待测试实际效果!)
* dev版本欢迎大家测试 currentPackage,currentActivity api并反馈效果
* 配置通讯录读写权限，具体使用由脚本决定
* 修复应用商店地址
* 修复 jsapi bug



### 5.3.0
优化以下特性：
*  开发版
*  新增无障碍服务描述配置
* [CcSimple](https://github.com/CcSimple)  优化currentPackage及currentActivity获取方式(待测试实际效果!)
* dev版本欢迎大家测试 currentPackage,currentActivity api并反馈效果


## 5.2.0
优化以下特性：
*  模板app稳定模式
*  打包页面新增主脚本名称、稳定模式、隐藏日志设置
*  新增单文件打包清爽模式设置
*  优化单文件打包(清爽模式/单文件.json配置)
*  [CcSimple](https://github.com/CcSimple) 更新上述功能
*  [listky]( https://github.com/listky) 完善文档
*  邀请第三方上线 【AIX】独立apk，支持单独或批量运行授权应用
*  第三方独立apk，AIX,知斗云等 支持vscode或商店服务器，websocket自动重连
*I 重启后console无法使用的bug
* autoxjs 上线：脚本（应用）商店
* autoxjs 支持试运行商店中的脚本（应用）。
* autoxjs 支持自助线授权脚本（应用）到第三方(AIX)中稳定运行，批量运行。
* autoxjs 开发者发布脚本，可以授权给第三方使用。
* autoxjs 上线推荐(码)奖励功能（分成比例，开发者70%，邀请20%[邀请人和开发者同一人90%]）
* autoxjs 支持AIX,AUTOXJS下载，分享下载。
* autoxjs 脚本备份，变成可配置，默认打开。
* 上线web端开发者平台
* 开发平台 可发布脚本（应用）到商店。
* 开发平台 提供自助授权配置（免费，按量，包年包月）
* 开发平台 提供授权币的购买，退货等
* 帮助中心（少量内容）


## 5.1.0
优化以下特性：
*  开发版
*  模板app稳定模式
*  打包页面新增主脚本名称、稳定模式、隐藏日志设置
*  新增单文件打包清爽模式设置
*  优化单文件打包(清爽模式/单文件.json配置)
*  [CcSimple](https://github.com/CcSimple) 更新上述功能
*  [listky]( https://github.com/listky) 完善文档
*  5.1.0版本预购，本周末发布：应用商店，文件备份配置，自动重连等


## 5.0.1
优化以下特性：
*  单文件打包bug，升级到最新版本

## 4.2.19
优化以下特性：
*  修复华为权限bug

## 4.2.18
优化以下特性：
*  自定义启动页配置等功能
*  新增可指定签名文件功能
*  优化打包流程
*  增加项目默认配置
*  欢迎更多开发者贡献代码，完善文档！

## 4.2.17.dev
优化以下特性：
*  感谢 [CcSimple](https://github.com/CcSimple)  大神升级了，自定义启动页配置等功能
*  修复一些bug
*  优化打包流程
*  开发版，有问题及时反馈！预计下次正式版可上线上述功能。
*  欢迎更多开发者贡献代码，实现更好的功能！

## 4.2.16
优化以下特性：
*  感谢 [CcSimple](https://github.com/CcSimple)  新增可指定签名文件功能
*  修复一些bug
*  发布开发版，两个版本并存，欢迎安装测试，有问题及时反馈！

## 4.2.15
优化以下特性：
* 修复截图权限,造成崩溃

## 4.2.14
优化以下特性：
* 调整应用配置页面:不缓存应用图标
* 修复修改应用配置后,ExplorerChangeEvent UI更新问题
* 打包应用页面支持修改应用配置信息
* 调整整target及compile版本到29 ，可能出现兼容问题，有问题请反馈！
* 感谢 CcSimple ，以上功能有 [CcSimple](https://github.com/CcSimple) 提交代码，欢迎点赞！

## 4.2.13
优化以下特性：
* 修复打包后的app启动另一个独立ui脚本无法执行的bug
* webpack-autox 项目支持可以输出多个文件，有需要请更新
* 注意解决第一个bug 是之前修复另一bug引起的，但是现在不知道是哪个bug了，发现请反馈
* 感谢大家支持

### 4.2.12
优化以下特性：
* 修复打包软件常驻被杀死的bug
* 修复长安函数无法加载文档的bug
* 修改一些其他配置

## 4.2.11
优化以下特性：
* 在线文档发布为国内版本
* 感谢开发者 icesValley 对国内文档建议和技术支持

## 4.2.10
优化以下特性：
* 稳定支持 websocket


## 4.2.9
优化以下特性：
* 感谢 Wang Zijian 修复 floating window permission check on miui
* console的自动隐藏改为参数控制
* 打包apk启动后finish掉启动也
* 升级apkbuild包,解决空文件夹问
* 升级ci 项目

## 4.2.8
优化以下特性：
* 调整模板app，添加常驻前台，现在是统一自动开启
* 修复打包插件，无法识别空目录，导致无法重新打包识别
* 统一模板app和autoxjs，不再采用插件形式打包
* 修复了4.2.7 的遗留问题


## 4.2.7
优化以下特性：
* 添加获取设备虚拟导航栏相关接口：是否有虚拟导航，虚拟导航高度
* 去除打包插件机制，直接使用模板打包
* 修复release发布，不能运行bug
* 修复release模式下语言包找不到问题
`注意`遗留一个问题这里面的模板不是最新,最新升级的接口，打包出来无法使用,在autoxjs中可以正常使用


## 4.2.6
优化以下特性：
* 修复console 界面未创建，修改过程产生空指针问题
* 默认可以不修改console 原标题
* 关闭console的提示，修改为系统消息
  


## 4.2.5
优化以下特性：
* 添加应用商店需要的功能（商店端暂未上线）
* 日志：标题字号，色彩，背景，文字大小，内容字号等可以设置和修改
* 和vscode 插件链接问题闪退问题
* TextView 导致小米手机闪退问题
* 


