plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = versions.compile

    defaultConfig {
        minSdk = versions.mini
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        named("release") {
            isMinifyEnabled = false
            setProguardFiles(listOf(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro"))
        }
    }

    sourceSets {
        named("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
    namespace = "org.autojs.autoxjs.paddleocr"
}

dependencies {
    implementation(libs.okhttp)
    implementation("androidx.core:core-ktx:1.8.0")
}
dependencies {
    testImplementation( "junit:junit:4.13.2")
    androidTestImplementation( "androidx.test.ext:junit:1.1.3")
    androidTestImplementation( "androidx.test.espresso:espresso-core:3.4.0")
}
repositories {
    mavenCentral()
}
