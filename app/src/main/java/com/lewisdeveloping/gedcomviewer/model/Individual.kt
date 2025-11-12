package com.lewisdeveloping.gedcomviewer.model

data class Individual(
    val id: String,
    val gedcomId: String,
    val sourceId: String,
    val fullName: String,
    val givenName: String?,
    val surname: String?,
    val title: String? = null,
    val gender: Gender = Gender.UNKNOWN,
    val birth: LifeEvent? = null,
    val death: LifeEvent? = null,
    val familiesAsSpouse: List<String> = emptyList(),
    val familiesAsChild: List<String> = emptyList(),
    val timeline: List<TimelineEntry> = emptyList(),
    val notes: List<String> = emptyList(),
    val primaryObjectId: String? = null
) {
    val displayName: String
        get() = when {
            fullName.isNotBlank() && !title.isNullOrBlank() -> {
                "${fullName.trim()} (${title.trim()})"
            }
            fullName.isNotBlank() -> fullName.trim()
            !title.isNullOrBlank() -> title.trim()
            else -> "Unnamed"
        }

    val birthSummary: String? get() = birth?.description()

    val deathSummary: String? get() = death?.description()

    enum class Gender { MALE, FEMALE, UNKNOWN }
}
