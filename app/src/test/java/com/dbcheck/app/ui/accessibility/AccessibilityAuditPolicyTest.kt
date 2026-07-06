package com.dbcheck.app.ui.accessibility

import com.dbcheck.app.projectFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityAuditPolicyTest {
    @Test
    fun meterCustomControlsExposeButtonRoles() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/meter/components/MeterControls.kt").readText()

        assertTrue(source.contains("import androidx.compose.ui.semantics.Role"))
        assertTrue(Regex("role = Role\\.Button").findAll(source).count() >= 2)
    }

    @Test
    fun sessionCardClickTargetsRemainAccessible() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/components/SessionCard.kt").readText()

        assertTrue(source.contains("role = Role.Button"))
        assertTrue(source.contains("IconButton(onClick = editAction.onClick, modifier = Modifier.size(48.dp))"))
        assertFalse(source.contains("IconButton(onClick = editAction.onClick, modifier = Modifier.size(36.dp))"))
    }

    @Test
    fun ambientSoundSelectionChipsExposeSelectedState() {
        val source = projectFile("src/main/java/com/dbcheck/app/ui/ambient/AmbientSoundPlaybackScreen.kt").readText()

        assertTrue(source.contains("DbCheckChip("))
        assertTrue(source.contains("selected = selectedPreset == preset"))
        assertTrue(source.contains("selected = selectedTimer == minutes"))
        assertFalse(source.contains("enabled = selectedPreset != preset"))
        assertFalse(source.contains("enabled = selectedTimer != minutes"))
    }
}
