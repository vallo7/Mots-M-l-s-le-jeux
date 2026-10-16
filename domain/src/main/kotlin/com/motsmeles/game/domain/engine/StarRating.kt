package com.motsmeles.game.domain.engine

/** < 1:00 = 3 étoiles ; 1:00 à < 3:00 = 2 étoiles ; ≥ 3:00 = 1 étoile (3:00 exactement = 1). */
object StarRating {
    const val THREE_STARS_BELOW_MS = 60_000L
    const val TWO_STARS_BELOW_MS = 180_000L

    fun starsFor(elapsedMillis: Long): Int = when {
        elapsedMillis < THREE_STARS_BELOW_MS -> 3
        elapsedMillis < TWO_STARS_BELOW_MS -> 2
        else -> 1
    }
}
