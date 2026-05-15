package com.dbcheck.app.ui.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dbcheck.app.data.local.preferences.model.UserPreferenceDefaults
import com.dbcheck.app.domain.audio.WeightingType
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.components.DbCheckSlider
import com.dbcheck.app.ui.components.ProLockOverlay
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.util.Locale

@Composable
fun AudioCalibrationSection(
    sensitivityOffset: Float,
    frequencyWeighting: String,
    isProUser: Boolean,
    onSensitivityChange: (Float) -> Unit,
    onWeightingChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onUpgradeClick: () -> Unit = {},
) {
    val typography = DbCheckTheme.typography
    val colors = DbCheckTheme.colorScheme
    val sensitivityMin = UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET_MIN
    val sensitivityMax = UserPreferenceDefaults.MIC_SENSITIVITY_OFFSET_MAX
    val sensitivityRange = sensitivityMin..sensitivityMax

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
                            text =
                                "${if (sensitivityOffset >= 0) "+" else ""}" +
                                    "${String.format(Locale.getDefault(), "%.1f", sensitivityOffset)} dB",
                            style = typography.dataMd,
                            color = colors.material.onSurface,
                        )
                    }
                    DbCheckSlider(
                        value = sensitivityOffset,
                        onValueChange = onSensitivityChange,
                        valueRange = sensitivityRange,
                    )
                    Text(
                        "Adjust device mic for accuracy",
                        style = typography.bodyMd,
                        color = colors.material.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(20.dp))

                    Text("Frequency Weighting", style = typography.bodyLg, color = colors.material.onSurface)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        WeightingType.entries.forEach { weight ->
                            DbCheckChip(
                                text = weight.displayName,
                                selected = frequencyWeighting == weight.name,
                                onClick = { onWeightingChange(weight.name) },
                            )
                        }
                    }
                }
            }
        }
    }
}
