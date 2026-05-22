plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.google.devtools.ksp")
    alias(libs.plugins.androidx.room)
}

tasks.jar {
    dependsOn(":domain:jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

dependencies {
    implementation(project(":domain"))
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
