package com.dbcheck.app.release

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class QodanaCiCompatibilityTest {
    @Test
    fun qodanaCompatibilityQaDocumentsAgpRiskAndDecision() {
        val qaDoc = qodanaQaFile()

        assertTrue("Osa 98 requires docs/qa/qodana-ci-compatibility.md", qaDoc.isFile)

        val content = qaDoc.readText()
        expectedQaMarkers.forEach { marker ->
            assertTrue("Qodana CI QA must document $marker", content.contains(marker))
        }
    }

    @Test
    fun qodanaWorkflowMakesNonBlockingAgpRiskVisible() {
        val workflow = projectRootFile(".github/workflows/qodana.yml").readText()
        listOf(
            "name: Qodana Analysis (non-blocking AGP 9.2 risk)",
            "JetBrains/qodana-action@4861e015da555e86a72b862892aba6c2b93e6891",
            "continue-on-error: true",
            "QODANA_TOKEN: \${{ secrets.QODANA_TOKEN }}",
            "Record Qodana compatibility risk",
            "GITHUB_STEP_SUMMARY",
            "ei-blokkaava AGP 9.2.1 -yhteensopivuusriski",
            "docs/qa/qodana-ci-compatibility.md",
        ).forEach { marker ->
            assertTrue("Qodana workflow must keep visible risk marker $marker", workflow.contains(marker))
        }
    }

    @Test
    fun qodanaYamlAndProjectAgpVersionStayAlignedWithQaScope() {
        val qodanaYaml = projectRootFile("qodana.yaml").readText()
        assertTrue(qodanaYaml.contains("linter: jetbrains/qodana-jvm-android:2026.1"))
        assertTrue(qodanaYaml.contains("name: qodana.recommended"))
        assertTrue(qodanaYaml.contains("CheckDependencyLicenses"))

        val versions = projectRootFile("gradle/libs.versions.toml").readText()
        val agpVersion =
            Regex("""agp = "([^"]+)"""")
                .find(versions)
                ?.groupValues
                ?.get(1)
                ?: error("AGP version must be declared in gradle/libs.versions.toml")
        assertEquals("9.2.1", agpVersion)
    }

    private fun qodanaQaFile(): File = listOf(
        File("docs/qa/qodana-ci-compatibility.md"),
        File("..", "docs/qa/qodana-ci-compatibility.md"),
    ).firstOrNull(File::isFile) ?: File("docs/qa/qodana-ci-compatibility.md")

    private fun projectRootFile(path: String): File = listOf(
        File(path),
        File("..", path),
    ).first(File::isFile)

    private companion object {
        val expectedQaMarkers = listOf(
            "# dBcheck Qodana/CI compatibility QA",
            "AGP 9.2.1",
            "jetbrains/qodana-jvm-android:2026.1",
            "JetBrains/qodana-action",
            "continue-on-error: true retained",
            "Docker: NOT AVAILABLE",
            "Qodana CLI: NOT AVAILABLE",
            "Local Qodana run: NOT RUN",
            "CI Qodana run: PASS",
            "Do not remove continue-on-error",
            "Qodana Analysis (non-blocking AGP 9.2 risk)",
            "GITHUB_STEP_SUMMARY",
            "CI-status",
            "Release risk",
            "Osa 99 - Final reports pass",
            "https://www.jetbrains.com/help/qodana/github.html",
            "https://www.jetbrains.com/help/qodana/jvm.html",
            "https://www.jetbrains.com/help/qodana/qodana-yaml.html",
            "https://www.jetbrains.com/help/qodana/deploy-qodana.html",
        )
    }
}
