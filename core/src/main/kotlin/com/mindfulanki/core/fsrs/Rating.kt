package com.mindfulanki.core.fsrs

/**
 * The four grades a reviewer can give a card, matching Anki/FSRS semantics.
 * The ordinal values (1..4) are the `G` used directly in the FSRS formulas.
 */
enum class Rating(val value: Int) {
    AGAIN(1),
    HARD(2),
    GOOD(3),
    EASY(4),
}
