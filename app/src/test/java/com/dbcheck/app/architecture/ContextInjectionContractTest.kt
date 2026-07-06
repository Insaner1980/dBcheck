package com.dbcheck.app.architecture

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextInjectionContractTest {
    @Test
    fun hapticFeedbackHelperUsesApplicationContextConstructorInjection() {
        val helperSource = projectFile("src/main/java/com/dbcheck/app/util/HapticFeedbackHelper.kt").readText()
        val appModuleSource = projectFile("src/main/java/com/dbcheck/app/di/AppModule.kt").readText()

        assertTrue(helperSource.contains("import dagger.hilt.android.qualifiers.ApplicationContext"))
        assertTrue(
            Regex(
                """constructor\(\s*@param:ApplicationContext private val context: Context,?\s*\)""",
            ).containsMatchIn(helperSource),
        )
        assertFalse(appModuleSource.contains("provideHapticFeedbackHelper"))
        assertFalse(appModuleSource.contains("HapticFeedbackHelper(context)"))
    }
}
