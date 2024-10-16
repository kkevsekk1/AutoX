plugins {
    id("com.android.library")
    id("kotlin-android")
}
android {
    compileSdk = versions.compile

    defaultConfig {
        minSdk = versions.mini
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.add("arm64-v8a")
        }
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
    namespace = "com.stardust.autojs.apkbuilder"
    compileOptions {
        sourceCompatibility = versions.javaVersion
        targetCompatibility = versions.javaVersion
    }

    sourceSets {
        named("main") {
            jniLibs.srcDirs("libs")
        }
    }
}

dependencies {
    implementation(libs.commons.exec)
    api(libs.timscriptov.apksigner)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("lib*.so"))))
    implementation(libs.okhttp)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.junit)
    api(libs.commons.io)
    implementation(libs.core.ktx)
}
repositories {
    mavenCentral()
}
