package com.mindfulanki.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A deck with its live due/new counts, for the deck list. */
data class DeckSummary(
    val id: Long,
    val name: String,
    val dueCount: Int,
    val newCount: Int,
    val totalCount: Int,
)

@Dao
interface DeckDao {
    @Insert
    suspend fun insert(deck: DeckEntity): Long

    @Query("SELECT id FROM decks WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Long?

    @Query("SELECT * FROM decks ORDER BY name")
    suspend fun decks(): List<DeckEntity>

    /**
     * Deck list with counts. `dueCount` is cards already studied that have come
     * due again; `newCount` is the subset never reviewed.
     */
    @Query(
        """
        SELECT d.id AS id, d.name AS name,
               COUNT(CASE WHEN c.lastReviewEpochMillis IS NOT NULL AND c.dueEpochMillis <= :now THEN 1 END) AS dueCount,
               COUNT(CASE WHEN c.lastReviewEpochMillis IS NULL THEN 1 END) AS newCount,
               COUNT(c.id) AS totalCount
        FROM decks d
        LEFT JOIN cards c ON c.deckId = d.id
        GROUP BY d.id, d.name
        ORDER BY d.name
        """,
    )
    fun observeSummaries(now: Long): Flow<List<DeckSummary>>

    @Query("SELECT name FROM decks WHERE id = :id")
    suspend fun deckName(id: Long): String?
}

@Dao
interface CardDao {
    @Insert
    suspend fun insertAll(cards: List<CardEntity>)

    @Insert
    suspend fun insert(card: CardEntity): Long

    @Update
    suspend fun update(card: CardEntity)

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: Long): CardEntity?

    /** Previously-studied cards that have come due again, soonest first. */
    @Query(
        "SELECT * FROM cards WHERE deckId = :deckId AND lastReviewEpochMillis IS NOT NULL " +
            "AND dueEpochMillis <= :now ORDER BY dueEpochMillis LIMIT :limit",
    )
    suspend fun reviewDueCards(deckId: Long, now: Long, limit: Int): List<CardEntity>

    /** Never-reviewed cards, oldest first — capped per session by "new per day". */
    @Query("SELECT * FROM cards WHERE deckId = :deckId AND lastReviewEpochMillis IS NULL ORDER BY id LIMIT :limit")
    suspend fun newCards(deckId: Long, limit: Int): List<CardEntity>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId AND lastReviewEpochMillis IS NOT NULL AND dueEpochMillis <= :now")
    suspend fun dueCount(deckId: Long, now: Long): Int

    @Query("SELECT COUNT(*) FROM cards WHERE lastReviewEpochMillis >= :since")
    suspend fun reviewedSince(since: Long): Int
}
