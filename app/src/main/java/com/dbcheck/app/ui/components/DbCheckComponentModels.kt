package com.dbcheck.app.ui.components

import androidx.compose.ui.graphics.vector.ImageVector

data class DbCheckLockedCtaContent(
    val title: String,
    val subtitle: String,
    val buttonText: String,
)

data class DbCheckSliderLabels(
    val value: String,
    val min: String,
    val max: String,
)

data class EmptyStateContent(
    val icon: ImageVector,
    val title: String,
    val description: String,
)
