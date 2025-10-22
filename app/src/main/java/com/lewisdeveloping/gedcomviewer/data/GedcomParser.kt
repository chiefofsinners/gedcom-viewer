package com.lewisdeveloping.gedcomviewer.data

import com.lewisdeveloping.gedcomviewer.model.Family
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.model.Individual.Gender
import com.lewisdeveloping.gedcomviewer.model.LifeEvent
import com.lewisdeveloping.gedcomviewer.model.TimelineEntry
import java.io.InputStream
import java.util.Locale

class GedcomParser {
    private val lineRegex = Regex("""^(\d+)\s+(?:(@[^@]+@)\s+)?([A-Z0-9_]+)(?:\s+(.*))?$""")
    private val individualEventTags = setOf(
        "BIRT", "DEAT", "BAPM", "BAPT", "CHR", "CHRA", "RESI", "OCCU", "BURI", "GRAD", "EDUC", "EVEN"
    )
    private val familyEventTags = setOf("MARR")

    fun parse(stream: InputStream): GedcomData {
        val individuals = linkedMapOf<String, IndividualBuilder>()
        val families = linkedMapOf<String, FamilyBuilder>()
        val noteRecords = linkedMapOf<String, NoteRecordBuilder>()

        var currentIndividual: IndividualBuilder? = null
        var currentFamily: FamilyBuilder? = null
        var currentNoteRecord: NoteRecordBuilder? = null
        val context = ArrayDeque<Context>()

        stream.bufferedReader(Charsets.UTF_8).useLines { sequence ->
            sequence.forEach { rawLine ->
                val line = rawLine.trimEnd().removePrefix("\uFEFF")
                if (line.isBlank()) return@forEach

                val match = lineRegex.matchEntire(line) ?: return@forEach
                val level = match.groupValues[1].toInt()
                val pointer = match.groupValues[2].takeIf { it.isNotBlank() }
                val tag = match.groupValues[3]
                val value = match.groupValues.getOrNull(4)?.trim()?.takeIf { it.isNotEmpty() }

                while (context.isNotEmpty() && context.last().level >= level) {
                    context.removeLast()
                }

                if (level == 0 && pointer != null) {
                    currentIndividual = null
                    currentFamily = null
                    currentNoteRecord = null
                    when (tag) {
                        "INDI" -> {
                            val builder = individuals.getOrPut(pointer.toId()) { IndividualBuilder(pointer.toId()) }
                            currentIndividual = builder
                        }
                        "FAM" -> {
                            val builder = families.getOrPut(pointer.toId()) { FamilyBuilder(pointer.toId()) }
                            currentFamily = builder
                        }
                        "NOTE" -> {
                            val builder = noteRecords.getOrPut(pointer.toId()) { NoteRecordBuilder(pointer.toId()) }
                            builder.setInitial(value)
                            currentNoteRecord = builder
                        }
                    }
                    context.clear()
                    context.add(Context(level, tag))
                    return@forEach
                }

                val parentTag = context.lastOrNull()?.tag
                val pointerId = parsePointer(value)
                val currentEventBuilder = context.lastOrNull { it.eventBuilder != null }?.eventBuilder

                var handled = false
                var addedContext = false

                currentNoteRecord?.let { note ->
                    if (context.lastOrNull()?.tag == "NOTE") {
                        when (tag) {
                            "CONC" -> note.appendConc(value)
                            "CONT" -> note.appendCont(value)
                        }
                        handled = true
                    }
                }

                if (!handled && currentIndividual != null && tag in individualEventTags) {
                    val eventBuilder = currentIndividual!!.beginEvent(tag, eventLabelFor(tag), value)
                    context.add(Context(level, tag, eventBuilder))
                    addedContext = true
                    handled = true
                }

                if (!handled && currentFamily != null && tag in familyEventTags) {
                    val eventBuilder = currentFamily!!.beginEvent(tag, value)
                    if (eventBuilder != null) {
                        context.add(Context(level, tag, eventBuilder))
                        addedContext = true
                        handled = true
                    }
                }

                if (!handled) {
                    currentIndividual?.let { individual ->
                        var consumed = false
                        currentEventBuilder?.let { builder ->
                            consumed = builder.handle(tag, value, pointerId, parentTag)
                        }
                        if (consumed) {
                            handled = true
                        } else {
                            when {
                                tag == "NAME" -> {
                                    individual.setName(value.orEmpty())
                                    handled = true
                                }
                                tag == "TITL" -> {
                                    individual.setTitle(value)
                                    handled = true
                                }
                                tag == "SEX" -> {
                                    individual.setGender(value)
                                    handled = true
                                }
                                tag == "FAMC" -> {
                                    pointerId?.let(individual.familiesAsChild::add)
                                    handled = true
                                }
                                tag == "FAMS" -> {
                                    pointerId?.let(individual.familiesAsSpouse::add)
                                    handled = true
                                }
                                tag == "OBJE" && individual.primaryObjectId == null -> {
                                    individual.primaryObjectId = pointerId
                                    handled = true
                                }
                                tag == "NOTE" -> {
                                    individual.addNote(value, pointerId)
                                    handled = true
                                }
                                parentTag == "TITL" && (tag == "CONC" || tag == "CONT") -> {
                                    individual.appendTitleContinuation(tag, value)
                                    handled = true
                                }
                                parentTag == "NOTE" && (tag == "CONC" || tag == "CONT") -> {
                                    individual.appendNoteContinuation(tag, value)
                                    handled = true
                                }
                            }
                        }
                    }
                }

                if (!handled) {
                    currentFamily?.let { family ->
                        var consumed = false
                        currentEventBuilder?.let { builder ->
                            consumed = builder.handle(tag, value, pointerId, parentTag)
                        }
                        if (consumed) {
                            handled = true
                        } else {
                            when (tag) {
                                "HUSB" -> {
                                    family.husbandId = pointerId
                                    handled = true
                                }
                                "WIFE" -> {
                                    family.wifeId = pointerId
                                    handled = true
                                }
                                "CHIL" -> {
                                    pointerId?.let(family.children::add)
                                    handled = true
                                }
                            }
                        }
                    }
                }

                if (!addedContext && (currentIndividual != null || currentFamily != null || currentNoteRecord != null)) {
                    context.add(Context(level, tag))
                }
            }
        }

        val resolvedNotes = noteRecords.mapValues { it.value.build() }

        return GedcomData(
            individuals = individuals.mapValues { it.value.build(resolvedNotes) },
            families = families.mapValues { it.value.build(resolvedNotes) }
        )
    }

    private fun parsePointer(raw: String?): String? = raw?.takeIf { it.startsWith("@") && it.endsWith("@") }?.toId()

    private fun String.toId(): String = trim('@')

    private data class Context(val level: Int, val tag: String, val eventBuilder: LifeEventBuilder? = null)

    private fun eventLabelFor(tag: String): String = when (tag) {
        "BIRT" -> "Birth"
        "DEAT" -> "Death"
        "BAPM", "BAPT" -> "Baptism"
        "CHR" -> "Christening"
        "CHRA" -> "Adult Christening"
        "RESI" -> "Residence"
        "OCCU" -> "Occupation"
        "BURI" -> "Burial"
        "GRAD" -> "Graduation"
        "EDUC" -> "Education"
        "EVEN" -> "Event"
        "MARR" -> "Marriage"
        else -> prettifyTag(tag)
    }

    private fun prettifyTag(tag: String): String = tag
        .trim('_')
        .lowercase(Locale.getDefault())
        .split(' ', '_')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part ->
            part.replaceFirstChar { ch ->
                if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
            }
        }
        .ifBlank { tag }

    private class IndividualBuilder(val id: String) {
        private var fullName: String = ""
        private var givenName: String? = null
        private var surname: String? = null
        private var title: String? = null
        private var titleBuilder: StringBuilder? = null
        private var gender: Gender = Gender.UNKNOWN

        private val birthEvent = LifeEventBuilder()
        private val deathEvent = LifeEventBuilder()
        private val timelineEntries: MutableList<TimelineEntryContext> = mutableListOf()
        private val notes: MutableList<NoteValue> = mutableListOf()
        private var lastInlineNote: NoteValue.Inline? = null

        val familiesAsSpouse: MutableList<String> = mutableListOf()
        val familiesAsChild: MutableList<String> = mutableListOf()
        var primaryObjectId: String? = null

        fun setName(raw: String) {
            if (raw.isBlank()) return
            val parts = raw.split('/')
            givenName = parts.getOrNull(0)?.trim()?.takeIf { it.isNotEmpty() }
            surname = parts.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
            val suffix = parts.getOrNull(2)?.trim()?.takeIf { it.isNotEmpty() }
            val constructed = listOfNotNull(givenName, surname, suffix).filter { it.isNotBlank() }
            fullName = constructed.joinToString(" ").ifBlank { raw.replace("/", "").trim() }
        }

        fun setTitle(raw: String?) {
            val trimmed = raw?.trim().takeIf { it?.isNotEmpty() == true } ?: return
            titleBuilder = StringBuilder(trimmed)
            title = trimmed
        }

        fun appendTitleContinuation(tag: String, raw: String?) {
            val value = raw ?: return
            val builder = titleBuilder ?: title?.let { existing ->
                StringBuilder(existing)
            }?.also { titleBuilder = it } ?: return
            if (tag == "CONT") builder.append('\n')
            builder.append(value)
            title = builder.toString()
        }

        fun setGender(raw: String?) {
            gender = when (raw?.uppercase()) {
                "M" -> Gender.MALE
                "F" -> Gender.FEMALE
                else -> Gender.UNKNOWN
            }
        }

        fun beginEvent(tag: String, label: String, rawValue: String?): LifeEventBuilder {
            val builder = when (tag) {
                "BIRT" -> birthEvent
                "DEAT" -> deathEvent
                else -> LifeEventBuilder()
            }
            if (timelineEntries.none { it.builder === builder }) {
                timelineEntries.add(TimelineEntryContext(tag, label, builder))
            }
            builder.setValue(rawValue)
            return builder
        }

        fun addNote(value: String?, pointer: String?) {
            val note = if (pointer != null) {
                NoteValue.Pointer(pointer)
            } else {
                NoteValue.Inline().apply { append(value, newline = false) }
            }
            notes.add(note)
            lastInlineNote = (note as? NoteValue.Inline)
        }

        fun appendNoteContinuation(tag: String, value: String?) {
            val inline = lastInlineNote ?: notes.lastOrNull() as? NoteValue.Inline ?: return
            inline.append(value, newline = tag == "CONT")
        }

        fun build(noteRecords: Map<String, String>): Individual {
            val birth = birthEvent.build(noteRecords)
            val death = deathEvent.build(noteRecords)
            val timeline = timelineEntries.mapNotNull { entry ->
                entry.builder.build(noteRecords)?.let { event ->
                    TimelineEntry(tag = entry.tag, label = entry.label, event = event)
                }
            }
            val resolvedFullName = fullName.trim()
            val resolvedTitle = titleBuilder?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                ?: title?.trim()?.takeIf { it.isNotEmpty() }

            return Individual(
                id = id,
                fullName = resolvedFullName,
                givenName = givenName,
                surname = surname,
                title = resolvedTitle,
                gender = gender,
                birth = birth,
                death = death,
                familiesAsSpouse = familiesAsSpouse.toList(),
                familiesAsChild = familiesAsChild.toList(),
                timeline = timeline,
                notes = notes.mapNotNull { it.resolve(noteRecords) },
                primaryObjectId = primaryObjectId
            )
        }

        private data class TimelineEntryContext(
            val tag: String,
            val label: String,
            val builder: LifeEventBuilder
        )
    }

    private class FamilyBuilder(val id: String) {
        var husbandId: String? = null
        var wifeId: String? = null
        val children: MutableList<String> = mutableListOf()
        private val marriageEvent = LifeEventBuilder()

        fun beginEvent(tag: String, rawValue: String?): LifeEventBuilder? = when (tag) {
            "MARR" -> {
                marriageEvent.setValue(rawValue)
                marriageEvent
            }
            else -> null
        }

        fun build(noteRecords: Map<String, String>): Family = Family(
            id = id,
            husbandId = husbandId,
            wifeId = wifeId,
            childrenIds = children.toList(),
            marriage = marriageEvent.build(noteRecords)
        )
    }

    private class LifeEventBuilder {
        private var date: String? = null
        private var place: String? = null
        private var addressBuilder: StringBuilder? = null
        private var value: String? = null
        private val details: MutableMap<String, MutableList<String>> = linkedMapOf()
        private val notes: MutableList<NoteValue> = mutableListOf()
        private var lastInlineNote: NoteValue.Inline? = null

        fun setValue(raw: String?) {
            value = raw?.takeIf { it.isNotBlank() }
        }

        fun handle(tag: String, value: String?, pointer: String?, parentTag: String?): Boolean {
            if (parentTag == "NOTE" && (tag == "CONC" || tag == "CONT")) {
                appendNoteContinuation(tag, value)
                return true
            }
            if (parentTag == "ADDR" && (tag == "CONC" || tag == "CONT")) {
                appendAddress(tag, value)
                return true
            }
            return when (tag) {
                "DATE" -> {
                    date = value
                    true
                }
                "PLAC" -> {
                    place = value
                    true
                }
                "ADDR" -> {
                    setAddress(value)
                    true
                }
                "NOTE" -> {
                    addNote(value, pointer)
                    true
                }
                "TYPE", "CAUS", "AGNC", "RELI" -> {
                    value?.takeIf { it.isNotBlank() }?.let { addDetail(tag, it.trim()) }
                    true
                }
                else -> false
            }
        }

        private fun addDetail(tag: String, value: String) {
            val label = formatLabel(tag)
            details.getOrPut(label) { mutableListOf() }.add(value)
        }

        private fun addNote(value: String?, pointer: String?) {
            val note = if (pointer != null) {
                NoteValue.Pointer(pointer)
            } else {
                NoteValue.Inline().apply { append(value, newline = false) }
            }
            notes.add(note)
            lastInlineNote = (note as? NoteValue.Inline)
        }

        private fun appendNoteContinuation(tag: String, value: String?) {
            val inline = lastInlineNote ?: notes.lastOrNull() as? NoteValue.Inline ?: return
            inline.append(value, newline = tag == "CONT")
        }

        private fun setAddress(value: String?) {
            addressBuilder = StringBuilder().apply {
                value?.let { append(it) }
            }
        }

        private fun appendAddress(tag: String, value: String?) {
            val builder = addressBuilder ?: StringBuilder().also { addressBuilder = it }
            if (tag == "CONT") {
                if (builder.isNotEmpty()) {
                    builder.append('\n')
                }
            }
            if (!value.isNullOrEmpty()) {
                builder.append(value)
            } else if (tag == "CONT" && builder.isEmpty()) {
                builder.append('\n')
            }
        }

        fun build(noteRecords: Map<String, String>): LifeEvent? {
            val address = addressBuilder?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val normalizedValue = value?.takeIf { it.isNotBlank() }
            val resolvedDetails = details
                .mapValues { entry -> entry.value.mapNotNull { it.takeIf { text -> text.isNotBlank() } } }
                .filterValues { it.isNotEmpty() }
            val resolvedNotes = notes.mapNotNull { it.resolve(noteRecords) }

            val hasCoreData = listOfNotNull(
                date?.takeIf { it.isNotBlank() },
                place?.takeIf { it.isNotBlank() },
                address,
                normalizedValue
            ).isNotEmpty()

            if (!hasCoreData && resolvedDetails.isEmpty() && resolvedNotes.isEmpty()) {
                return null
            }

            return LifeEvent(
                date = date?.takeIf { it.isNotBlank() },
                place = place?.takeIf { it.isNotBlank() },
                address = address,
                value = normalizedValue,
                details = resolvedDetails,
                notes = resolvedNotes
            )
        }

        private fun formatLabel(tag: String): String = when (tag) {
            "CAUS" -> "Cause"
            "TYPE" -> "Type"
            "AGNC" -> "Agency"
            "RELI" -> "Religion"
            else -> tag
                .trim('_')
                .lowercase(Locale.getDefault())
                .replace('_', ' ')
                .split(' ')
                .filter { it.isNotBlank() }
                .joinToString(" ") { part ->
                    part.replaceFirstChar { ch ->
                        if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
                    }
                }
                .ifBlank { tag }
        }
    }

    private sealed interface NoteValue {
        fun resolve(noteRecords: Map<String, String>): String?

        class Inline : NoteValue {
            private val builder = StringBuilder()

            fun append(value: String?, newline: Boolean) {
                if (newline) {
                    if (builder.isNotEmpty()) {
                        builder.append('\n')
                    } else if (value == null) {
                        builder.append('\n')
                    }
                }
                if (!value.isNullOrEmpty()) {
                    builder.append(value)
                }
            }

            override fun resolve(noteRecords: Map<String, String>): String? =
                builder.toString().trim().takeIf { it.isNotEmpty() }
        }

        class Pointer(private val id: String) : NoteValue {
            override fun resolve(noteRecords: Map<String, String>): String? =
                noteRecords[id]?.takeIf { it.isNotBlank() }
        }
    }

    private class NoteRecordBuilder(@Suppress("unused") val id: String) {
        private val builder = StringBuilder()

        fun setInitial(value: String?) {
            value?.let { builder.append(it) }
        }

        fun appendConc(value: String?) {
            value?.let { builder.append(it) }
        }

        fun appendCont(value: String?) {
            if (builder.isNotEmpty()) {
                builder.append('\n')
            }
            value?.let { builder.append(it) }
        }

        fun build(): String = builder.toString().trim()
    }
}
