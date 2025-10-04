package com.lewisdeveloping.gedcomviewer.data

import com.lewisdeveloping.gedcomviewer.model.Family
import com.lewisdeveloping.gedcomviewer.model.Individual

data class GedcomData(
    val individuals: Map<String, Individual> = emptyMap(),
    val families: Map<String, Family> = emptyMap()
) {
    val individualsSortedByName: List<Individual> = individuals.values
        .sortedWith(compareBy({ it.surname?.lowercase() ?: "" }, { it.givenName?.lowercase() ?: it.displayName.lowercase() }))
}
