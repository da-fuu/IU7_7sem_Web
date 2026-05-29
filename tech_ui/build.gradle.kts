plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

application {
    mainClass.set("com.ppo.tech_ui.TechUiAppKt")
}

tasks.jar {
    dependsOn(":domain:jar", ":data:jar")
    archiveFileName.set("tech_ui.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "com.ppo.tech_ui.TechUiAppKt"
    }

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

val copyJar = tasks.register<Copy>("copyJar") {
    from(tasks.jar.get().archiveFile)
    into("${project.rootDir}/dist")
}

tasks.assembleDist {
    dependsOn(copyJar)
}
tasks.register<Jar>("fatJar") {
    dependsOn("jar")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation(libs.androidx.room.runtime)
    implementation(libs.sqlite.bundled)
    implementation(libs.koin.core)
}

val androidDex: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class, Category.LIBRARY))
    }
}

dependencies {
    androidDex("androidx.sqlite:sqlite-framework-android:${libs.versions.sqlite.get()}")
    androidDex("androidx.sqlite:sqlite-bundled-android:${libs.versions.sqlite.get()}")
    androidDex("androidx.sqlite:sqlite-android:${libs.versions.sqlite.get()}")
    androidDex("androidx.sqlite:sqlite-framework:${libs.versions.sqlite.get()}")
}

tasks.register<Jar>("dexJar") {
    group = "build"
    description = "Assembles a JAR for dexing"
    archiveClassifier.set("dex-input")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    androidDex.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        if (artifact.extension == "aar") {
            from(zipTree(artifact.file).matching { include("classes.jar") }.map { zipTree(it) })
        } else {
            from(zipTree(artifact.file))
        }
    }

    from(sourceSets.main.get().output)

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
tasks.register<Exec>("dex") {
    dependsOn("dexJar")
    group = "build"
    description = "Dex the JAR and extract native libs"

    val sdkDir = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT") ?: run {
        val localProperties = file("${project.rootDir}/local.properties")
        if (localProperties.exists()) {
            localProperties.readLines()
                .find { it.startsWith("sdk.dir=") }
                ?.substringAfter("sdk.dir=")
                ?.trim()
        } else {
            null
        }
    }

    val distDir = file("${project.rootDir}/dist")
    val outputJar = file("${distDir}/timetracker-dex.jar")
    val inputJar = tasks.named<Jar>("dexJar").get().archiveFile.get().asFile

    doFirst {
        if (sdkDir == null) throw GradleException("Android SDK not found")
        distDir.mkdirs()

        val nativeLibsDir = file("${distDir}/lib/arm64-v8a")
        nativeLibsDir.mkdirs()

        androidDex.files.forEach { file ->
            if (file.name.contains("sqlite-bundled")) {
                copy {
                    from(zipTree(file))
                    into(nativeLibsDir)
                    include("**/*.so")
                    eachFile {
                        if (path.contains("arm64-v8a")) {
                            path = name
                        } else {
                            exclude()
                        }
                    }
                    includeEmptyDirs = false
                }
            }
        }

        val d8 = file("$sdkDir/build-tools/").listFiles()?.filter { it.isDirectory }?.maxByOrNull { it.name }?.let { file("${it.absolutePath}/d8") }
        val androidJar = file("$sdkDir/platforms/").listFiles()?.filter { it.isDirectory && it.name.startsWith("android-") }?.maxByOrNull { it.name.removePrefix("android-").toIntOrNull() ?: 0 }?.let { file("${it.absolutePath}/android.jar") }

        if (d8 == null || !d8.exists()) throw GradleException("d8 not found")
        if (androidJar == null || !androidJar.exists()) throw GradleException("android.jar not found")

        commandLine(
            d8.absolutePath,
            "--min-api", "26",
            "--lib", androidJar.absolutePath,
            "--output", outputJar.absolutePath,
            inputJar.absolutePath
        )
    }

    inputs.file(inputJar)
    outputs.file(outputJar)
    outputs.dir("${distDir}/lib")
}
