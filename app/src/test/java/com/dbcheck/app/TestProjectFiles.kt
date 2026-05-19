package com.dbcheck.app

import java.io.File
import java.util.Locale

internal fun projectFile(path: String): File = listOf(
        File(path),
        File("app", path),
    ).first(File::isFile)

internal inline fun <T> withDefaultLocale(locale: Locale, block: () -> T): T {
    val original = Locale.getDefault()
    Locale.setDefault(locale)
    return try {
        block()
    } finally {
        Locale.setDefault(original)
    }
}
