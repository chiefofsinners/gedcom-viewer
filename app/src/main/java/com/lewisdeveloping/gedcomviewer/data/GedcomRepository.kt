package com.lewisdeveloping.gedcomviewer.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GedcomRepository(
    private val assets: AssetManager,
    private val parser: GedcomParser = GedcomParser()
) {
    suspend fun load(fileName: String = DEFAULT_FILE): GedcomData = withContext(Dispatchers.IO) {
        assets.open(fileName).use(parser::parse)
    }

    companion object {
        const val DEFAULT_FILE = "Sample-GEDCOM.ged"
    }
}
