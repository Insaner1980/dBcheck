package com.dbcheck.app.ui.history.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dbcheck.app.R
import com.dbcheck.app.domain.session.SessionMetadata
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
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
    var selectedTags by remember { mutableStateOf(SessionMetadata.normalizeTags(currentTags).toSet()) }
    var customTag by remember { mutableStateOf("") }
    val predefinedTags = stringArrayResource(R.array.session_predefined_tags)
    val selectedStateDescription = stringResource(R.string.a11y_selected)
    val notSelectedStateDescription = stringResource(R.string.a11y_not_selected)

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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(R.string.session_name_title),
                style = typography.headlineMd,
                color = colors.material.onSurface,
            )
            Spacer(Modifier.height(16.dp))

            SessionNameField(
                name = name,
                onNameChange = { name = it },
            )

            Spacer(Modifier.height(20.dp))

            SessionEmojiPicker(
                selectedEmoji = selectedEmoji,
                selectedStateDescription = selectedStateDescription,
                notSelectedStateDescription = notSelectedStateDescription,
                onEmojiChange = { selectedEmoji = it },
            )

            Spacer(Modifier.height(20.dp))

            SessionTagPicker(
                predefinedTags = predefinedTags,
                selectedTags = selectedTags,
                onSelectedTagsChange = { selectedTags = it },
            )

            Spacer(Modifier.height(12.dp))

            CustomTagRow(
                customTag = customTag,
                onCustomTagChange = { customTag = it },
                onAddCustomTag = ::addCustomTag,
            )

            Spacer(Modifier.height(24.dp))

            DbCheckButton(
                text = stringResource(R.string.action_save),
                onClick = { onSave(name, selectedEmoji, normalizedTags) },
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp,
                enabled = hasChanges,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SessionNameField(name: String, onNameChange: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.session_name_placeholder)) },
        shape = RoundedCornerShape(12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DbCheckTheme.colorScheme.material.primary.copy(alpha = 0.3f),
                unfocusedBorderColor = DbCheckTheme.colorScheme.ghostBorder,
            ),
        singleLine = true,
    )
}

@Composable
private fun SessionEmojiPicker(
    selectedEmoji: String,
    selectedStateDescription: String,
    notSelectedStateDescription: String,
    onEmojiChange: (String) -> Unit,
) {
    NamingFlowGroup(title = stringResource(R.string.session_name_emoji)) {
        EMOJIS.forEach { emoji ->
            SessionEmojiOption(
                emoji = emoji,
                selectedEmoji = selectedEmoji,
                selectedStateDescription = selectedStateDescription,
                notSelectedStateDescription = notSelectedStateDescription,
                onEmojiChange = onEmojiChange,
            )
        }
    }
}

@Composable
private fun SessionEmojiOption(
    emoji: String,
    selectedEmoji: String,
    selectedStateDescription: String,
    notSelectedStateDescription: String,
    onEmojiChange: (String) -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val isSelected = emoji == selectedEmoji
    val emojiDescription = stringResource(R.string.a11y_session_emoji, emoji)
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) {
                        colors.material.primaryContainer
                    } else {
                        colors.material.surfaceContainerHigh
                    },
                ).semantics {
                    contentDescription = emojiDescription
                    stateDescription =
                        if (isSelected) {
                            selectedStateDescription
                        } else {
                            notSelectedStateDescription
                        }
                }.selectable(
                    selected = isSelected,
                    role = Role.RadioButton,
                    onClick = { onEmojiChange(emoji) },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            style = DbCheckTheme.typography.headlineMd,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SessionTagPicker(
    predefinedTags: Array<String>,
    selectedTags: Set<String>,
    onSelectedTagsChange: (Set<String>) -> Unit,
) {
    NamingFlowGroup(title = stringResource(R.string.session_name_tags)) {
        predefinedTags.forEach { tag ->
            DbCheckChip(
                text = tag,
                selected = tag in selectedTags,
                onClick = { onSelectedTagsChange(selectedTags.toggleTag(tag)) },
            )
        }
    }
}

private fun Set<String>.toggleTag(tag: String): Set<String> = if (tag in this) {
        this - tag
    } else {
        this + tag
    }

@Composable
private fun CustomTagRow(customTag: String, onCustomTagChange: (String) -> Unit, onAddCustomTag: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = customTag,
            onValueChange = onCustomTagChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.session_name_custom_tag_placeholder)) },
            shape = RoundedCornerShape(12.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DbCheckTheme.colorScheme.material.primary.copy(alpha = 0.3f),
                    unfocusedBorderColor = DbCheckTheme.colorScheme.ghostBorder,
                ),
            singleLine = true,
        )
        DbCheckButton(
            text = stringResource(R.string.action_add),
            onClick = onAddCustomTag,
            style = DbCheckButtonStyle.Secondary,
            height = 56.dp,
            enabled = customTag.isNotBlank(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NamingFlowGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title,
            style = DbCheckTheme.typography.labelLg,
            color = DbCheckTheme.colorScheme.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            content()
        }
    }
}
