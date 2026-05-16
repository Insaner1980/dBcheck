package com.dbcheck.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckErrorMessage(
    text: String,
    horizontalPadding: Dp,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = DbCheckTheme.typography.bodyMd,
        color = DbCheckTheme.colorScheme.material.error,
        textAlign = TextAlign.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
    )
}
