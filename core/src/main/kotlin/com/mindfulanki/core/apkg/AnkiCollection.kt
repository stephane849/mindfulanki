package com.mindfulanki.core.apkg

/** A card template within a note type (Anki's `tmpls`). */
data class CardTemplate(val ord: Int, val qfmt: String, val afmt: String)

/** An Anki note type / model: its field order and its card templates. */
data class NoteType(
    val id: Long,
    val fieldNames: List<String>,
    val templates: List<CardTemplate>,
)

/** A raw note: ordered field values for a given note type. */
data class RawNote(val id: Long, val noteTypeId: Long, val fields: List<String>)

/** A raw card instance: which note + which template + which deck. */
data class RawCard(val noteId: Long, val templateOrd: Int, val deckId: Long)

/**
 * The schema-agnostic shape both readers produce. The assembler turns this into
 * rendered, deck-grouped cards.
 */
data class AnkiCollection(
    val deckNames: Map<Long, String>,
    val noteTypes: Map<Long, NoteType>,
    val notes: Map<Long, RawNote>,
    val cards: List<RawCard>,
)
