package com.lewisdeveloping.gedcomviewer.data

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GedcomRepository(
    private val application: Application,
    private val parser: GedcomParser = GedcomParser()
) {
    suspend fun loadSample(): GedcomData = withContext(Dispatchers.IO) {
        application.assets.open(DEFAULT_FILE).use(parser::parse)
    }

    suspend fun loadFromUri(uri: Uri): GedcomData = withContext(Dispatchers.IO) {
        val resolver = application.contentResolver
        val stream = resolver.openInputStream(uri) ?: throw IllegalArgumentException("Unable to open file")
        stream.use(parser::parse)
    }

    companion object {
        const val DEFAULT_FILE = "Sample-GEDCOM.ged"
    }
}
