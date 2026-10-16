package com.motsmeles.game.presentation.theme

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.domain.model.VisualStyle

/**
 * Jetons du système de design (voir docs/DESIGN_SYSTEM.md). Palette vive sur fond blanc ; chaque couleur
 * d'action a sa « lèvre » (variante foncée) qui donne le relief 3D des boutons bulles.
 */
object WgColors {
    val White = Color(0xFFFFFFFF)
    val Background = Color(0xFFFFFFFF)
    val Ink = Color(0xFF2A2750)
    val InkMuted = Color(0xFF7B79A6)
    val Outline = Color(0xFFE4E1F7)
    val Track = Color(0xFFE9E6FA)

    val Purple = Color(0xFF7B5CFF)
    val PurpleDark = Color(0xFF5B3EE0)
    val PurpleDeep = Color(0xFF3F2AB0)
    val PurpleSoft = Color(0xFFEEE9FF)

    val Blue = Color(0xFF2EB8FF)
    val BlueDark = Color(0xFF1693DB)

    val Green = Color(0xFF5CD62F)
    val GreenDark = Color(0xFF38A818)

    val Yellow = Color(0xFFFFC81E)
    val YellowDark = Color(0xFFF29E00)
    val YellowSoft = Color(0xFFFFF4C7)

    val Orange = Color(0xFFFF8A1F)
    val OrangeDark = Color(0xFFD96A00)

    val Pink = Color(0xFFFF3D8B)
    val PinkDark = Color(0xFFC71F68)

    val Red = Color(0xFFFF4B55)
    val RedDark = Color(0xFFC42A33)

    val Teal = Color(0xFF14D1B8)
    val TealDark = Color(0xFF0E9F8B)

    /** Niveaux verrouillés (lavande grisée, comme la maquette de liste de niveaux). */
    val Locked = Color(0xFFD9D7EF)
    val LockedDark = Color(0xFFBDBAE0)
    val LockedInk = Color(0xFF9592C4)

    val Error = Red
    val Scrim = Color(0x80201A4A)

    // Alias historiques (anciens noms utilisés par des écrans non refondus).
    val Cream = Background
    val Surface = White
    val Coral = Orange
    val Sun = Yellow
    val Plum = Purple
}

object WgSpacing {
    val Xs = 4.dp
    val Sm = 8.dp
    val Md = 16.dp
    val Lg = 24.dp
    val Xl = 32.dp
}

object WgRadius {
    val Sm = 10.dp
    val Md = 18.dp
    val Lg = 28.dp
    val Pill = 999.dp
}

/** Tailles tactiles : boutons visuellement plus compacts ; la zone cliquable reste de 48 dp (accessibilité). */
object WgSize {
    val MinTouch = 48.dp
    val IconButton = 38.dp
    val Button = 46.dp
    val Tile = 56.dp
    /** Épaisseur de la « lèvre » 3D sous un bouton bulle. */
    val Lip = 4.dp
}

/** Durées d'animation (ms). Interactions 100–300 ms, récompenses plus longues ; aucune animation de fondu (D-011). */
object WgMotion {
    const val QuickMs = 120
    const val StandardMs = 220
    const val SlowMs = 300
    /** Micro-délai avant une action de navigation, pour laisser jouer l'animation de pression. */
    const val PressDelayMs = 80L
}

/** Couleurs d'un style visuel de difficulté. Trois styles tournent selon le chapitre, FINAL est réservé au niveau 10. */
data class StylePalette(val accent: Color, val dark: Color, val soft: Color, val onAccent: Color)

object WgStyles {
    fun forStyle(style: VisualStyle): StylePalette = when (style) {
        VisualStyle.STYLE_1 -> StylePalette(WgColors.Teal, WgColors.TealDark, Color(0xFFDDFAF5), WgColors.White)
        VisualStyle.STYLE_2 -> StylePalette(WgColors.Yellow, WgColors.YellowDark, WgColors.YellowSoft, WgColors.Ink)
        VisualStyle.STYLE_3 -> StylePalette(WgColors.Pink, WgColors.PinkDark, Color(0xFFFFE3EF), WgColors.White)
        VisualStyle.FINAL -> StylePalette(WgColors.Purple, WgColors.PurpleDeep, WgColors.PurpleSoft, WgColors.White)
    }
}

/**
 * Ambiance d'un chapitre (clé `paletteKey` des données). [color]/[dark] colorent le bouton jouer, la barre de
 * progression et les rubans ; [sky] est le fond de l'illustration. Clé inconnue : ambiance par défaut.
 */
data class ChapterTheme(val color: Color, val dark: Color, val sky: Color, @DrawableRes val illustration: Int)

object WgChapters {
    fun forKey(key: String): ChapterTheme = when (key) {
        "dusk" -> ChapterTheme(WgColors.Purple, WgColors.PurpleDark, Color(0xFF9B7BFF), R.drawable.ill_chapter_dusk)
        else -> ChapterTheme(WgColors.Green, WgColors.GreenDark, Color(0xFFFFE45C), R.drawable.ill_chapter_meadow)
    }
}
