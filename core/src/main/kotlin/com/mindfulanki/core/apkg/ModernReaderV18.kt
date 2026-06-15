package com.mindfulanki.core.apkg

/**
 * Reader for the modern Anki schema (v18), used by `collection.anki21b`
 * (zstd-compressed) exports. Note types, fields, templates and decks live in
 * dedicated tables; the `col` JSON blobs are empty.
 *
 * The catch: card template format strings are stored inside a protobuf-encoded
 * `templates.config` BLOB (CardTemplateConfig: q_format=1, a_format=2), decoded
 * here with [ProtoReader].
 */
class ModernReaderV18 : AnkiCollectionReader {

    private companion object {
        const val Q_FORMAT_FIELD = 1
        const val A_FORMAT_FIELD = 2
    }

    override fun read(db: SqlQuerier): AnkiCollection {
        val deckNames = db.query("SELECT id, name FROM decks") { row ->
            // v18 uses the unit separator for deck hierarchy; normalise to "::".
            row.getLong("id") to row.getString("name").orEmpty().replace(FIELD_SEPARATOR.toString(), "::")
        }.toMap()

        val fieldsByType = db.query("SELECT ntid, ord, name FROM fields ORDER BY ntid, ord") { row ->
            Triple(row.getLong("ntid"), row.getLong("ord").toInt(), row.getString("name").orEmpty())
        }.groupBy({ it.first }) { it.second to it.third }

        val templatesByType = db.query("SELECT ntid, ord, config FROM templates ORDER BY ntid, ord") { row ->
            val config = row.getBlob("config") ?: ByteArray(0)
            row.getLong("ntid") to CardTemplate(
                ord = row.getLong("ord").toInt(),
                qfmt = ProtoReader.stringField(config, Q_FORMAT_FIELD),
                afmt = ProtoReader.stringField(config, A_FORMAT_FIELD),
            )
        }.groupBy({ it.first }) { it.second }

        val noteTypes = db.query("SELECT id FROM notetypes") { row -> row.getLong("id") }
            .associateWith { id ->
                NoteType(
                    id = id,
                    fieldNames = fieldsByType[id].orEmpty().sortedBy { it.first }.map { it.second },
                    templates = templatesByType[id].orEmpty().sortedBy { it.ord },
                )
            }

        val notes = db.query("SELECT id, mid, flds FROM notes") { row ->
            RawNote(
                id = row.getLong("id"),
                noteTypeId = row.getLong("mid"),
                fields = row.getString("flds").orEmpty().split(FIELD_SEPARATOR),
            )
        }.associateBy { it.id }

        val cards = db.query("SELECT nid, ord, did FROM cards") { row ->
            RawCard(
                noteId = row.getLong("nid"),
                templateOrd = row.getLong("ord").toInt(),
                deckId = row.getLong("did"),
            )
        }

        return AnkiCollection(deckNames, noteTypes, notes, cards)
    }
}
