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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.R
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckTopAppBar
import com.dbcheck.app.ui.components.EmptyState
import com.dbcheck.app.ui.components.SessionCard
import com.dbcheck.app.ui.components.SessionCardEditAction
import com.dbcheck.app.ui.components.SessionCardState
import com.dbcheck.app.ui.components.SkeletonLoader
import com.dbcheck.app.ui.history.components.Last24HoursChart
import com.dbcheck.app.ui.history.components.SafeHoursCard
import com.dbcheck.app.ui.history.components.SessionNamingSheet
import com.dbcheck.app.ui.history.components.WeeklyTrendCard
import com.dbcheck.app.ui.history.state.HistoryUiState
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onNavigateToMeter: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onNavigateToUpgrade: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        DbCheckTopAppBar(
            actionIcon = Icons.Outlined.Settings,
            actionContentDescription = stringResource(R.string.a11y_open_settings),
            onActionClick = onNavigateToSettings,
        )

        when (val state = uiState) {
            is HistoryUiState.Loading -> HistoryLoading()

            is HistoryUiState.Empty -> HistoryEmpty(onNavigateToMeter)

            is HistoryUiState.Error -> HistoryError(state.message, onNavigateToMeter)

            is HistoryUiState.Success ->
                HistorySuccessContent(
                    state = state,
                    onSessionClick = onSessionClick,
                    onNavigateToUpgrade = onNavigateToUpgrade,
                    onSaveSessionMetadata = viewModel::saveSessionMetadata,
                    onViewAllSessions = viewModel::showAllSessions,
                )
        }
    }
}

@Composable
private fun HistoryLoading() {
    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SkeletonLoader(height = 180.dp)
        SkeletonLoader(height = 80.dp)
        SkeletonLoader(height = 80.dp)
    }
}

@Composable
private fun HistoryEmpty(onNavigateToMeter: () -> Unit) {
    EmptyState(
        icon = Icons.Outlined.History,
        title = stringResource(R.string.history_empty_title),
        description = stringResource(R.string.history_empty_description),
        ctaText = stringResource(R.string.action_go_to_meter),
        onCtaClick = onNavigateToMeter,
    )
}

@Composable
private fun HistoryError(message: String, onNavigateToMeter: () -> Unit) {
    EmptyState(
        icon = Icons.Outlined.History,
        title = message,
        description = "",
        ctaText = stringResource(R.string.action_go_to_meter),
        onCtaClick = onNavigateToMeter,
    )
}

@Composable
private fun HistorySuccessContent(
    state: HistoryUiState.Success,
    onSessionClick: (Long) -> Unit,
    onNavigateToUpgrade: () -> Unit,
    onSaveSessionMetadata: (Long, String, String, List<String>) -> Unit,
    onViewAllSessions: () -> Unit,
) {
    val spacing = DbCheckTheme.spacing
    var editingSession by remember { mutableStateOf<Session?>(null) }

    editingSession?.let { session ->
        SessionNamingSheet(
            currentName = session.name.orEmpty(),
            currentEmoji = session.emoji ?: autoEmoji(session.name, session.startTime),
            currentTags = session.tags,
            onDismiss = { editingSession = null },
            onSave = { name, emoji, tags ->
                onSaveSessionMetadata(session.id, name, emoji, tags)
                editingSession = null
            },
        )
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        item {
            HistoryHeader()
        }

        state.metadataErrorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.error,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        item {
            Last24HoursChart(
                hourlyAverages = state.last24HoursData,
                avgDb = state.last24HoursAvg,
                maxDb = state.last24HoursMax,
                trend = state.last24HoursTrend,
                windowStartMs = state.last24HoursWindowStartMs,
                windowEndMs = state.last24HoursWindowEndMs,
            )
        }

        item {
            HistoryRecentSessionsHeader(
                isShowingAllSessions = state.isShowingAllSessions,
                onViewAllSessions = onViewAllSessions,
            )
        }

        recentSessionItems(
            state = state,
            onSessionClick = onSessionClick,
            onEditSession = { editingSession = it },
            onNavigateToUpgrade = onNavigateToUpgrade,
        )

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

private fun LazyListScope.recentSessionItems(
    state: HistoryUiState.Success,
    onSessionClick: (Long) -> Unit,
    onEditSession: (Session) -> Unit,
    onNavigateToUpgrade: () -> Unit,
) {
    items(
        items = state.recentSessions,
        key = { it.id },
    ) { session ->
        val dateFormat = SimpleDateFormat("MMM dd · HH:mm", Locale.getDefault())
        SessionCard(
            state =
                SessionCardState(
                    emoji = session.emoji ?: autoEmoji(session.name, session.startTime),
                    title = session.name ?: autoSessionName(session.startTime),
                    metadata = dateFormat.format(Date(session.startTime)),
                    peakDb = session.peakDb,
                    avgDb = session.avgDb,
                    tags = session.tags,
                ),
            editAction =
                SessionCardEditAction(
                    isLocked = !state.isProUser,
                    onClick = {
                        if (state.isProUser) {
                            onEditSession(session)
                        } else {
                            onNavigateToUpgrade()
                        }
                    },
                ),
            onClick = { onSessionClick(session.id) },
        )
    }
}

@Composable
private fun HistoryHeader() {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Text(
        text = stringResource(R.string.history_exposure_insights),
        style = typography.labelMd,
        color = colors.material.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.history_title),
        style = typography.headlineLg,
        color = colors.material.onSurface,
    )
    Spacer(Modifier.height(spacing.space2))
}

@Composable
private fun HistoryRecentSessionsHeader(isShowingAllSessions: Boolean, onViewAllSessions: () -> Unit) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.history_recent_sessions),
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        DbCheckButton(
            text =
                if (isShowingAllSessions) {
                    stringResource(R.string.action_showing_all)
                } else {
                    stringResource(R.string.action_view_all)
                },
            onClick = onViewAllSessions,
            enabled = !isShowingAllSessions,
            style = DbCheckButtonStyle.Tertiary,
        )
    }
}

private fun hourOfDay(timestampMs: Long): Int =
    Calendar.getInstance().apply { timeInMillis = timestampMs }.get(Calendar.HOUR_OF_DAY)

@Composable
private fun autoSessionName(startTime: Long): String = when (hourOfDay(startTime)) {
        in 5..11 -> stringResource(R.string.session_name_morning)
        in 12..16 -> stringResource(R.string.session_name_afternoon)
        in 17..20 -> stringResource(R.string.session_name_evening)
        else -> stringResource(R.string.session_name_late_night)
    }

private fun autoEmoji(name: String?, startTime: Long): String = if (name != null) {
        "\uD83C\uDFA4"
    } else {
        when (hourOfDay(startTime)) {
            in 5..11 -> "☀\uFE0F"
            in 12..16 -> "☕"
            in 17..20 -> "\uD83C\uDF07"
            else -> "\uD83C\uDF19"
        }
    }
