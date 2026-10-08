package com.dbcheck.app.ui.analytics.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckCardEmphasis
import com.dbcheck.app.ui.theme.ChartTokens
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
internal fun AnalyticsEmptyPreviewCard(modifier: Modifier = Modifier) {
    val spacing = DbCheckTheme.spacing

    DbCheckCard(
        modifier = modifier.fillMaxWidth(),
        emphasis = DbCheckCardEmphasis.Subdued,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.space4),
        ) {
            EmptyPreviewMetric(R.string.analytics_empty_preview_weekly_exposure)
            NeutralChartScaffold()
            EmptyPreviewMetric(R.string.analytics_empty_preview_monthly_trend)
            EmptyPreviewMetric(R.string.analytics_empty_preview_reports)
        }
    }
}

@Composable
private fun EmptyPreviewMetric(@StringRes labelResId: Int) {
    val colors = DbCheckTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(labelResId),
            style = DbCheckTheme.typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.analytics_empty_preview_unknown),
            style = DbCheckTheme.typography.dataMd,
            color = colors.material.onSurfaceVariant,
        )
    }
}

@Composable
private fun NeutralChartScaffold() {
    val gridColor =
        DbCheckTheme.colorScheme.material.outlineVariant.copy(alpha = ChartTokens.PreviewGridAlpha)
    val chartDescription = stringResource(R.string.analytics_empty_preview_chart_description)

    Canvas(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(DbCheckTheme.spacing.space16)
                .semantics { contentDescription = chartDescription },
    ) {
        val gridStroke = ChartTokens.GridLineWidth.toPx()
        repeat(4) { index ->
            val y = size.height * index / 3f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = gridStroke,
            )
        }
        repeat(7) { index ->
            val x = size.width * index / 6f
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = gridStroke,
            )
        }
    }
}
