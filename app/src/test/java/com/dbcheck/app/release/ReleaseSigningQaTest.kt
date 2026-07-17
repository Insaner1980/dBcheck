package com.dbcheck.app.release

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReleaseSigningQaTest {
    @Test
    fun releaseSigningQaDocumentsArtifactChecksAndKnownManualGaps() {
        val qaDoc = releaseSigningQaFile()

        assertTrue("Osa 97 requires docs/qa/release-signing-qa.md", qaDoc.isFile)

        val content = qaDoc.readText()
        expectedQaMarkers.forEach { marker ->
            assertTrue("Release signing QA must document $marker", content.contains(marker))
        }
    }

    @Test
    fun gradleAndCiReleaseSigningContractsStayGuarded() {
        val build = projectRootFile("app/build.gradle.kts").readText()
        listOf(
            "DBCHECK_RELEASE_STORE_FILE",
            "DBCHECK_RELEASE_STORE_PASSWORD",
            "DBCHECK_RELEASE_KEY_ALIAS",
            "DBCHECK_RELEASE_KEY_PASSWORD",
            "configuredReleaseSigningInputs.isNotEmpty() && !hasReleaseSigning",
            "Release signing configuration is incomplete",
            "Release signing keystore was not found",
            "signingConfigs",
            "create(\"release\")",
            "signingConfig = signingConfigs.getByName(\"release\")",
            "isMinifyEnabled = true",
            "isShrinkResources = true",
        ).forEach { marker ->
            assertTrue("Gradle release signing contract must keep $marker", build.contains(marker))
        }
        assertFalse("Keystore files must not be referenced by a hardcoded project path", build.contains(".jks\""))

        val workflow = projectRootFile(".github/workflows/release-build.yml").readText()
        listOf(
            "DBCHECK_RELEASE_KEYSTORE_BASE64",
            "Release signing secrets are required for non-PR release builds.",
            ":app:assembleRelease :app:bundleRelease",
            "apksigner",
            "verify --print-certs",
            "jarsigner -verify",
            "app/build/outputs/apk/release/app-release.apk",
            "app/build/outputs/bundle/release/app-release.aab",
        ).forEach { marker ->
            assertTrue("Release workflow must keep $marker", workflow.contains(marker))
        }
        val decodeStep =
            workflow
                .substringAfter("- name: Decode release keystore")
                .substringBefore("- name: Build unsigned release APK and bundle for PR")
        assertTrue("Incomplete non-PR signing secrets must fail the workflow", decodeStep.contains("exit 1"))

        val verificationStep = workflow.substringAfter("- name: Verify signed release artifacts")
        assertTrue(
            "Signed artifact verification must run only for non-PR builds",
            verificationStep.contains("if: github.event_name != 'pull_request'"),
        )
        assertFalse(
            "Signed artifact verification must not accept an unsigned fallback",
            verificationStep.contains("unsigned release build was validated"),
        )
    }

    private fun releaseSigningQaFile(): File = listOf(
        File("docs/qa/release-signing-qa.md"),
        File("..", "docs/qa/release-signing-qa.md"),
    ).firstOrNull(File::isFile) ?: File("docs/qa/release-signing-qa.md")

    private fun projectRootFile(path: String): File = listOf(
        File(path),
        File("..", path),
    ).first(File::isFile)

    private companion object {
        val expectedQaMarkers = listOf(
            "# dBcheck Release signing QA",
            "DBCHECK_RELEASE_STORE_FILE",
            "DBCHECK_RELEASE_STORE_PASSWORD",
            "DBCHECK_RELEASE_KEY_ALIAS",
            "DBCHECK_RELEASE_KEY_PASSWORD",
            "DBCHECK_RELEASE_KEYSTORE_BASE64",
            "Signing secrets: NOT CONFIGURED",
            "Signed AAB build: NOT RUN",
            "Release AAB install: NOT RUN",
            "16 KB compatibility: PASS",
            "Unsigned release APK/AAB build: RUN",
            "apksigner verify --print-certs",
            "jarsigner -verify",
            ":app:assembleRelease :app:bundleRelease",
            "app-release.apk",
            "app-release.aab",
            "Play upload: NOT RUN",
            "Release artifact",
            "Signing lineage",
            "Release risk",
            "Osa 98 - Qodana/CI compatibility",
            "https://developer.android.com/studio/publish/app-signing",
            "https://developer.android.com/tools/apksigner",
            "https://developer.android.com/studio/publish/upload-bundle",
            "https://support.google.com/googleplay/android-developer/answer/9842756",
        )
    }
}
