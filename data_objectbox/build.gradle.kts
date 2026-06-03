plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.objectbox)
}

android {
    namespace = "com.ppo.data_objectbox"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.io.objectbox.android)
    implementation(libs.io.objectbox.kotlin)
    kapt("io.objectbox:objectbox-processor:${libs.versions.objectbox.get()}")
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
}
