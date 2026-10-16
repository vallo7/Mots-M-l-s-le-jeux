package com.motsmeles.game.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.HapticsManager

/**
 * Retour haptique à quatre intensités : sélection légère, validation moyenne, erreur très courte, succès marqué.
 * Respecte l'option du jeu ET le réglage Android « retour tactile ».
 */
class VibratorHapticsManager(private val context: Context) : HapticsManager {
    @Volatile private var enabled = true

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    override fun perform(effect: HapticEffect) {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator() || !systemHapticsAllowed()) return
        val vibration = when (effect) {
            HapticEffect.SELECTION -> VibrationEffect.createOneShot(12, 60)
            HapticEffect.CONFIRM -> VibrationEffect.createOneShot(22, 140)
            HapticEffect.ERROR -> VibrationEffect.createOneShot(14, 90)
            HapticEffect.SUCCESS -> VibrationEffect.createWaveform(longArrayOf(0, 30, 50, 70), intArrayOf(0, 120, 0, 230), -1)
        }
        v.vibrate(vibration)
    }

    private fun systemHapticsAllowed(): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
}
