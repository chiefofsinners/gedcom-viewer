package com.lewisdeveloping.gedcomviewer.model

data class Individual(
    val id: String,
    val fullName: String,
    val givenName: String?,
    val surname: String?,
    val gender: Gender = Gender.UNKNOWN,
    val birth: LifeEvent? = null,
    val death: LifeEvent? = null,
    val familiesAsSpouse: List<String> = emptyList(),
    val familiesAsChild: List<String> = emptyList(),
    val notes: List<String> = emptyList(),
    val primaryObjectId: String? = null
) {
    val displayName: String = if (fullName.isNotBlank()) fullName else id

    enum class Gender { MALE, FEMALE, UNKNOWN }
}
