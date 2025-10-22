package com.lewisdeveloping.gedcomviewer

import com.lewisdeveloping.gedcomviewer.data.GedcomParser
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GedcomParserTest {
    @Test
    fun parsesSimpleGedcom() {
        val gedcom = """
            0 HEAD
            0 @I1@ INDI
            1 NAME John /Doe/
            1 SEX M
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        assertEquals(1, data.individuals.size)
        assertEquals("John Doe", data.individuals.values.first().displayName)
    }

    @Test
    fun parsesSampleGedcom() {
        val path = Path.of("src", "main", "assets", "Sample-GEDCOM.ged")
        assertTrue("Sample GEDCOM asset is missing", Files.exists(path))

        val data = Files.newInputStream(path).use { input ->
            GedcomParser().parse(input)
        }
        assertTrue("Expected individuals to be parsed", data.individuals.isNotEmpty())
        assertTrue("Expected families to be parsed", data.families.isNotEmpty())
    }

    @Test
    fun fallsBackToTitleWhenNameMissing() {
        val gedcom = """
            0 HEAD
            0 @I1@ INDI
            1 TITL Duke of Testing
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        val individual = data.individuals.values.single()
        assertEquals("Duke of Testing", individual.displayName)
        assertEquals("Duke of Testing", individual.title)
        assertTrue(individual.fullName.isBlank())
    }

    @Test
    fun qualifiesNameWithTitleWhenBothPresent() {
        val gedcom = """
            0 HEAD
            0 @I1@ INDI
            1 NAME Jane /Doe/
            1 TITL PhD
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        val individual = data.individuals.values.single()
        assertEquals("Jane Doe (PhD)", individual.displayName)
        assertEquals("Jane Doe", individual.fullName)
        assertEquals("PhD", individual.title)
    }

    @Test
    fun displaysUnnamedWhenNameAndTitleMissing() {
        val gedcom = """
            0 HEAD
            0 @I1@ INDI
            1 SEX F
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        val individual = data.individuals.values.single()
        assertEquals("Unnamed", individual.displayName)
        assertTrue(individual.fullName.isBlank())
        assertTrue(individual.title.isNullOrBlank())
    }
}
