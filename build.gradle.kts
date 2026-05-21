// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.android.library") version "9.2.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
}

tasks.register<Copy>("assembleDist") {
    dependsOn(":domain:jar", ":data:jar", ":tech_ui:jar")
    into(layout.projectDirectory.dir("dist"))
    into("components") {
        from(project(":domain").tasks.named("jar"))
        from(project(":data").tasks.named("jar"))
        from(project(":tech_ui").tasks.named("jar"))
    }
    into("libs") {
        val runtimeConfigs = listOf(
            project(":tech_ui").configurations.getByName("runtimeClasspath"),
            project(":data").configurations.getByName("runtimeClasspath")
        )
        from(runtimeConfigs)
    }
}
