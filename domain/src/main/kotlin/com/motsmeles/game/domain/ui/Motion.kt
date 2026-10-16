package com.motsmeles.game.domain.ui

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mathématiques de mouvement pures (testables sur JVM) : ressorts amortis analytiques, courbes d'accélération
 * et arcs. L'interface les évalue à chaque image à partir d'un temps écoulé : aucune animation par case,
 * aucun état par élément, donc fluide même sur une grille 12×12.
 */
object Motion {
    fun clamp01(x: Float): Float = if (x < 0f) 0f else if (x > 1f) 1f else x
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * Réponse en échelon d'un ressort amorti : 0 à t = 0 puis tend vers 1 en dépassant 1 (rebond) si [damping] < 1.
     * [frequency] en rad/s (≈ 10 = mou, ≈ 22 = nerveux), [damping] ∈ (0, ∞) (≈ 0,45 = bien rebondissant).
     */
    fun spring(t: Float, frequency: Float = 16f, damping: Float = 0.5f): Float {
        if (t <= 0f) return 0f
        val w = frequency.toDouble()
        val z = damping.toDouble()
        val tt = t.toDouble()
        val v = if (z < 1.0) {
            val wd = w * sqrt(1.0 - z * z)
            1.0 - exp(-z * w * tt) * (cos(wd * tt) + (z * w / wd) * sin(wd * tt))
        } else {
            1.0 - exp(-w * tt) * (1.0 + w * tt)
        }
        return v.toFloat()
    }

    /** Durée (s) au bout de laquelle le ressort est à moins de [tolerance] de sa cible : sert à arrêter la boucle d'images. */
    fun springSettleSeconds(frequency: Float = 16f, damping: Float = 0.5f, tolerance: Float = 0.01f): Float =
        (-ln(tolerance.toDouble()) / (damping.coerceAtMost(1f) * frequency)).toFloat()

    /** Départ lent, arrivée avec un léger dépassement (0..1 → 0..~1,1..1). */
    fun easeOutBack(t: Float, overshoot: Float = 1.70158f): Float {
        val x = clamp01(t) - 1f
        return 1f + x * x * ((overshoot + 1f) * x + overshoot)
    }

    fun easeOutCubic(t: Float): Float {
        val x = 1f - clamp01(t)
        return 1f - x * x * x
    }

    fun easeInOutCubic(t: Float): Float {
        val x = clamp01(t)
        return if (x < 0.5f) 4f * x * x * x else 1f - ((-2f * x + 2f).let { it * it * it }) / 2f
    }

    /** Bosse 0 → 1 → 0 (sinus) : impulsion de « squash » ou de saut. */
    fun bump(t: Float): Float = sin(PI * clamp01(t)).toFloat()

    /** Point d'une courbe de Bézier quadratique (déplacements en arc des lettres). */
    fun quad(a: Float, control: Float, b: Float, t: Float): Float {
        val u = 1f - t
        return u * u * a + 2f * u * t * control + t * t * b
    }
}
