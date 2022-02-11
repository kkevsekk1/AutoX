# Change Log
autoxjs 整个项目的的一些更新日志,双版本号为稳定版，单版本号为激进版本。

### 5.7.3
优化以下特性：
*  感谢 [heham](https://github.com/heham/)  修复屏幕宽带为0 的bug
*  感谢 [syhyz](https://github.com/syhyz/)  rhino-1.7.14  升级
*  修改一些文档,修复ocr内存泄露问题
*  新年已开始，欢迎反馈bug，最近一个月会大力支持新功能


### 5.7.2
优化以下特性：
*  感谢 [heham](https://github.com/heham/)  修复屏幕宽带为0 的bug
*  感谢 [syhyz](https://github.com/syhyz/)  rhino-1.7.14  升级
*  修改一些文档等
*  新年已开始，欢迎反馈bug，最近一个月会大力支持新功能


### 5.7.1
优化以下特性：
*  5.7.0默认发布为 64位版本，优化性能问题，需32位版本到github下载 5.7.0以前的版本。
*  感谢 [Aioure](https://github.com/Aioure)  升级如下功能：
*  修复64位版本中的一些bug
*  修复字体大小设置无法保存问题
*  其他一些bug
*  欢迎反馈64位版本的bug


### 5.7.0
优化以下特性：
*  感谢 [Aioure](https://github.com/Aioure)  升级如下功能：
*  5.7.0默认发布为 64位版本，优化性能问题，需32位版本到github下载。
*  ocr相关bug
*  其他一些bug
*  欢迎提交bug，欢迎pr本项目，欢迎pr文档，欢迎给贡献代码的开发者点赞！


### 5.6.4
优化以下特性：
*  版本检查问题
*  感谢 [Aioure](https://github.com/Aioure)  升级如下功能：
*  ocr一些调整和完善，更多模型
*  7zip一下完善，更多参数
*  欢迎提交bug，欢迎pr本项目，欢迎pr文档，欢迎给贡献代码的开发者点赞！

### 5.6.3
优化以下特性：
*  感谢 [Aioure](https://github.com/Aioure)  升级如下功能：
*  ocr一些调整和完善，更多模型
*  7zip一下完善，更多参数
*  欢迎提交bug，欢迎pr本项目，欢迎pr文档，欢迎给贡献代码的开发者点赞！


### 5.6.2
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



### 5.5.6
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



### 5.5.2
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


### 5.3.2
优化以下特性：
* 剔除登录和用户模块
* 测试opencv 4 准备升级

### 5.3.1
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


### 5.2.0
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


### 5.1.0
优化以下特性：
*  开发版
*  模板app稳定模式
*  打包页面新增主脚本名称、稳定模式、隐藏日志设置
*  新增单文件打包清爽模式设置
*  优化单文件打包(清爽模式/单文件.json配置)
*  [CcSimple](https://github.com/CcSimple) 更新上述功能
*  [listky]( https://github.com/listky) 完善文档
*  5.1.0版本预购，本周末发布：应用商店，文件备份配置，自动重连等


### 5.0.1
优化以下特性：
*  单文件打包bug，升级到最新版本

### 4.2.19
优化以下特性：
*  修复华为权限bug

### 4.2.18
优化以下特性：
*  自定义启动页配置等功能
*  新增可指定签名文件功能
*  优化打包流程
*  增加项目默认配置
*  欢迎更多开发者贡献代码，完善文档！

### 4.2.17.dev
优化以下特性：
*  感谢 [CcSimple](https://github.com/CcSimple)  大神升级了，自定义启动页配置等功能
*  修复一些bug
*  优化打包流程
*  开发版，有问题及时反馈！预计下次正式版可上线上述功能。
*  欢迎更多开发者贡献代码，实现更好的功能！

### 4.2.16
优化以下特性：
*  感谢 [CcSimple](https://github.com/CcSimple)  新增可指定签名文件功能
*  修复一些bug
*  发布开发版，两个版本并存，欢迎安装测试，有问题及时反馈！

### 4.2.15
优化以下特性：
* 修复截图权限,造成崩溃

### 4.2.14
优化以下特性：
* 调整应用配置页面:不缓存应用图标
* 修复修改应用配置后,ExplorerChangeEvent UI更新问题
* 打包应用页面支持修改应用配置信息
* 调整整target及compile版本到29 ，可能出现兼容问题，有问题请反馈！
* 感谢 CcSimple ，以上功能有 [CcSimple](https://github.com/CcSimple) 提交代码，欢迎点赞！

### 4.2.13
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

### 4.2.11
优化以下特性：
* 在线文档发布为国内版本
* 感谢开发者 icesValley 对国内文档建议和技术支持

### 4.2.10
优化以下特性：
* 稳定支持 websocket


### 4.2.9
优化以下特性：
* 感谢 Wang Zijian 修复 floating window permission check on miui
* console的自动隐藏改为参数控制
* 打包apk启动后finish掉启动也
* 升级apkbuild包,解决空文件夹问
* 升级ci 项目

### 4.2.8
优化以下特性：
* 调整模板app，添加常驻前台，现在是统一自动开启
* 修复打包插件，无法识别空目录，导致无法重新打包识别
* 统一模板app和autoxjs，不再采用插件形式打包
* 修复了4.2.7 的遗留问题


### 4.2.7
优化以下特性：
* 添加获取设备虚拟导航栏相关接口：是否有虚拟导航，虚拟导航高度
* 去除打包插件机制，直接使用模板打包
* 修复release发布，不能运行bug
* 修复release模式下语言包找不到问题
`注意`遗留一个问题这里面的模板不是最新,最新升级的接口，打包出来无法使用,在autoxjs中可以正常使用


### 4.2.6
优化以下特性：
* 修复console 界面未创建，修改过程产生空指针问题
* 默认可以不修改console 原标题
* 关闭console的提示，修改为系统消息
  


### 4.2.5
优化以下特性：
* 添加应用商店需要的功能（商店端暂未上线）
* 日志：标题字号，色彩，背景，文字大小，内容字号等可以设置和修改
* 和vscode 插件链接问题闪退问题
* TextView 导致小米手机闪退问题
* 

