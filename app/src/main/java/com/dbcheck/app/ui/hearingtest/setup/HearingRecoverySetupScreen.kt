package com.dbcheck.app.ui.hearingtest.setup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckSetupScaffold
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingRecoverySetupScreen(onStartCheck: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    DbCheckSetupScaffold(onBack = onBack, modifier = modifier) {
        Text(
            text = stringResource(R.string.hearing_recovery_setup_phase),
            style = typography.labelMd,
            color = colors.material.primary,
        )
        Spacer(Modifier.height(spacing.space2))
        Text(
            text = stringResource(R.string.hearing_recovery_setup_title),
            style = typography.headlineLg,
            color = colors.material.onSurface,
        )
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = stringResource(R.string.hearing_recovery_setup_description),
            style = typography.bodyLg,
            color = colors.material.onSurfaceVariant,
        )

        Spacer(Modifier.height(spacing.space8))

        HearingSetupChecklist(
            secondIcon = Icons.Filled.GraphicEq,
            secondTitle = stringResource(R.string.hearing_recovery_setup_compare_title),
            secondDescription = stringResource(R.string.hearing_recovery_setup_compare_description),
        )

        Spacer(Modifier.height(spacing.space16))

        DbCheckButton(
            text = stringResource(R.string.hearing_recovery_start_short_check),
            onClick = onStartCheck,
            modifier = Modifier.fillMaxWidth(),
            height = 56.dp,
        )

        Spacer(Modifier.height(spacing.space8))
    }
}
