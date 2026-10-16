package com.motsmeles.game.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.motsmeles.game.domain.ports.AdProvider
import com.motsmeles.game.domain.ports.RewardedAdResult
import com.motsmeles.game.domain.ports.UnavailableReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume

data class AdUnitIds(val banner: String, val appOpen: String, val rewarded: String)

/**
 * Google Mobile Ads (identifiants de TEST uniquement). Ne bloque jamais le jeu : toute indisponibilité
 * (hors ligne, pas d'annonce, erreur, consentement refusé) se traduit par un résultat explicite, sans exception.
 */
class GoogleAdProvider(
    private val appContext: Context,
    private val activities: ActivityHolder,
    private val connectivity: ConnectivityChecker,
    private val consent: ConsentManager,
    private val scope: CoroutineScope,
    private val ids: AdUnitIds,
) : AdProvider {
    private val main = Handler(Looper.getMainLooper())
    private var rewarded: RewardedAd? = null
    private var appOpen: AppOpenAd? = null
    private var appOpenLoadedAt = 0L
    private var loadingRewarded = false
    private var loadingAppOpen = false
    @Volatile private var initialized = false
    @Volatile private var fullScreenShowing = false

    /** À appeler quand le consentement est connu. Initialise le SDK hors du thread principal puis précharge. */
    fun start() {
        if (initialized || !consent.canRequestAds) return
        initialized = true
        scope.launch(Dispatchers.Default) {
            MobileAds.initialize(appContext) { }
            main.post { preload() }
        }
    }

    /** Précharge les annonces plein écran si possible (appelé au démarrage et après chaque affichage). */
    fun preload() {
        if (!initialized || !connectivity.isOnline()) return
        loadRewarded()
        loadAppOpen()
    }

    private fun loadRewarded() {
        if (rewarded != null || loadingRewarded) return
        loadingRewarded = true
        RewardedAd.load(appContext, ids.rewarded, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewarded = ad
                loadingRewarded = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                rewarded = null
                loadingRewarded = false
            }
        })
    }

    private fun loadAppOpen() {
        if (loadingAppOpen || (appOpen != null && !isAppOpenExpired())) return
        loadingAppOpen = true
        AppOpenAd.load(appContext, ids.appOpen, AdRequest.Builder().build(), object : AppOpenAdLoadCallback() {
            override fun onAdLoaded(ad: AppOpenAd) {
                appOpen = ad
                appOpenLoadedAt = SystemClock.elapsedRealtime()
                loadingAppOpen = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                appOpen = null
                loadingAppOpen = false
            }
        })
    }

    /** Une annonce d'ouverture expire au bout de 4 h : on ne l'affiche jamais périmée. */
    private fun isAppOpenExpired(): Boolean = SystemClock.elapsedRealtime() - appOpenLoadedAt >= APP_OPEN_TTL_MS

    override suspend fun rewardedUnavailableReason(): UnavailableReason? = withContext(Dispatchers.Main) {
        when {
            !connectivity.isOnline() -> UnavailableReason.OFFLINE
            !consent.canRequestAds -> UnavailableReason.ERROR
            rewarded == null -> {
                start()
                preload()
                UnavailableReason.NOT_LOADED
            }
            else -> null
        }
    }

    override suspend fun showRewarded(): RewardedAdResult = withContext(Dispatchers.Main) {
        val activity = activities.current
        val ad = rewarded
        if (ad == null || activity == null) return@withContext RewardedAdResult.Unavailable(UnavailableReason.NOT_LOADED)
        rewarded = null
        suspendCancellableCoroutine<RewardedAdResult> { cont ->
            var earned = false
            val rewardId = UUID.randomUUID().toString()
            fullScreenShowing = true
            fun finish(result: RewardedAdResult) {
                fullScreenShowing = false
                if (cont.isActive) cont.resume(result)
                preload()
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    finish(if (earned) RewardedAdResult.Rewarded(rewardId) else RewardedAdResult.Dismissed)
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    finish(RewardedAdResult.Unavailable(UnavailableReason.ERROR))
                }
            }
            ad.show(activity) { earned = true }
        }
    }

    override suspend fun showReturnAdIfReady(): Boolean = withContext(Dispatchers.Main) {
        val activity = activities.current
        val ad = appOpen
        if (ad == null || activity == null || isAppOpenExpired() || fullScreenShowing) {
            preload()
            return@withContext false
        }
        appOpen = null
        suspendCancellableCoroutine<Boolean> { cont ->
            fullScreenShowing = true
            fun finish(shown: Boolean) {
                fullScreenShowing = false
                if (cont.isActive) cont.resume(shown)
                preload()
            }
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() = finish(true)
                override fun onAdFailedToShowFullScreenContent(error: AdError) = finish(false)
            }
            ad.show(activity)
        }
    }

    override fun isFullScreenAdShowing(): Boolean = fullScreenShowing

    private companion object {
        const val APP_OPEN_TTL_MS = 4L * 60 * 60 * 1000
    }
}
