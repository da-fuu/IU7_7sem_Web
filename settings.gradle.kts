pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TimeTracker"
include(":domain", ":app", ":data", ":data_objectbox", ":migration", ":tech_ui")
project(":app").projectDir = file("ppo/app")
project(":data").projectDir = file("ppo/data")
project(":data_objectbox").projectDir = file("ppo/data_objectbox")
project(":migration").projectDir = file("ppo/migration")
project(":tech_ui").projectDir = file("ppo/tech_ui")

include(":web:data")
project(":web:data").projectDir = file("web/data")

include(":web:backend")
project(":web:backend").projectDir = file("web/backend")
