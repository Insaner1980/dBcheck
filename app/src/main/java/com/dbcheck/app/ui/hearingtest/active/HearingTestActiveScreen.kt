package com.dbcheck.app.ui.hearingtest.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.shouldUseCompactHeightScrolling
import com.dbcheck.app.ui.theme.DbCheckTheme
import java.util.Locale

@Composable
fun HearingTestActiveScreen(
    onTestComplete: (Long) -> Unit,
    viewModel: ActiveTestViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    LaunchedEffect(Unit) {
        viewModel.startTest()
    }

    LaunchedEffect(state.completedTestId) {
        state.completedTestId?.let(onTestComplete)
    }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.material.background),
    ) {
        val useScrollableContent = shouldUseCompactHeightScrolling(maxHeight.value)
        val scrollState = rememberScrollState()
        val phaseLabel =
            "PHASE ${String.format(Locale.getDefault(), "%02d", state.currentPhase)} OF ${state.totalPhases}"
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (useScrollableContent) {
                            Modifier.verticalScroll(scrollState)
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(spacing.space10))

            // Phase indicator
            Text(
                text = phaseLabel,
                style = typography.labelMd,
                color = colors.material.onSurfaceVariant,
            )

            Spacer(Modifier.height(spacing.space3))

            // Progress bar
            LinearProgressIndicator(
                progress = { state.currentPhase.toFloat() / state.totalPhases },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(8.dp)),
                color = colors.material.primary,
                trackColor = colors.material.surfaceContainerHigh,
            )

            Spacer(Modifier.height(spacing.space4))

            // Ear indicator
            Text(
                text = state.currentEar.label,
                style = typography.labelLg,
                color = colors.material.primary,
            )

            Spacer(Modifier.height(spacing.space16))

            // Frequency display in circle
            Box(
                modifier =
                    Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(colors.material.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "TESTING",
                        style = typography.labelMd,
                        color = colors.material.onSurfaceVariant,
                    )
                    Text(
                        text = "${state.currentFrequency.toInt()} Hz",
                        style = typography.displayMd,
                        color = colors.material.onSurface,
                    )
                }
            }

            Spacer(Modifier.height(spacing.space8))

            Text(
                text =
                    "Focus on the subtle tones. Tap as soon as you detect the sound " +
                        "in your ${state.currentEar.name.lowercase()} ear.",
                style = typography.bodyLg,
                color = colors.material.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            if (useScrollableContent) {
                Spacer(Modifier.height(spacing.space8))
            } else {
                Spacer(Modifier.weight(1f))
            }

            state.errorMessage?.let { error ->
                Text(
                    text = error,
                    style = typography.bodyMd,
                    color = colors.material.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(spacing.space3))
            }

            if (state.canRetrySave) {
                DbCheckButton(
                    text = "Try Again",
                    onClick = viewModel::retrySaveResult,
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
                Spacer(Modifier.height(spacing.space3))
            }

            // Response buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.space3),
            ) {
                DbCheckButton(
                    text = "I Hear It",
                    onClick = viewModel::onHeard,
                    enabled = !state.isSavingResult && !state.isLocked && !state.isComplete,
                    modifier = Modifier.fillMaxWidth(),
                    height = 56.dp,
                )
                DbCheckButton(
                    text = "I Don't Hear It",
                    onClick = viewModel::onNotHeard,
                    enabled = !state.isSavingResult && !state.isLocked && !state.isComplete,
                    modifier = Modifier.fillMaxWidth(),
                    style = DbCheckButtonStyle.Secondary,
                    height = 56.dp,
                )
            }

            Spacer(Modifier.height(spacing.space8))
        }
    }
}
