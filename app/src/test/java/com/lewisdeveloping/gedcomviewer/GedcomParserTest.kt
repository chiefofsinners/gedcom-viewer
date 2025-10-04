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
}
