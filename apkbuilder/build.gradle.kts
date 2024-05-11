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
    namespace = "com.stardust.autojs.apkbuilder"
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
