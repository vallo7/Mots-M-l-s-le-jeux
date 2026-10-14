package com.motsmeles.game.presentation.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
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
import com.motsmeles.game.presentation.components.*
import com.motsmeles.game.presentation.game.GameController
import com.motsmeles.game.presentation.game.GameDialog
import com.motsmeles.game.presentation.theme.*

private val FoundColors = listOf(WgColors.Teal, WgColors.Sun, WgColors.Coral, WgColors.Plum)

/**
 * Écran de jeu. De haut en bas : barre (retour, niveau, redémarrer), chronomètre, mots à trouver, grille (point focal),
 * bouton Indice à droite sous la grille, zone réservée à la bannière. Barres système masquées (immersif sticky).
 */
@Composable
fun GameScreen(controller: GameController, state: AppState, vm: AppViewModel) {
    val ui by controller.ui.collectAsStateWithLifecycle()
    val catalog = state.catalog ?: return
    val level = ui.level
    val language = ui.language
    val chapter = catalog.chapterOf(level)
    val chapterTheme = WgChapters.forKey(chapter?.paletteKey ?: "meadow")
    val style = WgStyles.forStyle(level.visualStyle)
    val variant = level.variant(language)
    val words = variant.words

    ImmersiveEffect()
    LifecycleResumeEffect(controller) {
        controller.onResume()
        onPauseOrDispose { controller.onPause() }
    }

    val highlights = remember(ui.foundWordIndices, variant) {
        ui.foundWordIndices.sorted().map { index -> FoundHighlight(words[index].cells, FoundColors[index % FoundColors.size]) }
    }

    Box(Modifier.fillMaxSize().testTag("game_screen")) {
        Column(
            Modifier
                .fillMaxSize()
                .background(chapterTheme.background)
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
                val time = TimeFormat.mmss(ui.elapsedMillis)
                val timerDescription = stringResource(R.string.game_timer_description, time)
                Text(
                    text = time,
                    modifier = Modifier.testTag("game_timer").semantics { contentDescription = timerDescription },
                    style = MaterialTheme.typography.titleMedium,
                    color = WgColors.InkMuted,
                )
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
                    shakeToken = ui.wrongToken,
                    onSelectionStart = controller::onSelectionStart,
                    onSelectionChange = controller::onSelectionChange,
                    onSelectionEnd = controller::onSelectionEnd,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = WgSpacing.Md, vertical = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                HintButton(
                    balance = ui.hintBalance,
                    accent = style.accent,
                    enabled = ui.ready && !ui.solved,
                    onClick = controller::onHintRequested,
                )
            }
            AdBanner(unitId = vm.bannerUnitId, enabled = state.adsEnabled)
        }

        ConfirmSheet(
            visible = ui.dialog == GameDialog.ConfirmQuit,
            title = stringResource(R.string.dialog_quit_title),
            body = stringResource(R.string.dialog_quit_body),
            confirmText = stringResource(R.string.dialog_quit_confirm),
            dismissText = stringResource(R.string.dialog_stay),
            onConfirm = controller::confirmQuit,
            onDismiss = controller::dismissDialog,
            tag = "quit_sheet",
        )
        ConfirmSheet(
            visible = ui.dialog == GameDialog.ConfirmRestart,
            title = stringResource(R.string.dialog_restart_title),
            body = stringResource(R.string.dialog_restart_body),
            confirmText = stringResource(R.string.dialog_restart_confirm),
            dismissText = stringResource(R.string.dialog_cancel),
            onConfirm = controller::confirmRestart,
            onDismiss = controller::dismissDialog,
            tag = "restart_sheet",
        )
        ConfirmSheet(
            visible = ui.dialog == GameDialog.RewardedOffer,
            title = stringResource(R.string.rewarded_title),
            body = stringResource(R.string.rewarded_body),
            confirmText = stringResource(R.string.rewarded_watch),
            dismissText = stringResource(R.string.rewarded_later),
            onConfirm = controller::onWatchAd,
            onDismiss = controller::dismissDialog,
            tag = "rewarded_sheet",
        )
    }
}

@Composable
private fun GameTopBar(title: String, theme: String, isFinal: Boolean, onBack: () -> Unit, onRestart: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = WgSpacing.Sm, vertical = WgSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WgIconButton(R.drawable.ic_back, stringResource(R.string.cd_back), onBack, Modifier.testTag("game_back"), sound = Sound.MENU_CHANGE)
        Column(Modifier.weight(1f).padding(horizontal = WgSpacing.Sm), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isFinal) Icon(painterResource(R.drawable.ic_crown), null, Modifier.size(18.dp), tint = WgColors.Plum)
                Text(title, style = MaterialTheme.typography.titleMedium, color = WgColors.Ink, maxLines = 1)
            }
            Text(theme, style = MaterialTheme.typography.bodyMedium, color = WgColors.InkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        WgIconButton(R.drawable.ic_restart, stringResource(R.string.cd_restart), onRestart, Modifier.testTag("game_restart"))
    }
}

/** Mots à trouver, au-dessus de la grille : 3 colonnes (4 pour les grandes listes). Un mot trouvé est barré, coché et coloré. */
@Composable
private fun WordsPanel(words: List<TargetWord>, found: Set<Int>) {
    val columns = if (words.size >= 9) 4 else 3
    val label = stringResource(R.string.game_words_label)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = WgSpacing.Sm).semantics { contentDescription = label }.testTag("words_panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        words.withIndex().chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (index, word) ->
                    WordChip(word.display, index in found, FoundColors[index % FoundColors.size], columns == 4, Modifier.weight(1f))
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun WordChip(text: String, found: Boolean, color: Color, compact: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(1f) }
    LaunchedEffect(found) {
        if (found && !reduce) {
            pop.snapTo(0.85f)
            pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium))
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
            .heightIn(min = 30.dp)
            .clip(shape)
            .background(if (found) color.copy(alpha = 0.35f) else WgColors.Surface)
            .border(1.5.dp, if (found) color else WgColors.Outline, shape)
            .padding(horizontal = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (found) {
            Icon(painterResource(R.drawable.ic_check), null, Modifier.size(14.dp), tint = WgColors.Ink)
            Spacer(Modifier.width(2.dp))
        }
        Text(
            text = text,
            fontSize = if (compact) 12.sp else 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (found) WgColors.InkMuted else WgColors.Ink,
            textDecoration = if (found) TextDecoration.LineThrough else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Bouton Indice : le solde est toujours visible ; à zéro, il propose d'en obtenir (vidéo récompensée). */
@Composable
private fun HintButton(balance: Int, accent: Color, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(WgRadius.Pill)
    val resources = LocalContext.current.resources
    val label = stringResource(if (balance > 0) R.string.game_hint else R.string.game_hint_more)
    val description = stringResource(R.string.game_hint_description, label, resources.getQuantityString(R.plurals.hints_count, balance, balance))
    Pressable(
        onClick = onClick,
        modifier = Modifier
            .testTag("hint_button")
            .height(WgSize.Button)
            .clip(shape)
            .background(WgColors.Surface)
            .border(3.dp, accent, shape)
            .padding(horizontal = WgSpacing.Md),
        enabled = enabled,
        description = description,
    ) {
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(painterResource(R.drawable.ic_hint), null, Modifier.size(24.dp), tint = WgColors.Sun)
            Text(label, style = MaterialTheme.typography.labelLarge, color = WgColors.Ink, maxLines = 1)
            Box(Modifier.size(28.dp).clip(CircleShape).background(if (balance > 0) WgColors.Coral else WgColors.Outline), contentAlignment = Alignment.Center) {
                Text(balance.toString(), style = MaterialTheme.typography.labelLarge, color = WgColors.Ink)
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
