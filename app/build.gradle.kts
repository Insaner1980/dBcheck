plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.compose.screenshot)
    alias(libs.plugins.owasp.dependency.check)
}

val releaseSigningInputs =
    mapOf(
        "DBCHECK_RELEASE_STORE_FILE" to
            providers.gradleProperty("DBCHECK_RELEASE_STORE_FILE")
                .orElse(providers.environmentVariable("DBCHECK_RELEASE_STORE_FILE"))
                .orNull,
        "DBCHECK_RELEASE_STORE_PASSWORD" to
            providers.gradleProperty("DBCHECK_RELEASE_STORE_PASSWORD")
                .orElse(providers.environmentVariable("DBCHECK_RELEASE_STORE_PASSWORD"))
                .orNull,
        "DBCHECK_RELEASE_KEY_ALIAS" to
            providers.gradleProperty("DBCHECK_RELEASE_KEY_ALIAS")
                .orElse(providers.environmentVariable("DBCHECK_RELEASE_KEY_ALIAS"))
                .orNull,
        "DBCHECK_RELEASE_KEY_PASSWORD" to
            providers.gradleProperty("DBCHECK_RELEASE_KEY_PASSWORD")
                .orElse(providers.environmentVariable("DBCHECK_RELEASE_KEY_PASSWORD"))
                .orNull,
    )
val configuredReleaseSigningInputs = releaseSigningInputs.filterValues { !it.isNullOrBlank() }
val hasReleaseSigning = configuredReleaseSigningInputs.size == releaseSigningInputs.size
fun releaseSigningInput(name: String): String =
    releaseSigningInputs.getValue(name)
        ?: throw org.gradle.api.GradleException("Missing release signing input: $name")

if (configuredReleaseSigningInputs.isNotEmpty() && !hasReleaseSigning) {
    throw org.gradle.api.GradleException(
        "Release signing configuration is incomplete. Provide all of: " +
            releaseSigningInputs.keys.joinToString(),
    )
}

if (hasReleaseSigning && !file(releaseSigningInput("DBCHECK_RELEASE_STORE_FILE")).isFile) {
    throw org.gradle.api.GradleException(
        "Release signing keystore was not found: " +
            releaseSigningInput("DBCHECK_RELEASE_STORE_FILE"),
    )
}

android {
    namespace = "com.dbcheck.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dbcheck.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseSigningInput("DBCHECK_RELEASE_STORE_FILE"))
                storePassword = releaseSigningInput("DBCHECK_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigningInput("DBCHECK_RELEASE_KEY_ALIAS")
                keyPassword = releaseSigningInput("DBCHECK_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

hilt {
    enableAggregatingTask = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    baseline = file("detekt-baseline.xml")
    parallel = true
}

dependencyCheck {
    formats = listOf("HTML", "JSON")
    outputDirectory = rootProject.layout.projectDirectory.dir("reports")
    data {
        directory =
            providers
                .environmentVariable("DEPENDENCY_CHECK_DATA_DIRECTORY")
                .orElse(rootProject.layout.projectDirectory.dir(".gradle/dependency-check-data").asFile.absolutePath)
                .get()
    }
    autoUpdate =
        (providers.environmentVariable("DEPENDENCY_CHECK_AUTO_UPDATE").orNull ?: "true")
            .toBoolean()
    failBuildOnCVSS =
        providers
            .environmentVariable("DEPENDENCY_CHECK_FAIL_BUILD_ON_CVSS")
            .orNull
            ?.toFloatOrNull()
            ?: 7f
    suppressionFiles =
        listOf(
            rootProject.file("config/dependency-check-suppressions.xml").absolutePath,
        )
    scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
    skipTestGroups = true
    hostedSuppressions {
        enabled = false
    }
    nvd {
        providers.environmentVariable("NVD_API_KEY").orNull?.let { apiKey = it }
    }
    analyzers {
        kev {
            enabled = false
        }
    }
}

// detekt-formatting bundles ktlint rules; expose a "ktlintCheck" alias so
// scripts that expect the standard task name still work.
tasks.register("ktlintCheck") {
    group = "verification"
    description = "Runs detekt (which includes ktlint formatting rules)."
    dependsOn("detekt")
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.foundation)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Health Connect
    implementation(libs.androidx.health.connect.client)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Billing
    implementation(libs.billing.ktx)

    // Widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    // Detekt
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose.rules)

    // Screenshot testing
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
