package com.motsmeles.game.presentation.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.motsmeles.game.R
import com.motsmeles.game.domain.model.Chapter
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.Level
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.ui.DifficultyBand
import com.motsmeles.game.domain.ui.HomeModel
import com.motsmeles.game.domain.ui.HomeModelBuilder
import com.motsmeles.game.presentation.AppState
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.components.*
import com.motsmeles.game.presentation.theme.*

/** Accueil : le niveau en cours est mis en avant, un seul tap sur « Jouer » lance la partie (aucun écran intermédiaire). */
@Composable
fun HomeScreen(state: AppState, vm: AppViewModel) {
    val catalog = state.catalog ?: return
    val language = state.language
    val home = remember(catalog, state.progress, language) { HomeModelBuilder.build(catalog, state.progress, language) }
    val level = home.currentLevel
    val currentChapter = home.currentChapter
    val giftChapter = home.giftChapter
    val chapterTheme = WgChapters.forKey((currentChapter ?: giftChapter)?.paletteKey ?: "meadow")

    WatermarkBackground(background = chapterTheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(WgSpacing.Md),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                WgIconButton(
                    R.drawable.ic_settings, stringResource(R.string.cd_settings), vm::openSettings,
                    modifier = Modifier.testTag("home_settings"), sound = Sound.MENU_CHANGE,
                )
                Spacer(Modifier.width(WgSpacing.Sm))
                LanguagePill(language, vm::toggleLanguage, Modifier.testTag("home_language"))
                Spacer(Modifier.weight(1f))
                WgIconButton(
                    R.drawable.ic_monument, stringResource(R.string.cd_monument), vm::openMonument,
                    modifier = Modifier.testTag("home_monument"), sound = Sound.MENU_CHANGE,
                )
            }
            Spacer(Modifier.weight(1f))
            if (level != null && currentChapter != null) {
                LevelCard(level, currentChapter, language, Modifier.testTag("home_level_card"))
            } else {
                AllDoneCard()
            }
            Spacer(Modifier.height(WgSpacing.Md))
            HintChip(home.hintBalance, Modifier.align(Alignment.CenterHorizontally).testTag("home_hints"))
            Spacer(Modifier.weight(1f))
            if (giftChapter != null) {
                GiftSection(home, onClaim = { vm.claimGift(giftChapter.index) })
                Spacer(Modifier.height(WgSpacing.Md))
            }
            if (level != null) {
                WgButton(
                    text = stringResource(if (home.hasSnapshot) R.string.home_resume else R.string.home_play),
                    onClick = vm::playCurrent,
                    modifier = Modifier.fillMaxWidth().testTag("home_play"),
                    enabled = !state.busy,
                    icon = R.drawable.ic_play,
                )
            }
        }
    }
}

@Composable
private fun LevelCard(level: Level, chapter: Chapter, language: Language, modifier: Modifier = Modifier) {
    val style = WgStyles.forStyle(level.visualStyle)
    val theme = WgChapters.forKey(chapter.paletteKey)
    val shape = RoundedCornerShape(WgRadius.Lg)
    Column(modifier.fillMaxWidth().clip(shape).background(WgColors.Surface).border(3.dp, style.accent, shape)) {
        Image(
            painter = painterResource(theme.illustration),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(120.dp),
        )
        Column(Modifier.padding(WgSpacing.Lg), verticalArrangement = Arrangement.spacedBy(WgSpacing.Sm)) {
            Text(
                stringResource(R.string.home_chapter_level, chapter.index, level.number),
                style = MaterialTheme.typography.labelLarge,
                color = WgColors.InkMuted,
            )
            Text(
                level.theme[language],
                style = MaterialTheme.typography.headlineLarge,
                color = WgColors.Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(WgSpacing.Sm), verticalAlignment = Alignment.CenterVertically) {
                DifficultyBadge(DifficultyBand.of(level.number))
                Text(
                    stringResource(R.string.monument_grid_size, level.gridSize),
                    style = MaterialTheme.typography.labelLarge,
                    color = WgColors.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun AllDoneCard() {
    val shape = RoundedCornerShape(WgRadius.Lg)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(WgColors.Surface).border(3.dp, WgColors.Plum, shape).padding(WgSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(WgSpacing.Sm),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(WgColors.Plum), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_crown), null, Modifier.size(40.dp), tint = Color.White)
        }
        Text(stringResource(R.string.home_all_done_title), style = MaterialTheme.typography.headlineLarge, color = WgColors.Ink, textAlign = TextAlign.Center)
        Text(stringResource(R.string.home_all_done_body), style = MaterialTheme.typography.bodyLarge, color = WgColors.InkMuted, textAlign = TextAlign.Center)
    }
}

/** Progression vers le cadeau du chapitre : la barre se remplit niveau après niveau jusqu'au cadeau. */
@Composable
private fun GiftSection(home: HomeModel, onClaim: () -> Unit) {
    val description = stringResource(R.string.home_gift_description, home.giftChapterCompleted, home.giftChapterTotal)
    Column(
        Modifier.fillMaxWidth().testTag("home_gift"),
        verticalArrangement = Arrangement.spacedBy(WgSpacing.Sm),
    ) {
        Row(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WgSpacing.Md),
        ) {
            ProgressSegments(home.giftChapterCompleted, home.giftChapterTotal, Modifier.weight(1f))
            GiftBadge(ready = home.giftClaimable)
        }
        Text(
            text = if (home.giftClaimable) stringResource(R.string.home_gift_ready)
            else stringResource(R.string.home_gift_progress, home.giftChapterCompleted, home.giftChapterTotal),
            style = MaterialTheme.typography.labelLarge,
            color = WgColors.InkMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        if (home.giftClaimable) {
            WgButton(
                text = stringResource(R.string.home_gift_open),
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth().testTag("home_gift_open"),
                kind = ButtonKind.Secondary,
                icon = R.drawable.ic_gift,
                sound = Sound.CLICK,
            )
        }
    }
}

/** Bascule FR | EN : la langue active est surlignée, la lecture de l'état se fait aussi par texte (pas par la couleur seule). */
@Composable
fun LanguagePill(current: Language, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(WgRadius.Pill)
    Pressable(
        onClick = onToggle,
        modifier = modifier
            .height(WgSize.MinTouch)
            .clip(shape)
            .background(WgColors.Surface)
            .border(2.dp, WgColors.Outline, shape)
            .padding(4.dp),
        description = stringResource(R.string.cd_language),
        stateText = current.code.uppercase(),
        sound = Sound.MENU_CHANGE,
    ) {
        Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            LanguageSegment(stringResource(R.string.language_fr_short), current == Language.FR)
            LanguageSegment(stringResource(R.string.language_en_short), current == Language.EN)
        }
    }
}

@Composable
private fun LanguageSegment(text: String, selected: Boolean) {
    Box(
        Modifier
            .defaultMinSize(minWidth = 40.dp, minHeight = 36.dp)
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(if (selected) WgColors.Coral else Color.Transparent)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = WgColors.Ink)
    }
}
