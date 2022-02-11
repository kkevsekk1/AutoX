# Auto.js 和 AutoX.js
## 简介
一个支持无障碍服务的Android平台上的JavaScript 运行环境 和 开发环境，其发展目标是类似JsBox和Workflow。

 ~~由于[原作者](https://github.com/hyb1996/Auto.js) 不再维护 Auto.js 项目
我计划在原来基础上继续维护者项目，本项目从[autojs](https://github.com/hyb1996/Auto.js) 并将原项目命名为AutoX.js。
 你现在看的是原4.1版基础上的项目，后面我将针对项目本身如何开发、运行的进行介绍，欢迎更多开发者参与这个项目维护升级，
最新的[AutoX地址](https://github.com/kkevsekk1/AutoX), 文档中很多原项目路径，
在原项目没有删除的情况下我并不打算替换掉，以表对于原作者的尊重。这篇文档里有加密相关的内容可能和实际运行情况有冲突，
如果你希望写的代码加密保护知识产权，请参考项目 https://github.com/kkevsekk1/webpack-autojs
我会逐步完善更新，程序代码，尽可能保持一致。~~

  本项目从[hyb1996](https://github.com/hyb1996/Auto.js) autojs 获得,并命名为AutoX.js （autojs 修改版本）， 你现在看的是原4.1版本基础上的项目，
后面我们将针对项目本身如何开发、运行的进行介绍，欢迎更多开发者参与这个项目维护升级。[hyb1996](https://github.com/hyb1996/Auto.js)采用的
[Mozilla Public License Version 2.0](https://github.com/hyb1996/NoRootScriptDroid/blob/master/LICENSE.md) +**非商业性使用**，出于多种因素考虑，
本产品采用 [GPL-V2](https://opensource.org/licenses/GPL-2.0) 许可证， 无论是其他贡献者，还是使用该产品，均需按照 MPL-2.0+非商业性使用 和 GPL-V2 的相关要求使用。


关于两种协议：
* GPL-V2（https://opensource.org/licenses/GPL-2.0）
* MPL-2（https://www.mozilla.org/MPL/2.0/）

### 现在AutoXjs：
* AutoX.js文档： http://doc.autoxjs.com/
* 开源地址  https://github.com/kkevsekk1/AutoX
* pc端开发[VS Code 插件](https://marketplace.visualstudio.com/items?itemName=aaroncheng.auto-js-vsce-fixed)
* 官方论坛： [www.autoxjs.com](http://www.autoxjs.com)
* autoxjs[更新日志](CHANGELOG.md)

### 特性
1. 由无障碍服务实现的简单易用的自动操作函数
2. 悬浮窗录制和运行
3. 更专业&强大的选择器API，提供对屏幕上的控件的寻找、遍历、获取信息、操作等。类似于Google的UI测试框架UiAutomator，您也可以把他当做移动版UI测试框架使用
4. 采用JavaScript为脚本语言，并支持代码补全、变量重命名、代码格式化、查找替换等功能，可以作为一个JavaScript IDE使用
5. 支持使用e4x编写界面，并可以将JavaScript打包为apk文件，您可以用它来开发小工具应用
6. 支持使用Root权限以提供更强大的屏幕点击、滑动、录制功能和运行shell命令。录制录制可产生js文件或二进制文件，录制动作的回放比较流畅
7. 提供截取屏幕、保存截图、图片找色、找图等函数
8. 可作为Tasker插件使用，结合Tasker可胜任日常工作流
9. 带有界面分析工具，类似Android Studio的LayoutInspector，可以分析界面层次和范围、获取界面上的控件信息的


本软件与按键精灵等软件不同，主要区别是：
1. Auto.js主要以自动化、工作流为目标，更多地是方便日常生活工作，例如启动游戏时自动屏蔽通知、一键与特定联系人微信视频（知乎上出现过该问题，老人难以进行复杂的操作和子女进行微信视频）等
2. Auto.js兼容性更好。以坐标为基础的按键精灵、脚本精灵很容易出现分辨率问题，而以控件为基础的Auto.js则没有这个问题
3. Auto.js执行大部分任务不需要root权限。只有需要精确坐标点击、滑动的相关函数才需要root权限
4. Auto.js可以提供界面编写等功能，不仅仅是作为一个脚本软件而存在


### autojs信息
* 官方论坛： [autojs.org](http://www.autojs.org)
* 文档：可在[这里](https://hyb1996.github.io/AutoJs-Docs/)查看在线文档。目前文档仍然不完善。
* 示例：可在[这里](https://github.com/hyb1996/NoRootScriptDroid/tree/master/app/src/main/assets/sample)查看一些示例，或者直接在应用内查看和运行。

### 架构图
 待补充，不过是否有人真对此干兴趣？欢迎联系我交流

## 关于License
##### 本产品采用 [GPL-V2](https://opensource.org/licenses/GPL-2.0) 许可证
##### 由于历史原因还得遵循[autojs项目](https://github.com/hyb1996/Auto.js)的协议：

基于[Mozilla Public License Version 2.0](https://github.com/hyb1996/NoRootScriptDroid/blob/master/LICENSE.md)并附加以下条款：
* **非商业性使用** — 不得将此项目及其衍生的项目的源代码和二进制产品用于任何商业和盈利用途

#### 本AutoXjs能不能采用GPL-V2?

关于GPL-V2 应该很容易理解， 著名linux 采用该许可证。但是对于MPL-2.0 有非常多的文章都是停留在MPL-1.1 的版本，对非常多国内开发者造成困扰,
这是一篇比较标准[译文](https://github.com/rachelzhang1/MPL2.0_zh-CN/blob/93d2feec60d8b0b5a54a1843c866994af4610d4f/Mozilla_Public_License_2.0_Simplified_Chinese_Reference.txt)
有兴趣可以研究一下。

#### 代码贡献者需要注意：

原文中没人声明license 即为MPL2.0 ,新加文件或修改（仅限于修你自己的）代码采用GPL-V2，需要做相关声明。
``` java
// SPDX-License-Identifier: GPL-2.0
// 申明你的版权
```
#### 其他人使用AutoXjs，做深度开发请注意
* 如果你使用了带有GPL-2.0 声明的代码 或编译出来的二进制。你需要开源你所有代码。
* 如果你仅使用了MPL-2.0 的东西，你需要开源你修改过的相关代码。
#### 抛开本产品谈 开源和商业
* 开源不等于随意使用，开源也不等于禁止商用！
* 开源东西可以商用，但你需要按规定开源！
* 商用的产品可以是开源的，比如redhat！
* 不按开源协议使用开源产品，那可了解openwrt的来源，以及近几年国内的侵权案例！

#### 关于其他人开发的js脚本，在这上面运行。是否需要遵循GPL-2.0进行开源
* 那是你的自由，不受这协议限制，如同linux 运行软件一样

#### 使用本产品或autojs 产品是否可以商用?
* 本产品 能不能商用，取决于 原来autojs，因为目前很多功能和代码版权归autojs 所有。
* autojs 能不能商用,取决于你对于附带的 “ **非商业性使用** ” 的理解和其法律效益。
* 反正本产品不会拿autojs 进行商用。



