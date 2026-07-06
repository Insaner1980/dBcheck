package com.dbcheck.app.ui.history.detail

import com.dbcheck.app.domain.report.DbHistogramBucket
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionDetailScreenActionTest {
    @Test
    fun freeUserPdfClickNavigatesToUpgradeInsteadOfLaunchingDocumentPicker() {
        var exportClicks = 0
        var upgradeClicks = 0

        runSessionDetailPdfExportClick(
            isProUser = false,
            onExportPdf = { exportClicks++ },
            onNavigateToUpgrade = { upgradeClicks++ },
        )

        assertEquals(0, exportClicks)
        assertEquals(1, upgradeClicks)
    }

    @Test
    fun proUserPdfClickLaunchesDocumentPicker() {
        var exportClicks = 0
        var upgradeClicks = 0

        runSessionDetailPdfExportClick(
            isProUser = true,
            onExportPdf = { exportClicks++ },
            onNavigateToUpgrade = { upgradeClicks++ },
        )

        assertEquals(1, exportClicks)
        assertEquals(0, upgradeClicks)
    }

    @Test
    fun freeUserWavExportClickNavigatesToUpgradeInsteadOfSharing() {
        var shareClicks = 0
        var upgradeClicks = 0

        runSessionDetailWavExportClick(
            isProUser = false,
            onShareWav = { shareClicks++ },
            onNavigateToUpgrade = { upgradeClicks++ },
        )

        assertEquals(0, shareClicks)
        assertEquals(1, upgradeClicks)
    }

    @Test
    fun proUserWavExportClickCreatesShareIntent() {
        var shareClicks = 0
        var upgradeClicks = 0

        runSessionDetailWavExportClick(
            isProUser = true,
            onShareWav = { shareClicks++ },
            onNavigateToUpgrade = { upgradeClicks++ },
        )

        assertEquals(1, shareClicks)
        assertEquals(0, upgradeClicks)
    }

    @Test
    fun histogramAccessibilitySummaryListsOnlyVisibleBuckets() {
        val summary =
            dbHistogramAccessibilitySummary(
                listOf(
                    DbHistogramBucket(minDb = 30, maxDb = 40, sampleCount = 0, percent = 0),
                    DbHistogramBucket(minDb = 40, maxDb = 50, sampleCount = 8, percent = 67),
                    DbHistogramBucket(minDb = 80, maxDb = 90, sampleCount = 4, percent = 33),
                ),
            )

        assertEquals("40-50 dB 67%, 80-90 dB 33%", summary)
    }

    @Test
    fun histogramAccessibilitySummaryIsEmptyWithoutSamples() {
        val summary =
            dbHistogramAccessibilitySummary(
                listOf(
                    DbHistogramBucket(minDb = 30, maxDb = 40, sampleCount = 0, percent = 0),
                    DbHistogramBucket(minDb = 40, maxDb = 50, sampleCount = 0, percent = 0),
                ),
            )

        assertEquals("", summary)
    }
}
