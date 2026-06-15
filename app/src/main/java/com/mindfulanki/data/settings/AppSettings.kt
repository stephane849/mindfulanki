package com.mindfulanki.data.settings

/** The three study layouts the design lets you switch between in Settings. */
enum class StudyVariant { CLASSIC, PAPER, FOCUS }

/**
 * User preferences mirrored from the design's Settings screen. Calm by default:
 * a gentle daily goal, no streaks.
 */
data class AppSettings(
    val invert: Boolean = false,
    val newPerDay: Int = 10,
    val dailyGoal: Int = 20,
    val showIntervals: Boolean = true,
    val variant: StudyVariant = StudyVariant.CLASSIC,
    /** Percent scale applied to flashcard text (100 = design default). */
    val textScalePercent: Int = 100,
) {
    /** Multiplier form of [textScalePercent] for sizing card text. */
    val textScale: Float get() = textScalePercent / 100f
}
