package com.dbcheck.app.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import com.dbcheck.app.domain.session.SessionMetadata
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonDefaults
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckChip
import com.dbcheck.app.ui.theme.DbCheckOpacity
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
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography
    val sheetState = rememberModalBottomSheetState()

    var name by remember { mutableStateOf(currentName) }
    var selectedEmoji by remember { mutableStateOf(currentEmoji) }
    var selectedTags by remember { mutableStateOf(SessionMetadata.normalizeTags(currentTags).toSet()) }
    var customTag by remember { mutableStateOf("") }

    val normalizedName = SessionMetadata.normalizeName(name)
    val normalizedEmoji = SessionMetadata.normalizeEmoji(selectedEmoji)
    val normalizedTags = SessionMetadata.normalizeTags(selectedTags.toList())
    val hasChanges =
        normalizedName != SessionMetadata.normalizeName(currentName) ||
            normalizedEmoji != SessionMetadata.normalizeEmoji(currentEmoji) ||
            normalizedTags != SessionMetadata.normalizeTags(currentTags)

    fun addCustomTag() {
        val tag = SessionMetadata.normalizeTags(listOf(customTag)).firstOrNull() ?: return
        selectedTags = SessionMetadata.normalizeTags(selectedTags.toList() + tag).toSet()
        customTag = ""
    }

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
                    .padding(horizontal = spacing.space5, vertical = spacing.space2),
        ) {
            Text("Name Session", style = typography.headlineMd, color = colors.material.onSurface)
            Spacer(Modifier.height(spacing.space4))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Session name") },
                shape = DbCheckTheme.shapes.medium,
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.material.primary.copy(alpha = DbCheckOpacity.FOCUSED_BORDER),
                        unfocusedBorderColor = colors.ghostBorder,
                    ),
                singleLine = true,
            )

            Spacer(Modifier.height(spacing.space5))

            LabeledFlowRow(label = "Emoji") {
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

            Spacer(Modifier.height(spacing.space5))

            LabeledFlowRow(label = "Tags") {
                SessionMetadata.PREDEFINED_TAGS.forEach { tag ->
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

            Spacer(Modifier.height(spacing.space3))

            Row(horizontalArrangement = Arrangement.spacedBy(spacing.space2)) {
                OutlinedTextField(
                    value = customTag,
                    onValueChange = { customTag = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Custom tag") },
                    shape = DbCheckTheme.shapes.medium,
                    colors =
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.material.primary.copy(alpha = DbCheckOpacity.FOCUSED_BORDER),
                            unfocusedBorderColor = colors.ghostBorder,
                        ),
                    singleLine = true,
                )
                DbCheckButton(
                    text = "Add",
                    onClick = ::addCustomTag,
                    style = DbCheckButtonStyle.Secondary,
                    enabled = customTag.isNotBlank(),
                )
            }

            Spacer(Modifier.height(spacing.space6))

            DbCheckButton(
                text = "Save",
                onClick = { onSave(name, selectedEmoji, normalizedTags) },
                modifier = Modifier.fillMaxWidth(),
                height = DbCheckButtonDefaults.CompactHeight,
                enabled = hasChanges,
            )

            Spacer(Modifier.height(spacing.space8))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LabeledFlowRow(
    label: String,
    content: @Composable () -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing
    val typography = DbCheckTheme.typography

    Text(label, style = typography.labelLg, color = colors.material.onSurfaceVariant)
    Spacer(Modifier.height(spacing.space2))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        content()
    }
}
