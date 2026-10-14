package com.motsmeles.game.domain.engine

/** < 3:00 = 3 étoiles ; 3:00 à < 10:00 = 2 étoiles ; ≥ 10:00 = 1 étoile (10:00 exactement = 1). */
object StarRating {
    const val THREE_STARS_BELOW_MS = 180_000L
    const val TWO_STARS_BELOW_MS = 600_000L

    fun starsFor(elapsedMillis: Long): Int = when {
        elapsedMillis < THREE_STARS_BELOW_MS -> 3
        elapsedMillis < TWO_STARS_BELOW_MS -> 2
        else -> 1
    }
}
