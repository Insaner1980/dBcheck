package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixRowUiState
import com.dbcheck.app.ui.analytics.state.YearlyReportUiState
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.util.Locale

@Composable
fun YearlyReportCard(
    yearlyReportState: YearlyReportUiState,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        val visibleState =
            if (isLocked) {
                YearlyReportUiState.LockedPreview
            } else {
                yearlyReportState
            }
        val cardState = visibleState.cardState()

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "12-MONTH NOISE REPORT",
                    style = DbCheckTheme.typography.labelMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    StatItem("Sessions", cardState.sessionsLabel)
                    StatItem("12mo LAeq", cardState.laeqLabel)
                    StatItem("Loudest", cardState.loudestLabel)
                }

                Text(
                    text = cardState.subtitle,
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )

                ZoneDistributionBar(rows = cardState.zoneRows)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cardState.zoneRows.forEach { row ->
                        ZoneRow(row)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneDistributionBar(rows: List<EnvironmentMixRowUiState>) {
    val trackColor = DbCheckTheme.colorScheme.material.outlineVariant.copy(alpha = 0.35f)
    val coloredRows = rows.map { row -> row to row.category.color }

    Canvas(modifier = Modifier.fillMaxWidth().height(24.dp)) {
        drawRoundRect(
            color = trackColor,
            cornerRadius = CornerRadius(BAR_CORNER_RADIUS, BAR_CORNER_RADIUS),
            size = size,
        )

        var startX = 0f
        coloredRows.forEachIndexed { index, (row, color) ->
            val width = size.width * (row.percent / PERCENT_TOTAL.toFloat())
            if (width > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(startX, 0f),
                    size = Size(width, size.height),
                    cornerRadius =
                        if (index == 0 || startX + width >= size.width) {
                            CornerRadius(BAR_CORNER_RADIUS, BAR_CORNER_RADIUS)
                        } else {
                            CornerRadius.Zero
                        },
                )
            }
            startX += width
        }
    }
}

@Composable
private fun ZoneRow(row: EnvironmentMixRowUiState) {
    val color = row.category.color

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Canvas(Modifier.size(8.dp)) {
                drawCircle(color = color)
            }
            Text(
                text = row.category.label,
                style = DbCheckTheme.typography.bodyMd,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
        }
        Text(
            text = "${row.percent}%",
            style = DbCheckTheme.typography.dataMd,
            color = DbCheckTheme.colorScheme.material.onSurface,
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = typography.dataLg, color = colors.material.onSurface)
        Text(label, style = typography.labelSm, color = colors.material.onSurfaceVariant)
    }
}

private fun YearlyReportUiState.cardState(): YearlyCardState =
    when (this) {
        YearlyReportUiState.Empty ->
            YearlyCardState(
                sessionsLabel = "0",
                laeqLabel = "--",
                loudestLabel = "--",
                subtitle = "No yearly exposure data",
                zoneRows = EMPTY_ZONE_ROWS,
            )
        YearlyReportUiState.LockedPreview -> LOCKED_PREVIEW_STATE
        is YearlyReportUiState.Data ->
            YearlyCardState(
                sessionsLabel = totalSessions.toString(),
                laeqLabel = String.format(Locale.getDefault(), "%.1f", laeqDb),
                loudestLabel = loudestDb?.let { "${it.toInt()} dB" } ?: "--",
                subtitle = "Loudest day $loudestDayLabel",
                zoneRows = zoneRows,
            )
    }

private data class YearlyCardState(
    val sessionsLabel: String,
    val laeqLabel: String,
    val loudestLabel: String,
    val subtitle: String,
    val zoneRows: List<EnvironmentMixRowUiState>,
)

private val EMPTY_ZONE_ROWS =
    listOf(
        EnvironmentMixRowUiState(EnvironmentMixCategory.QUIET, 0),
        EnvironmentMixRowUiState(EnvironmentMixCategory.MODERATE, 0),
        EnvironmentMixRowUiState(EnvironmentMixCategory.LOUD, 0),
        EnvironmentMixRowUiState(EnvironmentMixCategory.CRITICAL, 0),
    )

private val LOCKED_PREVIEW_STATE =
    YearlyCardState(
        sessionsLabel = "86",
        laeqLabel = "67.8",
        loudestLabel = "94 dB",
        subtitle = "Loudest day May 8",
        zoneRows =
            listOf(
                EnvironmentMixRowUiState(EnvironmentMixCategory.QUIET, 34),
                EnvironmentMixRowUiState(EnvironmentMixCategory.MODERATE, 42),
                EnvironmentMixRowUiState(EnvironmentMixCategory.LOUD, 18),
                EnvironmentMixRowUiState(EnvironmentMixCategory.CRITICAL, 6),
            ),
    )

private const val PERCENT_TOTAL = 100
private const val BAR_CORNER_RADIUS = 12f
