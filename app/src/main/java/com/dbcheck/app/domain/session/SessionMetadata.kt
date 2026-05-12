package com.dbcheck.app.domain.session

import java.text.Normalizer
import java.util.Locale

object SessionMetadata {
    val PREDEFINED_TAGS =
        listOf(
            "Work",
            "Commute",
            "Sleep",
            "Leisure",
            "Music",
            "Exercise",
            "Concert",
            "Home",
        )

    fun normalizeName(name: String?): String? =
        name
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    fun normalizeEmoji(emoji: String?): String? =
        emoji
            ?.trim()
            ?.takeIf { it.isNotBlank() }

    fun normalizeTags(tags: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        return tags
            .asSequence()
            .map { tag ->
                tag
                    .replace(",", " ")
                    .trim()
                    .replace(Regex("\\s+"), " ")
                    .take(MAX_TAG_LENGTH)
                    .trim()
            }.filter { it.isNotBlank() }
            .filter { tag -> seen.add(tag.lowercase(Locale.US)) }
            .take(MAX_TAGS)
            .toList()
    }

    fun serializeTags(tags: List<String>): String? =
        normalizeTags(tags)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = ",")

    fun parseTags(tags: String?): List<String> =
        tags
            ?.split(",")
            ?.let(::normalizeTags)
            ?: emptyList()

    fun slugify(name: String?): String {
        val normalized =
            Normalizer
                .normalize(normalizeName(name).orEmpty(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
                .lowercase(Locale.US)
        val slug =
            normalized
                .replace(Regex("[^\\p{L}\\p{Nd}]+"), "-")
                .trim('-')
        return slug.takeIf { it.isNotBlank() } ?: "session"
    }

    private const val MAX_TAGS = 6
    private const val MAX_TAG_LENGTH = 24
}
