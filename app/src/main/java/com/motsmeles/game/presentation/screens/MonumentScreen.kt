package com.motsmeles.game.presentation.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.motsmeles.game.R
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.progress.LevelStatus
import com.motsmeles.game.domain.progress.ProgressRules
import com.motsmeles.game.domain.ui.DifficultyBand
import com.motsmeles.game.domain.ui.MonumentBuilder
import com.motsmeles.game.domain.ui.MonumentChapter
import com.motsmeles.game.domain.ui.MonumentLevel
import com.motsmeles.game.presentation.AppState
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.components.*
import com.motsmeles.game.presentation.theme.*

/**
 * Monument des niveaux : chaque chapitre est une tour de dix paliers (le niveau 1 en bas, le défi final au sommet).
 * Les niveaux terminés, disponible et verrouillés se distinguent par la forme, le texte et l'icône (pas par la couleur seule).
 */
@Composable
fun MonumentScreen(state: AppState, vm: AppViewModel) {
    val catalog = state.catalog ?: return
    val language = state.language
    val chapters = remember(catalog, state.progress) { MonumentBuilder.build(catalog, state.progress) }
    val currentChapter = remember(catalog, state.progress) { ProgressRules.currentLevel(catalog, state.progress)?.chapterIndex ?: chapters.size }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (currentChapter - 1).coerceIn(0, (chapters.size - 1).coerceAtLeast(0)))

    FillBox(Modifier.testTag("monument_screen")) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenTopBar(stringResource(R.string.monument_title), vm::onBack)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = WgSpacing.Md, vertical = WgSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(WgSpacing.Lg),
            ) {
                items(chapters, key = { it.index }) { chapter ->
                    ChapterBlock(chapter, language, onLevel = vm::openLevel, onClaim = { vm.claimGift(chapter.index) })
                }
            }
        }
    }
}

@Composable
private fun ChapterBlock(chapter: MonumentChapter, language: Language, onLevel: (String) -> Unit, onClaim: () -> Unit) {
    val theme = WgChapters.forKey(chapter.paletteKey)
    val shape = RoundedCornerShape(WgRadius.Lg)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(theme.background).border(2.dp, WgColors.Outline, shape).padding(WgSpacing.Md),
        verticalArrangement = Arrangement.spacedBy(WgSpacing.Md),
    ) {
        Image(
            painter = painterResource(theme.illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(WgRadius.Md)),
        )
        Text(
            stringResource(R.string.monument_chapter, chapter.index, chapter.theme[language]),
            style = MaterialTheme.typography.titleLarge,
            color = WgColors.Ink,
            modifier = Modifier.semantics { heading() },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WgSpacing.Md)) {
            ProgressSegments(chapter.completedCount, chapter.levels.size, Modifier.weight(1f))
            GiftBadge(ready = chapter.giftClaimable)
        }
        Text(
            text = stringResource(
                when {
                    chapter.giftClaimed -> R.string.monument_gift_claimed
                    chapter.giftClaimable -> R.string.home_gift_ready
                    else -> R.string.monument_gift_pending
                },
            ),
            style = MaterialTheme.typography.labelLarge,
            color = WgColors.InkMuted,
        )
        if (chapter.giftClaimable) {
            WgButton(
                text = stringResource(R.string.home_gift_open),
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth(),
                kind = ButtonKind.Secondary,
                icon = R.drawable.ic_gift,
            )
        }
        chapter.levels.reversed().forEach { level ->
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                LevelTile(level, language, onLevel, Modifier.fillMaxWidth(widthFraction(level.number)))
            }
        }
    }
}

private fun widthFraction(number: Int): Float = when (number) {
    10 -> 0.68f
    9, 8 -> 0.76f
    7, 6 -> 0.84f
    5, 4 -> 0.92f
    else -> 1f
}

@Composable
private fun LevelTile(level: MonumentLevel, language: Language, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val palette = WgStyles.forStyle(level.style)
    val shape = RoundedCornerShape(WgRadius.Md)
    val locked = level.status == LevelStatus.LOCKED
    val available = level.status == LevelStatus.AVAILABLE
    val container = when (level.status) {
        LevelStatus.COMPLETED -> palette.soft
        LevelStatus.AVAILABLE -> WgColors.Surface
        LevelStatus.LOCKED -> Color(0xFFEFE9DD)
    }
    val borderColor = when (level.status) {
        LevelStatus.COMPLETED -> palette.accent
        LevelStatus.AVAILABLE -> WgColors.Coral
        LevelStatus.LOCKED -> WgColors.Outline
    }
    val statusText = stringResource(
        when (level.status) {
            LevelStatus.COMPLETED -> R.string.monument_status_completed
            LevelStatus.AVAILABLE -> R.string.monument_status_available
            LevelStatus.LOCKED -> R.string.monument_status_locked
        },
    )
    val bandText = stringResource(
        when (level.band) {
            DifficultyBand.EASY -> R.string.band_easy
            DifficultyBand.MEDIUM -> R.string.band_medium
            DifficultyBand.HARD -> R.string.band_hard
            DifficultyBand.FINAL -> R.string.band_final
        },
    )
    val themeText = level.theme[language]
    val description = stringResource(R.string.monument_level_description, level.number, themeText, bandText, statusText)
    Pressable(
        onClick = { onClick(level.levelId) },
        modifier = modifier
            .testTag("level_${level.levelId}")
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(container)
            .border(if (available) 3.dp else 2.dp, borderColor, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        description = description,
        sound = if (locked) Sound.WRONG else Sound.CLICK,
        haptic = if (locked) HapticEffect.ERROR else HapticEffect.SELECTION,
    ) {
        Row(Modifier.fillMaxWidth().align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (locked) WgColors.Outline else palette.accent),
                contentAlignment = Alignment.Center,
            ) {
                if (level.isFinal) {
                    Icon(painterResource(R.drawable.ic_crown), null, Modifier.size(22.dp), tint = if (locked) WgColors.InkMuted else palette.onAccent)
                } else {
                    Text(level.number.toString(), style = MaterialTheme.typography.titleMedium, color = if (locked) WgColors.InkMuted else palette.onAccent)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    themeText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (locked) WgColors.InkMuted else WgColors.Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(R.string.monument_level_subtitle, bandText, stringResource(R.string.monument_grid_size, level.gridSize)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = WgColors.InkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            when (level.status) {
                LevelStatus.COMPLETED -> StarsRow(level.stars, 16.dp)
                LevelStatus.AVAILABLE -> Box(Modifier.size(32.dp).clip(CircleShape).background(WgColors.Coral), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_play), null, Modifier.size(18.dp), tint = WgColors.Ink)
                }
                LevelStatus.LOCKED -> Icon(painterResource(R.drawable.ic_lock), null, Modifier.size(22.dp), tint = WgColors.InkMuted)
            }
        }
    }
}
