package com.dbcheck.app.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.components.EmptyState
import com.dbcheck.app.ui.components.SessionCard
import com.dbcheck.app.ui.components.SkeletonLoader
import com.dbcheck.app.ui.history.components.Last24HoursChart
import com.dbcheck.app.ui.history.components.SafeHoursCard
import com.dbcheck.app.ui.history.components.WeeklyTrendCard
import com.dbcheck.app.ui.history.state.HistoryUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(modifier = Modifier.fillMaxSize()) {
        DbCheckTopAppBar(actionIcon = Icons.Outlined.Settings)

        when (val state = uiState) {
            is HistoryUiState.Loading -> {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SkeletonLoader(height = 180.dp)
                    SkeletonLoader(height = 80.dp)
                    SkeletonLoader(height = 80.dp)
                }
            }

            is HistoryUiState.Empty -> {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = "No Sessions Yet",
                    description = "Start measuring to see your history here.",
                    ctaText = "Go to Meter",
                    onCtaClick = { /* Navigate to meter */ },
                )
            }

            is HistoryUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(spacing.space4),
                ) {
                    item {
                        Text(
                            text = "EXPOSURE INSIGHTS",
                            style = typography.labelMd,
                            color = colors.material.onSurfaceVariant,
                        )
                        Text(
                            text = "History",
                            style = typography.headlineLg,
                            color = colors.material.onSurface,
                        )
                        Spacer(Modifier.height(spacing.space2))
                    }

                    item {
                        Last24HoursChart(
                            hourlyAverages = state.last24HoursData,
                            avgDb = state.last24HoursAvg,
                            peakDb = state.last24HoursPeak,
                            trend = state.last24HoursTrend,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "RECENT SESSIONS",
                                style = typography.labelMd,
                                color = colors.material.onSurfaceVariant,
                            )
                            DbCheckButton(
                                text = "View All",
                                onClick = { /* TODO */ },
                                style = DbCheckButtonStyle.Tertiary,
                            )
                        }
                    }

                    items(
                        items = state.recentSessions,
                        key = { it.id },
                    ) { session ->
                        val dateFormat = SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault())
                        SessionCard(
                            emoji = session.emoji ?: "\uD83C\uDFA4",
                            title = session.name ?: "Session",
                            metadata = dateFormat.format(Date(session.startTime)),
                            peakDb = session.peakDb,
                            avgDb = session.avgDb,
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            WeeklyTrendCard(
                                percent = state.weeklyTrendPercent,
                                label = state.weeklyTrendLabel,
                                modifier = Modifier.weight(1f),
                            )
                            SafeHoursCard(
                                hours = state.safeHours,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    item { Spacer(Modifier.height(spacing.space4)) }
                }
            }
        }
    }
}
