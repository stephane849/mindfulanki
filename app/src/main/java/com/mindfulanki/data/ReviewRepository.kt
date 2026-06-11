package com.mindfulanki.data

import com.mindfulanki.core.fsrs.Fsrs
import com.mindfulanki.core.fsrs.Rating
import com.mindfulanki.core.fsrs.SchedulingState
import com.mindfulanki.data.db.AppDatabase
import com.mindfulanki.data.db.CardEntity
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

    suspend fun dueQueue(deckId: Long, limit: Int = SESSION_LIMIT): List<CardEntity> =
        withContext(Dispatchers.IO) {
            cardDao.dueCards(deckId, Instant.now().toEpochMilli(), limit)
        }

    suspend fun dueCount(deckId: Long): Int = withContext(Dispatchers.IO) {
        cardDao.dueCount(deckId, Instant.now().toEpochMilli())
    }

    suspend fun reviewedToday(startOfDay: Instant): Int = withContext(Dispatchers.IO) {
        cardDao.reviewedSince(startOfDay.toEpochMilli())
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
