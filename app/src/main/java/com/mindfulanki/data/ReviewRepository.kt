package com.mindfulanki.data

import com.mindfulanki.core.fsrs.Fsrs
import com.mindfulanki.core.fsrs.Rating
import com.mindfulanki.core.fsrs.SchedulingState
import com.mindfulanki.data.db.AppDatabase
import com.mindfulanki.data.db.CardEntity
import com.mindfulanki.data.db.DeckEntity
import com.mindfulanki.data.db.DeckSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Bridges the persisted [CardEntity] rows and the pure-JVM [Fsrs] scheduler:
 * loads the review queue, applies grades, and writes the next state back.
 */
class ReviewRepository(
    private val db: AppDatabase,
    private val fsrs: Fsrs = Fsrs(),
) {
    private val deckDao = db.deckDao()
    private val cardDao = db.cardDao()

    fun observeDecks(now: Instant = Instant.now()): Flow<List<DeckSummary>> =
        deckDao.observeSummaries(now.toEpochMilli())

    /**
     * The study queue for a session: due reviews first (soonest due first),
     * then up to [newPerDay] never-seen cards — mirroring the design's
     * learns → dues → news ordering.
     */
    suspend fun studyQueue(deckId: Long, newPerDay: Int): List<CardEntity> =
        withContext(Dispatchers.IO) {
            val now = Instant.now().toEpochMilli()
            val due = cardDao.reviewDueCards(deckId, now, SESSION_LIMIT)
            val new = if (newPerDay > 0) cardDao.newCards(deckId, newPerDay) else emptyList()
            due + new
        }

    suspend fun dueCount(deckId: Long): Int = withContext(Dispatchers.IO) {
        cardDao.dueCount(deckId, Instant.now().toEpochMilli())
    }

    suspend fun decks(): List<DeckEntity> = withContext(Dispatchers.IO) { deckDao.decks() }

    suspend fun card(id: Long): CardEntity? = withContext(Dispatchers.IO) { cardDao.getById(id) }

    /** Create a new card in a deck, due immediately so it enters the new queue. */
    suspend fun addCard(deckId: Long, front: String, back: String): Unit = withContext(Dispatchers.IO) {
        cardDao.insert(
            CardEntity(
                deckId = deckId,
                front = front,
                back = back,
                dueEpochMillis = Instant.now().toEpochMilli(),
            ),
        )
    }

    /** Update a card's text content, leaving its scheduling untouched. */
    suspend fun updateContent(id: Long, front: String, back: String): Unit = withContext(Dispatchers.IO) {
        cardDao.getById(id)?.let { cardDao.update(it.copy(front = front, back = back)) }
    }

    suspend fun reviewedToday(startOfDay: Instant): Int = withContext(Dispatchers.IO) {
        cardDao.reviewedSince(startOfDay.toEpochMilli())
    }

    suspend fun deckName(deckId: Long): String? = withContext(Dispatchers.IO) {
        deckDao.deckName(deckId)
    }

    /**
     * The next interval (in whole days) each grade would schedule for [card],
     * without persisting anything. Used to label the grade buttons, like Anki.
     * Pure/in-memory — no DB access.
     */
    fun previewIntervals(card: CardEntity, now: Instant = Instant.now()): Map<Rating, Long> {
        val state = card.toSchedulingState()
        return Rating.values().associateWith { rating ->
            fsrs.intervalDays(fsrs.review(state, rating, now))
        }
    }

    /** Apply a [rating] to a card, persisting its next FSRS state. */
    suspend fun grade(card: CardEntity, rating: Rating, now: Instant = Instant.now()): CardEntity =
        withContext(Dispatchers.IO) {
            val next = fsrs.review(card.toSchedulingState(), rating, now)
            val updated = card.applyScheduling(next)
            cardDao.update(updated)
            updated
        }

    private fun CardEntity.toSchedulingState(): SchedulingState = SchedulingState(
        stability = stability,
        difficulty = difficulty,
        due = Instant.ofEpochMilli(dueEpochMillis),
        lastReview = lastReviewEpochMillis?.let(Instant::ofEpochMilli),
        reps = reps,
        lapses = lapses,
    )

    private fun CardEntity.applyScheduling(state: SchedulingState): CardEntity = copy(
        stability = state.stability,
        difficulty = state.difficulty,
        dueEpochMillis = state.due.toEpochMilli(),
        lastReviewEpochMillis = state.lastReview?.toEpochMilli(),
        reps = state.reps,
        lapses = state.lapses,
    )

    private companion object {
        const val SESSION_LIMIT = 100
    }
}
