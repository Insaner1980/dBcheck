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
import androidx.compose.ui.unit.dp
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
                text = "Unlock All Features",
                style = typography.headlineMd,
                color = colors.material.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "One-time purchase · No subscription",
                style = typography.bodyMd,
                color = colors.material.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            DbCheckButton(
                text = if (state.isPurchaseLaunching) "Opening Google Play..." else "Upgrade to Pro",
                onClick = actions.onUpgradeClick,
                enabled = !state.isPurchaseLaunching,
                height = 48.dp,
            )
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
            if (state.showDebugForceFree) {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Debug: Force Free mode",
                            style = typography.bodyLg,
                            color = colors.material.onSurface,
                        )
                        Text(
                            text = "Show Pro locks in debug builds",
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
    }
}
