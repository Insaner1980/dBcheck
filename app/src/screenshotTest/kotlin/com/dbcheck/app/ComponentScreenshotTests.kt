package com.dbcheck.app

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.dbcheck.app.ui.components.DbCheckButton
import com.dbcheck.app.ui.components.DbCheckButtonStyle
import com.dbcheck.app.ui.components.DbCheckCard
import com.dbcheck.app.ui.theme.DbCheckTheme

@PreviewTest
@Preview(showBackground = true, widthDp = 360)
@Composable
fun ButtonStylesPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DbCheckButton(text = "Primary", onClick = {}, style = DbCheckButtonStyle.Primary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Secondary", onClick = {}, style = DbCheckButtonStyle.Secondary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Tertiary", onClick = {}, style = DbCheckButtonStyle.Tertiary)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ButtonStylesDarkPreview() {
    DbCheckTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DbCheckButton(text = "Primary", onClick = {}, style = DbCheckButtonStyle.Primary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Secondary", onClick = {}, style = DbCheckButtonStyle.Secondary)
            Spacer(modifier = Modifier.height(8.dp))
            DbCheckButton(text = "Tertiary", onClick = {}, style = DbCheckButtonStyle.Tertiary)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300)
@Composable
fun CardPreview() {
    DbCheckTheme {
        DbCheckCard(modifier = Modifier.width(280.dp)) {
            Text(
                text = "42.5 dB",
                style = DbCheckTheme.typography.displayLg,
                color = DbCheckTheme.colorScheme.onSurface,
            )
        }
    }
}

@PreviewTest
@Preview(showBackground = true, widthDp = 300, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CardDarkPreview() {
    DbCheckTheme {
        DbCheckCard(modifier = Modifier.width(280.dp)) {
            Text(
                text = "42.5 dB",
                style = DbCheckTheme.typography.displayLg,
                color = DbCheckTheme.colorScheme.onSurface,
            )
        }
    }
}
