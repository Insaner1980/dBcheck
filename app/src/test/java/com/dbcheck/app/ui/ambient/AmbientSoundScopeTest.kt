package com.dbcheck.app.ui.ambient

import com.dbcheck.app.projectSourcesLowercase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientSoundScopeTest {
    @Test
    fun ambientSoundScopeAvoidsMedicalClaimsAndSensitiveDataPaths() {
        val sources =
            projectSourcesLowercase(
                "src/main/java/com/dbcheck/app/domain/ambient/AmbientSoundPolicy.kt",
                "src/main/java/com/dbcheck/app/domain/ambient/AmbientSoundGenerator.kt",
                "src/main/java/com/dbcheck/app/service/AmbientSoundPlaybackService.kt",
                "src/main/java/com/dbcheck/app/service/AmbientSoundPlaybackController.kt",
                "src/main/java/com/dbcheck/app/ui/ambient/AmbientSoundPlaybackViewModel.kt",
                "src/main/java/com/dbcheck/app/ui/ambient/AmbientSoundPlaybackScreen.kt",
                "src/main/res/values/strings.xml",
            )

        assertTrue(sources.contains("ambient sound"))
        assertTrue(sources.contains("local playback"))
        listOf(
            "therapy",
            "treatment",
            "relief",
            "reduce tinnitus",
            "cure",
            "hearing protection",
            "healthconnect",
            "record_audio",
            "measurementforegroundservice",
            "auto-trigger",
            "journal",
        ).forEach { forbidden ->
            assertFalse("ambient scope must not contain $forbidden", sources.contains(forbidden))
        }
    }
}
