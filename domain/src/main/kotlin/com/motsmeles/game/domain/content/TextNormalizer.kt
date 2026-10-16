package com.motsmeles.game.domain.content

import java.text.Normalizer

object TextNormalizer {
    /** « Île » -> « ILE » : majuscules, sans accents, lettres A-Z uniquement. */
    fun toGridLetters(display: String): String =
        Normalizer.normalize(display, Normalizer.Form.NFD)
            .uppercase()
            .filter { it in 'A'..'Z' }
}
