buildscript {
    val securityPinnedBuildscriptModules =
        mapOf(
            "com.fasterxml.jackson.core:jackson-databind" to "2.22.0",
            "org.apache.httpcomponents.client5:httpclient5" to "5.6.1",
            "org.bitbucket.b_c:jose4j" to "0.9.6",
            "org.jdom:jdom2" to "2.0.6.1",
        )

    configurations.classpath {
        resolutionStrategy.eachDependency {
            val requestedModule = "${requested.group}:${requested.name}"
            val secureVersion = securityPinnedBuildscriptModules[requestedModule]
            if (secureVersion != null && requested.version != secureVersion) {
                useVersion(secureVersion)
                because("Pidetaan Dependency-Checkin Gradle-plugin-classpath security-checkin korjatuissa versioissa.")
            }
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.compose.screenshot) apply false
    alias(libs.plugins.stability.analyzer) apply false
    alias(libs.plugins.owasp.dependency.check) apply false
    alias(libs.plugins.sonarqube)
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}

val sonarProjectProperties =
    java.util.Properties().apply {
        val file = rootProject.file("sonar-project.properties")
        if (file.isFile) {
            file.inputStream().use(::load)
        }
    }

val gradleManagedSonarProperties =
    setOf(
        "sonar.sources",
        "sonar.tests",
        "sonar.java.binaries",
        "sonar.java.test.binaries",
        "sonar.java.libraries",
        "sonar.java.test.libraries",
        "sonar.kotlin.binaries",
        "sonar.coverage.jacoco.xmlReportPaths",
    )

sonar {
    properties {
        property("sonar.host.url", sonarProjectProperties.getProperty("sonar.host.url", "https://sonarcloud.io"))
        sonarProjectProperties.forEach { key, value ->
            val propertyName = key.toString()
            if (propertyName !in gradleManagedSonarProperties) {
                property(propertyName, value.toString())
            }
        }
    }
}

project(":app") {
    sonar {
        properties {
            property(
                "sonar.coverage.jacoco.xmlReportPaths",
                layout.buildDirectory
                    .file("reports/jacoco/debugUnitTest/jacocoDebugUnitTestReport.xml")
                    .get()
                    .asFile
                    .absolutePath,
            )
        }
    }
}
