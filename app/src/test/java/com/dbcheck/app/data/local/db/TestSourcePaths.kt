package com.dbcheck.app.data.local.db

import java.nio.file.Path

internal fun mainSource(relativePath: String): Path =
    Path.of("src", "main", "java", "com", "dbcheck", "app", *relativePath.split("/").toTypedArray())
