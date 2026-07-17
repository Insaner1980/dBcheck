import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.StringReader
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.compose.screenshot)
    alias(libs.plugins.stability.analyzer)
    alias(libs.plugins.owasp.dependency.check)
    id("jacoco")
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
val debugCredentialsFile = rootProject.layout.projectDirectory.file("debug.credentials.properties")
val debugCredentialsText = providers.fileContents(debugCredentialsFile).asText.orElse("")

fun releaseSigningInput(name: String): String =
    releaseSigningInputs.getValue(name)
        ?: throw org.gradle.api.GradleException("Missing release signing input: $name")

fun debugCredential(
    name: String,
    vararg envNames: String,
): String {
    val localValue =
        Properties()
            .also { properties ->
                StringReader(debugCredentialsText.orNull.orEmpty()).use { properties.load(it) }
            }.getProperty(name, "")
    return envNames
        .firstNotNullOfOrNull { envName ->
            providers.environmentVariable(envName).orNull?.takeIf { it.isNotBlank() }
        }
        ?: localValue
}

fun quotedBuildConfigValue(value: String): String =
    "\"${value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")}\""

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
    compileSdk = 37

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
        debug {
            buildConfigField(
                "String",
                "SENTRY_DSN",
                quotedBuildConfigValue(debugCredential("sentry.dsn", "DBCHECK_SENTRY_DSN", "SENTRY_DSN")),
            )
        }
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

tasks.withType<Test>().configureEach {
    maxHeapSize = "2g"
}

@CacheableRule
abstract class SecureTransitiveDependencyRule
    @Inject
    constructor(
        private val targetModule: String,
        private val targetVersion: String,
        private val reason: String,
    ) : ComponentMetadataRule {
        override fun execute(context: ComponentMetadataContext) {
            context.details.allVariants {
                withDependencies {
                    filter { dependency -> "${dependency.group}:${dependency.name}" == targetModule }
                        .forEach { dependency ->
                            dependency.version { require(targetVersion) }
                            dependency.because(reason)
                        }
                }
            }
        }
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

val securityPinnedTransitiveGroups =
    mapOf(
        "io.netty" to libs.versions.netty.get(),
    )

val securityPinnedTransitiveModules =
    mapOf(
        "com.google.protobuf:protobuf-javalite" to libs.versions.protobufJavalite.get(),
        "org.apache.commons:commons-lang3" to libs.versions.commonsLang3.get(),
        "org.apache.httpcomponents:httpclient" to libs.versions.httpClient4.get(),
        "org.bouncycastle:bcpkix-jdk18on" to libs.versions.bouncycastle.get(),
        "org.bouncycastle:bcprov-jdk18on" to libs.versions.bouncycastle.get(),
        "org.bouncycastle:bcutil-jdk18on" to libs.versions.bouncycastle.get(),
    )

val securityPinnedTransitiveReason =
    "Pidetaan build-tyokalujen haavoittuvat transitiivit security-checkin vaatimalla korjatulla versiolla."

configurations.configureEach {
    resolutionStrategy.eachDependency {
        val requestedModule = "${requested.group}:${requested.name}"
        val secureVersion =
            securityPinnedTransitiveModules[requestedModule]
                ?: securityPinnedTransitiveGroups[requested.group]

        if (secureVersion != null && requested.version != secureVersion) {
            useVersion(secureVersion)
            because(securityPinnedTransitiveReason)
        }
    }
}

dependencyCheck {
    formats = listOf("HTML", "JSON", "SARIF")
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
        delay =
            providers
                .environmentVariable("NVD_API_DELAY_MS")
                .orNull
                ?.toIntOrNull()
                ?: 6_000
        maxRetryCount =
            providers
                .environmentVariable("NVD_API_MAX_RETRY_COUNT")
                .orNull
                ?.toIntOrNull()
                ?: 20
        validForHours =
            providers
                .environmentVariable("NVD_VALID_FOR_HOURS")
                .orNull
                ?.toIntOrNull()
                ?: 24
    }
    analyzers {
        kev {
            enabled = false
        }
        ossIndex {
            enabled = false
        }
    }
}

jacoco {
    toolVersion = "0.8.14"
}

val jacocoGeneratedClassExclusions =
    listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "android/**/*.*",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/*_Provide*Factory.*",
        "**/*ComponentTreeDeps.*",
        "**/*Hilt*.*",
        "**/Hilt_*.*",
        "**/Dagger*.*",
        "**/*_Impl*.*",
    )

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Luo JaCoCo XML- ja HTML-coverageraportit debug-yksikkotesteista."
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/debugUnitTest/jacocoDebugUnitTestReport.xml"),
        )
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/debugUnitTest/html"))
    }

    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("intermediates/classes/debug/transformDebugClassesWithAsm/dirs")) {
                exclude(jacocoGeneratedClassExclusions)
            },
        ),
    )
    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec",
            )
        },
    )
}

// detekt-formatting bundles ktlint rules; expose a "ktlintCheck" alias so
// scripts that expect the standard task name still work.
tasks.register("ktlintCheck") {
    group = "verification"
    description = "Runs detekt (which includes ktlint formatting rules)."
    dependsOn("detekt")
}

// Windowsilla AGP 9.1:n lint-analyysit voivat lukita samoja Kotlin-lahdetiedostoja rinnakkaisajossa.
tasks.configureEach {
    if (name.startsWith("lintAnalyze") && name.endsWith("UnitTest")) {
        mustRunAfter(name.removeSuffix("UnitTest"))
    }
    if (name.startsWith("lintAnalyze") && name.endsWith("AndroidTest")) {
        mustRunAfter("${name.removeSuffix("AndroidTest")}UnitTest")
    }
}

dependencies {
    components {
        withModule<SecureTransitiveDependencyRule>("org.apache.commons:commons-compress") {
            params(
                "org.apache.commons:commons-lang3",
                securityPinnedTransitiveModules.getValue("org.apache.commons:commons-lang3"),
                securityPinnedTransitiveReason,
            )
        }
        withModule<SecureTransitiveDependencyRule>("org.apache.httpcomponents:httpmime") {
            params(
                "org.apache.httpcomponents:httpclient",
                securityPinnedTransitiveModules.getValue("org.apache.httpcomponents:httpclient"),
                securityPinnedTransitiveReason,
            )
        }
    }

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

    // Sentry on vain debug-diagnostiikkaa. Release-luokkapolku tarkistetaan tools\sentry.ps1-komennolla.
    debugImplementation(libs.sentry.android.core)

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

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.video)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Billing
    implementation(libs.billing.ktx)

    // Aaniluokittelu
    implementation(libs.mediapipe.tasks.audio)

    // Widgets
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)

    constraints {
        implementation(libs.androidx.work.runtime) {
            because("Glance 1.1.1 toisi muuten WorkManager 2.7.1:n haavoittuvan inspector/protobuf-jarin.")
        }
        implementation(libs.androidx.work.runtime.ktx) {
            because("WorkManagerin runtime- ja ktx-artefaktit pidetaan samassa korjatussa versiossa.")
        }
        implementation(libs.guava.android) {
            because("Health Connect ja coroutines-guava ratkaisevat muuten Guava 31.1-android -version.")
        }
    }

    // Detekt
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose.rules)

    // Androidin security lint
    lintChecks(libs.android.security.lints)

    // Screenshot testing
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
}
