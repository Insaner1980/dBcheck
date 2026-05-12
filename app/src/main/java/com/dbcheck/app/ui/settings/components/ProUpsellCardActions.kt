package com.dbcheck.app.ui.settings.components

data class ProUpsellCardActions(val onUpgradeClick: () -> Unit, val onDebugForceFreeChange: (Boolean) -> Unit = {})
