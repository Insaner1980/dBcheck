package com.dbcheck.app.build

import com.dbcheck.app.projectFile
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleBuildscriptSecurityPinsTest {
    @Test
    fun buildscriptClasspathPinsSecuritySensitivePluginTransitivesAtRoot() {
        val rootBuildGradle = projectFile("../build.gradle.kts").readText()

        expectedBuildscriptPins.forEach { pin ->
            assertTrue(
                "Root buildscript classpath must pin ${pin.module} to ${pin.version}.",
                rootBuildGradle.contains("\"${pin.module}\" to \"${pin.version}\""),
            )
        }
        assertTrue(rootBuildGradle.contains("configurations.classpath"))
        assertTrue(rootBuildGradle.contains("useVersion(secureVersion)"))
    }

    private data class BuildscriptPin(val module: String, val version: String)

    private companion object {
        val expectedBuildscriptPins = listOf(
            BuildscriptPin("com.fasterxml.jackson.core:jackson-databind", "2.22.0"),
            BuildscriptPin("org.apache.httpcomponents.client5:httpclient5", "5.6.1"),
            BuildscriptPin("org.bitbucket.b_c:jose4j", "0.9.6"),
            BuildscriptPin("org.jdom:jdom2", "2.0.6.1"),
        )
    }
}
