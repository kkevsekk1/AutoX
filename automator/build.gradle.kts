plugins {
    alias(libs.plugins.autojs.android.library)
}

android {
    lint{
        abortOnError = false
    }
    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
        }
    }
    sourceSets {
        named("main") {
            jniLibs.srcDirs("/libs")
        }
    }
    namespace = "com.stardust.automator"
}

dependencies {
    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.junit)
    api(libs.appcompat)
    api(project(":common"))
}
