package com.motsmeles.game.presentation

import android.content.res.Resources
import androidx.annotation.AnyRes
import androidx.annotation.StringRes
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.progress.ProgressState
import com.motsmeles.game.domain.settings.AppSettings
import com.motsmeles.game.domain.ui.HomeModel
import com.motsmeles.game.domain.ui.HomeModelBuilder
import com.motsmeles.game.domain.usecases.VictoryResult

sealed interface Screen {
    val key: String

    data object Home : Screen { override val key = "home" }
    data class Game(val levelId: String, val token: Long) : Screen { override val key = "game-$levelId-$token" }
    data object Monument : Screen { override val key = "monument" }
    data object Settings : Screen { override val key = "settings" }
    data object Credits : Screen { override val key = "credits" }
    data class Result(val result: VictoryResult) : Screen { override val key = "result-${result.levelId}" }
}

enum class NavDirection { Forward, Back }

sealed interface Phase {
    data class Loading(val progress: Float, @StringRes val stepLabel: Int) : Phase
    data object Failed : Phase
    data object Ready : Phase
}

/** Message bref affiché en haut de l'écran. [plural] : [res] est un pluriel et [quantity] sa quantité. */
data class UiMessage(val id: Long, @AnyRes val res: Int, val quantity: Int? = null, val plural: Boolean = false)

fun UiMessage.resolve(resources: Resources): String =
    if (plural) resources.getQuantityString(res, quantity ?: 0, quantity ?: 0) else resources.getString(res)

data class AppState(
    val phase: Phase = Phase.Loading(0f, com.motsmeles.game.R.string.loading_step_read),
    val catalog: LevelCatalog? = null,
    val progress: ProgressState = ProgressState(),
    val settings: AppSettings = AppSettings(),
    val language: Language = Language.EN,
    val stack: List<Screen> = listOf(Screen.Home),
    val direction: NavDirection = NavDirection.Forward,
    val message: UiMessage? = null,
    val adsEnabled: Boolean = false,
    /** Vrai pendant le lancement d'un niveau (évite les doubles lancements). */
    val busy: Boolean = false,
)

/** Modèle d'affichage de l'accueil (carrousel de chapitres), ou null tant que le catalogue n'est pas chargé. */
fun AppState.home(): HomeModel? = catalog?.let { HomeModelBuilder.build(it, progress, language) }
