//import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("kotlin-android")
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = compose_version
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
        getByName("main") {
            res.srcDirs("src/main/res","src/main/res-i18n")
        }
    }
    compileOptions {
        sourceCompatibility = versions.javaVersion
        targetCompatibility = versions.javaVersion
    }
    namespace = "com.stardust"
}

dependencies {
    androidTestImplementation(libs.espresso.core)
    implementation(platform(libs.compose.bom))
    api(libs.activity.compose)
    api(libs.compose.ui)
    api(libs.compose.ui.tooling.preview)
    api(libs.compose.material3)
    api(libs.compose.material3.window.size)
    api(libs.compose.material3.adaptive.navigation.suite)
    testImplementation(libs.junit)
    api(libs.androidx.annotation)
    api("com.github.hyb1996:settingscompat:1.1.5")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    api(kotlin("reflect", version = "1.7.10"))

    //MQTT
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0")
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
}
