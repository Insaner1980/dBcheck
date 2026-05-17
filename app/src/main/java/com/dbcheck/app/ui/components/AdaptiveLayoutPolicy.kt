package com.dbcheck.app.ui.components

internal const val COMPACT_HEIGHT_SCROLL_BREAKPOINT_DP = 640f

internal fun shouldUseCompactHeightScrolling(availableHeightDp: Float): Boolean =
    availableHeightDp < COMPACT_HEIGHT_SCROLL_BREAKPOINT_DP
