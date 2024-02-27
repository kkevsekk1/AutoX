### 编译相关：
环境要求:`jdk`版本17以上

命令说明：在项目根目录下运行命令，如果使用 Windows powerShell < 7.0，请使用包含 ";" 的命令

##### 本地安装调试版本到设备：
```shell
./gradlew app:buildDebugTemplateApp && ./gradlew app:assembleV6Debug && ./gradlew app:installV6Debug
#或
./gradlew app:buildDebugTemplateApp ; ./gradlew app:assembleV6Debug ; ./gradlew app:installV6Debug
```
生成的调试版本APK文件在 app/build/outputs/apk/v6/debug 下，使用默认签名

##### 本地编译发布版本：
```shell
./gradlew app:buildTemplateApp && ./gradlew inrt:cp2APP && ./gradlew app:assembleV6
#或
./gradlew app:buildTemplateApp ; ./gradlew inrt:cp2APP ; ./gradlew app:assembleV6
```
生成的是未签名的APK文件，在 app/build/outputs/apk/v6/release 下，需要签名后才能安装

##### 本地 Android Studio 运行调试版本到设备：
先运行以下命令：

```shell
./gradlew app:buildDebugTemplateApp
```

再点击 Android Studio 运行按钮

##### 本地 Android Studio 编译发布版本并签名：
先运行以下命令：

```shell
./gradlew app:buildTemplateApp
```

再点击 Android Studio 菜单 
"Build" ->"Generate Signed Bundle /APK..." ->
勾选"APK" ->"Next" ->
选择或新建证书 ->"Next" ->
选择"v6Release" -> "Finish"生成的APK文件，在 app/v6/release 下
