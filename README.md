# GEDCOM Viewer

A native Android app for browsing genealogy data from GEDCOM files. Built with Kotlin and Jetpack Compose.

## Features

- Open any `.ged` file from device storage, or explore the bundled sample
- Searchable index of individuals
- Family tree view with navigation history (swipe back through ancestors)
- Per-individual details: life events, timeline, sources, notes
- Material 3 theming with two built-in palettes
- Supports GEDCOM 5.5.1, including `CONC`/`CONT` continuations and `GIVN`/`SURN` subtags
- Multiple-marriage and nested-event handling

## Requirements

- Android 9.0 (API 28) or later
- Android Studio (Ladybug or newer recommended)
- JDK 21

## Build

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK with ProGuard
./gradlew test                   # unit tests
./gradlew connectedAndroidTest   # requires a connected device/emulator
```

The release build is unsigned by default — add your own signing config in `app/build.gradle.kts` if you intend to distribute.

## Project Layout

```
app/src/main/java/com/lewisdeveloping/gedcomviewer/
├── data/        GEDCOM parsing, repository, encoding
├── model/       Individual, Family, LifeEvent, TimelineEntry
└── ui/
    ├── screens/     Home, Index, Family
    ├── components/  PersonCard, IndividualDetailsDialog, FileActionBar
    └── theme/       Material3 palettes and typography
```

State is held in a single `GedcomViewModel` exposing a `StateFlow<GedcomUiState>`.

## Sample Data

`app/src/main/assets/Sample-GEDCOM.ged` is the fictional "Munro family" sample distributed with [Family Historian](https://www.calicopie.com/) (© Calico Pie Limited), included for demo purposes.

## License

MIT — see [LICENSE](LICENSE).
