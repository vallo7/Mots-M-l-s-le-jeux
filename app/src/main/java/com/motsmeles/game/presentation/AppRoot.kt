package com.motsmeles.game.presentation

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motsmeles.game.presentation.components.MessageHost
import com.motsmeles.game.presentation.screens.CreditsScreen
import com.motsmeles.game.presentation.screens.GameScreen
import com.motsmeles.game.presentation.screens.HomeScreen
import com.motsmeles.game.presentation.screens.MonumentScreen
import com.motsmeles.game.presentation.screens.ResultScreen
import com.motsmeles.game.presentation.screens.SettingsScreen
import com.motsmeles.game.presentation.screens.StartupErrorScreen
import com.motsmeles.game.presentation.startup.LoadingScreen
import com.motsmeles.game.presentation.theme.LocalFeedback
import com.motsmeles.game.presentation.theme.LocalLanguage
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgMotion
import com.motsmeles.game.presentation.theme.rememberReduceMotion
import java.util.Locale

/**
 * Racine de l'interface : fournit la langue choisie (changement immédiat), la taille de texte, la réduction des animations
 * et les retours sonores/haptiques ; affiche chargement, erreur ou navigation. Toutes les transitions glissent (aucun fondu).
 */
@Composable
fun AppRoot(vm: AppViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val baseContext = LocalContext.current
    val baseDensity = LocalDensity.current
    val systemReduceMotion = rememberReduceMotion()

    // Bouton Retour : placé hors du contexte localisé (il doit retrouver l'activité).
    BackHandler(enabled = state.phase == Phase.Ready && state.stack.size > 1) { vm.onBack() }

    val language = state.language
    val localizedContext = remember(baseContext, language) {
        val config = Configuration(baseContext.resources.configuration).apply { setLocale(Locale(language.code)) }
        baseContext.createConfigurationContext(config)
    }
    val density = remember(baseDensity, state.settings.largeText) {
        val scale = baseDensity.fontScale.coerceIn(0.85f, 1.3f) * (if (state.settings.largeText) 1.15f else 1f)
        Density(baseDensity.density, scale)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalDensity provides density,
        LocalReduceMotion provides (systemReduceMotion || state.settings.reduceMotion),
        LocalLanguage provides language,
        LocalFeedback provides vm.feedback,
    ) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.phase,
                contentKey = { it::class },
                transitionSpec = {
                    slideInHorizontally(animationSpec = tween(WgMotion.SlowMs)) { width -> width } togetherWith
                        slideOutHorizontally(animationSpec = tween(WgMotion.SlowMs)) { width -> -width }
                },
                label = "phase",
            ) { phase ->
                when (phase) {
                    is Phase.Loading -> LoadingScreen(phase.progress, phase.stepLabel)
                    Phase.Failed -> StartupErrorScreen()
                    Phase.Ready -> Screens(state, vm)
                }
            }
            MessageHost(state.message)
        }
    }
}

@Composable
private fun Screens(state: AppState, vm: AppViewModel) {
    val top = state.stack.last()
    AnimatedContent(
        targetState = top,
        contentKey = { it.key },
        transitionSpec = {
            if (state.direction == NavDirection.Forward) {
                slideInHorizontally(animationSpec = tween(WgMotion.SlowMs)) { width -> width } togetherWith
                    slideOutHorizontally(animationSpec = tween(WgMotion.SlowMs)) { width -> -width }
            } else {
                slideInHorizontally(animationSpec = tween(WgMotion.SlowMs)) { width -> -width } togetherWith
                    slideOutHorizontally(animationSpec = tween(WgMotion.SlowMs)) { width -> width }
            }
        },
        label = "screens",
    ) { screen ->
        when (screen) {
            Screen.Home -> HomeScreen(state, vm)
            is Screen.Game -> vm.activeGame?.let { GameScreen(it, state, vm) }
            Screen.Monument -> MonumentScreen(state, vm)
            Screen.Settings -> SettingsScreen(state, vm)
            Screen.Credits -> CreditsScreen(vm)
            is Screen.Result -> ResultScreen(screen.result, state, vm)
        }
    }
}
