package com.dbcheck.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dbcheck.app.R
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckSetupScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentVerticalArrangement: Arrangement.Vertical = Arrangement.Top,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    cta: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = DbCheckTheme.colorScheme
    val spacing = DbCheckTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.material.background)
                .verticalScroll(rememberScrollState()),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(spacing.space3),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.a11y_back),
                tint = colors.material.onSurface,
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.pageMargin),
        ) {
            header?.invoke(this)
            if (header != null) {
                Spacer(Modifier.height(spacing.sectionGap))
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = contentVerticalArrangement,
                content = content,
            )
            cta?.let { ctaContent ->
                Spacer(Modifier.height(spacing.sectionGap))
                ctaContent()
            }
            Spacer(Modifier.height(spacing.space8))
        }
    }
}

@Composable
fun DbCheckSetupHeader(phase: String, title: String, description: String, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = phase,
            style = typography.labelMd,
            color = colors.material.primary,
        )
        Spacer(Modifier.height(spacing.space2))
        Text(
            text = title,
            style = typography.headlineLg,
            color = colors.material.onSurface,
        )
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = description,
            style = typography.bodyLg,
            color = colors.material.onSurfaceVariant,
        )
    }
}
