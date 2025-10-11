package com.lewisdeveloping.gedcomviewer.model

data class TimelineEntry(
    val tag: String,
    val label: String,
    val event: LifeEvent
)
