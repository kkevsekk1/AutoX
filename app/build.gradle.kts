import com.android.build.gradle.internal.tasks.factory.dependsOn
import okhttp3.Request
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.jakewharton.butterknife")
    id("kotlin-kapt")
}

//val SupportLibVersion = "28.0.0"

val propFile: File = File("E:/资料/jks/autojs-app/sign.properties");
val properties = Properties()
if (propFile.exists()) {
    propFile.inputStream().reader().use {
        properties.load(it)
    }
}

//configurations.all {
//    resolutionStrategy {
//        force("com.android.support:appcompat-v7:${SupportLibVersion}")
//        force("com.android.support:support-v4:${SupportLibVersion}")
//    }
//}
android {
    compileSdk = versions.compile
    defaultConfig {
        applicationId = "org.autojs.autoxjs"
        minSdk = versions.mini
        targetSdk = versions.target
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
//        multiDexEnabled = true
        buildConfigField("boolean", "isMarket", "false")
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["resourcePackageName"] = applicationId.toString()
                arguments["androidManifestFile"] = "$projectDir/src/main/AndroidManifest.xml"
            }
        }
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = false
        disable.addAll(listOf("MissingTranslation", "ExtraTranslation"))
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    composeOptions {
        kotlinCompilerExtensionVersion = compose_version
    }
    signingConfigs {
        if (propFile.exists()) {
            getByName("release") {
                storeFile = file(properties.getProperty("storeFile"))
                storePassword = properties.getProperty("storePassword")
                keyAlias = properties.getProperty("keyAlias")
                keyPassword = properties.getProperty("keyPassword")
            }
        }
    }
    splits {

        // Configures multiple APKs based on ABI.
        abi {

            // Enables building multiple APKs per ABI.
            isEnable = true

            // By default all ABIs are included, so use reset() and include to specify that we only
            // want APKs for x86 and x86_64.

            // Resets the list of ABIs that Gradle should create APKs for to none.
            reset()

            // Specifies a list of ABIs that Gradle should create APKs for.
            include("armeabi-v7a", "arm64-v8a")

            // Specifies that we do not want to also generate a universal APK that includes all ABIs.
            isUniversalApk = true
        }
    }
    buildTypes {
        named("debug") {
            isShrinkResources = false
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            )
            if (propFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        named("release") {
            isShrinkResources = false
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            )
            if (propFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    flavorDimensions.add("channel")
    productFlavors {
        create("common") {
            versionCode = versions.appVersionCode
            versionName = versions.appVersionName
            buildConfigField("String", "CHANNEL", "\"common\"")
//            buildConfigField("String", "APPID", "\"?id=21\"")
            manifestPlaceholders.putAll(mapOf("appName" to "@string/app_name"))
        }
        create("v6") {
            applicationIdSuffix = ".v6"
            versionCode = versions.devVersionCode
            versionName = versions.devVersionName
            buildConfigField("String", "CHANNEL", "\"v6\"")
//            buildConfigField("String", "APPID", "\"?id=23\"")
            manifestPlaceholders.putAll(mapOf("appName" to "Autox.js v6"))
        }
    }

    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res", "src/main/res-i18n")
            jniLibs.srcDirs("/libs")
        }
    }

    configurations.all {
        resolutionStrategy.force("com.google.code.findbugs:jsr305:3.0.1")
        exclude(group = "org.jetbrains", module = "annotations-java5")
//        exclude(group = "com.atlassian.commonmark",) module = "commonmark"
        exclude(group = "com.github.atlassian.commonmark-java", module = "commonmark")
    }

    packagingOptions {
        //ktor netty implementation("io.ktor:ktor-server-netty:2.0.1")
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/io.netty.versions.properties",
                "META-INF/INDEX.LIST"
            )
        )
    }
    namespace = "org.autojs.autoxjs"

}

dependencies {
    val AAVersion = "4.5.2"

    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation(libs.androidx.webkit)

    implementation(libs.bundles.accompanist)

    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.ui.tooling.preview)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)

    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)
    // Kotlin携程
    implementation(libs.kotlinx.coroutines.android)
    // Android Annotations
    annotationProcessor("org.androidannotations:androidannotations:$AAVersion")
    kapt("org.androidannotations:androidannotations:$AAVersion")
    //noinspection GradleDependency
    implementation("org.androidannotations:androidannotations-api:$AAVersion")
    // ButterKnife
    implementation("com.jakewharton:butterknife:10.2.1") {
        exclude(group = "com.android.support")
    }
    annotationProcessor("com.jakewharton:butterknife-compiler:10.2.3")
    kapt("com.jakewharton:butterknife-compiler:10.2.3")
    // Android supports
    implementation("androidx.preference:preference-ktx:1.2.0")
    implementation(libs.appcompat) //
    implementation("androidx.cardview:cardview:1.0.0")
    implementation(libs.material)
    // Personal libraries
    implementation("com.github.hyb1996:MutableTheme:1.0.0")
    // Material Dialogs
    implementation("com.afollestad.material-dialogs:core:0.9.2.3") {
        exclude(group = "com.android.support")
    }
    // Common Markdown
    implementation("com.github.atlassian:commonmark-java:commonmark-parent-0.9.0")
    // Android issue reporter (a github issue reporter)
    implementation("com.heinrichreimersoftware:android-issue-reporter:1.3.1") {
        exclude(group = "com.afollestad.material-dialogs")
        exclude(group = "com.android.support")
    }
    //MultiLevelListView
    implementation("com.github.hyb1996:android-multi-level-listview:1.1")
    //Licenses Dialog
    implementation("de.psdev.licensesdialog:licensesdialog:2.2.0")
    //Expandable RecyclerView
    implementation("com.bignerdranch.android:expandablerecyclerview:3.0.0-RC1")
    //FlexibleDivider
    implementation("com.yqritc:recyclerview-flexibledivider:1.4.0")
    //???
    implementation("com.wang.avi:library:2.1.3")
    //Commons-lang
    implementation("org.apache.commons:commons-lang3:3.12.0")
    // 证书签名相关
    implementation("com.madgag.spongycastle:bcpkix-jdk15on:1.56.0.0")
    //Expandable RecyclerView
    implementation("com.thoughtbot:expandablerecyclerview:1.3")
//    implementation("org.signal.autox:apkbuilder:1.0.3")
    // RxJava
    implementation(libs.rxjava2)
    implementation(libs.rxjava2.rxandroid)
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    debugImplementation(libs.leakcanary.android)
    // Optional, if you use support library fragments:
    implementation("com.jakewharton.retrofit:retrofit2-rxjava2-adapter:1.0.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    //Glide
    implementation("com.github.bumptech.glide:glide:4.8.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.12.0")
    //joda time
    implementation("net.danlew:android.joda:2.10.14")
    // Tasker Plugin
    implementation("com.twofortyfouram:android-plugin-client-sdk-for-locale:4.0.3")
    // Flurry
    implementation("com.flurry.android:analytics:13.1.0@aar")
    // tencent
    implementation("com.tencent.bugly:crashreport:4.0.0")
    api("com.tencent.tbs:tbssdk:44181")
    // MaterialDialogCommon
    implementation("com.afollestad.material-dialogs:commons:0.9.2.3")
    // WorkManager
    implementation("androidx.work:work-runtime:2.7.1")
    // Android job
    implementation("com.evernote:android-job:1.4.2")
    // Optional, if you use support library fragments:
    implementation(project(":autojs"))
    implementation(project(":apkbuilder"))
    implementation(project(":codeeditor"))
    implementation("androidx.multidex:multidex:2.0.1")

    val lifecycle_version = "2.5.1"
    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycle_version")
    // ViewModel utilities for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_version")
    // Lifecycles only (without ViewModel or LiveData)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:$lifecycle_version")
    // Saved state module for ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:$lifecycle_version")
    // Annotation processor
    kapt("androidx.lifecycle:lifecycle-compiler:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-service:$lifecycle_version")
    implementation("androidx.savedstate:savedstate-ktx:1.2.0")
    implementation("androidx.savedstate:savedstate:1.2.0")

    implementation(libs.bundles.ktor)
    //qr scan
    implementation("io.github.g00fy2.quickie:quickie-bundled:1.5.0")
    //Fab button with menu, please do not upgrade, download dependencies will be error after upgrade
    //noinspection GradleDependency
    implementation("com.leinardi.android:speed-dial.compose:1.0.0-alpha03")
    //TextView markdown
    implementation("io.noties.markwon:core:4.6.2")
    implementation("androidx.viewpager2:viewpager2:1.1.0-beta01")
    implementation("io.coil-kt:coil-compose:2.0.0-rc03")
}

fun copyTemplateToAPP(isDebug: Boolean, to: File) {
    val outName = if (isDebug) "template-debug" else "template-release"
    val outFile = project(":inrt").buildOutputs.named(outName).get().outputFile
    copy {
        from(outFile)
        into(to)
        delete(File(to, "template.apk"))
        rename(outFile.name, "template.apk")
    }
    logger.lifecycle("buildTemplate success, debugMode: $isDebug")
}

val assetsDir = File(projectDir, "src/main/assets")
if (!File(assetsDir, "template.apk").isFile) {
    tasks.named("preBuild").dependsOn("buildTemplateApp")
}

tasks.register("buildTemplateApp") {
    dependsOn(":inrt:assembleTemplateRelease")
    doFirst {
        copyTemplateToAPP(false, assetsDir)
    }
}
tasks.register("buildDebugTemplateApp") {
    dependsOn(":inrt:assembleTemplateDebug")
    doFirst {
        copyTemplateToAPP(true, assetsDir)
    }
}
tasks.named("clean").configure {
    doFirst {
        delete(File(assetsDir, "template.apk"))
    }
}
//离线文档下载安装
val docsDir = File(projectDir, "src/main/assets/docs")
tasks.named("preBuild").dependsOn("installationDocumentation")
tasks.register("installationDocumentation") {
    val docV1Uri = "https://codeload.github.com/kkevsekk1/kkevsekk1.github.io/zip/refs/heads/main"
    val docV1Dir = File(docsDir, "v1")
    doFirst {
        if (File(docV1Dir, "index.html").isFile) {
            return@doFirst
        }
        okhttp3.OkHttpClient().newCall(Request.Builder().url(docV1Uri).build()).execute()
            .use { response ->
                check(response.isSuccessful) { "installationDocumentation failed" }
                val body = response.body!!
                ZipInputStream(body.byteStream()).use { zip ->
                    var zipEntry: ZipEntry?;
                    while (true) {
                        zipEntry = zip.nextEntry ?: break
                        val file = File(docV1Dir, zipEntry.name.replaceFirst(Regex(".+?/"), ""))
                        if (zipEntry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.outputStream().use {
                                zip.copyTo(it)
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
    }
}
tasks.named("clean").configure {
    doFirst { delete(docsDir) }
}