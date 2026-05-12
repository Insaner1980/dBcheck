package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.dbcheck.app.domain.noise.NoiseLevel
import com.dbcheck.app.ui.analytics.state.MonthlyTrendPointUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendUiState
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.util.Locale

@Composable
fun MonthlyTrendChart(
    monthlyTrendState: MonthlyTrendUiState,
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
                MonthlyTrendUiState.LockedPreview
            } else {
                monthlyTrendState
            }
        val chartState = visibleState.chartState()

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HeaderRow(chartState)

                Spacer(Modifier.height(DbCheckTheme.spacing.space4))

                MonthlyTrendCanvas(
                    points = chartState.points,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(148.dp),
                )

                Spacer(Modifier.height(DbCheckTheme.spacing.space3))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "30 DAYS AGO",
                        style = DbCheckTheme.typography.labelSm,
                        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                    )
                    Text(
                        text = "TODAY",
                        style = DbCheckTheme.typography.labelSm,
                        color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(chartState: MonthlyChartState) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = "30-DAY LAEQ TREND",
                style = typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )
            Text(
                text = chartState.subtitle,
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = chartState.laeqLabel,
                style = typography.dataLg,
                color = colors.material.onSurface,
            )
            Text(
                text = "LAEQ",
                style = typography.labelSm,
                color = colors.material.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MonthlyTrendCanvas(
    points: List<MonthlyTrendPointUiState>,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme
    val lineColor = colors.material.primary
    val pointColor = colors.material.secondary
    val emptyColor = colors.material.outlineVariant.copy(alpha = 0.4f)
    val gridColor = colors.material.outlineVariant.copy(alpha = 0.24f)
    val normalizedPoints =
        remember(points) {
            val values = points.mapNotNull { it.laeqDb }
            val minDb = (values.minOrNull() ?: MinChartDb).coerceAtMost(MinChartDb)
            val maxDb = (values.maxOrNull() ?: MAX_CHART_DB).coerceAtLeast(minDb + MIN_DB_RANGE)
            NormalizedMonthlyPoints(
                minDb = minDb,
                maxDb = maxDb,
                points = points,
            )
        }

    Canvas(modifier = modifier.fillMaxSize()) {
        repeat(GRID_LINE_COUNT) { index ->
            val y = size.height * index / (GRID_LINE_COUNT - 1)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = GRID_STROKE_WIDTH,
            )
        }

        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        points.forEachIndexed { index, point ->
            if (point.laeqDb == null) {
                val x = index * stepX
                drawCircle(
                    color = emptyColor,
                    radius = EMPTY_POINT_RADIUS,
                    center = Offset(x, size.height),
                )
            }
        }

        val path = Path()
        var hasActiveSegment = false
        normalizedPoints.points.forEachIndexed { index, point ->
            val laeqDb = point.laeqDb
            if (laeqDb == null) {
                hasActiveSegment = false
            } else {
                val x = index * stepX
                val y = normalizedPoints.yFor(db = laeqDb, height = size.height)
                if (hasActiveSegment) {
                    path.lineTo(x, y)
                } else {
                    path.moveTo(x, y)
                    hasActiveSegment = true
                }
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style =
                Stroke(
                    width = TREND_STROKE_WIDTH,
                    cap = StrokeCap.Round,
                ),
        )

        normalizedPoints.points.forEachIndexed { index, point ->
            point.laeqDb?.let { laeqDb ->
                drawCircle(
                    color = pointColor,
                    radius = DATA_POINT_RADIUS,
                    center = Offset(index * stepX, normalizedPoints.yFor(db = laeqDb, height = size.height)),
                )
            }
        }
    }
}

private fun MonthlyTrendUiState.chartState(): MonthlyChartState =
    when (this) {
        MonthlyTrendUiState.Empty ->
            MonthlyChartState(
                points = EMPTY_POINTS,
                laeqLabel = "--",
                subtitle = "No monthly exposure data",
            )
        MonthlyTrendUiState.LockedPreview ->
            MonthlyChartState(
                points = LOCKED_PREVIEW_POINTS,
                laeqLabel = "68.4",
                subtitle = "Rolling Pro insight",
            )
        is MonthlyTrendUiState.Data ->
            MonthlyChartState(
                points = points,
                laeqLabel = String.format(Locale.getDefault(), "%.1f", laeqDb),
                subtitle = loudestDb?.let { "Peak ${it.toInt()} dB" } ?: "Rolling Pro insight",
            )
    }

private data class MonthlyChartState(
    val points: List<MonthlyTrendPointUiState>,
    val laeqLabel: String,
    val subtitle: String,
)

private data class NormalizedMonthlyPoints(
    val minDb: Float,
    val maxDb: Float,
    val points: List<MonthlyTrendPointUiState>,
) {
    fun yFor(
        db: Float,
        height: Float,
    ): Float {
        val normalized = ((db - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
        val verticalPadding = height * CHART_VERTICAL_PADDING_FRACTION
        val drawableHeight = height - verticalPadding * 2f
        return height - verticalPadding - normalized * drawableHeight
    }
}

private val EMPTY_POINTS = List(30) { index -> MonthlyTrendPointUiState(index.toLong(), null, null) }

private val LOCKED_PREVIEW_POINTS =
    List(30) { index ->
        MonthlyTrendPointUiState(
            dayStartMs = index.toLong(),
            laeqDb = 58f + (index % 9) * 2.2f + if (index > 21) 7f else 0f,
            maxDb = 72f + (index % 7) * 2f,
        )
    }

private val MinChartDb = NoiseLevel.QUIET.maxDb
private const val MAX_CHART_DB = 100f
private const val MIN_DB_RANGE = 1f
private const val GRID_LINE_COUNT = 4
private const val GRID_STROKE_WIDTH = 1f
private const val TREND_STROKE_WIDTH = 4f
private const val DATA_POINT_RADIUS = 4f
private const val EMPTY_POINT_RADIUS = 2.5f
private const val CHART_VERTICAL_PADDING_FRACTION = 0.08f
