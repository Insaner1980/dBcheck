package com.dbcheck.app.ui.meter.components

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.noise.DosimeterCalculator
import com.dbcheck.app.domain.noise.DosimeterStandard
import com.dbcheck.app.ui.components.assertTextFits
import com.dbcheck.app.ui.meter.state.DosimeterUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [28], qualifiers = "w400dp-h900dp-mdpi")
class DosimeterGaugeConstraintsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun highDoseAt320Dp() = verifyDose(320, 1f)

    @Test
    fun highDoseAt200PercentFont() = verifyDose(400, 2f)

    @Test
    fun highDoseAtCombinedConstraints() = verifyDose(320, 2f)

    private fun verifyDose(widthDp: Int, fontScale: Float) {
        val durationMs = 8 * 60 * 60 * 1_000L
        val exposure = DosimeterCalculator.calculate(DosimeterStandard.NIOSH_REL, 115f, durationMs)
        val state = DosimeterUiState.Data(
            standard = exposure.standard,
            laeqDb = 115f,
            twaDb = exposure.twaDb,
            dosePercent = exposure.dosePercent,
            projectedDosePercent = exposure.projectedDosePercent,
            remainingExposureMs = exposure.remainingExposureMs,
            durationMs = durationMs,
            sampleCount = 1,
        )
        composeTestRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    Column(Modifier.requiredWidth(widthDp.dp).verticalScroll(rememberScrollState()).padding(20.dp)) {
                        DosimeterGaugeCard(state)
                    }
                }
            }
        }
        val doseLabel = DosimeterGaugeFormatter.percent(state.dosePercent)
        val doseNodes = composeTestRule.onAllNodesWithText(doseLabel, useUnmergedTree = true)
        assertEquals(2, doseNodes.fetchSemanticsNodes().size)
        repeat(2) { doseNodes[it].assertTextFits(doseLabel) }
        val levelLabel = DosimeterGaugeFormatter.decibel(115f)
        val levelNodes = composeTestRule.onAllNodesWithText(levelLabel, useUnmergedTree = true)
        assertEquals(2, levelNodes.fetchSemanticsNodes().size)
        repeat(2) { levelNodes[it].assertTextFits(levelLabel) }
    }
}
