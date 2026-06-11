package com.mindfulanki.core.fsrs

import java.time.Instant

/**
 * Per-card spaced-repetition memory state.
 *
 * A freshly imported card starts as [new] (no stability/difficulty and no
 * review history). After the first grade it gains a [stability] (memory
 * half-life, in days) and a [difficulty] in [1, 10].
 */
data class SchedulingState(
    val stability: Double? = null,
    val difficulty: Double? = null,
    val due: Instant,
    val lastReview: Instant? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
) {
    val isNew: Boolean get() = stability == null || lastReview == null

    companion object {
        /** A brand-new card, due immediately so it enters the new-card queue. */
        fun new(now: Instant): SchedulingState = SchedulingState(due = now)
    }
}
