plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin)
}
android {
    compileSdk = versions.compile

    defaultConfig {
        minSdk = versions.mini
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
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

}

dependencies {
    implementation(libs.core.ktx)

    implementation(libs.okhttp)

    api(files("libs/tiny-sign-0.9.jar"))

    api(libs.commons.io)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.junit)
}
