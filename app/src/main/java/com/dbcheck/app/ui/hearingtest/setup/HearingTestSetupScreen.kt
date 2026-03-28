package com.dbcheck.app.ui.hearingtest.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun HearingTestSetupScreen(
    onStartTest: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.material.background)
                .verticalScroll(rememberScrollState()),
    ) {
        // Top bar with back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(12.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = colors.material.onSurface,
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "PHASE 01: SETUP",
                style = typography.labelMd,
                color = colors.material.primary,
            )
            Spacer(Modifier.height(spacing.space2))
            Text(
                text = "Ready to start?",
                style = typography.headlineLg,
                color = colors.material.onSurface,
            )
            Spacer(Modifier.height(spacing.space3))
            Text(
                text = "Please ensure you are in a controlled environment for accurate results.",
                style = typography.bodyLg,
                color = colors.material.onSurfaceVariant,
            )

            Spacer(Modifier.height(spacing.space8))

            // Checklist items
            ChecklistItem(
                icon = Icons.Outlined.Headphones,
                title = "Use Headphones",
                description = "Use wired or high-quality in-ear buds. Avoid using phone speakers.",
            )

            Spacer(Modifier.height(spacing.space4))

            ChecklistItem(
                icon = Icons.Filled.GraphicEq,
                title = "Find Silence",
                description = "The test requires a room noise floor under 50dB for precision.",
            )

            Spacer(Modifier.height(spacing.space16))

            DbCheckButton(
                text = "Start Test",
                onClick = onStartTest,
                modifier = Modifier.fillMaxWidth(),
                height = 56.dp,
            )

            Spacer(Modifier.height(spacing.space8))
        }
    }
}

@Composable
private fun ChecklistItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.material.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.material.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Column {
            Text(text = title, style = typography.bodyLg, color = colors.material.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(text = description, style = typography.bodyMd, color = colors.material.onSurfaceVariant)
        }
    }
}
