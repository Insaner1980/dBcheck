# Professional Monochrome Theme Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Toteuta dBcheckiin hyväksytty ammattimainen monokrominen dark theme ja C-linjan mukainen light theme ilman layout- tai feature-muutoksia.

**Architecture:** Pidä `app/src/main/java/com/dbcheck/app/ui/theme` ilmeen lähteenä ja muuta värit keskitetysti. Launcher-ikoni ja käyttäjälle näkyvät share/export-bittikarttojen vanhat lime-värit päivitetään samaan linjaan, ja resurssitesti varmistaa ettei vanhoja neon-brändivärejä jää theme- tai launcher-pintaan.

**Tech Stack:** Android, Kotlin, Jetpack Compose Material 3 `ColorScheme`, Android adaptive icon vector resources, JUnit resource/source scan tests, Gradle.

---

## File Structure

- Modify `app/src/main/java/com/dbcheck/app/ui/theme/Color.kt`: korvaa neon-lime/keltavihreä brändipaletti musta-valko-harmaalla ja hillityillä statusväreillä.
- Modify `app/src/main/java/com/dbcheck/app/ui/theme/Theme.kt`: varmista `ColorScheme`-roolit ja `signatureGradient` käyttävät neutraaleja arvoja.
- Modify `app/src/main/res/drawable/ic_launcher_foreground.xml`: vaihda waveform-palkit valkoiseksi ja neutraaleiksi harmaiksi.
- Modify `app/src/main/res/values/colors.xml`: vaihda notification-dot-värit hillityiksi ei-neon statusväreiksi.
- Modify `app/src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt`: vaihda käyttäjälle näkyvät vanhat lime/green export-korttivärit neutraaleiksi.
- Modify `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt`: vaihda camera share -burn-in statusväri neutraaliksi.
- Create `app/src/test/java/com/dbcheck/app/ui/theme/ProfessionalMonochromeThemeResourceTest.kt`: lähde-/resurssitesti vanhojen neon-värien poissaololle.
- Modify `app/src/screenshotTest/kotlin/com/dbcheck/app/ComponentScreenshotTests.kt`: varmista dark button preview käyttää theme-taustaa, jotta monokromisen dark-tilan kontrasti näkyy oikein referenssissä.
- Update screenshot references under `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/` only through screenshot update command after code changes.

## Task 1: Add Monochrome Guard Test

**Files:**
- Create: `app/src/test/java/com/dbcheck/app/ui/theme/ProfessionalMonochromeThemeResourceTest.kt`

- [x] **Step 1: Write failing resource scan test**

```kotlin
package com.dbcheck.app.ui.theme

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfessionalMonochromeThemeResourceTest {
    @Test
    fun themeAndLauncherResourcesDoNotUseOldNeonBrandColors() {
        val projectRoot = findProjectRoot()
        val checkedFiles =
            listOf(
                "app/src/main/java/com/dbcheck/app/ui/theme/Color.kt",
                "app/src/main/java/com/dbcheck/app/ui/theme/Theme.kt",
                "app/src/main/res/drawable/ic_launcher_foreground.xml",
                "app/src/main/res/values/colors.xml",
                "app/src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt",
                "app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt",
            )

        val oldBrandColors =
            listOf(
                "C5FE00",
                "B9EF00",
                "DFEC60",
                "4ADE80",
                "FBBF24",
                "F87171",
                "466906",
                "5A8A0A",
                "D4F5A0",
                "954B00",
                "2D7A2D",
                "CC7700",
            )

        val matches =
            checkedFiles.flatMap { relativePath ->
                val text = projectRoot.resolve(relativePath).readText()
                oldBrandColors
                    .filter { color -> text.contains(color, ignoreCase = true) }
                    .map { color -> "$relativePath contains $color" }
            }

        assertTrue(matches.joinToString(separator = "\n"), matches.isEmpty())
    }

    private fun findProjectRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (current.parent != null) {
            if (Files.exists(current.resolve("settings.gradle.kts"))) {
                return current
            }
            current = current.parent
        }
        error("settings.gradle.kts not found from ${Path.of("").toAbsolutePath()}")
    }
}
```

- [x] **Step 2: Run test to verify it fails**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dbcheck.app.ui.theme.ProfessionalMonochromeThemeResourceTest"`

Expected: FAIL, with matches for `C5FE00`, `DFEC60`, and old green light-theme values in current theme/resources.

## Task 2: Implement Central Monochrome Theme Tokens

**Files:**
- Modify: `app/src/main/java/com/dbcheck/app/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/dbcheck/app/ui/theme/Theme.kt`

- [x] **Step 1: Replace palette in `Color.kt`**

Use these concrete values:

```kotlin
val DarkBackground = Color(0xFF080808)
val DarkSurface = Color(0xFF101010)
val DarkSurfaceContainer = Color(0xFF171717)
val DarkSurfaceContainerHigh = Color(0xFF202020)
val DarkSurfaceContainerHighest = Color(0xFF2A2A2A)
val DarkSurfaceContainerLowest = Color(0xFF000000)
val DarkOnSurface = Color(0xFFF5F5F5)
val DarkOnSurfaceVariant = Color(0xFFB8B8B8)
val DarkPrimary = Color(0xFFF7F7F7)
val DarkPrimaryDim = Color(0xFFCFCFCF)
val DarkPrimaryContainer = Color(0xFFEDEDED)
val DarkOnPrimaryContainer = Color(0xFF080808)
val DarkSecondary = Color(0xFF8F8F8F)
val DarkTertiary = Color(0xFF5E5E5E)
val DarkTertiaryFixedDim = Color(0xFF242424)
val DarkOutlineVariant = Color(0xFF8C8C8C)
val DarkError = Color(0xFFE07A7A)
val DarkWarning = Color(0xFFC9A24D)
val DarkSuccess = Color(0xFF8EA58E)

val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFF0F0EF)
val LightSurfaceContainerHigh = Color(0xFFE6E6E3)
val LightSurfaceContainerHighest = Color(0xFFDADAD7)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF111111)
val LightOnSurfaceVariant = Color(0xFF5F5F5F)
val LightPrimary = Color(0xFF111111)
val LightPrimaryDim = Color(0xFF4A4A4A)
val LightPrimaryContainer = Color(0xFFE4E4E1)
val LightOnPrimaryContainer = Color(0xFF111111)
val LightSecondary = Color(0xFF6E6E6E)
val LightTertiary = Color(0xFF8A8A86)
val LightTertiaryFixedDim = Color(0xFFF2F2F0)
val LightOutlineVariant = Color(0xFF8E8E8A)
val LightError = Color(0xFFB45F5F)
val LightWarning = Color(0xFF9A7A33)
val LightSuccess = Color(0xFF607460)
```

- [x] **Step 2: Keep `Theme.kt` role mapping, update gradient inputs only if needed**

`Theme.kt` can keep the current `darkColorScheme`, `lightColorScheme`, and `signatureGradient = Brush.linearGradient(colors = listOf(DarkPrimary, DarkSecondary))` mapping because the new `DarkPrimary` and `DarkSecondary` are neutral.

- [x] **Step 3: Run theme guard test**

Run: `.\gradlew :app:testDebugUnitTest --tests "com.dbcheck.app.ui.theme.ProfessionalMonochromeThemeResourceTest"`

Expected: still FAIL until icon/share/export old colors are replaced.

## Task 3: Update Launcher And XML Resource Colors

**Files:**
- Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/values/colors.xml`

- [x] **Step 1: Replace launcher waveform colors**

Use:

```xml
android:fillColor="#F7F7F7"
```

for the first three waveform bars and:

```xml
android:fillColor="#B8B8B8"
```

for the last three waveform bars.

- [x] **Step 2: Replace notification status colors**

Use:

```xml
<color name="notification_dot_green">#8EA58E</color>
<color name="notification_dot_yellow">#C9A24D</color>
<color name="notification_dot_red">#E07A7A</color>
```

Keep background/surface/text colors unless the current values conflict with the new theme.

- [x] **Step 3: Run resource compile check**

Run: `.\gradlew :app:compileDebugKotlin`

Expected: PASS.

## Task 4: Update User-Visible Share And Export Colors

**Files:**
- Modify: `app/src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt`
- Modify: `app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt`

- [x] **Step 1: Replace old lime in meter/hearing/session share renderers**

Change old values:

```kotlin
0xFFC5FE00
0xFF466906
```

to neutral values:

```kotlin
0xFFF7F7F7
0xFF111111
```

Use dark neutral white for dark cards and near-black for light report cards.

- [x] **Step 2: Replace camera overlay status paint**

Change:

```kotlin
val statusPaint = sansSerifPaint(color = 0xFFC5FE00.toInt(), textSize = 18f * scale, bold = true)
```

to:

```kotlin
val statusPaint = sansSerifPaint(color = 0xFFF7F7F7.toInt(), textSize = 18f * scale, bold = true)
```

- [x] **Step 3: Run affected unit tests**

Run:

```powershell
.\gradlew :app:testDebugUnitTest --tests "com.dbcheck.app.util.ShareResultsGeneratorTest" --tests "com.dbcheck.app.ui.camera.CameraOverlayShareGeneratorTest" --tests "com.dbcheck.app.ui.theme.ProfessionalMonochromeThemeResourceTest"
```

Expected: PASS.

## Task 5: Screenshot Refresh And Final Verification

**Files:**
- Update generated screenshots under `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/`

- [x] **Step 1: Compile screenshot tests**

Run: `.\gradlew :app:compileDebugScreenshotTestKotlin`

Expected: PASS.

- [x] **Step 2: Regenerate screenshot references**

Run: `.\gradlew :app:updateDebugScreenshotTest`

Expected: PASS and updated PNG references for changed theme colors.

- [x] **Step 3: Validate screenshot references**

Run: `.\gradlew :app:validateDebugScreenshotTest`

Expected: PASS.

- [x] **Step 4: Run release Kotlin compile**

Run: `.\gradlew :app:compileReleaseKotlin`

Expected: PASS.

- [x] **Step 5: Run final color audit**

Run:

```powershell
rg -n "C5FE00|B9EF00|DFEC60|4ADE80|FBBF24|F87171|466906|5A8A0A|D4F5A0|954B00|2D7A2D|CC7700" app/src/main/java app/src/main/res
```

Expected: no matches except none.

- [x] **Step 6: Run diff whitespace check**

Run: `git diff --check`

Expected: no output.

## Task 6: Commit Implementation

**Files:**
- Stage only files changed for this implementation.

- [ ] **Step 1: Inspect scoped diff**

Run:

```powershell
git diff -- app/src/main/java/com/dbcheck/app/ui/theme/Color.kt app/src/main/java/com/dbcheck/app/ui/theme/Theme.kt app/src/main/res/drawable/ic_launcher_foreground.xml app/src/main/res/values/colors.xml app/src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt app/src/test/java/com/dbcheck/app/ui/theme/ProfessionalMonochromeThemeResourceTest.kt docs/superpowers/plans/2026-06-26-professional-monochrome-theme.md
```

Expected: only approved theme/resource/share/export/test/plan changes.

- [ ] **Step 2: Stage scoped files**

Run:

```powershell
git add -- app/src/main/java/com/dbcheck/app/ui/theme/Color.kt app/src/main/java/com/dbcheck/app/ui/theme/Theme.kt app/src/main/res/drawable/ic_launcher_foreground.xml app/src/main/res/values/colors.xml app/src/main/java/com/dbcheck/app/util/ShareResultsGenerator.kt app/src/main/java/com/dbcheck/app/ui/camera/CameraOverlayShareGenerator.kt app/src/test/java/com/dbcheck/app/ui/theme/ProfessionalMonochromeThemeResourceTest.kt docs/superpowers/plans/2026-06-26-professional-monochrome-theme.md
```

If screenshot references changed in Task 5, include only changed files under `app/src/screenshotTestDebug/reference/com/dbcheck/app/ComponentScreenshotTestsKt/`.

- [ ] **Step 3: Commit in Finnish**

Run: `git commit -m "Toteuta dBcheckin monokrominen ilme"`

Expected: commit succeeds without staging unrelated dirty-worktree changes.
