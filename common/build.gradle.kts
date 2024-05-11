//import org.jetbrains.kotlin.config.KotlinCompilerVersion

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
    androidTestImplementation(libs.espresso.core) {
        exclude(group = "com.android.support", module = "support-annotations")
    }
    testImplementation(libs.junit)
    api(libs.androidx.annotation)
    api(libs.settingscompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    api(kotlin("reflect"))

    //MQTT
    implementation(libs.org.eclipse.paho.client.mqttv3)
    implementation(libs.org.eclipse.paho.android.service)
}
