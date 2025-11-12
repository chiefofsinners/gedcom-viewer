package com.lewisdeveloping.gedcomviewer.data

import java.util.UUID

internal class IdentifierScope(rawSourceIdentifier: String?) {
    val sourceId: String = normalize(rawSourceIdentifier) ?: fallback()

    fun scoped(raw: String?): String? = raw?.let { "$sourceId::$it" }

    private fun normalize(raw: String?): String? {
        val trimmed = raw
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        val sanitized = trimmed
            .replace("::", "__")
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')

        return sanitized.takeIf { it.isNotEmpty() }
    }

    private fun fallback(): String = "src-${UUID.randomUUID()}"
}
