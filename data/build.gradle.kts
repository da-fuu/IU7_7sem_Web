plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.androidx.room)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

tasks.jar {
    manifest {
        attributes["Module-Name"] = "com.ppo.data"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.slf4j.api)
    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.sqlite.bundled)
    testImplementation(libs.sqlite.bundled.jvm)
    testImplementation(libs.kotlinx.coroutines.test)
}

room {
    schemaDirectory("$projectDir/schemas")
}
