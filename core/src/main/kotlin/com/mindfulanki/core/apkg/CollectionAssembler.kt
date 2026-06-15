package com.mindfulanki.core.apkg

import com.mindfulanki.core.template.TemplateRenderer

/**
 * Turns a parsed [AnkiCollection] into rendered, text-only [ImportedCard]s by
 * applying each card's note-type template to its note fields.
 */
class CollectionAssembler(
    private val renderer: TemplateRenderer = TemplateRenderer(),
) {

    fun assemble(collection: AnkiCollection): List<ImportedCard> {
        val result = ArrayList<ImportedCard>(collection.cards.size)
        for (card in collection.cards) {
            val note = collection.notes[card.noteId] ?: continue
            val noteType = collection.noteTypes[note.noteTypeId] ?: continue
            val template = noteType.templates.firstOrNull { it.ord == card.templateOrd } ?: continue

            val fields = buildFieldMap(noteType, note)
            val front = renderer.render(template.qfmt, fields)
            // The back template can embed the rendered front via {{FrontSide}}.
            val back = renderer.render(template.afmt, fields + ("FrontSide" to front))

            // Skip cards that render to nothing (e.g. conditional cards with no content).
            if (front.isBlank() && back.isBlank()) continue

            result += ImportedCard(
                deckName = collection.deckNames[card.deckId] ?: DEFAULT_DECK_NAME,
                front = front,
                back = back,
            )
        }
        return result
    }

    private fun buildFieldMap(noteType: NoteType, note: RawNote): Map<String, String> {
        val map = LinkedHashMap<String, String>(noteType.fieldNames.size)
        noteType.fieldNames.forEachIndexed { index, name ->
            map[name] = note.fields.getOrElse(index) { "" }
        }
        return map
    }

    private companion object {
        const val DEFAULT_DECK_NAME = "Default"
    }
}
