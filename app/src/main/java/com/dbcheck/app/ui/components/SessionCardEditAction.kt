package com.dbcheck.app.ui.components

data class SessionCardEditAction(
    val isLocked: Boolean,
    val onClick: () -> Unit,
)
