package com.dbcheck.app.ui.history.detail

internal fun runSessionDetailPdfExportClick(
    isProUser: Boolean,
    onExportPdf: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
) {
    if (isProUser) {
        onExportPdf()
    } else {
        onNavigateToUpgrade()
    }
}

internal fun runSessionDetailWavExportClick(
    isProUser: Boolean,
    onShareWav: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
) {
    if (isProUser) {
        onShareWav()
    } else {
        onNavigateToUpgrade()
    }
}
