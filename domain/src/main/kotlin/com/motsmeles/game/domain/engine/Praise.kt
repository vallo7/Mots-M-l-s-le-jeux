package com.motsmeles.game.domain.engine

/** Échelle de praise, du plus modeste au plus rare. Les clés techniques servent aussi de noms de fichiers. */
enum class Praise(val key: String) {
    NICE("nice"),
    GREAT("great"),
    AWESOME("awesome"),
    AMAZING("amazing"),
    INSANE("insane"),
    UNREAL("unreal"),
    DIVINE("divine"),
    LEGENDARY("legendary");

    companion object {
        fun fromKey(key: String?): Praise? = entries.firstOrNull { it.key == key }
    }
}

data class PraiseContext(
    val stars: Int,
    val hintsUsed: Int,
    val wrongSelections: Int,
    val elapsedMillis: Long,
    val isFinalChallenge: Boolean,
    /** Nombre de niveaux consécutifs à 3 étoiles *avant* celui-ci. */
    val perfectStreakBefore: Int,
)

/**
 * Praise pseudo-aléatoire contrôlé : le niveau maximal dépend de la performance (0..7 points),
 * le tirage est déterministe (graine) et évite de répéter le praise précédent.
 */
object PraiseSelector {
    const val FAST_BELOW_MS = 90_000L

    fun points(ctx: PraiseContext): Int {
        var p = (ctx.stars.coerceIn(1, 3) - 1)
        if (ctx.hintsUsed == 0) p += 1
        if (ctx.wrongSelections == 0) p += 1
        if (ctx.elapsedMillis < FAST_BELOW_MS) p += 1
        if (ctx.isFinalChallenge && ctx.stars >= 2) p += 1
        if (ctx.stars == 3 && ctx.perfectStreakBefore >= 2) p += 1
        return p.coerceIn(0, Praise.entries.lastIndex)
    }

    fun select(ctx: PraiseContext, previous: Praise?, seed: Long): Praise {
        val cap = points(ctx)
        var candidates = Praise.entries.subList(maxOf(0, cap - 1), cap + 1)
        if (previous != null && candidates.size > 1) {
            candidates = candidates.filter { it != previous }
        }
        val index = Math.floorMod(mix(seed), candidates.size.toLong()).toInt()
        return candidates[index]
    }

    private fun mix(x: Long): Long {
        var z = x + -7046029254386353131L
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
    }
}
