package com.mindfulanki.core.fsrs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class FsrsTest {

    private val fsrs = Fsrs()
    private val t0: Instant = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun `new card gains stability and difficulty after first grade`() {
        val card = SchedulingState.new(t0)
        assertTrue(card.isNew)

        val reviewed = fsrs.review(card, Rating.GOOD, t0)

        assertTrue(!reviewed.isNew)
        assertTrue(reviewed.stability!! > 0.0)
        assertTrue(reviewed.difficulty!! in 1.0..10.0)
        assertEquals(1, reviewed.reps)
        assertEquals(0, reviewed.lapses)
        assertTrue(reviewed.due.isAfter(t0))
    }

    @Test
    fun `initial stability follows the default weights and grade ordering`() {
        val again = fsrs.review(SchedulingState.new(t0), Rating.AGAIN, t0).stability!!
        val good = fsrs.review(SchedulingState.new(t0), Rating.GOOD, t0).stability!!
        val easy = fsrs.review(SchedulingState.new(t0), Rating.EASY, t0).stability!!

        // Better grades start with longer memory stability.
        assertTrue(again < good)
        assertTrue(good < easy)
        // w[0] and w[2] are the Again/Good initial stabilities.
        assertEquals(Fsrs.DEFAULT_WEIGHTS[0], again, 1e-9)
        assertEquals(Fsrs.DEFAULT_WEIGHTS[2], good, 1e-9)
    }

    @Test
    fun `successful review on time grows the interval`() {
        val first = fsrs.review(SchedulingState.new(t0), Rating.GOOD, t0)
        val firstInterval = fsrs.intervalDays(first)
        val dueDate = first.due

        val second = fsrs.review(first, Rating.GOOD, dueDate)

        assertTrue(second.stability!! > first.stability!!, "stability should increase on recall")
        assertTrue(fsrs.intervalDays(second) > firstInterval, "interval should grow")
        assertEquals(2, second.reps)
    }

    @Test
    fun `a lapse reduces stability and counts a lapse`() {
        val learned = fsrs.review(SchedulingState.new(t0), Rating.EASY, t0)
        val laterReview = learned.due

        val lapsed = fsrs.review(learned, Rating.AGAIN, laterReview)

        assertTrue(lapsed.stability!! <= learned.stability!!, "lapse must not increase stability")
        assertEquals(1, lapsed.lapses)
    }

    @Test
    fun `retrievability decays from 1 toward 0 over time`() {
        val card = fsrs.review(SchedulingState.new(t0), Rating.GOOD, t0)

        val immediately = fsrs.retrievability(card, card.lastReview!!)
        val muchLater = fsrs.retrievability(card, card.lastReview!!.plus(Duration.ofDays(365)))

        assertTrue(immediately > 0.99, "recall right after review should be ~1.0")
        assertTrue(muchLater < immediately)
        assertTrue(muchLater in 0.0..1.0)
    }

    @Test
    fun `easy grade schedules a longer interval than hard`() {
        val hard = fsrs.review(SchedulingState.new(t0), Rating.HARD, t0)
        val easy = fsrs.review(SchedulingState.new(t0), Rating.EASY, t0)

        assertTrue(fsrs.intervalDays(easy) > fsrs.intervalDays(hard))
    }
}
