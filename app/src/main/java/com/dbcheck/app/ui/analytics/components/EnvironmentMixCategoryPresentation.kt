package com.dbcheck.app.ui.analytics.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.theme.DbCheckTheme

internal val EnvironmentMixCategory.label: String
    @Composable
    get() =
        when (this) {
            EnvironmentMixCategory.QUIET -> stringResource(R.string.environment_mix_quiet)
            EnvironmentMixCategory.MODERATE -> stringResource(R.string.environment_mix_moderate)
            EnvironmentMixCategory.LOUD -> stringResource(R.string.environment_mix_loud)
            EnvironmentMixCategory.CRITICAL -> stringResource(R.string.environment_mix_critical)
        }

internal val EnvironmentMixCategory.color: Color
    @Composable
    get() {
        val colors = DbCheckTheme.colorScheme
        return when (this) {
            EnvironmentMixCategory.QUIET -> colors.noiseLevels.quiet
            EnvironmentMixCategory.MODERATE -> colors.noiseLevels.normal
            EnvironmentMixCategory.LOUD -> colors.noiseLevels.elevated
            EnvironmentMixCategory.CRITICAL -> colors.noiseLevels.dangerous
        }
    }
