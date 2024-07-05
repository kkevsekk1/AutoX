plugins {
    alias(libs.plugins.autojs.android.library)
}

android {
    namespace = "org.autojs.autojs.core.network"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(projects.core.model)

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)

    implementation(libs.ktor.client.websockets)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    //https://github.com/tony19/logback-android
    implementation(libs.slf4jApi)
    implementation(libs.logbackAndroid)
    implementation(libs.google.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
