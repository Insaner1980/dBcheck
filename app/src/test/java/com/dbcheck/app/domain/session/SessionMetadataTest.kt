package com.dbcheck.app.domain.session

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionMetadataTest {
    @Test
    fun normalizeNameTrimsWhitespaceAndStoresBlankAsNull() {
        assertEquals("Workshop", SessionMetadata.normalizeName("  Workshop  "))
        assertNull(SessionMetadata.normalizeName("   "))
    }

    @Test
    fun normalizeNameLimitsStoredNameLength() {
        val longName = "A".repeat(80)

        assertEquals("A".repeat(48), SessionMetadata.normalizeName(longName))
    }

    @Test
    fun normalizeTagsCleansCommasLimitsLengthAndRemovesDuplicatesCaseInsensitively() {
        val tags =
            SessionMetadata.normalizeTags(
                listOf(
                    " Work ",
                    "work",
                    "Concert, hall",
                    "This tag name is much too long for storage",
                    "",
                    "Commute",
                    "Music",
                    "Exercise",
                    "Sleep",
                    "Leisure",
                ),
            )

        assertEquals(
            listOf(
                "Work",
                "Concert hall",
                "This tag name is much to",
                "Commute",
                "Music",
                "Exercise",
            ),
            tags,
        )
    }

    @Test
    fun serializeAndParseTagsRoundTripNormalizedTags() {
        val stored = SessionMetadata.serializeTags(listOf(" Work ", "Music", "work"))

        assertEquals("Work,Music", stored)
        assertEquals(listOf("Work", "Music"), SessionMetadata.parseTags(stored))
        assertEquals(emptyList<String>(), SessionMetadata.parseTags(null))
    }

    @Test
    fun slugifyUsesSafeFallbackForBlankNames() {
        assertEquals("workshop-main-hall", SessionMetadata.slugify("Workshop / Main Hall"))
        assertEquals("session", SessionMetadata.slugify("   "))
    }
}
