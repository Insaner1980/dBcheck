package com.dbcheck.app.ui.analytics.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.theme.DbCheckTheme

internal val EnvironmentMixCategory.label: String
    get() =
        when (this) {
            EnvironmentMixCategory.QUIET -> "Quiet"
            EnvironmentMixCategory.MODERATE -> "Moderate"
            EnvironmentMixCategory.LOUD -> "Loud"
            EnvironmentMixCategory.CRITICAL -> "Critical"
        }

internal val EnvironmentMixCategory.color: Color
    @Composable
    get() {
        val colors = DbCheckTheme.colorScheme
        return when (this) {
            EnvironmentMixCategory.QUIET -> colors.success
            EnvironmentMixCategory.MODERATE -> colors.material.primary
            EnvironmentMixCategory.LOUD -> colors.warning
            EnvironmentMixCategory.CRITICAL -> colors.material.error
        }
    }
