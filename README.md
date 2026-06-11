# MindfulAnki

A calm, offline **Anki flashcard reviewer for the [Mudita Kompakt](https://mudita.com/products/phones/mudita-kompakt/)**,
built with Mudita's [Mindful Design (MMD)](https://github.com/mudita/MMD) UI
framework for E Ink.

Import an Anki `.apkg` deck, review it with real spaced repetition (FSRS), and
keep your progress on-device. No accounts, no sync, no network — in keeping with
the Kompakt's minimalist, privacy-first philosophy.

## Features (MVP)

- **Import `.apkg` decks** — both **legacy** (`collection.anki2`) and **modern**
  (`collection.anki21b`, zstd-compressed, schema v18) exports.
- **Full FSRS scheduling** — Anki's modern spaced-repetition algorithm (FSRS-5),
  implemented from scratch in Kotlin.
- **Local progress** — scheduling state persists in a Room database on the device.
- **Text-only, E Ink-friendly** — card templates are rendered and HTML-stripped to
  clean plain text; one card per screen, large tap targets, minimal refresh.

Out of scope for now: images/audio, cloze cards, writing progress back to `.apkg`,
and AnkiWeb sync. See the plan for the roadmap.

## Architecture

```
:core   Pure Kotlin/JVM — no Android deps. Fully unit-tested.
        ├─ fsrs/      FSRS-5 scheduler (Fsrs, SchedulingState, Rating)
        ├─ template/  Anki template renderer + HTML stripper
        └─ apkg/      .apkg pipeline: unzip + zstd, schema-aware readers
                      (LegacyReaderV11 / ModernReaderV18), CollectionAssembler

:app    Android (Jetpack Compose + MMD)
        ├─ data/      Room (DeckEntity/CardEntity), repositories, Android
        │             SqlQuerier + ApkgImportService
        └─ ui/        ThemeMMD-wrapped screens: DeckList, Review, Stats
```

The hard logic lives in `:core` behind a small `SqlQuerier` abstraction, so the
same readers run under sqlite-jdbc (tests) and `android.database.sqlite` (app).
The modern schema stores template format strings in a protobuf `templates.config`
blob; `ProtoReader` decodes the two fields we need (`q_format`, `a_format`)
without a protobuf runtime.

## Build & test

Requires JDK 17+. The Android `:app` module additionally needs the Android SDK
(set `sdk.dir` in `local.properties` or `ANDROID_HOME`).

```bash
# Run the core logic test suite (FSRS, template, both .apkg parsers)
./gradlew :core:test

# Build a sideloadable APK
./gradlew :app:assembleDebug

# Install on a connected Kompakt
adb install app/build/outputs/apk/debug/app-debug.apk
```

> **Note:** MMD is consumed as `com.mudita:MMD:1.0.0`. Adjust the repository in
> `settings.gradle.kts` if Mudita publishes it elsewhere. The exact MMD component
> signatures (`ThemeMMD`, `TextMMD`, `ButtonMMD`) are the only spots that may need
> tweaking against the released artifact; they are isolated in `ui/theme/Theme.kt`
> and the screen composables.

## Getting a compatible deck

In Anki desktop: **File → Export**, choose **Anki Deck Package (.apkg)**. Either
the default (modern) or "Support older Anki versions" (legacy) export works.
Then transfer the `.apkg` to the Kompakt and pick it via **Import deck**.
