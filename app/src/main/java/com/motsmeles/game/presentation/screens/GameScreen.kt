package com.motsmeles.game.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motsmeles.game.R
import com.motsmeles.game.domain.model.TargetWord
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.ui.DifficultyBand
import com.motsmeles.game.domain.ui.TimeFormat
import com.motsmeles.game.platform.ImmersiveMode
import com.motsmeles.game.presentation.AppState
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.components.AdBanner
import com.motsmeles.game.presentation.components.BubbleFace
import com.motsmeles.game.presentation.components.BubbleIconButton
import com.motsmeles.game.presentation.components.ColorIcon
import com.motsmeles.game.presentation.components.ConfirmPopup
import com.motsmeles.game.presentation.components.DifficultyBadge
import com.motsmeles.game.presentation.components.JuicyPressable
import com.motsmeles.game.presentation.components.LetterGrid
import com.motsmeles.game.presentation.components.RewardedOfferPopup
import com.motsmeles.game.presentation.components.rememberBounce
import com.motsmeles.game.presentation.components.rememberWobble
import com.motsmeles.game.presentation.fx.FoundHighlight
import com.motsmeles.game.presentation.game.GameController
import com.motsmeles.game.presentation.game.GameDialog
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSize
import com.motsmeles.game.presentation.theme.WgSpacing
import com.motsmeles.game.presentation.theme.WgStyles

/** Couleurs des mots trouvés (capsule dans la grille + pastille dans la liste) : vives et bien distinctes. */
private val WordColors = listOf(
    WgColors.Pink, WgColors.Blue, WgColors.Green, WgColors.Orange,
    WgColors.Purple, WgColors.Teal, WgColors.Red, WgColors.YellowDark,
)

/**
 * Écran de jeu. De haut en bas : barre (retour, niveau, redémarrer), chronomètre agrandi, mots à trouver, grille (point
 * focal), bouton Indice à droite sous la grille, zone réservée à la bannière. Barres système masquées (immersif sticky).
 * Le redémarrage est immédiat : la grille mélange ses lettres, sans fenêtre de confirmation.
 */
@Composable
fun GameScreen(controller: GameController, state: AppState, vm: AppViewModel) {
    val ui by controller.ui.collectAsStateWithLifecycle()
    val catalog = state.catalog ?: return
    val level = ui.level
    val language = ui.language
    val style = WgStyles.forStyle(level.visualStyle)
    val variant = level.variant(language)
    val words = variant.words

    ImmersiveEffect()
    LifecycleResumeEffect(controller) {
        controller.onResume()
        onPauseOrDispose { controller.onPause() }
    }

    val highlights = remember(ui.foundWordIndices, variant) {
        ui.foundWordIndices.sorted().map { index -> FoundHighlight(words[index].cells, WordColors[index % WordColors.size]) }
    }

    Box(Modifier.fillMaxSize().testTag("game_screen")) {
        Column(
            Modifier
                .fillMaxSize()
                .background(WgColors.Background)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            GameTopBar(
                title = stringResource(R.string.home_chapter_level, level.chapterIndex, level.number),
                theme = level.theme[language],
                isFinal = level.isFinalChallenge,
                onBack = controller::onBackRequested,
                onRestart = controller::onRestartRequested,
            )
            Row(
                Modifier.fillMaxWidth().padding(bottom = WgSpacing.Sm),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DifficultyBadge(DifficultyBand.of(level.number))
                Spacer(Modifier.width(WgSpacing.Md))
                TimerPill(TimeFormat.mmss(ui.elapsedMillis), style.accent)
            }
            WordsPanel(words, ui.foundWordIndices)
            Box(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = WgSpacing.Md, vertical = WgSpacing.Sm),
                contentAlignment = Alignment.Center,
            ) {
                LetterGrid(
                    grid = variant.grid,
                    found = highlights,
                    hintCells = ui.hintCells,
                    selection = ui.selection,
                    selectionColor = style.accent,
                    borderColor = style.accent,
                    enabled = ui.ready && !ui.solved && ui.dialog == GameDialog.None,
                    wrongToken = ui.wrongToken,
                    restartToken = ui.restartToken,
                    onSelectionStart = controller::onSelectionStart,
                    onSelectionChange = controller::onSelectionChange,
                    onSelectionEnd = controller::onSelectionEnd,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = WgSpacing.Md, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                HintButton(
                    balance = ui.hintBalance,
                    enabled = ui.ready && !ui.solved,
                    onClick = controller::onHintRequested,
                )
            }
            AdBanner(unitId = vm.bannerUnitId, enabled = state.adsEnabled)
        }

        ConfirmPopup(
            visible = ui.dialog == GameDialog.ConfirmQuit,
            title = stringResource(R.string.dialog_quit_title),
            body = stringResource(R.string.dialog_quit_body),
            confirmText = stringResource(R.string.dialog_quit_confirm),
            dismissText = stringResource(R.string.dialog_stay),
            onConfirm = controller::confirmQuit,
            onDismiss = controller::dismissDialog,
            tag = "quit_popup",
        )
        RewardedOfferPopup(
            visible = ui.dialog == GameDialog.RewardedOffer,
            title = stringResource(R.string.rewarded_title),
            body = stringResource(R.string.rewarded_body),
            watchDescription = stringResource(R.string.rewarded_watch),
            laterText = stringResource(R.string.rewarded_later),
            onWatch = controller::onWatchAd,
            onLater = controller::dismissDialog,
        )
    }
}

@Composable
private fun GameTopBar(title: String, theme: String, isFinal: Boolean, onBack: () -> Unit, onRestart: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = WgSpacing.Sm, vertical = WgSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BubbleIconButton(
            R.drawable.ic_back, stringResource(R.string.cd_back), onBack, Modifier.testTag("game_back"),
            sound = Sound.MENU_CHANGE,
        )
        Column(Modifier.weight(1f).padding(horizontal = WgSpacing.Sm), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isFinal) ColorIcon(R.drawable.ic_crown, Modifier.size(22.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = WgColors.Ink, maxLines = 1)
            }
            Text(theme, style = MaterialTheme.typography.bodyMedium, color = WgColors.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        BubbleIconButton(
            R.drawable.ic_restart, stringResource(R.string.cd_restart), onRestart, Modifier.testTag("game_restart"),
            color = WgColors.Orange, dark = WgColors.OrangeDark,
        )
    }
}

/** Chronomètre agrandi : horloge dorée + chiffres de même largeur (pas de saut quand ils changent) dans une grosse pastille. */
@Composable
private fun TimerPill(time: String, accent: Color) {
    val timerDescription = stringResource(R.string.game_timer_description, time)
    Row(
        Modifier
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(WgColors.Track)
            .padding(start = 8.dp, end = 18.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ColorIcon(R.drawable.ic_clock, Modifier.size(32.dp))
        Text(
            text = time,
            modifier = Modifier.testTag("game_timer").semantics { contentDescription = timerDescription },
            style = TextStyle(fontWeight = FontWeight.Black, fontSize = 30.sp, fontFeatureSettings = "tnum"),
            color = WgColors.Ink,
            maxLines = 1,
        )
    }
}

/** Mots à trouver, au-dessus de la grille : pastilles qui s'enchaînent à la ligne. Un mot trouvé se remplit de sa couleur. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordsPanel(words: List<TargetWord>, found: Set<Int>) {
    val label = stringResource(R.string.game_words_label)
    val compact = words.size > 10
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = WgSpacing.Md).semantics { contentDescription = label }.testTag("words_panel"),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        words.forEachIndexed { index, word ->
            WordChip(word.display, index in found, WordColors[index % WordColors.size], compact)
        }
    }
}

@Composable
private fun WordChip(text: String, found: Boolean, color: Color, compact: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(1f) }
    LaunchedEffect(found) {
        if (found && !reduce) {
            pop.snapTo(1.32f)
            pop.animateTo(1f, spring<Float>(dampingRatio = 0.34f, stiffness = 460f))
        }
    }
    val description = stringResource(if (found) R.string.game_word_found_desc else R.string.game_word_todo_desc, text)
    val shape = RoundedCornerShape(WgRadius.Pill)
    Row(
        modifier
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
            .heightIn(min = if (compact) 28.dp else 32.dp)
            .clip(shape)
            .background(if (found) color else WgColors.Track)
            .padding(horizontal = if (compact) 10.dp else 12.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (found) {
            Icon(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp), tint = WgColors.White)
            Spacer(Modifier.width(3.dp))
        }
        Text(
            text = text,
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (found) WgColors.White else WgColors.Ink,
            textDecoration = if (found) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Bouton Indice sous la grille, à droite. Le solde est toujours visible ; à zéro, la pastille de nombre laisse place à
 * un badge « pub » (triangle de lecture vert) sur l'ampoule, qui se balance régulièrement : il faut regarder une publicité.
 */
@Composable
private fun HintButton(balance: Int, enabled: Boolean, onClick: () -> Unit) {
    val noHints = balance <= 0
    val wobble = rememberWobble(noHints && enabled)
    val bounce = rememberBounce(balance)
    val resources = LocalContext.current.resources
    val label = stringResource(if (!noHints) R.string.game_hint else R.string.game_hint_more)
    val description = stringResource(R.string.game_hint_description, label, resources.getQuantityString(R.plurals.hints_count, balance, balance))
    val face = if (noHints) WgColors.Green else WgColors.Purple
    val lip = if (noHints) WgColors.GreenDark else WgColors.PurpleDark
    JuicyPressable(
        onClick = onClick,
        modifier = Modifier.testTag("hint_button").height(WgSize.Button + WgSize.Lip),
        enabled = enabled,
        description = description,
    ) { press ->
        BubbleFace(Modifier.fillMaxSize(), face, lip, RoundedCornerShape(WgRadius.Pill), press) {
            Row(
                Modifier.padding(start = 10.dp, end = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(Modifier.size(38.dp).graphicsLayer { rotationZ = wobble }) {
                    ColorIcon(R.drawable.ic_bulb, Modifier.size(34.dp).align(Alignment.Center))
                    if (noHints) {
                        ColorIcon(R.drawable.ic_ad_badge, Modifier.size(20.dp).align(Alignment.TopEnd).offset(x = 4.dp, y = (-2).dp))
                    }
                }
                Text(label, style = MaterialTheme.typography.labelLarge, color = WgColors.White, maxLines = 1)
                if (!noHints) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .graphicsLayer {
                                scaleX = bounce
                                scaleY = bounce
                            }
                            .clip(CircleShape)
                            .background(WgColors.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(balance.toString(), style = MaterialTheme.typography.labelLarge, color = WgColors.PurpleDark)
                    }
                }
            }
        }
    }
}

/** Mode immersif sticky pendant la partie ; les barres sont restaurées en quittant l'écran. */
@Composable
private fun ImmersiveEffect() {
    val view = LocalView.current
    DisposableEffect(view) {
        val activity = view.context.findActivity()
        activity?.let { ImmersiveMode.set(it, true) }
        onDispose { activity?.let { ImmersiveMode.set(it, false) } }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
