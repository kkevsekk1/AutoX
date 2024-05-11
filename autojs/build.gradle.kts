plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin)
}
kotlin {
    jvmToolchain(17)
}
android {
    compileSdk = versions.compile

    defaultConfig {
        minSdk = versions.mini
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(
                listOf(
                    getDefaultProguardFile("proguard-android.txt"),
                    "proguard-rules.pro"
                )
            )
        }
    }


    lint.abortOnError = false
    sourceSets {
        named("main") {
//            jniLibs.srcDirs = listOf("src/main/jniLibs")
            res.srcDirs("src/main/res", "src/main/res-i18n")
        }
    }
    compileOptions {
        sourceCompatibility = versions.javaVersion
        targetCompatibility = versions.javaVersion
    }
    namespace = "com.stardust.autojs"
}

dependencies {
    androidTestImplementation(libs.espresso.core)
    debugImplementation(libs.leakcanary.android)
    implementation(libs.leakcanary.`object`.watcher.android)
    testImplementation(libs.junit)

    implementation(libs.documentfile)
    implementation(libs.androidx.preference.ktx)
    api(libs.eventbus)
    api(libs.zip4j)
    api(libs.core)
    api(libs.material)
    api(libs.enhancedfloaty)
    api(libs.roundedimageview)
    // OkHttp
    api(libs.okhttp)
    // JDeferred
    api(libs.jdeferred.android.aar)
    // RootShell
    api(libs.rootshell)
    // Gson
    api(libs.google.gson)
    // log4j
    api(libs.android.logging.log4j)
    api(libs.log4j)
    api(project(":common"))
    api(project(":automator"))
    api(project(":LocalRepo:libtermexec"))
    api(project(":LocalRepo:emulatorview"))
    api(project(":LocalRepo:term"))
    api(project(":LocalRepo:p7zip"))
    api(project(":LocalRepo:OpenCV"))
    api(project(":paddleocr"))
    // libs
    api(fileTree("../app/libs") { include("dx.jar", "rhino-1.7.14-jdk7.jar") })
    api(libs.tesseract4android)
    api(libs.text.recognition)
    api(libs.text.recognition.chinese)
    api(libs.text.recognition.devanagari)
    api(libs.text.recognition.japanese)
    api(libs.text.recognition.korean)
}

