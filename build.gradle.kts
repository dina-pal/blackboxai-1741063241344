buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(Deps.androidGradlePlugin)
        classpath(Deps.kotlinGradlePlugin)
    }
}

plugins {
    id(Plugins.application) version Versions.gradle apply false
    id(Plugins.androidLibrary) version Versions.gradle apply false
    id(Plugins.kotlinAndroid) version Versions.kotlin apply false
    id(Plugins.kotlinSerialization) version Versions.kotlin apply false
    id(Plugins.ksp) version "2.0.0-Beta1-1.0.15" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
