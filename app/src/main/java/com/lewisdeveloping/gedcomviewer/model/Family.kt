package com.lewisdeveloping.gedcomviewer.model

data class Family(
    val id: String,
    val husbandId: String? = null,
    val wifeId: String? = null,
    val childrenIds: List<String> = emptyList(),
    val marriage: LifeEvent? = null
)
