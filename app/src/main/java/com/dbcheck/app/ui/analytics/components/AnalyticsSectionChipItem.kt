package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.state.AnalyticsSection
import com.dbcheck.app.ui.theme.DbCheckTheme

internal data class AnalyticsSectionChipItem(
    val section: AnalyticsSection,
    val labelResId: Int,
    val isSelected: Boolean,
    val isLocked: Boolean,
)

internal fun analyticsSectionChipItems(
    selectedSection: AnalyticsSection,
    isProUser: Boolean,
): List<AnalyticsSectionChipItem> = enumValues<AnalyticsSection>().map { section ->
        AnalyticsSectionChipItem(
            section = section,
            labelResId = section.labelResId,
            isSelected = section == selectedSection,
            isLocked = !isProUser && section.requiresPro,
        )
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnalyticsSectionChipRow(
    selectedSection: AnalyticsSection,
    isProUser: Boolean,
    onSectionSelect: (AnalyticsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
        verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space2),
    ) {
        analyticsSectionChipItems(
            selectedSection = selectedSection,
            isProUser = isProUser,
        ).forEach { item ->
            val label = stringResource(item.labelResId)
            AnalyticsSelectableChip(
                text = label,
                selected = item.isSelected,
                locked = item.isLocked,
                chipContentDescription =
                    analyticsSectionContentDescription(
                        label = label,
                        isSelected = item.isSelected,
                        isLocked = item.isLocked,
                    ),
                onClick = { onSectionSelect(item.section) },
            )
        }
    }
}

@Composable
private fun analyticsSectionContentDescription(label: String, isSelected: Boolean, isLocked: Boolean): String = when {
        isLocked && isSelected -> stringResource(R.string.a11y_analytics_section_locked_selected, label)
        isLocked -> stringResource(R.string.a11y_analytics_section_locked, label)
        isSelected -> stringResource(R.string.a11y_analytics_section_selected, label)
        else -> stringResource(R.string.a11y_analytics_section, label)
    }

private val AnalyticsSection.labelResId: Int
    get() =
        when (this) {
            AnalyticsSection.OVERVIEW -> R.string.analytics_section_overview
            AnalyticsSection.SPECTRAL -> R.string.analytics_section_spectral
            AnalyticsSection.ENVIRONMENT -> R.string.analytics_section_environment
        }

private val AnalyticsSection.requiresPro: Boolean
    get() =
        when (this) {
            AnalyticsSection.OVERVIEW -> false

            AnalyticsSection.SPECTRAL,
            AnalyticsSection.ENVIRONMENT,
            -> true
        }
