package com.lewisdeveloping.gedcomviewer.data

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class GedcomRepository(
    private val application: Application,
    private val parser: GedcomParser = GedcomParser()
) {
    suspend fun loadSample(): GedcomData = withContext(Dispatchers.IO) {
        application.assets.open(DEFAULT_FILE).use { stream ->
            parser.parse(stream, SAMPLE_SOURCE_ID)
        }
    }

    suspend fun loadFromUri(uri: Uri): GedcomData = withContext(Dispatchers.IO) {
        val resolver = application.contentResolver
        val stream = resolver.openInputStream(uri) ?: throw IllegalArgumentException("Unable to open file")
        stream.use { parser.parse(it, sourceIdentifierFor(uri)) }
    }

    private fun sourceIdentifierFor(uri: Uri): String {
        val scheme = uri.scheme?.lowercase(Locale.getDefault())
        return when (scheme) {
            "file" -> {
                val canonical = uri.path?.takeIf { it.isNotBlank() } ?: uri.toString()
                "file::$canonical"
            }
            else -> "uri::${uri.toString()}"
        }
    }

    companion object {
        const val DEFAULT_FILE = "Sample-GEDCOM.ged"
        private const val SAMPLE_SOURCE_ID = "sample::Sample-GEDCOM"
    }
}
