# Auto.js and AutoX.js
[中文文档](README.md)
## Introduction
A JavaScript runtime and development environment on the Android platform that supports accessibility services, 
and its development goals are similar to JsBox and Workflow.

 ~~Since the [original author](https://github.com/hyb1996) no longer maintains the Auto.js project,
 I plan to continue the maintainer project on the original basis. This project will be transferred 
 from [autojs](https://github.com/hyb1996/Auto.js) The project is named Autox.js. What you are looking 
 at is the project based on the original version 4.1. Later, I will introduce how to develop and 
 run the project itself. More developers are welcome to participate in the maintenance and upgrade 
 of this project. The latest [Autox.js Address](https://github.com/kkevsekk1/AutoX), many original project
 paths in the document, I do not intend to replace them if the original project is not deleted,
 to show respect for the original author. The encryption-related content in this document may 
 conflict with the actual operation. If you want to write code encrypted to protect intellectual 
 property rights, please refer to the project https:github.comkkevsekk1webpack-autojs I will gradually
 improve and update the program code, keep it as much as possible Consistent.~~

This project is obtained from [hyb1996](https://github.com/hyb1996/Auto.js) autojs and named as 
Autox.js (autojs modified version). What you are looking at now is the project based on the original
4.1 version. Later we will discuss how the project itself The development and operation are introduced,
and more developers are welcome to participate in the maintenance and upgrade of this project.
[Mozilla Public License Version 2.0](https://github.com/hyb1996/NoRootScriptDroid/blob/master/LICENSE.md)
adopted by [hyb1996](https://github.com/hyb1996/Auto.js) + non-commercial use, for various reasons, 
this product adopts [GPL-V2 ](https://opensource.org/licenses/GPL-2.0) license, whether other contributors,
or use of this product, must be used in accordance with the relevant requirements of MPL-2.0+ 
non-commercial use and GPL-V2.


About the two protocols:
* GPL-V2（https://opensource.org/licenses/GPL-2.0）
* MPL-2 (https://www.mozilla.org/MPL/2.0)

### Autoxjs Now：
* Autox.js Docs： http://doc.autoxjs.com/
* Open Source Address:  https://github.com/kkevsekk1/AutoX
* PC Develop: [VS Code Extension](https://marketplace.visualstudio.com/items?itemName=aaroncheng.auto-js-vsce-fixed)
* Official Forums：[www.autoxjs.com](http://www.autoxjs.com)
* autoxjs[Changelog](CHANGELOG.md)

### Autox.js download address: 
[https://github.com/kkevsekk1/AutoX/releases](https://github.com/kkevsekk1/AutoX/releases)
#### APK version description：
- universal: Universal version (don't care about the size of the installation package, just use this version, including the following 2 CPU architectures so)
- armeabi-v7a: 32-bit ARM device (preferred for spare)
- arm64-v8a: 64-bit ARM device (mainstream flagship)

### Feature
1. Simple and easy-to-use automation functions implemented by accessibility services
2. Floating window recording and running
3. More professional & powerful selector API, providing search, traversal, information acquisition, operation, etc. on the controls on the screen. Similar to Google's UI testing framework UiAutomator, you can also use it as a mobile UI testing framework
4. Using JavaScript as the scripting language, and supporting functions such as code completion, variable renaming, code formatting, search and replacement, etc., it can be used as a JavaScript IDE
5. Supports the use of e 4 x to write the interface, and can package Java Script as apk file, you can use it to develop gadget applications
6. Supports the use of root privileges to provide more powerful screen tap, swipe, record and run shell commands. Recording and recording can generate js files or binary files, and the playback of recorded actions is relatively smooth
7. Provides functions such as taking screenshots, saving screenshots, finding colors in pictures, and finding pictures
8. Can be used as a Tasker plugin, combined with Tasker for daily workflow
9. With an interface analysis tool, similar to Android Studio's LayoutInspector, it can analyze the interface level and scope, and obtain the control information on the interface.


This software is different from software such as Button Wizard. The main differences are:
1. Autox.js is mainly aimed at automation and workflow, and is more convenient for daily work, such as automatically blocking notifications when starting a game, one-click WeChat video with a specific contact (this problem has occurred on Zhihu, and it is difficult for the elderly to perform complex tasks.) operation and children for WeChat video), etc.
2. Autox.js compatibility is better. Coordinate-based button sprites and script sprites are prone to resolution problems, while control-based Auto.js does not have this problem
3. Autox.js does not require root privileges to perform most tasks. Only related functions that require precise coordinates to click and slide require root privileges
4. Autox.js can provide functions such as interface writing, not only as a script software


### Autojs information
* Official Forums： [autojs.org](http://www.autojs.org)
* Docs：Online documentation is [here](https://hyb1996.github.io/AutoJs-Docs/). Documentation is still incomplete at the moment.
* Example：Some examples can be viewed [here](https://github.com/hyb1996/NoRootScriptDroid/tree/master/app/src/main/assets/sample), or viewed and run directly within the app.

### Architecture diagram
To be added, but is anyone really interested in this? Welcome to contact me to communicate

## About License
##### This product is licensed under the [GPL-V2](https://opensource.org/licenses/GPL-2.0) license
##### For historical reasons, the protocol of the [Autojs Project](https://github.com/hyb1996/Auto.js) has to be followed:

Based on the [Mozilla Public License Version 2.0](https://github.com/hyb1996/NoRootScriptDroid/blob/master/LICENSE.md)
with the following terms: Non-Commercial Use - The source code and binary products of this project and its derived projects may not be used for any commercial or for-profit use

#### Code contributors need to take note：

No one in the original text states that the license is MPL2.0, and the newly added files or modified
(only for repairing your own) code adopts GPL-V2, and relevant declarations are required.
``` java
// SPDX-License-Identifier: GPL-2.0
// Claim your copyright
```
#### Others use Autox.js, please pay attention to in-depth development
* If you use GPL-2.0 declared code or compiled binaries. You need to open source all your code.
* If you only use MPL-2.0 stuff, you need to open source the relevant code you modified.
#### Aside from this product, talk about open source and business
* Open source does not mean free use, and open source does not mean prohibiting commercial use!
* Open source things can be commercialized, but you need to open source according to the regulations!
* Commercial products can be open source, such as redhat!
* If you do not use open source products according to the open source agreement, you can understand the source of openwrt and the domestic infringement cases in recent years!

#### For js scripts developed by other people, run it on this. Does it need to follow GPL-2.0
* That is your freedom, not restricted by this agreement, just like linux runs software

#### Is it possible to use this product or autojs product for commercial use?
* Whether this product can be used for commercial use depends on the original autojs, because many functions and codes are copyrighted by autojs.
* Whether autojs can be commercialized depends on your understanding of the accompanying "non-commercial use" and its legal benefits.
* Anyway, this product will not use autojs for commercial use.

#### Compilation related：
Command description: Run the command in the project root directory, if using Windows powerShell < 7.0, use the command containing ";"

##### Install the debug build locally to the device：
```shell
./gradlew inrt:assembleTemplateDebug && ./gradlew inrt:cp2APPDebug && ./gradlew app:assembleV6Debug && ./gradlew app:installV6Debug
#or
./gradlew inrt:assembleTemplateDebug ; ./gradlew inrt:cp2APPDebug ; ./gradlew app:assembleV6Debug ; ./gradlew app:installV6Debug
```
The generated debug version APK file is under app/build/outputs/apk/v6/debug with the default signature

##### Compile the release version locally：
```shell
./gradlew inrt:assembleTemplate && ./gradlew inrt:cp2APP && ./gradlew app:assembleV6
#or
./gradlew inrt:assembleTemplate ; ./gradlew inrt:cp2APP ; ./gradlew app:assembleV6
```
The generated APK file is an unsigned APK file. Under app/build/outputs/apk/v6/release, it needs to be signed before it can be installed.

##### Local Android Studio to run the debug build to the device:
First run the following command:

```shell
./gradlew inrt:assembleTemplate && ./gradlew inrt:cp2APP
#or
./gradlew inrt:assembleTemplate ; ./gradlew inrt:cp2APP
```

Then click the Android Studio Run button

##### Local Android Studio compiles and signs the release APK:
First run the following command:

```shell
./gradlew inrt:assembleTemplate && ./gradlew inrt:cp2APP
#or
./gradlew inrt:assembleTemplate ; ./gradlew inrt:cp2APP
```

Then click Android Studio menu "Build" -> "Generate Signed Bundle APK..." -> check "APK" 
-> "Next" -> select or create a new certificate -> "Next" -> select "v6Release" -> "Finish"
Generated APK file, under app/v6/release

