package com.mindfulanki.core.apkg

/**
 * Reads an opened Anki collection database into the schema-agnostic
 * [AnkiCollection] shape. Implementations handle a specific on-disk schema.
 */
interface AnkiCollectionReader {
    fun read(db: SqlQuerier): AnkiCollection

    companion object {
        /**
         * Pick the right reader for an opened database. The modern schema (v18,
         * used by `collection.anki21b`) has dedicated `notetypes`/`templates`
         * tables; the legacy schema (v11, `collection.anki2`) keeps everything
         * in `col` JSON blobs.
         */
        fun forDatabase(db: SqlQuerier): AnkiCollectionReader =
            if (db.hasTable("notetypes")) ModernReaderV18() else LegacyReaderV11()
    }
}

/** Anki separates note fields (and, in v18, deck-name hierarchy) with US (0x1F). */
internal const val FIELD_SEPARATOR = ''
