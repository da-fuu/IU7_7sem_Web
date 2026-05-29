plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
    alias(libs.plugins.androidx.room)
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
