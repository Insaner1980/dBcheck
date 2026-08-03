package com.dbcheck.app.ui.hearing.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckLockedCtaCard

@Composable
fun HearingTestCta(
    onStartTest: () -> Unit,
    modifier: Modifier = Modifier,
    isLocked: Boolean = false,
    onUpgradeClick: () -> Unit = {},
    presentation: HearingTestCtaPresentation = HearingTestCtaPresentation.Standard,
) {
    DbCheckLockedCtaCard(
        title =
            stringResource(
                when (presentation) {
                    HearingTestCtaPresentation.Standard -> R.string.hearing_test_cta_title
                    HearingTestCtaPresentation.Baseline -> R.string.hearing_baseline_cta_title
                },
            ),
        subtitle =
            stringResource(
                when (presentation) {
                    HearingTestCtaPresentation.Standard -> R.string.hearing_test_cta_subtitle
                    HearingTestCtaPresentation.Baseline -> R.string.hearing_baseline_cta_subtitle
                },
            ),
        buttonText =
            stringResource(
                when (presentation) {
                    HearingTestCtaPresentation.Standard -> R.string.action_start_test_arrow
                    HearingTestCtaPresentation.Baseline -> R.string.hearing_baseline_cta_action
                },
            ),
        onClick = onStartTest,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
        isLocked = isLocked,
    )
}
