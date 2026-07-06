package com.dbcheck.app.ui.tinnitus

import com.dbcheck.app.projectSourcesLowercase
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TinnitusPitchMatcherScopeTest {
    @Test
    fun pitchMatcherDoesNotDeclareSoundTherapyOrBackgroundPlaybackScope() {
        val sources =
            projectSourcesLowercase(
                "src/main/java/com/dbcheck/app/ui/tinnitus/TinnitusPitchMatcherScreen.kt",
                "src/main/java/com/dbcheck/app/ui/tinnitus/TinnitusPitchMatcherViewModel.kt",
                "src/main/java/com/dbcheck/app/domain/tinnitus/TinnitusPitchProfile.kt",
                "src/main/res/values/strings.xml",
            )

        assertTrue(sources.contains("personal tracking"))
        assertFalse(sources.contains("sound therapy"))
        assertFalse(sources.contains("background playback"))
        assertFalse(sources.contains("startforegroundservice"))
        assertFalse(sources.contains("healthconnect"))
    }
}
