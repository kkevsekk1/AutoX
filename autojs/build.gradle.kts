plugins {
    alias(libs.plugins.autojs.android.library)
}

android {
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
    lint{
        abortOnError = false
    }
    sourceSets {
        named("main") {
            res.srcDirs("src/main/res", "src/main/res-i18n")
        }
    }
    namespace = "com.stardust.autojs"
}

dependencies {
    api(projects.common)
    api(projects.automator)
    api(projects.localRepo.libtermexec)
    api(projects.localRepo.emulatorview)
    api(projects.localRepo.term)
    api(projects.localRepo.p7zip)
    api(projects.localRepo.openCV)
    api(projects.paddleocr)
    // libs
    api(fileTree("../app/libs") { include("dx.jar", "rhino-1.7.14-jdk7.jar") })

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

    // RootShell
    api(libs.rootshell)
    // Gson
    api(libs.google.gson)
    // log4j
    api(libs.android.logging.log4j)
    api(libs.log4j)
    api(libs.tesseract4android)
    api(libs.text.recognition)
    api(libs.text.recognition.chinese)
    api(libs.text.recognition.devanagari)
    api(libs.text.recognition.japanese)
    api(libs.text.recognition.korean)
}

