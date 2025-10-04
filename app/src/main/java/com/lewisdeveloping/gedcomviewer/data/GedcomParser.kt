package com.lewisdeveloping.gedcomviewer.data

import com.lewisdeveloping.gedcomviewer.model.Family
import com.lewisdeveloping.gedcomviewer.model.Individual
import com.lewisdeveloping.gedcomviewer.model.Individual.Gender
import com.lewisdeveloping.gedcomviewer.model.LifeEvent
import java.io.InputStream

class GedcomParser {
    private val lineRegex = Regex("""^(\\d+)\\s+(?:(@[^@]+@)\\s+)?([A-Z0-9_]+)(?:\\s+(.*))?$""")
    private val containerTags = setOf("BIRT", "DEAT", "MARR")

    fun parse(stream: InputStream): GedcomData {
        val individuals = linkedMapOf<String, IndividualBuilder>()
        val families = linkedMapOf<String, FamilyBuilder>()
        var currentIndividual: IndividualBuilder? = null
        var currentFamily: FamilyBuilder? = null
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
                    when (tag) {
                        "INDI" -> {
                            val builder = individuals.getOrPut(pointer.toId()) { IndividualBuilder(pointer.toId()) }
                            currentIndividual = builder
                        }
                        "FAM" -> {
                            val builder = families.getOrPut(pointer.toId()) { FamilyBuilder(pointer.toId()) }
                            currentFamily = builder
                        }
                        else -> Unit
                    }
                    context.clear()
                    context.add(Context(level, tag))
                    return@forEach
                }

                val parentTag = context.lastOrNull()?.tag

                currentIndividual?.let { individual ->
                    when {
                        tag == "NAME" -> individual.setName(value.orEmpty())
                        tag == "SEX" -> individual.setGender(value)
                        tag == "FAMC" -> parsePointer(value)?.let(individual.familiesAsChild::add)
                        tag == "FAMS" -> parsePointer(value)?.let(individual.familiesAsSpouse::add)
                        tag == "OBJE" && individual.primaryObjectId == null -> individual.primaryObjectId = parsePointer(value)
                        parentTag == "BIRT" -> individual.applyBirthDetail(tag, value)
                        parentTag == "DEAT" -> individual.applyDeathDetail(tag, value)
                    }
                }

                currentFamily?.let { family ->
                    when {
                        tag == "HUSB" -> family.husbandId = parsePointer(value)
                        tag == "WIFE" -> family.wifeId = parsePointer(value)
                        tag == "CHIL" -> parsePointer(value)?.let(family.children::add)
                        parentTag == "MARR" -> family.applyMarriageDetail(tag, value)
                    }
                }

                if (tag in containerTags) {
                    context.add(Context(level, tag))
                } else if (currentIndividual != null || currentFamily != null) {
                    context.add(Context(level, tag))
                }
            }
        }

        return GedcomData(
            individuals = individuals.mapValues { it.value.build() },
            families = families.mapValues { it.value.build() }
        )
    }

    private fun parsePointer(raw: String?): String? = raw?.takeIf { it.startsWith("@") && it.endsWith("@") }?.toId()

    private fun String.toId(): String = trim('@')

    private data class Context(val level: Int, val tag: String)

    private class IndividualBuilder(val id: String) {
        private var fullName: String = id
        private var givenName: String? = null
        private var surname: String? = null
        private var birthDate: String? = null
        private var birthPlace: String? = null
        private var deathDate: String? = null
        private var deathPlace: String? = null
        private var gender: Gender = Gender.UNKNOWN
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

        fun setGender(raw: String?) {
            gender = when (raw?.uppercase()) {
                "M" -> Gender.MALE
                "F" -> Gender.FEMALE
                else -> Gender.UNKNOWN
            }
        }

        fun applyBirthDetail(tag: String, value: String?) {
            when (tag) {
                "DATE" -> birthDate = value
                "PLAC" -> birthPlace = value
            }
        }

        fun applyDeathDetail(tag: String, value: String?) {
            when (tag) {
                "DATE" -> deathDate = value
                "PLAC" -> deathPlace = value
            }
        }

        fun build(): Individual = Individual(
            id = id,
            fullName = fullName,
            givenName = givenName,
            surname = surname,
            gender = gender,
            birth = if (birthDate != null || birthPlace != null) LifeEvent(birthDate, birthPlace) else null,
            death = if (deathDate != null || deathPlace != null) LifeEvent(deathDate, deathPlace) else null,
            familiesAsSpouse = familiesAsSpouse.toList(),
            familiesAsChild = familiesAsChild.toList(),
            primaryObjectId = primaryObjectId
        )
    }

    private class FamilyBuilder(val id: String) {
        var husbandId: String? = null
        var wifeId: String? = null
        val children: MutableList<String> = mutableListOf()
        private var marriageDate: String? = null
        private var marriagePlace: String? = null

        fun applyMarriageDetail(tag: String, value: String?) {
            when (tag) {
                "DATE" -> marriageDate = value
                "PLAC" -> marriagePlace = value
            }
        }

        fun build(): Family = Family(
            id = id,
            husbandId = husbandId,
            wifeId = wifeId,
            childrenIds = children.toList(),
            marriage = if (marriageDate != null || marriagePlace != null) LifeEvent(marriageDate, marriagePlace) else null
        )
    }
}
