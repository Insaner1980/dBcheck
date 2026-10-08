package com.dbcheck.app.ui.components

sealed interface DbCheckTopAppBarModel {
    val title: String

    data class TopLevel(override val title: String) : DbCheckTopAppBarModel

    data class Pushed(override val title: String, val onBackClick: () -> Unit) : DbCheckTopAppBarModel
}
