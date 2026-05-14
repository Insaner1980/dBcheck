package com.dbcheck.app

import androidx.lifecycle.ViewModel

fun ViewModel.clearForTest() {
    ViewModel::class.java
        .getDeclaredMethod("clear\$lifecycle_viewmodel_release")
        .invoke(this)
}
