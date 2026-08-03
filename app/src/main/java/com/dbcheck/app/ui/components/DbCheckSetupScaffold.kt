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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dbcheck.app.ui.theme.DbCheckTheme

@Composable
fun DbCheckSetupScaffold(
    title: String,
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
        DbCheckTopAppBar(
            model = DbCheckTopAppBarModel.Pushed(title = title, onBackClick = onBack),
        )

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
fun DbCheckSetupHeader(phase: String, description: String, modifier: Modifier = Modifier) {
    val colors = DbCheckTheme.colorScheme
    val typography = DbCheckTheme.typography
    val spacing = DbCheckTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = phase,
            style = typography.labelMd,
            color = colors.material.onSurfaceVariant,
        )
        Spacer(Modifier.height(spacing.space3))
        Text(
            text = description,
            style = typography.bodyLg,
            color = colors.material.onSurfaceVariant,
        )
    }
}
