package com.dbcheck.app.ui.hearingtest.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckSetupHeader
import com.dbcheck.app.ui.components.DbCheckSetupScaffold
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingTestSetupScreen(onStartTest: () -> Unit, onBack: () -> Unit) {
    val spacing = DbCheckTheme.spacing

    DbCheckSetupScaffold(
        onBack = onBack,
        header = {
            DbCheckSetupHeader(
                phase = stringResource(R.string.hearing_setup_phase),
                title = stringResource(R.string.hearing_setup_ready),
                description = stringResource(R.string.hearing_setup_description),
            )
        },
        cta = {
            DbCheckButton(
                text = stringResource(R.string.action_start_test),
                onClick = onStartTest,
                modifier = Modifier.fillMaxWidth(),
                height = spacing.space12,
            )
        },
    ) {
        HearingSetupChecklist(
            secondIcon = Icons.Filled.GraphicEq,
            secondTitle = stringResource(R.string.hearing_setup_find_silence_title),
            secondDescription = stringResource(R.string.hearing_setup_find_silence_description),
        )
    }
}

@Composable
internal fun HearingSetupChecklist(secondIcon: ImageVector, secondTitle: String, secondDescription: String) {
    val spacing = DbCheckTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space4),
    ) {
        ChecklistItem(
            icon = Icons.Outlined.Headphones,
            title = stringResource(R.string.hearing_setup_use_headphones_title),
            description = stringResource(R.string.hearing_setup_use_headphones_description),
        )

        ChecklistItem(
            icon = secondIcon,
            title = secondTitle,
            description = secondDescription,
        )
    }
}

@Composable
internal fun ChecklistItem(icon: ImageVector, title: String, description: String) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        horizontalArrangement = Arrangement.spacedBy(DbCheckTheme.spacing.space4),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .size(DbCheckTheme.spacing.iconCircle)
                    .clip(CircleShape)
                    .background(colors.material.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.material.primary,
                modifier = Modifier.size(DbCheckTheme.spacing.space6),
            )
        }
        Column {
            Text(text = title, style = typography.bodyLg, color = colors.material.onSurface)
            Spacer(Modifier.height(DbCheckTheme.spacing.space1))
            Text(text = description, style = typography.bodyMd, color = colors.material.onSurfaceVariant)
        }
    }
}
