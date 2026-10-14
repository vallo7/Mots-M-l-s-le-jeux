package com.motsmeles.game

import android.app.Application
import com.motsmeles.game.data.AssetLevelRepository
import com.motsmeles.game.data.DataStoreProgressRepository
import com.motsmeles.game.data.DataStoreSettingsRepository
import com.motsmeles.game.data.PraiseMap
import com.motsmeles.game.domain.engine.MonotonicClock
import com.motsmeles.game.domain.ports.AdProvider
import com.motsmeles.game.domain.ports.AudioManager
import com.motsmeles.game.domain.ports.HapticsManager
import com.motsmeles.game.domain.ports.LevelRepository
import com.motsmeles.game.domain.ports.ProgressRepository
import com.motsmeles.game.domain.ports.SettingsRepository
import com.motsmeles.game.domain.progress.RandomSource
import com.motsmeles.game.platform.ActivityHolder
import com.motsmeles.game.platform.AdUnitIds
import com.motsmeles.game.platform.ConnectivityChecker
import com.motsmeles.game.platform.ConsentManager
import com.motsmeles.game.platform.GoogleAdProvider
import com.motsmeles.game.platform.SoundPoolAudioManager
import com.motsmeles.game.platform.VibratorHapticsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File
import kotlin.random.Random

/** Injection de dépendances manuelle : simple, testable, sans génération de code (D-014). */
class AppContainer(val application: Application) {
    val clock: MonotonicClock = MonotonicClock { android.os.SystemClock.elapsedRealtime() }

    /** Portée de l'application : les écritures de progression y survivent à la fermeture d'un écran. */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Portée des écritures de sauvegarde de partie : un seul fil, donc ordre d'envoi respecté
     * (un redémarrage ne peut pas être écrasé par une sauvegarde plus ancienne).
     */
    val persistScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    val random: RandomSource = RandomSource { from, until -> Random.nextInt(from, until) }
    val activityHolder = ActivityHolder()
    val connectivity = ConnectivityChecker(application)

    val levelRepository: LevelRepository = AssetLevelRepository(application.assets)
    val progressRepository: ProgressRepository = DataStoreProgressRepository(File(application.filesDir, "progress.json"), appScope)
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(File(application.filesDir, "settings.json"), appScope)

    private val audioImpl = SoundPoolAudioManager(application.assets) {
        PraiseMap.parse(application.assets.open(PraiseMap.FILE).bufferedReader(Charsets.UTF_8).use { it.readText() })
    }
    val audio: AudioManager get() = audioImpl
    val audioEngine: SoundPoolAudioManager get() = audioImpl
    val haptics: HapticsManager = VibratorHapticsManager(application)

    val consent = ConsentManager(application)
    private val adsImpl = GoogleAdProvider(
        appContext = application,
        activities = activityHolder,
        connectivity = connectivity,
        consent = consent,
        scope = appScope,
        ids = AdUnitIds(
            banner = application.getString(R.string.admob_banner_unit_id),
            interstitial = application.getString(R.string.admob_interstitial_unit_id),
            rewarded = application.getString(R.string.admob_rewarded_unit_id),
        ),
    )
    val ads: AdProvider get() = adsImpl
    val adsStarter: GoogleAdProvider get() = adsImpl
    val bannerUnitId: String get() = application.getString(R.string.admob_banner_unit_id)
}

class WordGameApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
