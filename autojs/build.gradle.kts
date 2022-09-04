plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-android-extensions")
}

android {
    buildToolsVersion = versions.buildTool
    compileSdk = versions.compile

    defaultConfig {
        minSdk = versions.mini
        targetSdk = versions.target
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    lintOptions.isAbortOnError = false
    sourceSets {
        named("main") {
//            jniLibs.srcDirs = listOf("src/main/jniLibs")
            res.srcDirs("src/main/res","src/main/res-i18n")
        }
    }
}

dependencies {
    androidTestImplementation("androidx.test.espresso:espresso-core:3.1.1-alpha01"){
        exclude(group = "com.android.support",module = "support-annotations")
    }
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.preference:preference-ktx:1.2.0")
    api("org.greenrobot:eventbus:3.3.1")
    api("net.lingala.zip4j:zip4j:1.3.2")
    api("com.afollestad.material-dialogs:core:0.9.2.3"){
        exclude(group = "com.android.support")
    }
    api("com.google.android.material:material:1.7.0-beta01")
    api("com.github.hyb1996:EnhancedFloaty:0.31")
    api("com.makeramen:roundedimageview:2.3.0")
    // OkHttp
    api("com.squareup.okhttp3:okhttp:4.10.0")
    // JDeferred
    api("org.jdeferred:jdeferred-android-aar:1.2.6")
    // RootShell
    api("com.github.Stericson:RootShell:1.6")
    // Gson
    api("com.google.code.gson:gson:2.9.1")
    // log4j
    api(group = "de.mindpipe.android", name = "android-logging-log4j", version = "1.0.3")
    api(group = "log4j", name = "log4j", version = "1.2.17")
    api(project(path = ":common"))
    api(project(path = ":automator"))
    api(project(path = ":LocalRepo:libtermexec"))
    api(project(path = ":LocalRepo:emulatorview"))
    api(project(path = ":LocalRepo:term"))
    api(project(path = ":LocalRepo:p7zip"))
    api(project(path = ":LocalRepo:OpenCV"))
    api(project(":paddleocr"))
    // libs
    api(fileTree("../app/libs"){include("dx.jar", "rhino-1.7.14-jdk7.jar")})
    api("cz.adaptech:tesseract4android:4.1.1")
    api("com.google.mlkit:text-recognition:16.0.0-beta5")
    api("com.google.mlkit:text-recognition-chinese:16.0.0-beta5")
    api("com.google.mlkit:text-recognition-devanagari:16.0.0-beta5")
    api("com.google.mlkit:text-recognition-japanese:16.0.0-beta5")
    api("com.google.mlkit:text-recognition-korean:16.0.0-beta5")
}

