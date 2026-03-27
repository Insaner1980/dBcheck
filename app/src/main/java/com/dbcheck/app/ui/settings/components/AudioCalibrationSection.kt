package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckSlider
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun AudioCalibrationSection(
    sensitivityOffset: Float,
    frequencyWeighting: String,
    isProUser: Boolean,
    onSensitivityChange: (Float) -> Unit,
    onWeightingChange: (String) -> Unit,
    onUpgradeClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "\uD83C\uDF9B AUDIO CALIBRATION",
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        ProLockOverlay(
            isLocked = !isProUser,
            onUpgradeClick = onUpgradeClick,
        ) {
            DbCheckCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Microphone Sensitivity", style = typography.bodyLg, color = colors.material.onSurface)
                        Text(
                            text = "${if (sensitivityOffset >= 0) "+" else ""}${String.format("%.1f", sensitivityOffset)} dB",
                            style = typography.dataMd,
                            color = colors.material.onSurface,
                        )
                    }
                    DbCheckSlider(
                        value = sensitivityOffset,
                        onValueChange = onSensitivityChange,
                        valueRange = -10f..10f,
                    )
                    Text(
                        "Adjust device mic for accuracy",
                        style = typography.bodyMd,
                        color = colors.material.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(20.dp))

                    Text("Frequency Weighting", style = typography.bodyLg, color = colors.material.onSurface)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("A", "C", "Z").forEach { weight ->
                            DbCheckChip(
                                text = "$weight-Weight",
                                selected = frequencyWeighting == weight,
                                onClick = { onWeightingChange(weight) },
                            )
                        }
                    }
                }
            }
        }
    }
}
