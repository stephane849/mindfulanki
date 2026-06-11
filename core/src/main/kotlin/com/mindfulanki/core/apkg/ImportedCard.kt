package com.mindfulanki.core.apkg

/** A fully rendered, text-only card ready to be stored and reviewed. */
data class ImportedCard(
    val deckName: String,
    val front: String,
    val back: String,
)
