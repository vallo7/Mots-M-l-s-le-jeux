package com.motsmeles.game.presentation.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.domain.model.VisualStyle

/** Jetons du système de design (voir docs/DESIGN_SYSTEM.md). */
object WgColors {
    val Cream = Color(0xFFFFF8EE)
    val Surface = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2B2A33)
    val InkMuted = Color(0xFF6E6C7A)
    val Outline = Color(0xFFE7DFD2)
    val Coral = Color(0xFFFF7A59)
    val Teal = Color(0xFF2BB3A3)
    val Sun = Color(0xFFFFC145)
    val Plum = Color(0xFF6C4AB6)
    val Error = Color(0xFFD64550)
    val Scrim = Color(0x66000000)
}

object WgSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Xl = 32.dp
}

object WgRadius {
    val Sm = 8.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Pill = 999.dp
}

object WgSize {
    val MinTouch = 48.dp
    val Button = 56.dp
    val Tile = 56.dp
}

/** Durées d'animation (ms). Interactions 100–300 ms ; aucune animation de fondu (D-011). */
object WgMotion {
    const val QuickMs = 120
    const val StandardMs = 220
    const val SlowMs = 300
    /** Micro-délai avant une action de navigation, pour laisser jouer l'animation de pression. */
    const val PressDelayMs = 90L
}

/** Couleurs d'un style visuel de difficulté. Trois styles tournent selon le chapitre, FINAL est réservé au niveau 10. */
data class StylePalette(val accent: Color, val soft: Color, val onAccent: Color)

object WgStyles {
    fun forStyle(style: VisualStyle): StylePalette = when (style) {
        VisualStyle.STYLE_1 -> StylePalette(WgColors.Teal, Color(0xFFD5F2EE), WgColors.Ink)
        VisualStyle.STYLE_2 -> StylePalette(WgColors.Sun, Color(0xFFFFF0CC), WgColors.Ink)
        VisualStyle.STYLE_3 -> StylePalette(WgColors.Coral, Color(0xFFFFE0D7), WgColors.Ink)
        VisualStyle.FINAL -> StylePalette(WgColors.Plum, Color(0xFFE4DAFA), Color.White)
    }
}

/** Ambiance d'un chapitre (clé `paletteKey` des données). Clé inconnue : ambiance par défaut. */
data class ChapterTheme(val background: Color, @DrawableRes val illustration: Int)

object WgChapters {
    fun forKey(key: String): ChapterTheme = when (key) {
        "dusk" -> ChapterTheme(Color(0xFFF7F3FC), R.drawable.ill_chapter_dusk)
        else -> ChapterTheme(Color(0xFFF8FBF1), R.drawable.ill_chapter_meadow)
    }
}
