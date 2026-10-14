package com.motsmeles.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.motsmeles.game.presentation.AppRoot
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.theme.WordGameTheme

class MainActivity : ComponentActivity() {
    private val container: AppContainer get() = (application as WordGameApp).container
    private val viewModel: AppViewModel by viewModels { AppViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen() // page de lancement blanche (thème Theme.WordGame.Starting)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        container.activityHolder.current = this
        setContent {
            WordGameTheme { AppRoot(viewModel) }
        }
        if (savedInstanceState == null) {
            // Consentement publicitaire (UMP) : affiché seulement si la région l'exige ; ne bloque jamais le jeu.
            if (container.consent.canRequestAds) viewModel.onConsentResult(true)
            container.consent.gatherConsent(this) { canRequestAds -> viewModel.onConsentResult(canRequestAds) }
        } else if (container.consent.canRequestAds) {
            viewModel.onConsentResult(true)
        }
    }

    override fun onStart() {
        super.onStart()
        container.activityHolder.current = this
        viewModel.onAppStarted()
    }

    override fun onStop() {
        viewModel.onAppStopped()
        super.onStop()
    }

    override fun onDestroy() {
        if (container.activityHolder.current === this) container.activityHolder.current = null
        if (isFinishing) container.audio.release()
        super.onDestroy()
    }
}
