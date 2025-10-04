package com.lewisdeveloping.gedcomviewer.model

data class LifeEvent(
    val date: String? = null,
    val place: String? = null
) {
    fun description(): String? {
        val parts = listOfNotNull(date?.takeIf { it.isNotBlank() }, place?.takeIf { it.isNotBlank() })
        return if (parts.isEmpty()) null else parts.joinToString(" • ")
    }
}
