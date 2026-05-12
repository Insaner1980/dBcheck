package com.dbcheck.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UserFacingErrorTest {
    @Test
    fun throwableMessageIsNotUsedAsUserFacingError() {
        val error = IllegalStateException("C:\\Users\\emma\\AppData\\dbcheck.db-wal")

        assertEquals(
            "Backup failed",
            error.toUserFacingMessage(fallback = "Backup failed"),
        )
    }
}
