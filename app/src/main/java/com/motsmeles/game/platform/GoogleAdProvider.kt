package com.motsmeles.game.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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

data class AdUnitIds(val banner: String, val interstitial: String, val rewarded: String)

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
    private var interstitial: InterstitialAd? = null
    private var loadingRewarded = false
    private var loadingInterstitial = false
    @Volatile private var initialized = false

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
        loadInterstitial()
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

    private fun loadInterstitial() {
        if (interstitial != null || loadingInterstitial) return
        loadingInterstitial = true
        InterstitialAd.load(appContext, ids.interstitial, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
                loadingInterstitial = false
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
                loadingInterstitial = false
            }
        })
    }

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
            fun finish(result: RewardedAdResult) {
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

    override suspend fun showInterstitialIfReady(): Boolean = withContext(Dispatchers.Main) {
        val activity = activities.current
        val ad = interstitial
        if (ad == null || activity == null) {
            preload()
            return@withContext false
        }
        interstitial = null
        suspendCancellableCoroutine<Boolean> { cont ->
            fun finish(shown: Boolean) {
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
}
