plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    application
    jacoco
}

application {
    mainClass.set("com.ppo.web.ApplicationKt")
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

dependencies {
    implementation(project(":domain"))
    implementation(project(":web:data"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)

    implementation(libs.auth0.jwt)
    implementation(libs.koin.core)
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation("io.qameta.allure:allure-junit4:2.27.0")
    testImplementation(libs.ktor.client.cio)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.ppo.web.ApplicationKt"
    }
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

tasks.test {
    include("**/*Test.*")
    exclude("**/*ITCase.*")
    systemProperty("allure.results.directory", rootProject.layout.buildDirectory.dir("allure-results").get().asFile.absolutePath)
}

val e2eTest by tasks.registering(Test::class) {
    description = "Runs end-to-end MVP demonstration scenario test"
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    include("**/MvpScenarioITCase.*")
    systemProperty("allure.results.directory", rootProject.layout.buildDirectory.dir("allure-results").get().asFile.absolutePath)
    systemProperty("DB_HOST", System.getenv("POSTGRES_TEST_HOST") ?: "localhost")
    systemProperty("DB_PORT", System.getenv("POSTGRES_TEST_PORT") ?: "5434")
    systemProperty("DB_NAME", System.getenv("POSTGRES_TEST_DB") ?: "timetracker_test")
    systemProperty("DB_USER", System.getenv("POSTGRES_TEST_USER") ?: "timetracker_test")
    systemProperty("DB_PASSWORD", System.getenv("POSTGRES_TEST_PASSWORD") ?: "timetracker_test_pass")
    systemProperty("SERVER_URL", System.getenv("SERVER_URL") ?: "http://localhost:8081")
    systemProperty("e2e.iterations", System.getProperty("e2e.iterations") ?: System.getenv("E2E_ITERATIONS") ?: "1")
}
