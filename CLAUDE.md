# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

GEDCOM Viewer is a native Android genealogy app for browsing and visualizing family tree data from GEDCOM files. Built with Kotlin and Jetpack Compose targeting Android API 28+ (minSdk), API 36 (compileSdk).

## Commands

```bash
# Build
./gradlew build
./gradlew assembleDebug
./gradlew assembleRelease    # includes ProGuard minification

# Test
./gradlew test               # unit tests only
./gradlew connectedAndroidTest  # requires connected device/emulator

# Run a single test class
./gradlew test --tests "com.lewisdeveloping.gedcomviewer.GedcomParserTest"

# Clean
./gradlew clean
```

## Architecture

MVVM with Jetpack Compose and StateFlow. Single ViewModel (`GedcomViewModel`) holds all UI state.

**Data flow:**
1. User picks a `.ged` file or loads sample data
2. `GedcomRepository` reads the file and delegates to `GedcomParser`
3. Parser produces `GedcomData` containing maps of `Individual` and `Family` records
4. ViewModel updates `GedcomUiState` (a single StateFlow-backed data class)
5. Compose UI reacts to state changes

**Key packages:**
- `data/` — GEDCOM parsing (`GedcomParser.kt` uses regex line-by-line), file access, character encoding
- `model/` — Domain models: `Individual`, `Family`, `LifeEvent`, `TimelineEntry`
- `ui/screens/` — Three screens: Home (file picker), Index (searchable individual list), Family (tree view with nav history)
- `ui/components/` — Reusable composables: `PersonCard`, `IndividualDetailsDialog`, `FileActionBar`
- `ui/theme/` — Material3 theme with two built-in palettes (SILVER, EARTH) persisted via SharedPreferences

**Navigation:** Bottom tab bar with three tabs (Home/Index/Family). Navigation history for the Family tree view is maintained as a stack within `GedcomUiState`.

**Persistence:** Last-loaded file URI and theme choice are stored in SharedPreferences. Index sort order is preserved across tab transitions.

## GEDCOM Parser Notes

The parser (`GedcomParser.kt`) uses the regex `^(\d+)\s+(?:(@[^@]+@)\s+)?([A-Z0-9_]+)(?:\s+(.*))?$` to parse each line. Supported individual event tags: BIRT, DEAT, BAPM, BAPT, CHR, CHRA, RESI, OCCU, BURI, GRAD, EDUC, EVEN. Supports CONC/CONT for multi-line values and GIVN/SURN subtags for structured name fields.
