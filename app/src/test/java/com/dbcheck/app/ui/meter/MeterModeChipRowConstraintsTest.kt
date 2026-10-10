package com.dbcheck.app.ui.meter

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.assertTextFits
import com.dbcheck.app.ui.meter.state.MeasurementMode
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
class MeterModeChipRowConstraintsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun freeAt320Dp() = verifyModes(320, 1f, false)

    @Test
    fun freeAt200PercentFont() = verifyModes(400, 2f, false)

    @Test
    fun freeAtCombinedConstraints() = verifyModes(320, 2f, false)

    @Test
    fun proAt320Dp() = verifyModes(320, 1f, true)

    @Test
    fun proAt200PercentFont() = verifyModes(400, 2f, true)

    @Test
    fun proAtCombinedConstraints() = verifyModes(320, 2f, true)

    private fun verifyModes(widthDp: Int, fontScale: Float, pro: Boolean) {
        var meterLabel = ""
        var dosimeterLabel = ""
        var selected: MeasurementMode? = null
        var upgrades = 0
        composeTestRule.setContent {
            meterLabel = stringResource(R.string.meter_mode_db_meter)
            dosimeterLabel = stringResource(
                if (pro) R.string.meter_mode_dosimeter else R.string.meter_mode_dosimeter_locked,
            )
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                DbCheckTheme {
                    Box(Modifier.requiredWidth(widthDp.dp).padding(horizontal = 20.dp)) {
                        MeterModeChipRow(
                            measurementMode = MeasurementMode.DB_METER,
                            isProUser = pro,
                            dosimeterCardEnabled = pro,
                            onSelectMode = { selected = it },
                            onLockedDosimeterClick = { upgrades++ },
                        )
                    }
                }
            }
        }
        val meter = composeTestRule.onNodeWithText(meterLabel)
        meter.assertTextFits(meterLabel)
        meter.performClick()
        assertEquals(MeasurementMode.DB_METER, selected)
        val dosimeter = composeTestRule.onNodeWithText(dosimeterLabel)
        dosimeter.assertTextFits(dosimeterLabel)
        dosimeter.performClick()
        assertEquals(if (pro) MeasurementMode.DOSIMETER else MeasurementMode.DB_METER, selected)
        assertEquals(if (pro) 0 else 1, upgrades)
    }
}
