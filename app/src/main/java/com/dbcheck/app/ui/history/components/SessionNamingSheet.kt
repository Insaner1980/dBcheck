package com.dbcheck.app.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.theme.DbCheckTheme

private val EMOJIS =
    listOf(
        "\uD83C\uDF19",
        "\u2615",
        "\uD83C\uDFA7",
        "\uD83D\uDE87",
        "\uD83C\uDFB5",
        "\uD83D\uDCBB",
        "\uD83C\uDFCB",
        "\uD83C\uDF33",
        "\uD83C\uDFE0",
        "\uD83D\uDE97",
        "\uD83C\uDF73",
        "\uD83D\uDCD6",
    )

private val TAGS = listOf("Work", "Commute", "Sleep", "Leisure", "Music", "Exercise")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SessionNamingSheet(
    currentName: String,
    currentEmoji: String,
    currentTags: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, tags: List<String>) -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val sheetState = rememberModalBottomSheetState()

    var name by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }
    var selectedTags by remember { mutableStateOf(currentTags.toSet()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.material.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("Name Session", style = typography.headlineMd, color = colors.material.onSurface)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Session name") },
                shape = RoundedCornerShape(12.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.material.primary.copy(alpha = 0.3f),
                        unfocusedBorderColor = colors.ghostBorder,
                    ),
                singleLine = true,
            )

            Spacer(Modifier.height(20.dp))

            Text("Emoji", style = typography.labelLg, color = colors.material.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                EMOJIS.forEach { emoji ->
                    Box(
                        modifier =
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (emoji == selectedEmoji) {
                                        colors.material.primaryContainer
                                    } else {
                                        colors.material.surfaceContainerHigh
                                    },
                                ).clickable { selectedEmoji = emoji },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(emoji, style = typography.headlineMd, textAlign = TextAlign.Center)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            Text("Tags", style = typography.labelLg, color = colors.material.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TAGS.forEach { tag ->
                    DbCheckChip(
                        text = tag,
                        selected = tag in selectedTags,
                        onClick = {
                            selectedTags =
                                if (tag in selectedTags) {
                                    selectedTags - tag
                                } else {
                                    selectedTags + tag
                                }
                        },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            DbCheckButton(
                text = "Save",
                onClick = { onSave(name, selectedEmoji, selectedTags.toList()) },
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}
