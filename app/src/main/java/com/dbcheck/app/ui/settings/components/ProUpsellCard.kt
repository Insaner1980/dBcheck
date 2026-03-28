package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun ProUpsellCard(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                text = "Upgrade to Pro",
                onClick = onUpgradeClick,
                height = 48.dp,
            )
        }
    }
}
