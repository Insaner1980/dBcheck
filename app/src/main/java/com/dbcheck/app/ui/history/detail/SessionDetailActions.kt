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

internal fun sessionDetailContentActions(
    state: SessionDetailUiState,
    viewModel: SessionDetailViewModel,
    onBack: () -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onLaunchPdfExport: () -> Unit,
    onShowNamingSheet: () -> Unit,
): SessionDetailContentActions = SessionDetailContentActions(
    onBack = onBack,
    onNavigateToUpgrade = onNavigateToUpgrade,
    onExportPdf = {
        runSessionDetailPdfExportClick(
            isProUser = state.isProUser,
            onExportPdf = onLaunchPdfExport,
            onNavigateToUpgrade = onNavigateToUpgrade,
        )
    },
    onEditMetadata = {
        if (state.isProUser) {
            onShowNamingSheet()
        } else {
            onNavigateToUpgrade()
        }
    },
    onSharePng = viewModel::createSharePngIntent,
    onShareWav = {
        runSessionDetailWavExportClick(
            isProUser = state.isProUser,
            onShareWav = viewModel::createShareWavIntent,
            onNavigateToUpgrade = onNavigateToUpgrade,
        )
    },
    onDeleteWav = viewModel::deleteWavRecording,
)

internal data class SessionDetailContentActions(
    val onBack: () -> Unit,
    val onNavigateToUpgrade: () -> Unit,
    val onExportPdf: () -> Unit,
    val onEditMetadata: () -> Unit,
    val onSharePng: () -> Unit,
    val onShareWav: () -> Unit,
    val onDeleteWav: () -> Unit,
)
