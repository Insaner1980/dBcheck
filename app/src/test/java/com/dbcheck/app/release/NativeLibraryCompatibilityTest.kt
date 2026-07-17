package com.dbcheck.app.release

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class NativeLibraryCompatibilityTest {
    @Test
    fun soundClassifierDoesNotUseLegacyFourKilobyteTaskAudioRuntime() {
        val versions = projectRootFile("gradle/libs.versions.toml").readText()
        val build = projectRootFile("app/build.gradle.kts").readText()

        assertFalse(versions.contains("tensorflowLiteTaskAudio"))
        assertFalse(versions.contains("tensorflow-lite-task-audio"))
        assertTrue(versions.contains("com.google.mediapipe"))
        assertTrue(versions.contains("tasks-audio"))
        assertTrue(build.contains("libs.mediapipe.tasks.audio"))
    }

    private fun projectRootFile(path: String): File = listOf(
        File(path),
        File("..", path),
    ).first(File::isFile)
}
