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

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

val requiredDependencyControlFiles =
    listOf(
        settingsDir.resolve("gradle/verification-metadata.xml"),
        settingsDir.resolve("buildscript-gradle.lockfile"),
    )
requiredDependencyControlFiles.forEach { dependencyControlFile ->
    check(dependencyControlFile.isFile && dependencyControlFile.length() > 0L) {
        "Gradle dependency control file puuttuu tai on tyhja: ${dependencyControlFile.absolutePath}"
    }
}

rootProject.name = "dBcheck"
include(":app")
