package com.dbcheck.app

import androidx.lifecycle.ViewModel

fun ViewModel.clearForTest() {
    val clearMethod =
        listOf(
            "clear\$lifecycle_viewmodel",
            "clear\$lifecycle_viewmodel_release",
        ).firstNotNullOfOrNull { methodName ->
            runCatching { ViewModel::class.java.getDeclaredMethod(methodName) }.getOrNull()
        } ?: error("Unable to find ViewModel clear method")

    clearMethod
        .apply { isAccessible = true }
        .invoke(this)
}
