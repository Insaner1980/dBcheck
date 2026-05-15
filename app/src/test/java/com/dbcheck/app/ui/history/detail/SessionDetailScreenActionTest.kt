package com.dbcheck.app.ui.history.detail

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
}
