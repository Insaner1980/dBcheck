package com.dbcheck.app.ui.hearing.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun AmbientSoundCard(
    isLocked: Boolean,
    onOpenAmbientSound: () -> Unit,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space3),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.ambient_sound_card_title),
                    style = DbCheckTheme.typography.labelMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.ambient_sound_card_description),
                    style = DbCheckTheme.typography.bodyMd,
                    color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
                )
                DbCheckButton(
                    text = stringResource(R.string.ambient_sound_open),
                    onClick = onOpenAmbientSound,
                    modifier = Modifier.fillMaxWidth(),
                    style = DbCheckButtonStyle.Secondary,
                    height = 48.dp,
                )
            }
        }
    }
}
