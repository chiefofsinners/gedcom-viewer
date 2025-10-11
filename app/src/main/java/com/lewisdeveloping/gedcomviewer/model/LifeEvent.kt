package com.lewisdeveloping.gedcomviewer.model

data class LifeEvent(
    val date: String? = null,
    val place: String? = null,
    val address: String? = null,
    val value: String? = null,
    val details: Map<String, List<String>> = emptyMap(),
    val notes: List<String> = emptyList()
) {
    fun description(): String? {
        val parts = listOfNotNull(
            date?.takeIf { it.isNotBlank() },
            place?.takeIf { it.isNotBlank() },
            address?.takeIf { it.isNotBlank() }
        )
        return if (parts.isEmpty()) null else parts.joinToString(" • ")
    }
}
