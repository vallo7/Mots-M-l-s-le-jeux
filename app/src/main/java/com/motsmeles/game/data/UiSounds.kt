package com.motsmeles.game.data

import com.motsmeles.game.domain.ports.Sound

/** Fichiers et volumes des sons d'interface (dossier assets/audio/ui). */
object UiSounds {
    const val DIRECTORY = "audio/ui"

    fun fileFor(sound: Sound): String = when (sound) {
        Sound.CLICK -> "click.wav"
        Sound.SELECT_CELL -> "select_cell.wav"
        Sound.CORRECT -> "correct.wav"
        Sound.WRONG -> "wrong.wav"
        Sound.HINT -> "hint.wav"
        Sound.GIFT -> "gift.wav"
        Sound.PROGRESS_UP -> "progress_up.wav"
        Sound.VICTORY -> "victory.wav"
        Sound.MENU_CHANGE -> "menu_change.wav"
        Sound.SUCCESS_GENERIC -> "success_generic.wav"
    }

    fun assetPath(sound: Sound): String = "$DIRECTORY/${fileFor(sound)}"

    /** Volume 0..1 (jamais de saturation : aucun son n'est joué au maximum). */
    fun volumeFor(sound: Sound): Float = when (sound) {
        Sound.CLICK -> 0.7f
        Sound.SELECT_CELL -> 0.5f
        Sound.CORRECT -> 0.8f
        Sound.WRONG -> 0.6f
        Sound.HINT -> 0.8f
        Sound.GIFT -> 0.9f
        Sound.PROGRESS_UP -> 0.8f
        Sound.VICTORY -> 0.85f
        Sound.MENU_CHANGE -> 0.5f
        Sound.SUCCESS_GENERIC -> 0.85f
    }
}
