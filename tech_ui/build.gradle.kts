plugins {
    id("org.jetbrains.kotlin.jvm")
    application
}

application {
    mainClass.set("com.ppo.tech_ui.TechUiAppKt")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation(libs.androidx.room.runtime)
    implementation(libs.sqlite.bundled)
}
