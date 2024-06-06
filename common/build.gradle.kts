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
    lint {
        abortOnError = false
    }
    sourceSets {
        getByName("main") {
            res.srcDirs("src/main/res", "src/main/res-i18n")
        }
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

    // MQTT
    implementation(libs.org.eclipse.paho.client.mqttv3)
    implementation(libs.org.eclipse.paho.android.service)

    api(libs.androidx.collection)
    api(libs.core.ktx)
}
