package com.mindfulanki.core.fsrs

import java.time.Duration
import java.time.Instant
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Free Spaced Repetition Scheduler (FSRS v5).
 *
 * A faithful, dependency-free Kotlin implementation of the algorithm Anki uses
 * to schedule reviews. Given a card's [SchedulingState] and a [Rating], it
 * returns the next state — updated stability, difficulty and due date.
 *
 * Formulas follow the published FSRS-4.5/5 specification. The 19 default
 * [weights] are the community/Anki defaults; per-user optimisation is future
 * work (we have no review history to train on yet).
 */
class Fsrs(
    private val weights: DoubleArray = DEFAULT_WEIGHTS,
    /** Target probability of recall when the next interval is scheduled. */
    private val requestRetention: Double = 0.9,
    /** Hard cap on interval length, in days (Anki's default is 100 years). */
    private val maximumIntervalDays: Long = 36_500L,
) {
    init {
        require(weights.size == DEFAULT_WEIGHTS.size) {
            "FSRS-5 expects ${DEFAULT_WEIGHTS.size} weights, got ${weights.size}"
        }
    }

    /** Apply a grade to a card and return its next [SchedulingState]. */
    fun review(state: SchedulingState, rating: Rating, now: Instant): SchedulingState {
        val g = rating.value
        return if (state.isNew) {
            val stability = initialStability(g)
            val difficulty = initialDifficulty(g)
            schedule(state, stability, difficulty, rating, now)
        } else {
            val elapsedDays = max(0.0, daysBetween(state.lastReview!!, now))
            val retrievability = forgettingCurve(elapsedDays, state.stability!!)
            val difficulty = nextDifficulty(state.difficulty!!, g)
            val stability = if (rating == Rating.AGAIN) {
                nextForgetStability(difficulty, state.stability, retrievability)
            } else {
                nextRecallStability(difficulty, state.stability, retrievability, rating)
            }
            schedule(state, stability, difficulty, rating, now)
        }
    }

    /** Number of whole days until this card is next due (for display/queues). */
    fun intervalDays(state: SchedulingState): Long {
        val s = state.stability ?: return 0L
        return nextIntervalDays(s)
    }

    /** Probability the card is currently recallable, in [0, 1]. */
    fun retrievability(state: SchedulingState, now: Instant): Double {
        val s = state.stability ?: return 0.0
        val last = state.lastReview ?: return 0.0
        return forgettingCurve(max(0.0, daysBetween(last, now)), s)
    }

    private fun schedule(
        prev: SchedulingState,
        stability: Double,
        difficulty: Double,
        rating: Rating,
        now: Instant,
    ): SchedulingState {
        val clampedStability = max(MIN_STABILITY, stability)
        val intervalDays = nextIntervalDays(clampedStability)
        return SchedulingState(
            stability = clampedStability,
            difficulty = clampDifficulty(difficulty),
            due = now.plus(Duration.ofDays(intervalDays)),
            lastReview = now,
            reps = prev.reps + 1,
            lapses = prev.lapses + if (rating == Rating.AGAIN) 1 else 0,
        )
    }

    // --- FSRS core formulas -------------------------------------------------

    private fun initialStability(g: Int): Double = max(MIN_STABILITY, weights[g - 1])

    private fun initialDifficulty(g: Int): Double =
        clampDifficulty(weights[4] - exp(weights[5] * (g - 1)) + 1)

    /** R(t) = (1 + FACTOR * t / S) ^ DECAY */
    private fun forgettingCurve(elapsedDays: Double, stability: Double): Double =
        (1.0 + FACTOR * elapsedDays / stability).pow(DECAY)

    private fun nextDifficulty(d: Double, g: Int): Double {
        val deltaD = -weights[6] * (g - 3)
        // Linear damping keeps difficulty from saturating at the extremes.
        val damped = d + deltaD * (10.0 - d) / 9.0
        // Mean reversion toward the "Easy" initial difficulty.
        val reverted = weights[7] * initialDifficulty(Rating.EASY.value) + (1.0 - weights[7]) * damped
        return clampDifficulty(reverted)
    }

    private fun nextRecallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.HARD) weights[15] else 1.0
        val easyBonus = if (rating == Rating.EASY) weights[16] else 1.0
        val growth = exp(weights[8]) *
            (11.0 - d) *
            s.pow(-weights[9]) *
            (exp(weights[10] * (1.0 - r)) - 1.0) *
            hardPenalty *
            easyBonus
        return s * (1.0 + growth)
    }

    private fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        val postLapse = weights[11] *
            d.pow(-weights[12]) *
            ((s + 1.0).pow(weights[13]) - 1.0) *
            exp(weights[14] * (1.0 - r))
        // A lapse must never increase stability.
        return min(postLapse, s)
    }

    /** Invert the forgetting curve to find the interval that hits [requestRetention]. */
    private fun nextIntervalDays(stability: Double): Long {
        val raw = (stability / FACTOR) * (requestRetention.pow(1.0 / DECAY) - 1.0)
        return raw.roundToLongClamped(1L, maximumIntervalDays)
    }

    private fun clampDifficulty(d: Double): Double = d.coerceIn(1.0, 10.0)

    private fun daysBetween(from: Instant, to: Instant): Double =
        Duration.between(from, to).toMillis() / MILLIS_PER_DAY

    private fun Double.roundToLongClamped(lo: Long, hi: Long): Long =
        Math.round(this).coerceIn(lo, hi)

    companion object {
        // FSRS-5 fixes the power-law decay; FSRS-6 makes it a learnable weight.
        private const val DECAY = -0.5
        private val FACTOR = 0.9.pow(1.0 / DECAY) - 1.0 // = 19/81
        private const val MIN_STABILITY = 0.01
        private const val MILLIS_PER_DAY = 86_400_000.0

        /** Anki's default FSRS-5 parameter set. */
        val DEFAULT_WEIGHTS = doubleArrayOf(
            0.40255, 1.18385, 3.173, 15.69105, 7.1949, 0.5345, 1.4604, 0.0046,
            1.54575, 0.1192, 1.01925, 1.9395, 0.11, 0.29605, 2.2698, 0.2315,
            2.9898, 0.51655, 0.6621,
        )
    }
}
