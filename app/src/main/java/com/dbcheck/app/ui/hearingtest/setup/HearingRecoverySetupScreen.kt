package com.dbcheck.app.ui.hearingtest.setup

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckSetupHeader
import com.dbcheck.app.ui.components.DbCheckSetupScaffold
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingRecoverySetupScreen(onStartCheck: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = DbCheckTheme.spacing

    DbCheckSetupScaffold(
        onBack = onBack,
        modifier = modifier,
        header = {
            DbCheckSetupHeader(
                phase = stringResource(R.string.hearing_recovery_setup_phase),
                title = stringResource(R.string.hearing_recovery_setup_title),
                description = stringResource(R.string.hearing_recovery_setup_description),
            )
        },
        cta = {
            DbCheckButton(
                text = stringResource(R.string.hearing_recovery_start_short_check),
                onClick = onStartCheck,
                modifier = Modifier.fillMaxWidth(),
                height = spacing.space12,
            )
        },
    ) {
        HearingSetupChecklist(
            secondIcon = Icons.Filled.GraphicEq,
            secondTitle = stringResource(R.string.hearing_recovery_setup_compare_title),
            secondDescription = stringResource(R.string.hearing_recovery_setup_compare_description),
        )
    }
}
