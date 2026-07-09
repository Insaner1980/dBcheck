package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.state.AnalyticsOverviewRange
import com.dbcheck.app.ui.theme.DbCheckTheme

internal data class AnalyticsOverviewRangeChipItem(
    val range: AnalyticsOverviewRange,
    val labelResId: Int,
    val isSelected: Boolean,
    val isLocked: Boolean,
)

internal fun analyticsOverviewRangeChipItems(
    selectedRange: AnalyticsOverviewRange,
    isProUser: Boolean,
): List<AnalyticsOverviewRangeChipItem> = enumValues<AnalyticsOverviewRange>().map { range ->
        AnalyticsOverviewRangeChipItem(
            range = range,
            labelResId = range.labelResId,
            isSelected = range == selectedRange,
            isLocked = !isProUser && range.requiresPro,
        )
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsOverviewRangeChipRow(
    selectedRange: AnalyticsOverviewRange,
    isProUser: Boolean,
    onRangeSelect: (AnalyticsOverviewRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
    ) {
        analyticsOverviewRangeChipItems(
            selectedRange = selectedRange,
            isProUser = isProUser,
        ).forEach { item ->
            val label = stringResource(item.labelResId)
            AnalyticsSelectableChip(
                text = label,
                selected = item.isSelected,
                locked = item.isLocked,
                chipContentDescription =
                    analyticsOverviewRangeContentDescription(
                        label = label,
                        isSelected = item.isSelected,
                        isLocked = item.isLocked,
                    ),
                onClick = { onRangeSelect(item.range) },
            )
        }
    }
}

@Composable
private fun analyticsOverviewRangeContentDescription(label: String, isSelected: Boolean, isLocked: Boolean): String =
    when {
        isLocked && isSelected -> stringResource(R.string.a11y_analytics_range_locked_selected, label)
        isLocked -> stringResource(R.string.a11y_analytics_range_locked, label)
        isSelected -> stringResource(R.string.a11y_analytics_range_selected, label)
        else -> stringResource(R.string.a11y_analytics_range, label)
    }

private val AnalyticsOverviewRange.labelResId: Int
    get() =
        when (this) {
            AnalyticsOverviewRange.WEEKLY -> R.string.analytics_range_weekly
            AnalyticsOverviewRange.MONTHLY -> R.string.analytics_range_monthly
        }

private val AnalyticsOverviewRange.requiresPro: Boolean
    get() =
        when (this) {
            AnalyticsOverviewRange.WEEKLY -> false
            AnalyticsOverviewRange.MONTHLY -> true
        }
