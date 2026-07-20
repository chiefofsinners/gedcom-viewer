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
    fun parsesReversedNameFormat() {
        val gedcom = """
            0 HEAD
            0 @I1@ INDI
            1 NAME /Bohe/ Walter Peter
            2 SURN Bohe
            2 GIVN Walter Peter
            1 SEX M
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        val individual = data.individuals.values.single()
        assertEquals("Walter Peter", individual.givenName)
        assertEquals("Bohe", individual.surname)
        assertEquals("Walter Peter Bohe", individual.fullName)
    }

    @Test
    fun givnAndSurnOverrideNameParsing() {
        val gedcom = """
            0 HEAD
            0 @I1@ INDI
            1 NAME /Doe/ John
            2 GIVN John
            2 SURN Doe
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        val individual = data.individuals.values.single()
        assertEquals("John", individual.givenName)
        assertEquals("Doe", individual.surname)
        assertEquals("John Doe", individual.fullName)
    }

    @Test
    fun usesFirstNameRecordWhenMultiplePresent() {
        val gedcom = """
            0 HEAD
            0 @I1@ INDI
            1 NAME John /Smith/
            2 TYPE aka
            1 NAME James /Smyth/
            2 TYPE birth
            2 GIVN James
            2 SURN Smyth
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        val individual = data.individuals.values.single()
        assertEquals("John Smith", individual.fullName)
        assertEquals("John Smith", individual.displayName)
    }

    @Test
    fun ignoresSourceCitationRecordingDateForEventDate() {
        val gedcom = """
            0 HEAD
            0 @I1@ INDI
            1 NAME Charles /Hafner/
            1 DEAT
            2 DATE 21 Oct 2016
            2 PLAC West Chester, Pa
            2 SOUR @S49@
            3 DATA
            4 TEXT Charles P. Hafner Jr., 56, passed away on Friday, Oct. 21, 2016.
            4 DATE 24 Oct 2016
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        val individual = data.individuals.values.single()
        assertEquals("21 Oct 2016", individual.death?.date)
    }

    @Test
    fun usesEventDateWhenMultipleSourceCitationsPresent() {
        val gedcom = """
            0 HEAD
            0 @I1@ INDI
            1 NAME John /Doe/
            1 DEAT
            2 DATE 21 Jun 1977
            2 SOUR @S1@
            3 DATA
            4 DATE 22 Jun 1977
            2 SOUR @S2@
            3 DATA
            4 DATE 23 Jun 1977
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        val individual = data.individuals.values.single()
        assertEquals("21 Jun 1977", individual.death?.date)
    }

    @Test
    fun ignoresSourceCitationRecordingDateForMarriageDate() {
        val gedcom = """
            0 HEAD
            0 @F1@ FAM
            1 MARR
            2 DATE 1 Mar 1975
            2 SOUR @S1@
            3 DATA
            4 DATE 1 Mar 1975
            2 SOUR @S2@
            3 DATA
            4 DATE 2 Mar 1975
            2 SOUR @S3@
            3 DATA
            4 DATE 2 Mar 1975
            0 TRLR
        """.trimIndent()

        val data = ByteArrayInputStream(gedcom.toByteArray(StandardCharsets.UTF_8)).use { stream ->
            GedcomParser().parse(stream)
        }

        val family = data.families.values.single()
        assertEquals("1 Mar 1975", family.marriage?.date)
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

    @Test
    fun parsesContentLargerThanHeaderWindow() {
        // Streaming decode reads an 8 KB header prefix, pushes it back, then decodes the
        // rest lazily. Generate well over 8 KB of records to exercise that boundary and
        // confirm nothing is dropped or duplicated across it.
        val builder = StringBuilder("0 HEAD\n1 CHAR UTF-8\n")
        val count = 1_000
        for (i in 1..count) {
            builder.append("0 @I$i@ INDI\n1 NAME Person$i /Surname$i/\n")
        }
        builder.append("0 TRLR\n")
        val bytes = builder.toString().toByteArray(StandardCharsets.UTF_8)
        assertTrue("Fixture should exceed the header window", bytes.size > 8_192)

        val data = ByteArrayInputStream(bytes).use { stream ->
            GedcomParser().parse(stream)
        }

        val names = data.individuals.values.map { it.displayName }.toSet()
        assertEquals(count, data.individuals.size)
        assertTrue("First record should survive the header boundary", "Person1 Surname1" in names)
        assertTrue("Last record should survive the header boundary", "Person$count Surname$count" in names)
    }

    @Test
    fun stripsUtf8ByteOrderMark() {
        val gedcom = "0 HEAD\n0 @I1@ INDI\n1 NAME Jane /Roe/\n0 TRLR\n"
        val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        val bytes = bom + gedcom.toByteArray(StandardCharsets.UTF_8)

        val data = ByteArrayInputStream(bytes).use { stream ->
            GedcomParser().parse(stream)
        }

        assertEquals("Jane Roe", data.individuals.values.single().displayName)
    }

    @Test
    fun decodesDeclaredWindows1252Charset() {
        // "1 CHAR ANSI" selects windows-1252; 0xE9 must decode to é, not a replacement char.
        val header = "0 HEAD\n1 CHAR ANSI\n0 @I1@ INDI\n1 NAME Ren".toByteArray(StandardCharsets.US_ASCII)
        val body = "e /Beaumont/\n0 TRLR\n".toByteArray(StandardCharsets.US_ASCII)
        val bytes = header + byteArrayOf(0xE9.toByte()) + body

        val data = ByteArrayInputStream(bytes).use { stream ->
            GedcomParser().parse(stream)
        }

        assertEquals("Renée Beaumont", data.individuals.values.single().displayName)
    }
}
