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
        attributes["Module-Name"] = "com.ppo.domain"
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    systemProperty("allure.results.directory", System.getenv("ALLURE_RESULTS_DIR") ?: rootProject.layout.buildDirectory.dir("allure-results").get().asFile.absolutePath)
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
    implementation(libs.slf4j.api)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation("io.qameta.allure:allure-junit4:2.27.0")
}
