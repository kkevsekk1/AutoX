initVersions(file("project-versions.json"))

// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {

    extra.apply {
        set("kotlin_version", kotlin_version)
    }

    repositories {
        mavenLocal()
        //首选国外镜像加快github CI
        google()
        mavenCentral()
        maven("https://www.jitpack.io")
        maven("https://120.25.164.233:8081/nexus/content/groups/public/")
        maven("https://maven.aliyun.com/repository/central")
        google { url = uri("https://maven.aliyun.com/repository/google") }
        mavenCentral { url = uri("https://maven.aliyun.com/repository/public") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.0.2")
        classpath(kotlin("gradle-plugin", version = kotlin_version))
        classpath("com.jakewharton:butterknife-gradle-plugin:10.2.3")
        classpath("org.codehaus.groovy:groovy-json:3.0.8")
        classpath("com.yanzhenjie.andserver:plugin:2.1.12")
        classpath(libs.okhttp)
    }
}

allprojects {
    repositories {
        mavenLocal()
        //首选国外镜像加快github CI
        google()
        mavenCentral()
        maven("https://www.jitpack.io")
        maven("https://120.25.164.233:8081/nexus/content/groups/public/")
        maven("https://maven.aliyun.com/repository/central")
        google { url = uri("https://maven.aliyun.com/repository/google") }
        mavenCentral { url = uri("https://maven.aliyun.com/repository/public") }
    }
//    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java){
//        kotlinOptions{
//            freeCompilerArgs = freeCompilerArgs.toMutableList().apply {
//                add("-P")
//                add("plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true")
//            }
//        }
//    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}
