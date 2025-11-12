package com.lewisdeveloping.gedcomviewer.data

import java.nio.charset.Charset
import java.nio.charset.IllegalCharsetNameException
import java.nio.charset.UnsupportedCharsetException
import kotlin.math.min

internal class GedcomTextDecoder {
    private val anselDecoder = AnselDecoder()

    fun decode(data: ByteArray): String {
        if (data.isEmpty()) return ""

        val strategies = decodingStrategies(
            bom = ByteOrderMark.detect(data),
            declared = DeclaredCharset.fromData(data)
        )

        for (strategy in strategies) {
            val decoded = decodeWithStrategy(data, strategy)
            if (decoded != null) {
                return decoded
            }
        }

        throw GedcomParserException(
            "Unable to decode the GEDCOM file using the declared character set."
        )
    }

    private fun decodingStrategies(
        bom: ByteOrderMark?,
        declared: DeclaredCharset?
    ): List<DecodingStrategy> {
        val ordered = linkedSetOf<DecodingStrategy>()
        bom?.let { ordered += it.strategy }
        declared?.preferredStrategies()?.forEach { ordered += it }
        DecodingStrategy.defaultFallbacks.forEach { ordered += it }
        return ordered.toList()
    }

    private fun decodeWithStrategy(data: ByteArray, strategy: DecodingStrategy): String? =
        when (strategy) {
            DecodingStrategy.UTF8 -> decodeWithCharset(data, Charsets.UTF_8)
            DecodingStrategy.UTF16_LE -> decodeWithCharset(data, Charsets.UTF_16LE)
            DecodingStrategy.UTF16_BE -> decodeWithCharset(data, Charsets.UTF_16BE)
            DecodingStrategy.WINDOWS_CP1252 -> decodeWithCharset(data, charsetOrNull("windows-1252"))
            DecodingStrategy.ISO_LATIN1 -> decodeWithCharset(data, Charsets.ISO_8859_1)
            DecodingStrategy.ASCII -> decodeWithCharset(data, Charsets.US_ASCII)
            DecodingStrategy.MAC_ROMAN -> decodeWithCharset(data, charsetOrNull("x-MacRoman"))
            DecodingStrategy.ANSEL -> anselDecoder.decode(data)
        }

    private fun decodeWithCharset(data: ByteArray, charset: Charset?): String? {
        if (charset == null) return null
        return try {
            String(data, charset)
        } catch (_: Throwable) {
            null
        }
    }

    private fun charsetOrNull(name: String): Charset? = try {
        Charset.forName(name)
    } catch (_: UnsupportedCharsetException) {
        null
    } catch (_: IllegalCharsetNameException) {
        null
    }
}

private enum class DecodingStrategy {
    UTF8,
    UTF16_LE,
    UTF16_BE,
    WINDOWS_CP1252,
    ISO_LATIN1,
    ASCII,
    MAC_ROMAN,
    ANSEL;

    companion object {
        val defaultFallbacks: List<DecodingStrategy> = listOf(
            UTF8,
            WINDOWS_CP1252,
            MAC_ROMAN,
            ISO_LATIN1,
            ANSEL,
            UTF16_LE,
            UTF16_BE
        )
    }
}

private enum class ByteOrderMark(val bytes: ByteArray, val strategy: DecodingStrategy) {
    UTF8(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()), DecodingStrategy.UTF8),
    UTF16_LE(byteArrayOf(0xFF.toByte(), 0xFE.toByte()), DecodingStrategy.UTF16_LE),
    UTF16_BE(byteArrayOf(0xFE.toByte(), 0xFF.toByte()), DecodingStrategy.UTF16_BE);

    companion object {
        fun detect(data: ByteArray): ByteOrderMark? = entries.firstOrNull { bom ->
            if (data.size < bom.bytes.size) return@firstOrNull false
            bom.bytes.indices.all { index -> data[index] == bom.bytes[index] }
        }
    }
}

private data class DeclaredCharset(
    private val type: Type,
    private val rawValue: String
) {
    enum class Type {
        UTF8,
        UNICODE,
        ANSEL,
        ANSI,
        ASCII,
        MACINTOSH,
        OTHER
    }

    fun preferredStrategies(): List<DecodingStrategy> = when (type) {
        Type.UTF8 -> listOf(DecodingStrategy.UTF8)
        Type.UNICODE -> listOf(DecodingStrategy.UTF16_LE, DecodingStrategy.UTF16_BE)
        Type.ANSEL -> listOf(DecodingStrategy.ANSEL, DecodingStrategy.WINDOWS_CP1252)
        Type.ANSI -> listOf(DecodingStrategy.WINDOWS_CP1252, DecodingStrategy.ISO_LATIN1)
        Type.ASCII -> listOf(DecodingStrategy.ASCII, DecodingStrategy.UTF8)
        Type.MACINTOSH -> listOf(DecodingStrategy.MAC_ROMAN)
        Type.OTHER -> when {
            rawValue.contains("UTF-16") || rawValue.contains("UTF16") ->
                listOf(DecodingStrategy.UTF16_LE, DecodingStrategy.UTF16_BE)
            rawValue.contains("UTF-8") || rawValue.contains("UTF8") ->
                listOf(DecodingStrategy.UTF8)
            else -> emptyList()
        }
    }

    companion object {
        fun fromData(data: ByteArray): DeclaredCharset? {
            val headerLimit = min(data.size, 8_192)
            if (headerLimit == 0) return null
            val header = String(data, 0, headerLimit, Charsets.US_ASCII)
            header.lineSequence().forEach { rawLine ->
                val trimmed = rawLine.trim()
                if (trimmed.startsWith("1 CHAR")) {
                    val value = trimmed
                        .removePrefix("1 CHAR")
                        .trim()
                    if (value.isNotEmpty()) {
                        return fromValue(value)
                    }
                }
            }
            return null
        }

        private fun fromValue(raw: String): DeclaredCharset? {
            val normalized = raw.trim().uppercase()
            if (normalized.isEmpty()) return null
            return when (normalized) {
                "UTF-8", "UTF8" -> DeclaredCharset(Type.UTF8, normalized)
                "UNICODE", "UTF-16", "UTF16" -> DeclaredCharset(Type.UNICODE, normalized)
                "ANSEL" -> DeclaredCharset(Type.ANSEL, normalized)
                "ANSI", "WINDOWS", "IBMPC", "IBM PC" -> DeclaredCharset(Type.ANSI, normalized)
                "ASCII" -> DeclaredCharset(Type.ASCII, normalized)
                "MACINTOSH", "MAC" -> DeclaredCharset(Type.MACINTOSH, normalized)
                else -> DeclaredCharset(Type.OTHER, normalized)
            }
        }
    }
}

private class AnselDecoder {
    fun decode(data: ByteArray): String {
        if (data.isEmpty()) return ""

        val scalars = mutableListOf<Int>()
        val modifiers = ArrayDeque<Int>()
        val space = 0x20
        val replacement = 0xFFFD

        fun flushModifiers() {
            while (modifiers.isNotEmpty()) {
                scalars += modifiers.removeFirst()
            }
        }

        for (byte in data) {
            val value = byte.toInt() and 0xFF
            when {
                ANSEL_CHAR_MAP.containsKey(value) -> {
                    scalars += ANSEL_CHAR_MAP.getValue(value)
                    if (modifiers.isNotEmpty()) {
                        flushModifiers()
                    }
                }
                ANSEL_CONTROL_MAP.containsKey(value) -> {
                    if (modifiers.isNotEmpty()) {
                        scalars += space
                        flushModifiers()
                    }
                    scalars += ANSEL_CONTROL_MAP.getValue(value)
                }
                ANSEL_MODIFIER_MAP.containsKey(value) -> {
                    modifiers.addFirst(ANSEL_MODIFIER_MAP.getValue(value))
                }
                else -> {
                    scalars += replacement
                    if (modifiers.isNotEmpty()) {
                        flushModifiers()
                    }
                }
            }
        }

        if (modifiers.isNotEmpty()) {
            scalars += space
            while (modifiers.isNotEmpty()) {
                scalars += modifiers.removeFirst()
            }
        }

        val builder = StringBuilder()
        for (codePoint in scalars) {
            builder.appendCodePointCompat(codePoint)
        }
        return builder.toString()
    }

    companion object {
        private val ANSEL_CONTROL_MAP: Map<Int, Int> = mapOf(
            0x00 to 0x0000,
            0x01 to 0x0001,
            0x02 to 0x0002,
            0x03 to 0x0003,
            0x04 to 0x0004,
            0x05 to 0x0005,
            0x06 to 0x0006,
            0x07 to 0x0007,
            0x08 to 0x0008,
            0x09 to 0x0009,
            0x0A to 0x000A,
            0x0B to 0x000B,
            0x0C to 0x000C,
            0x0D to 0x000D,
            0x0E to 0x000E,
            0x0F to 0x000F,
            0x10 to 0x0010,
            0x11 to 0x0011,
            0x12 to 0x0012,
            0x13 to 0x0013,
            0x14 to 0x0014,
            0x15 to 0x0015,
            0x16 to 0x0016,
            0x17 to 0x0017,
            0x18 to 0x0018,
            0x19 to 0x0019,
            0x1A to 0x001A,
            0x1B to 0x001B,
            0x1C to 0x001C,
            0x1D to 0x001D,
            0x1E to 0x001E,
            0x1F to 0x001F,
        )

        private val ANSEL_CHAR_MAP: Map<Int, Int> = mapOf(
            0x20 to 0x0020,
            0x21 to 0x0021,
            0x22 to 0x0022,
            0x23 to 0x0023,
            0x24 to 0x0024,
            0x25 to 0x0025,
            0x26 to 0x0026,
            0x27 to 0x0027,
            0x28 to 0x0028,
            0x29 to 0x0029,
            0x2A to 0x002A,
            0x2B to 0x002B,
            0x2C to 0x002C,
            0x2D to 0x002D,
            0x2E to 0x002E,
            0x2F to 0x002F,
            0x30 to 0x0030,
            0x31 to 0x0031,
            0x32 to 0x0032,
            0x33 to 0x0033,
            0x34 to 0x0034,
            0x35 to 0x0035,
            0x36 to 0x0036,
            0x37 to 0x0037,
            0x38 to 0x0038,
            0x39 to 0x0039,
            0x3A to 0x003A,
            0x3B to 0x003B,
            0x3C to 0x003C,
            0x3D to 0x003D,
            0x3E to 0x003E,
            0x3F to 0x003F,
            0x40 to 0x0040,
            0x41 to 0x0041,
            0x42 to 0x0042,
            0x43 to 0x0043,
            0x44 to 0x0044,
            0x45 to 0x0045,
            0x46 to 0x0046,
            0x47 to 0x0047,
            0x48 to 0x0048,
            0x49 to 0x0049,
            0x4A to 0x004A,
            0x4B to 0x004B,
            0x4C to 0x004C,
            0x4D to 0x004D,
            0x4E to 0x004E,
            0x4F to 0x004F,
            0x50 to 0x0050,
            0x51 to 0x0051,
            0x52 to 0x0052,
            0x53 to 0x0053,
            0x54 to 0x0054,
            0x55 to 0x0055,
            0x56 to 0x0056,
            0x57 to 0x0057,
            0x58 to 0x0058,
            0x59 to 0x0059,
            0x5A to 0x005A,
            0x5B to 0x005B,
            0x5C to 0x005C,
            0x5D to 0x005D,
            0x5E to 0x005E,
            0x5F to 0x005F,
            0x60 to 0x0060,
            0x61 to 0x0061,
            0x62 to 0x0062,
            0x63 to 0x0063,
            0x64 to 0x0064,
            0x65 to 0x0065,
            0x66 to 0x0066,
            0x67 to 0x0067,
            0x68 to 0x0068,
            0x69 to 0x0069,
            0x6A to 0x006A,
            0x6B to 0x006B,
            0x6C to 0x006C,
            0x6D to 0x006D,
            0x6E to 0x006E,
            0x6F to 0x006F,
            0x70 to 0x0070,
            0x71 to 0x0071,
            0x72 to 0x0072,
            0x73 to 0x0073,
            0x74 to 0x0074,
            0x75 to 0x0075,
            0x76 to 0x0076,
            0x77 to 0x0077,
            0x78 to 0x0078,
            0x79 to 0x0079,
            0x7A to 0x007A,
            0x7B to 0x007B,
            0x7C to 0x007C,
            0x7D to 0x007D,
            0x7E to 0x007E,
            0x7F to 0x007F,
            0xA1 to 0x0141,
            0xA2 to 0x00D8,
            0xA3 to 0x0110,
            0xA4 to 0x00DE,
            0xA5 to 0x00C6,
            0xA6 to 0x0152,
            0xA7 to 0x02B9,
            0xA8 to 0x00B7,
            0xA9 to 0x266D,
            0xAA to 0x00AE,
            0xAB to 0x00B1,
            0xAC to 0x01A0,
            0xAD to 0x01AF,
            0xAE to 0x02BC,
            0xB0 to 0x02BB,
            0xB1 to 0x0142,
            0xB2 to 0x00F8,
            0xB3 to 0x0111,
            0xB4 to 0x00FE,
            0xB5 to 0x00E6,
            0xB6 to 0x0153,
            0xB7 to 0x02BA,
            0xB8 to 0x0131,
            0xB9 to 0x00A3,
            0xBA to 0x00F0,
            0xBC to 0x01A1,
            0xBD to 0x01B0,
            0xC0 to 0x00B0,
            0xC1 to 0x2113,
            0xC2 to 0x2117,
            0xC3 to 0x00A9,
            0xC4 to 0x266F,
            0xC5 to 0x00BF,
            0xC6 to 0x00A1,
        )

        private val ANSEL_MODIFIER_MAP: Map<Int, Int> = mapOf(
            0xE0 to 0x0309,
            0xE1 to 0x0300,
            0xE2 to 0x0301,
            0xE3 to 0x0302,
            0xE4 to 0x0303,
            0xE5 to 0x0304,
            0xE6 to 0x0306,
            0xE7 to 0x0307,
            0xE8 to 0x0308,
            0xE9 to 0x030C,
            0xEA to 0x030A,
            0xEB to 0xFE20,
            0xEC to 0xFE21,
            0xED to 0x0315,
            0xEE to 0x030B,
            0xEF to 0x0310,
            0xF0 to 0x0327,
            0xF1 to 0x0328,
            0xF2 to 0x0323,
            0xF3 to 0x0324,
            0xF4 to 0x0325,
            0xF5 to 0x0333,
            0xF6 to 0x0332,
            0xF7 to 0x0326,
            0xF8 to 0x031C,
            0xF9 to 0x032E,
            0xFA to 0xFE22,
            0xFB to 0xFE23,
            0xFE to 0x0313,
        )
    }
}

private fun StringBuilder.appendCodePointCompat(codePoint: Int) {
    append(Character.toChars(codePoint))
}
