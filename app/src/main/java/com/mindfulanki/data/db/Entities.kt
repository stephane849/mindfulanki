package com.mindfulanki.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A deck the user has imported. */
@Entity(tableName = "decks")
data class DeckEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
)

/**
 * A reviewable card: rendered text content plus its FSRS scheduling state.
 * Content and scheduling are co-located for simple, single-row review updates.
 *
 * A card is "new" when [lastReviewEpochMillis] is null. [dueEpochMillis] is set
 * to import time for new cards so they enter the queue immediately.
 */
@Entity(
    tableName = "cards",
    indices = [Index("deckId"), Index("dueEpochMillis")],
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val front: String,
    val back: String,
    val stability: Double? = null,
    val difficulty: Double? = null,
    val dueEpochMillis: Long,
    val lastReviewEpochMillis: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
)
