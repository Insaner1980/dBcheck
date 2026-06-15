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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
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
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.history.state.HistorySearchFilter
import com.dbcheck.app.ui.theme.DbCheckTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistorySearchControls(
    searchQuery: String,
    selectedFilter: HistorySearchFilter,
    isLocked: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (HistorySearchFilter) -> Unit,
    onClearSearch: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previewQuery = stringResource(R.string.history_search_locked_preview_query)
    val displayQuery = if (isLocked && searchQuery.isBlank()) previewQuery else searchQuery
    val displayFilter =
        if (isLocked && selectedFilter == HistorySearchFilter.ALL) {
            HistorySearchFilter.LOUD
        } else {
            selectedFilter
        }

    if (isLocked) {
        LockedHistorySearchCard(
            searchQuery = displayQuery,
            selectedFilter = displayFilter,
            onSearchQueryChange = onSearchQueryChange,
            onFilterSelect = onFilterSelect,
            onClearSearch = onClearSearch,
            onUpgradeClick = onUpgradeClick,
            modifier = modifier.fillMaxWidth(),
        )
    } else {
        HistorySearchCard(
            searchQuery = displayQuery,
            selectedFilter = displayFilter,
            isLocked = false,
            onSearchQueryChange = onSearchQueryChange,
            onFilterSelect = onFilterSelect,
            onClearSearch = onClearSearch,
            onUpgradeClick = onUpgradeClick,
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LockedHistorySearchCard(
    searchQuery: String,
    selectedFilter: HistorySearchFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (HistorySearchFilter) -> Unit,
    onClearSearch: () -> Unit,
    onUpgradeClick: () -> Unit,
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
                searchQuery = searchQuery,
                selectedFilter = selectedFilter,
                isLocked = true,
                onSearchQueryChange = onSearchQueryChange,
                onFilterSelect = onFilterSelect,
                onClearSearch = onClearSearch,
                onUpgradeClick = onUpgradeClick,
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
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = colors.material.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = stringResource(R.string.pro_lock_title),
                style = DbCheckTheme.typography.bodyMd,
                color = colors.material.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            DbCheckButton(
                text = stringResource(R.string.action_upgrade),
                onClick = onUpgradeClick,
                style = DbCheckButtonStyle.Primary,
                height = 48.dp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HistorySearchCard(
    searchQuery: String,
    selectedFilter: HistorySearchFilter,
    isLocked: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (HistorySearchFilter) -> Unit,
    onClearSearch: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DbCheckCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3)) {
            HistorySearchField(
                searchQuery = searchQuery,
                isLocked = isLocked,
                onSearchQueryChange = onSearchQueryChange,
                onClearSearch = onClearSearch,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistorySearchFilter.entries.forEach { filter ->
                    DbCheckChip(
                        text = stringResource(filter.labelResId),
                        selected = filter == selectedFilter,
                        onClick = {
                            if (isLocked) {
                                onUpgradeClick()
                            } else {
                                onFilterSelect(filter)
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
