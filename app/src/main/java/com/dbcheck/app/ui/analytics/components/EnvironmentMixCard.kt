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
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun EnvironmentMixCard(
    isLocked: Boolean,
    onUpgradeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    ProLockOverlay(
        isLocked = isLocked,
        onUpgradeClick = onUpgradeClick,
        modifier = modifier,
    ) {
        DbCheckCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ENVIRONMENT MIX",
                    style = typography.labelMd,
                    color = colors.material.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                // Placeholder data matching spec wireframe
                val categories = listOf(
                    Triple("Quiet", "52%", colors.success),
                    Triple("Moderate", "34%", colors.material.primary),
                    Triple("Loud", "12%", colors.warning),
                    Triple("Critical", "2%", colors.material.error),
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    categories.forEach { (label, percent, color) ->
                        MixRow(label = label, percent = percent, color = color)
                    }
                }
            }
        }
    }
}

@Composable
private fun MixRow(
    label: String,
    percent: String,
    color: Color,
) {
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
                modifier = Modifier
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
