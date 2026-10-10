package com.dbcheck.app.ui.components

import androidx.compose.runtime.Immutable

@Immutable
sealed interface DbCheckTopAppBarModel {
    val title: String

    data class TopLevel(override val title: String) : DbCheckTopAppBarModel

    data class Pushed(override val title: String, val onBackClick: () -> Unit) : DbCheckTopAppBarModel
}
