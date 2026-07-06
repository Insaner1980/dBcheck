package com.dbcheck.app.ui.history.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.ProUpgradePrompt
import com.dbcheck.app.ui.history.state.HistorySearchFilter
import com.dbcheck.app.ui.theme.DbCheckTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HistorySearchControls(
    state: HistorySearchControlsState,
    actions: HistorySearchControlsActions,
    modifier: Modifier = Modifier,
) {
    val previewQuery = stringResource(R.string.history_search_locked_preview_query)
    val displayQuery = if (state.isLocked && state.searchQuery.isBlank()) previewQuery else state.searchQuery
    val displayFilter =
        if (state.isLocked && state.selectedFilter == HistorySearchFilter.ALL) {
            HistorySearchFilter.LOUD
        } else {
            state.selectedFilter
        }
    val cardState =
        state.copy(
            searchQuery = displayQuery,
            selectedFilter = displayFilter,
        )

    if (state.isLocked) {
        LockedHistorySearchCard(
            state = cardState,
            actions = actions,
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        HistorySearchCard(
            state = cardState,
            actions = actions,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

internal data class HistorySearchControlsState(
    val searchQuery: String,
    val selectedFilter: HistorySearchFilter,
    val isLocked: Boolean,
)

internal data class HistorySearchControlsActions(
    val onSearchQueryChange: (String) -> Unit,
    val onFilterSelect: (HistorySearchFilter) -> Unit,
    val onClearSearch: () -> Unit,
    val onUpgradeClick: () -> Unit,
)

@Composable
private fun LockedHistorySearchCard(
    state: HistorySearchControlsState,
    actions: HistorySearchControlsActions,
    modifier: Modifier = Modifier,
) {
    val colors = DbCheckTheme.colorScheme

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(4.dp)
                } else {
                    Modifier.alpha(0.4f)
                },
        ) {
            HistorySearchCard(
                state = state,
                actions = actions,
            )
        }

        Column(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.material.surface.copy(alpha = 0.62f))
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            ProUpgradePrompt(onUpgradeClick = actions.onUpgradeClick, iconSize = 28.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistorySearchCard(
    state: HistorySearchControlsState,
    actions: HistorySearchControlsActions,
    modifier: Modifier = Modifier,
) {
    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3)) {
            HistorySearchField(
                searchQuery = state.searchQuery,
                isLocked = state.isLocked,
                onSearchQueryChange = actions.onSearchQueryChange,
                onClearSearch = actions.onClearSearch,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistorySearchFilter.entries.forEach { filter ->
                    DbCheckChip(
                        text = stringResource(filter.labelResId),
                        selected = filter == state.selectedFilter,
                        onClick = {
                            if (state.isLocked) {
                                actions.onUpgradeClick()
                            } else {
                                actions.onFilterSelect(filter)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorySearchField(
    searchQuery: String,
    isLocked: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = {
            if (!isLocked) {
                onSearchQueryChange(it)
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLocked,
        readOnly = isLocked,
        singleLine = true,
        label = { Text(stringResource(R.string.history_search_label)) },
        placeholder = { Text(stringResource(R.string.history_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            if (!isLocked && searchQuery.isNotBlank()) {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.a11y_clear_history_search),
                    )
                }
            }
        },
        shape = HistorySearchFieldShape,
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DbCheckTheme.colorScheme.material.primary.copy(alpha = 0.3f),
                unfocusedBorderColor = DbCheckTheme.colorScheme.ghostBorder,
            ),
    )
}

private val HistorySearchFieldShape = RoundedCornerShape(12.dp)

private val HistorySearchFilter.labelResId: Int
    get() =
        when (this) {
            HistorySearchFilter.ALL -> R.string.history_filter_all
            HistorySearchFilter.A_WEIGHTED -> R.string.history_filter_a_weighted
            HistorySearchFilter.LOUD -> R.string.history_filter_loud
            HistorySearchFilter.WITH_LOCATION -> R.string.history_filter_with_location
        }
