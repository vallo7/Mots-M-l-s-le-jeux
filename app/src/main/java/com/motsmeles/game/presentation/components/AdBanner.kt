package com.motsmeles.game.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.motsmeles.game.presentation.theme.WgColors

/** Hauteur réservée à la bannière (AdSize.BANNER = 50 dp) : la zone existe même sans annonce, la grille ne bouge pas. */
val AdBannerHeight = 50.dp

/**
 * Bannière publicitaire dans sa zone réservée en bas de l'écran de jeu. Si les annonces sont indisponibles
 * (consentement, hors ligne, pas d'annonce), la zone reste vide : le jeu n'en dépend jamais.
 */
@Composable
fun AdBanner(unitId: String, enabled: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().height(AdBannerHeight).background(WgColors.Cream).testTag("ad_banner_area"),
        contentAlignment = Alignment.Center,
    ) {
        if (enabled) {
            val context = LocalView.current.context
            val adView = remember(unitId) {
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = unitId
                    loadAd(AdRequest.Builder().build())
                }
            }
            DisposableEffect(adView) { onDispose { adView.destroy() } }
            LifecycleResumeEffect(adView) {
                adView.resume()
                onPauseOrDispose { adView.pause() }
            }
            AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(AdBannerHeight))
        }
    }
}
