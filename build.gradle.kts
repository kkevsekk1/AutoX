initVersions(file("project-versions.json"))

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://www.jitpack.io")
        maven("https://maven.aliyun.com/repository/central")
    }
    dependencies {
        classpath(libs.butterknife.gradle.plugin)
        classpath(libs.groovy.json)
        classpath(libs.andserver)
        classpath(libs.okhttp)
    }
}
plugins{
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

