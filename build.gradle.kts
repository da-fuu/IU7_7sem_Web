plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.ksp) apply false
    alias(libs.plugins.androidx.room) apply false
    alias(libs.plugins.objectbox) apply false
    alias(libs.plugins.android.legacy.kapt) apply false
    jacoco
}

allprojects {
    tasks.withType<Test>().configureEach {
        if (providers.environmentVariable("CI").orNull == "true") {
            outputs.upToDateWhen { false }
            outputs.doNotCacheIf("CI executes every test run") { true }
        }
    }
    configurations.all {
        resolutionStrategy {
            force("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
        }
    }
}

val allureCommandLine: Configuration by configurations.creating

dependencies {
    allureCommandLine("io.qameta.allure:allure-commandline:2.27.0@zip")
}

val installAllureCli by tasks.registering(Sync::class) {
    from(zipTree(allureCommandLine.elements.map { it.first().asFile }))
    into(layout.buildDirectory.dir("allure-cli"))
}

tasks.register<Exec>("allureReport") {
    dependsOn(installAllureCli)
    val cliDir = layout.buildDirectory.dir("allure-cli/allure-2.27.0/bin/allure").get().asFile
    val resultsDir = layout.buildDirectory.dir("allure-results").get().asFile
    val reportDir = layout.buildDirectory.dir("reports/allure-report").get().asFile
    commandLine(cliDir.absolutePath, "generate", resultsDir.absolutePath, "-o", reportDir.absolutePath, "--clean")
}

tasks.register<Exec>("allureServe") {
    dependsOn(installAllureCli)
    val cliDir = layout.buildDirectory.dir("allure-cli/allure-2.27.0/bin/allure").get().asFile
    val resultsDir = layout.buildDirectory.dir("allure-results").get().asFile
    commandLine(cliDir.absolutePath, "serve", resultsDir.absolutePath)
}

tasks.register("integrationTest") {
    group = "verification"
    description = "Executes integration tests"
    dependsOn(":web:data:integrationTest")
}

tasks.register("e2eTest") {
    group = "verification"
    description = "Executes end-to-end MVP scenario test"
    dependsOn(":web:backend:e2eTest")
}

tasks.register<Exec>("allureOpen") {
    dependsOn(installAllureCli)
    val cliDir = layout.buildDirectory.dir("allure-cli/allure-2.27.0/bin/allure").get().asFile
    val reportDir = layout.buildDirectory.dir("reports/allure-report").get().asFile
    commandLine(cliDir.absolutePath, "open", reportDir.absolutePath)
}

val jacocoRootReport by tasks.registering(JacocoReport::class) {
    description = "Generates an aggregate JaCoCo code coverage report for the whole project"
    group = "verification"

    val targetProjects = listOf(project(":domain"), project(":web:data"))
    dependsOn(targetProjects.map { it.tasks.named("test") })

    val execFiles = targetProjects.map {
        it.layout.buildDirectory.file("jacoco/test.exec").get().asFile
    }
    executionData.setFrom(files(execFiles.filter { it.exists() }))

    val sources = targetProjects.flatMap { p ->
        listOf(
            p.file("src/main/java"),
            p.file("src/main/kotlin")
        )
    }
    sourceDirectories.setFrom(files(sources.filter { it.exists() }))

    val classes = targetProjects.map { p ->
        p.fileTree(p.layout.buildDirectory.dir("classes/kotlin/main"))
    }
    classDirectories.setFrom(files(classes))

    reports {
        xml.required.set(true)
        csv.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/root/html"))
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/root/jacocoRootReport.xml"))
        csv.outputLocation.set(layout.buildDirectory.file("reports/jacoco/root/jacocoRootReport.csv"))
    }
}
