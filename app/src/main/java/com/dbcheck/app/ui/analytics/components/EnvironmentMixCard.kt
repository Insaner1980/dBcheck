package com.dbcheck.app.ui.analytics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.analytics.state.EnvironmentMixCategory
import com.dbcheck.app.ui.analytics.state.EnvironmentMixRowUiState
import com.dbcheck.app.ui.analytics.state.EnvironmentMixUiState
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun EnvironmentMixCard(
    environmentMixState: EnvironmentMixUiState,
    isLocked: Boolean,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        val visibleState =
            if (isLocked) {
                EnvironmentMixUiState.LockedPreview
            } else {
                environmentMixState
            }
        val rows = rowsFor(visibleState)

        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.environment_mix_title),
                    style = typography.labelMd,
                    color = colors.material.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { row ->
                        MixRow(
                            label = row.category.label,
                            percent = "${row.percent}%",
                            color = row.category.color,
                        )
                    }
                }
            }
        }
    }
}

private fun rowsFor(state: EnvironmentMixUiState): List<EnvironmentMixRowUiState> = when (state) {
        EnvironmentMixUiState.Empty -> EMPTY_ROWS
        EnvironmentMixUiState.LockedPreview -> LOCKED_PREVIEW_ROWS
        is EnvironmentMixUiState.Data -> state.rows
    }

private val EMPTY_ROWS =
    listOf(
        EnvironmentMixRowUiState(EnvironmentMixCategory.QUIET, 0),
        EnvironmentMixRowUiState(EnvironmentMixCategory.MODERATE, 0),
        EnvironmentMixRowUiState(EnvironmentMixCategory.LOUD, 0),
        EnvironmentMixRowUiState(EnvironmentMixCategory.CRITICAL, 0),
    )

private val LOCKED_PREVIEW_ROWS =
    listOf(
        EnvironmentMixRowUiState(EnvironmentMixCategory.QUIET, 52),
        EnvironmentMixRowUiState(EnvironmentMixCategory.MODERATE, 34),
        EnvironmentMixRowUiState(EnvironmentMixCategory.LOUD, 12),
        EnvironmentMixRowUiState(EnvironmentMixCategory.CRITICAL, 2),
    )

@Composable
private fun MixRow(label: String, percent: String, color: Color) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = typography.bodyMd,
                color = colors.material.onSurface,
            )
        }
        Text(
            text = percent,
            style = typography.dataMd,
            color = colors.material.onSurface,
        )
    }
}
