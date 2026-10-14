package com.motsmeles.game.domain.settings

import com.motsmeles.game.domain.model.Language

/** Réglages utilisateur. [language] = null : suit la langue du téléphone tant que le joueur n'a pas choisi. */
data class AppSettings(
    val language: Language? = null,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val largeText: Boolean = false,
) {
    fun resolveLanguage(systemLanguageCode: String?): Language = language ?: Language.fromCode(systemLanguageCode)

    companion object {
        /** Valeurs par défaut (restauration des paramètres). */
        val DEFAULT = AppSettings()
    }
}
