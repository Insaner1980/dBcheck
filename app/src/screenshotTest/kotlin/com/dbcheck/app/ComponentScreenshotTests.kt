package com.dbcheck.app

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.dbcheck.app.domain.audio.SpectralBandwidth
import com.dbcheck.app.ui.analytics.components.MonthlyTrendChart
import com.dbcheck.app.ui.analytics.components.SpectralAnalysisCard
import com.dbcheck.app.ui.analytics.components.YearlyReportCard
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixRowUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendPointUiState
import com.dbcheck.app.ui.analytics.state.MonthlyTrendUiState
import com.dbcheck.app.ui.analytics.state.SpectralAnalysisUiState
import com.dbcheck.app.ui.analytics.state.SpectralBandUiState
import com.dbcheck.app.ui.analytics.state.YearlyReportUiState
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun ButtonStylesPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DbCheckButton(text = "Primary", onClick = {}, style = DbCheckButtonStyle.Primary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Secondary", onClick = {}, style = DbCheckButtonStyle.Secondary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Tertiary", onClick = {}, style = DbCheckButtonStyle.Tertiary)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ButtonStylesDarkPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DbCheckButton(text = "Primary", onClick = {}, style = DbCheckButtonStyle.Primary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Secondary", onClick = {}, style = DbCheckButtonStyle.Secondary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Tertiary", onClick = {}, style = DbCheckButtonStyle.Tertiary)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300)
@Composable
fun CardPreview() {
    DbCheckTheme {
        DbCheckCard(modifier = Modifier.width(280.dp)) {
            Text(
                text = "42.5 dB",
                style = DbCheckTheme.typography.displayLg,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CardDarkPreview() {
    DbCheckTheme {
        DbCheckCard(modifier = Modifier.width(280.dp)) {
            Text(
                text = "42.5 dB",
                style = DbCheckTheme.typography.displayLg,
                color = DbCheckTheme.colorScheme.material.onSurface,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SpectralAnalysisLockedPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SpectralAnalysisCard(
                spectralState = SpectralAnalysisUiState.LockedPreview,
                isLocked = true,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SpectralAnalysisIdlePreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SpectralAnalysisCard(
                spectralState = SpectralAnalysisUiState.Idle,
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun SpectralAnalysisLivePreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            SpectralAnalysisCard(
                spectralState =
                    SpectralAnalysisUiState.Live(
                        bands =
                            List(24) { index ->
                                SpectralBandUiState(
                                    normalizedAmplitude = ((index % 8) + 1) / 8f,
                                )
                            },
                        dominantFrequencyHz = 2400f,
                        bandwidth = SpectralBandwidth.WIDE,
                    ),
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MonthlyTrendLockedPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthlyTrendChart(
                monthlyTrendState = MonthlyTrendUiState.LockedPreview,
                isLocked = true,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MonthlyTrendDataPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthlyTrendChart(
                monthlyTrendState =
                    MonthlyTrendUiState.Data(
                        points =
                            List(30) { index ->
                                MonthlyTrendPointUiState(
                                    dayStartMs = index.toLong(),
                                    laeqDb =
                                        if (index % 5 == 0) {
                                            null
                                        } else {
                                            58f + (index % 8) * 3f
                                        },
                                    maxDb =
                                        if (index % 5 == 0) {
                                            null
                                        } else {
                                            72f + (index % 8) * 2f
                                        },
                                )
                            },
                        laeqDb = 68.4f,
                        loudestDb = 91.2f,
                    ),
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun MonthlyTrendEmptyPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            MonthlyTrendChart(
                monthlyTrendState = MonthlyTrendUiState.Empty,
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun YearlyReportLockedPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            YearlyReportCard(
                yearlyReportState = YearlyReportUiState.LockedPreview,
                isLocked = true,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun YearlyReportDataPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            YearlyReportCard(
                yearlyReportState =
                    YearlyReportUiState.Data(
                        totalSessions = 86,
                        laeqDb = 67.8f,
                        loudestDayLabel = "May 8",
                        loudestDb = 94.2f,
                        zoneRows =
                            listOf(
                                EnvironmentMixRowUiState(EnvironmentMixCategory.QUIET, 34),
                                EnvironmentMixRowUiState(EnvironmentMixCategory.MODERATE, 42),
                                EnvironmentMixRowUiState(EnvironmentMixCategory.LOUD, 18),
                                EnvironmentMixRowUiState(EnvironmentMixCategory.CRITICAL, 6),
                            ),
                    ),
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun YearlyReportEmptyPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            YearlyReportCard(
                yearlyReportState = YearlyReportUiState.Empty,
                isLocked = false,
                onUpgradeClick = {},
            )
        }
    }
}
