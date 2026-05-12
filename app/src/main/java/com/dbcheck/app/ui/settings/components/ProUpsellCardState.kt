package com.dbcheck.app.ui.settings.components

data class ProUpsellCardState(
    val isPurchaseLaunching: Boolean = false,
    val purchaseMessage: String? = null,
    val purchaseErrorMessage: String? = null,
    val showDebugForceFree: Boolean = false,
    val debugForceFreeEnabled: Boolean = false,
)
