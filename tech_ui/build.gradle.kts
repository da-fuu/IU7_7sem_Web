import java.io.File

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    application
}

application {
    mainClass.set("com.ppo.tech_ui.TechUiAppKt")
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
    dependsOn(":domain:jar", ":data:jar")
    archiveFileName.set("tech_ui.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = "com.ppo.tech_ui.TechUiAppKt"
    }

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    val distDir = project.rootDir.resolve("dist")
    doLast {
        val outputFile = archiveFile.get().asFile
        outputFile.copyTo(File(distDir, outputFile.name), overwrite = true)
    }
}

val extractNativeLibs = tasks.register<Copy>("extractNativeLibs") {
    val distDir = project.rootDir.resolve("dist")
    val nativeLibsDir = distDir.resolve("lib/arm64-v8a")

    androidDex.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        if (artifact.file.name.contains("sqlite-bundled")) {
            from(zipTree(artifact.file)) {
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
    into(nativeLibsDir)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)
    implementation(libs.kotlinx.coroutines.core)
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

val dexRuntimeClasspath by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.runtimeClasspath.get())
    exclude(group = "androidx.sqlite")
    exclude(group = "org.jetbrains", module = "annotations")
}

dependencies {
    androidDex("androidx.sqlite:sqlite-framework-android:${libs.versions.sqlite.get()}")
    androidDex("androidx.sqlite:sqlite-bundled-android:${libs.versions.sqlite.get()}")
    androidDex("androidx.sqlite:sqlite-android:${libs.versions.sqlite.get()}")
}

val thinJar = tasks.register<Jar>("thinJar") {
    from(sourceSets.main.get().output)
    archiveClassifier.set("thin")
}

val collectDexInputs = tasks.register<Sync>("collectDexInputs") {
    dependsOn(thinJar, ":domain:jar", ":data:jar")

    into(project.layout.buildDirectory.dir("dex-inputs"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(thinJar.map { it.archiveFile })

    from(dexRuntimeClasspath) {
        include { it.name.endsWith(".jar") && !it.name.contains("sqlite") && !it.name.contains("annotations") }
    }

    androidDex.files.forEach { file ->
        if (file.extension == "aar") {
            from(zipTree(file)) {
                include("classes.jar")
                rename { "${file.nameWithoutExtension}-classes.jar" }
            }
        } else if (file.extension == "jar") {
            from(file)
        }
    }
}

tasks.register<Exec>("dex") {
    dependsOn(collectDexInputs, extractNativeLibs)
    group = "build"
    description = "Dex the JARs"

    val sdkDirStr = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT") ?: run {
        val localProperties = project.rootDir.resolve("local.properties")
        if (localProperties.exists()) {
            localProperties.readLines()
                .find { it.startsWith("sdk.dir=") }
                ?.substringAfter("sdk.dir=")
                ?.trim()
        } else {
            null
        }
    } ?: throw GradleException("Android SDK not found")

    val sdkDir = File(sdkDirStr)
    val distDir = project.rootDir.resolve("dist")
    val outputJar = distDir.resolve("timetracker-dex.jar")
    val dexInputsDirProvider = collectDexInputs.map { it.outputs.files.singleFile }

    inputs.dir(collectDexInputs.map { it.outputs.files.singleFile })
    outputs.file(outputJar)
    outputs.dir(distDir.resolve("lib"))

    val d8 = sdkDir.resolve("build-tools").listFiles()?.filter { it.isDirectory }?.maxByOrNull { it.name }?.let { it.resolve("d8") }
    val androidJar = sdkDir.resolve("platforms").listFiles()?.filter { it.isDirectory && it.name.startsWith("android-") }?.maxByOrNull { it.name.removePrefix("android-").toIntOrNull() ?: 0 }?.let { it.resolve("android.jar") }

    if (d8 == null || !d8.exists()) throw GradleException("d8 not found")
    if (androidJar == null || !androidJar.exists()) throw GradleException("android.jar not found")

    doFirst {
        val dexInputsDir = dexInputsDirProvider.get()
        val allInputs = dexInputsDir.listFiles()?.filter { file ->
            val name = file.name
            !name.contains("sqlite-jvm") &&
            !name.contains("sqlite-framework-2")
        }?.map { it.absolutePath } ?: emptyList()

        commandLine(
            d8.absolutePath,
            "--min-api", "29",
            "--lib", androidJar.absolutePath,
            "--release",
            "--output", outputJar.absolutePath,
            *allInputs.toTypedArray()
        )
    }
}
