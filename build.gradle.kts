// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.devtools.ksp") version "1.8.10-1.0.9" apply false
}

allprojects {
    buildscript {
        repositories {
            google()
            mavenCentral()
            maven { url =  uri("https://www.maven.google.com") }
            maven { url = uri("https://jitpack.io") }
            maven { url = uri("https://dl.bintray.com/wotomas/maven") }
        }
        dependencies {
            //noinspection GradlePluginVersion
            classpath ("com.android.tools.build:gradle:3.1.2")
        }
    }
}