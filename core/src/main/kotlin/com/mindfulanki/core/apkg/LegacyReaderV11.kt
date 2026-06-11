package com.mindfulanki.core.apkg

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reader for the legacy Anki schema (v11), used by `collection.anki2` exports
 * (Anki's "Support older Anki versions" option). Note types and decks live in
 * JSON blobs in the single-row `col` table.
 */
class LegacyReaderV11 : AnkiCollectionReader {

    private val json = Json { ignoreUnknownKeys = true }

    override fun read(db: SqlQuerier): AnkiCollection {
        val col = db.query("SELECT models, decks FROM col LIMIT 1") { row ->
            (row.getString("models").orEmpty()) to (row.getString("decks").orEmpty())
        }.firstOrNull() ?: error("collection has no `col` row")

        val noteTypes = parseModels(col.first)
        val deckNames = parseDecks(col.second)

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

    private fun parseModels(modelsJson: String): Map<Long, NoteType> {
        val root = json.parseToJsonElement(modelsJson).jsonObject
        return root.values.associate { element ->
            val model = element.jsonObject
            val id = model["id"]!!.jsonPrimitive.content.toLong()
            val fields = model["flds"]!!.jsonArray
                .map { it.jsonObject }
                .sortedBy { it["ord"]!!.jsonPrimitive.int }
                .map { it["name"]!!.jsonPrimitive.content }
            val templates = model["tmpls"]!!.jsonArray.map { it.toCardTemplate() }
            id to NoteType(id, fields, templates)
        }
    }

    private fun parseDecks(decksJson: String): Map<Long, String> {
        val root = json.parseToJsonElement(decksJson).jsonObject
        return root.values.associate { element ->
            val deck = element.jsonObject
            val id = deck["id"]!!.jsonPrimitive.content.toLong()
            id to deck["name"]!!.jsonPrimitive.content
        }
    }

    private fun JsonElement.toCardTemplate(): CardTemplate {
        val tmpl = jsonObject
        return CardTemplate(
            ord = tmpl["ord"]!!.jsonPrimitive.int,
            qfmt = tmpl["qfmt"]!!.jsonPrimitive.content,
            afmt = tmpl["afmt"]!!.jsonPrimitive.content,
        )
    }
}
