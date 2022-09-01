import java.util.*
import java.io.File

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-android-extensions")
    id("com.jakewharton.butterknife")
    id("kotlin-kapt")
}

val AAVersion = "4.5.2"
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
    buildToolsVersion = versions.buildTool
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
        kotlinCompilerExtensionVersion = "1.2.0-rc01"
        kotlinCompilerVersion = "1.6.20"
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

}

dependencies {

    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    val accompanist_version = "0.24.13-rc"
    implementation("com.google.accompanist:accompanist-permissions:0.24.13-rc")
    implementation("com.google.accompanist:accompanist-pager-indicators:$accompanist_version")
    implementation("com.google.accompanist:accompanist-pager:$accompanist_version")
    implementation("com.google.accompanist:accompanist-swiperefresh:$accompanist_version")
    implementation("com.google.accompanist:accompanist-appcompat-theme:$accompanist_version")
    implementation("com.google.accompanist:accompanist-insets:$accompanist_version")
    implementation("com.google.accompanist:accompanist-insets-ui:$accompanist_version")
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanist_version")
    implementation("com.google.accompanist:accompanist-webview:$accompanist_version")

    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.material:material:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation("androidx.activity:activity-compose:1.3.1")
    implementation("org.chromium.net:cronet-embedded:76.3809.111")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")


    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.1-alpha01") {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation("junit:junit:4.13.2")
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.2")
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
    implementation("androidx.appcompat:appcompat:1.4.2") //
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.7.0-alpha03")
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
    implementation("de.psdev.licensesdialog:licensesdialog:1.9.0")
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
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.8.1")
    releaseImplementation("com.squareup.leakcanary:leakcanary-android-no-op:1.6.3")
    // Optional, if you use support library fragments:
    debugImplementation("com.squareup.leakcanary:leakcanary-support-fragment:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-rxjava2-adapter:1.0.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")
    //Glide
    implementation("com.github.bumptech.glide:glide:4.8.0") {
        exclude(group = "com.android.support")
    }
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
    implementation("com.afollestad.material-dialogs:commons:0.9.2.3") {
        exclude(group = "com.android.support")
    }
    // WorkManager
    implementation("androidx.work:work-runtime:2.7.1")
    // Android job
    implementation("com.evernote:android-job:1.4.2")
    // Optional, if you use support library fragments:
    implementation(project(":automator"))
    implementation(project(":common"))
    implementation(project(":autojs"))
    implementation(project(":apkbuilder"))
    implementation("androidx.multidex:multidex:2.0.1")

    val lifecycle_version = "2.5.0-rc01"
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

    val ktor_version = "2.0.3"
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-okhttp:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")

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
