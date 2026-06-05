plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ppo.timetracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ppo.timetracker"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

afterEvaluate {
    val copyDebugApk = tasks.register<Copy>("copyDebugApk") {
        from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
        into(rootProject.layout.projectDirectory.dir("dist"))
        rename { "timetracker.apk" }
    }

    tasks.findByName("assembleDebug")?.finalizedBy(copyDebugApk)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":data_objectbox"))
    implementation(project(":migration"))
    
    implementation(libs.io.objectbox.android)
    implementation(libs.io.objectbox.kotlin)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.splashscreen)

    implementation(libs.androidx.room.runtime)
    implementation(libs.sqlite.bundled)

    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    implementation(libs.slf4j.api)
    implementation(libs.slf4j.simple)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
