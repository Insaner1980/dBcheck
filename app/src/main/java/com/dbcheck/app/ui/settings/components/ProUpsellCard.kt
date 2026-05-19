package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckToggle
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun ProUpsellCard(state: ProUpsellCardState, actions: ProUpsellCardActions, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    DbCheckCard(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    brush = colors.signatureGradient,
                    shape = RoundedCornerShape(24.dp),
                ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.pro_upsell_title),
                style = typography.headlineMd,
                color = colors.material.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.pro_upsell_subtitle),
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            DbCheckButton(
                text =
                    if (state.isPurchaseLaunching) {
                        stringResource(R.string.billing_opening_google_play)
                    } else {
                        stringResource(R.string.action_upgrade_to_pro)
                    },
                onClick = actions.onUpgradeClick,
                enabled = !state.isPurchaseLaunching,
                height = 48.dp,
            )
            ProUpsellMessages(state)
            if (state.showDebugForceFree) {
                ProUpsellDebugForceFreeRow(state, actions)
            }
        }
    }
}

@Composable
private fun ProUpsellMessages(state: ProUpsellCardState) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column {
        state.purchaseMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = typography.bodyMd,
                color = colors.success,
            )
        }
        state.purchaseErrorMessage?.let { message ->
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                style = typography.bodyMd,
                color = colors.material.error,
            )
        }
    }
}

@Composable
private fun ProUpsellDebugForceFreeRow(state: ProUpsellCardState, actions: ProUpsellCardActions) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Column {
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.pro_debug_force_free_title),
                    style = typography.bodyLg,
                    color = colors.material.onSurface,
                )
                Text(
                    text = stringResource(R.string.pro_debug_force_free_description),
                    style = typography.bodyMd,
                    color = colors.material.onSurfaceVariant,
                )
            }
            DbCheckToggle(
                checked = state.debugForceFreeEnabled,
                onCheckedChange = actions.onDebugForceFreeChange,
            )
        }
    }
}
