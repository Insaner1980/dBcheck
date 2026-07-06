package com.dbcheck.app

import java.io.File
import java.util.Locale

internal fun projectFile(path: String): File = requireNotNull(optionalProjectFile(path)) {
    "Project file does not exist: $path"
}

internal fun projectSourcesLowercase(vararg paths: String): String = paths
    .joinToString(separator = "\n") { path -> optionalProjectFile(path)?.readText().orEmpty() }
    .lowercase()

private fun optionalProjectFile(path: String): File? = listOf(
        File(path),
        File("app", path),
    ).firstOrNull(File::isFile)

internal inline fun <T> withDefaultLocale(locale: Locale, block: () -> T): T {
    val original = Locale.getDefault()
    Locale.setDefault(locale)
    return try {
        block()
    } finally {
        Locale.setDefault(original)
    }
}
