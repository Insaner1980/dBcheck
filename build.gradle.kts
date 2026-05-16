plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.compose.screenshot) apply false
    alias(libs.plugins.owasp.dependency.check) apply false
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.stability.analyzer) apply false
}

allprojects {
    dependencyLocking {
        lockAllConfigurations()
    }

    configurations.configureEach {
        resolutionStrategy.eachDependency {
            when {
                requested.group == "io.netty" && requested.name.startsWith("netty-") -> {
                    useVersion(rootProject.libs.versions.netty.get())
                    because("AGP:n sisaiset testityokalut ratkaisevat muuten haavoittuvia Netty-versioita.")
                }
                requested.group == "org.apache.commons" && requested.name == "commons-lang3" -> {
                    useVersion(rootProject.libs.versions.commonsLang3.get())
                    because("AGP:n sisaiset testityokalut ratkaisevat muuten haavoittuvan commons-lang3-version.")
                }
                requested.group == "org.apache.httpcomponents" && requested.name == "httpclient" -> {
                    useVersion(rootProject.libs.versions.apacheHttpClient.get())
                    because("AGP:n sisaiset testityokalut ratkaisevat muuten haavoittuvan HttpClient 4.x -version.")
                }
                requested.group == "org.bouncycastle" && requested.name.endsWith("-jdk18on") -> {
                    useVersion(rootProject.libs.versions.bouncycastle.get())
                    because("AGP:n sisaiset testityokalut ratkaisevat muuten haavoittuvan Bouncy Castle -version.")
                }
            }
        }
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
