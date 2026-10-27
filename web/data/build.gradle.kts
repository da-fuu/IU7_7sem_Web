plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    jacoco
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
        attributes["Module-Name"] = "com.ppo.web.data"
    }
}

tasks.test {
    exclude("**/*ITCase.*")
    finalizedBy(tasks.jacocoTestReport)
    systemProperty("allure.results.directory", rootProject.layout.buildDirectory.dir("allure-results").get().asFile.absolutePath)
    project.findProperty("test.random.seed")?.let {
        systemProperty("test.random.seed", it.toString())
    }
    project.findProperty("maxParallelForks")?.toString()?.toIntOrNull()?.let {
        maxParallelForks = it
    }
    project.findProperty("forkEvery")?.toString()?.toLongOrNull()?.let {
        forkEvery = it
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.slf4j.api)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)

    testImplementation(libs.mockk)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("com.h2database:h2:2.2.224")
    testImplementation("io.qameta.allure:allure-junit4:2.27.0")
}

val integrationTest by tasks.registering(Test::class) {
    outputs.upToDateWhen { false }
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/*ITCase.*")
    systemProperty("allure.results.directory", rootProject.layout.buildDirectory.dir("allure-results").get().asFile.absolutePath)
}
