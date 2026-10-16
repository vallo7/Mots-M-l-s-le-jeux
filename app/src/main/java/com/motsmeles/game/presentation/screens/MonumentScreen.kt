package com.motsmeles.game.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.progress.LevelStatus
import com.motsmeles.game.domain.ui.MonumentBuilder
import com.motsmeles.game.domain.ui.MonumentChapter
import com.motsmeles.game.domain.ui.MonumentLevel
import com.motsmeles.game.presentation.AppState
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.components.BubbleIconButton
import com.motsmeles.game.presentation.components.ButtonKind
import com.motsmeles.game.presentation.components.ChunkyProgressBar
import com.motsmeles.game.presentation.components.ColorIcon
import com.motsmeles.game.presentation.components.HintChip
import com.motsmeles.game.presentation.components.JuicyPressable
import com.motsmeles.game.presentation.components.RibbonBanner
import com.motsmeles.game.presentation.components.Ribbons
import com.motsmeles.game.presentation.components.StarsRow
import com.motsmeles.game.presentation.components.TrophyBadge
import com.motsmeles.game.presentation.components.WgButton
import com.motsmeles.game.presentation.components.popIn
import com.motsmeles.game.presentation.theme.LocalLanguage
import com.motsmeles.game.presentation.theme.WgChapters
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSpacing
import com.motsmeles.game.presentation.theme.WgStyles

/**
 * Monument des niveaux, dans le style de la maquette « liste de niveaux » : barre du haut (retour, progression
 * générale continue, indices), bandeau illustré par chapitre, puis dix pilules. Niveau terminé : étoiles + coche,
 * pas de relance. Niveau disponible : bouton lecture. Niveau verrouillé : cadenas. Le niveau 10 (défi final) est doré.
 */
@Composable
fun MonumentScreen(state: AppState, vm: AppViewModel) {
    val catalog = state.catalog ?: return
    val language = LocalLanguage.current
    val chapters = remember(catalog, state.progress) { MonumentBuilder.build(catalog, state.progress) }
    val total = chapters.sumOf { it.levels.size }
    val done = chapters.sumOf { it.completedCount }
    val currentChapter = chapters.firstOrNull { c -> c.levels.any { it.status == LevelStatus.AVAILABLE } }?.index ?: chapters.lastOrNull()?.index ?: 1

    Box(Modifier.fillMaxSize().background(WgColors.Background).testTag("monument_screen")) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            TopProgressBar(
                currentChapter = currentChapter,
                fraction = if (total == 0) 0f else done.toFloat() / total,
                hintBalance = state.progress.hintBalance,
                onBack = vm::onBack,
            )
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = WgSpacing.Xl),
            ) {
                chapters.forEach { chapter ->
                    item(key = "chapter-${chapter.index}") { ChapterHeader(chapter, language, onClaim = { vm.claimGift(chapter.index) }) }
                    items(chapter.levels, key = { it.levelId }) { level ->
                        LevelPill(chapter, level, language, Modifier.padding(horizontal = 16.dp), onClick = { vm.openLevel(level.levelId) })
                    }
                    item(key = "gap-${chapter.index}") { Spacer(Modifier.height(10.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TopProgressBar(currentChapter: Int, fraction: Float, hintBalance: Int, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BubbleIconButton(R.drawable.ic_back, stringResource(R.string.cd_back), onBack, sound = Sound.MENU_CHANGE)
        // Pastille de chapitre + barre continue de progression générale
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            ChunkyProgressBar(
                fraction = fraction,
                modifier = Modifier.padding(start = 16.dp),
                color = WgColors.Green,
                dark = WgColors.GreenDark,
                height = 18.dp,
            )
            Box(
                Modifier.size(32.dp).background(WgColors.GreenDark, CircleShape).padding(bottom = 2.dp).background(WgColors.Green, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(currentChapter.toString(), style = MaterialTheme.typography.labelLarge, color = WgColors.White)
            }
        }
        HintChip(hintBalance)
    }
}

@Composable
private fun ChapterHeader(chapter: MonumentChapter, language: Language, onClaim: () -> Unit) {
    val theme = WgChapters.forKey(chapter.paletteKey)
    val progressDescription = stringResource(R.string.home_gift_description, chapter.completedCount, chapter.levels.size)
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(150.dp).background(theme.sky)) {
            Image(
                painterResource(theme.illustration),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            RibbonBanner(
                text = stringResource(R.string.monument_chapter, chapter.index, chapter.theme[language]),
                colors = if (chapter.index % 2 == 1) Ribbons.Green else Ribbons.Purple,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.9f).padding(bottom = 8.dp),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) { contentDescription = progressDescription },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ChunkyProgressBar(
                fraction = chapter.completedCount.toFloat() / chapter.levels.size.coerceAtLeast(1),
                modifier = Modifier.weight(1f),
                color = theme.color,
                dark = theme.dark,
                height = 18.dp,
            )
            TrophyBadge(ready = chapter.giftClaimable, size = 44.dp)
        }
        if (chapter.giftClaimable) {
            WgButton(
                text = stringResource(R.string.home_trophy_claim),
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("monument_trophy_${chapter.index}"),
                kind = ButtonKind.Primary,
                sound = Sound.GIFT,
                haptic = HapticEffect.SUCCESS,
            )
        }
    }
}

/** Une pilule de niveau : pastille numérotée cerclée de blanc, titre « chapitre-niveau THÈME », étoiles, action à droite. */
@Composable
private fun LevelPill(chapter: MonumentChapter, level: MonumentLevel, language: Language, modifier: Modifier, onClick: () -> Unit) {
    val locked = level.status == LevelStatus.LOCKED
    val completed = level.status == LevelStatus.COMPLETED
    val style = WgStyles.forStyle(level.style)
    val shape = RoundedCornerShape(WgRadius.Pill)
    val face = when {
        locked -> WgColors.Locked
        level.isFinal -> WgColors.Yellow
        else -> WgColors.Purple
    }
    val lip = when {
        locked -> WgColors.LockedDark
        level.isFinal -> WgColors.YellowDark
        else -> WgColors.PurpleDark
    }
    val textColor = when {
        locked -> WgColors.LockedInk
        level.isFinal -> WgColors.Ink
        else -> WgColors.White
    }
    val statusText = stringResource(
        when (level.status) {
            LevelStatus.LOCKED -> R.string.monument_status_locked
            LevelStatus.AVAILABLE -> R.string.monument_status_available
            LevelStatus.COMPLETED -> R.string.monument_status_completed
        },
    )
    val description = stringResource(R.string.monument_level_a11y, chapter.index, level.number, level.theme[language], statusText)
    JuicyPressable(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().testTag("level_${level.levelId}").popIn(delayMs = (level.number - 1) * 30, key = level.levelId),
        description = description,
        sound = if (locked) Sound.WRONG else Sound.MENU_CHANGE,
        haptic = if (locked) HapticEffect.ERROR else HapticEffect.CONFIRM,
        squash = 0.5f,
    ) { press ->
        Box(Modifier.fillMaxWidth().height(70.dp)) {
            Box(Modifier.fillMaxSize().padding(top = 5.dp).background(lip, shape))
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 5.dp)
                    .background(face, shape)
                    .padding(start = 8.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberBadge(level, locked, style.accent, style.dark)
                Column(Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = stringResource(R.string.monument_level_title, chapter.index, level.number, level.theme[language].uppercase()),
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    when {
                        locked -> Text(
                            stringResource(R.string.monument_grid_size, level.gridSize),
                            style = MaterialTheme.typography.labelMedium, color = textColor,
                        )
                        else -> StarsRow(level.stars, 20.dp)
                    }
                }
                when {
                    completed -> ColorIcon(R.drawable.ic_check_green, Modifier.size(34.dp))
                    locked -> Icon(painterResource(R.drawable.ic_lock), null, Modifier.size(26.dp), tint = textColor)
                    else -> Box(
                        Modifier.size(42.dp).background(WgColors.White.copy(alpha = 0.22f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_play), null, Modifier.size(24.dp), tint = textColor)
                    }
                }
            }
        }
    }
}

/** Pastille numérotée cerclée de blanc (couronne pour le défi final, cadenas pour un niveau verrouillé). */
@Composable
private fun NumberBadge(level: MonumentLevel, locked: Boolean, accent: Color, dark: Color) {
    Box(Modifier.size(54.dp).background(WgColors.White, CircleShape).padding(4.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(if (locked) WgColors.LockedDark else if (level.isFinal) WgColors.Purple else accent),
            contentAlignment = Alignment.Center,
        ) {
            when {
                locked -> Icon(painterResource(R.drawable.ic_lock), null, Modifier.size(22.dp), tint = WgColors.White)
                level.isFinal -> ColorIcon(R.drawable.ic_crown, Modifier.size(28.dp))
                else -> Text(
                    level.number.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (accent == WgColors.Yellow) WgColors.Ink else WgColors.White,
                )
            }
        }
    }
}
