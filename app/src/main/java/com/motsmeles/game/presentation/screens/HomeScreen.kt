package com.motsmeles.game.presentation.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.ui.ChapterCard
import com.motsmeles.game.domain.ui.ChapterStatus
import com.motsmeles.game.domain.ui.DifficultyBand
import com.motsmeles.game.presentation.AppState
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.components.BubbleFace
import com.motsmeles.game.presentation.components.BubbleIconButton
import com.motsmeles.game.presentation.components.ButtonKind
import com.motsmeles.game.presentation.components.ChunkyProgressBar
import com.motsmeles.game.presentation.components.ColorIcon
import com.motsmeles.game.presentation.components.DifficultyBadge
import com.motsmeles.game.presentation.components.HintChip
import com.motsmeles.game.presentation.components.JuicyPressable
import com.motsmeles.game.presentation.components.RibbonBanner
import com.motsmeles.game.presentation.components.RibbonColors
import com.motsmeles.game.presentation.components.Ribbons
import com.motsmeles.game.presentation.components.TrophyBadge
import com.motsmeles.game.presentation.components.WatermarkBackground
import com.motsmeles.game.presentation.components.WgButton
import com.motsmeles.game.presentation.components.popIn
import com.motsmeles.game.presentation.home
import com.motsmeles.game.presentation.theme.ChapterTheme
import com.motsmeles.game.presentation.theme.LocalFeedback
import com.motsmeles.game.presentation.theme.LocalLanguage
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgChapters
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSize
import kotlin.math.abs

private val LockedRibbon = RibbonColors(WgColors.LockedInk, WgColors.LockedDark, WgColors.LockedInk)

/**
 * Accueil : un carrousel de chapitres que l'on fait glisser. Toucher le chapitre en cours ouvre directement son niveau
 * en cours ; un chapitre verrouillé ou terminé répond par un court message (un niveau gagné ne se rejoue jamais).
 * En haut : réglages à gauche, solde d'indices au centre, menu des niveaux à droite. Fond blanc + filigrane animé.
 */
@Composable
fun HomeScreen(state: AppState, vm: AppViewModel) {
    val home = remember(state.catalog, state.progress, state.language) { state.home() } ?: return
    val chapters = home.chapters
    val language = LocalLanguage.current
    val feedback = LocalFeedback.current
    val startPage = home.startPage.coerceIn(0, (chapters.size - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = startPage) { chapters.size }

    // Son + retour haptique léger à chaque changement de page (pas à l'ouverture).
    var firstPage by remember { mutableStateOf(true) }
    LaunchedEffect(pagerState.currentPage) {
        if (firstPage) {
            firstPage = false
        } else {
            feedback.play(Sound.SWIPE)
            feedback.perform(HapticEffect.SELECTION)
        }
    }

    WatermarkBackground {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("home_screen")) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BubbleIconButton(
                    R.drawable.ic_settings, stringResource(R.string.cd_settings), vm::openSettings,
                    Modifier.testTag("home_settings"), color = WgColors.Blue, dark = WgColors.BlueDark,
                    sound = Sound.MENU_CHANGE,
                )
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { HintChip(home.hintBalance) }
                BubbleIconButton(
                    R.drawable.ic_levels, stringResource(R.string.cd_monument), vm::openMonument,
                    Modifier.testTag("home_monument"), sound = Sound.MENU_CHANGE,
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("chapter_pager"),
                contentPadding = PaddingValues(horizontal = 44.dp, vertical = 6.dp),
                pageSpacing = 4.dp,
            ) { page ->
                val card = chapters[page]
                ChapterCardView(
                    card = card,
                    language = language,
                    pagerState = pagerState,
                    page = page,
                    onOpen = { vm.openChapter(card.index) },
                    onClaim = { vm.claimGift(card.index) },
                )
            }

            PageDots(pagerState, chapters.size, chapters.map { WgChapters.forKey(it.paletteKey).color })
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** Indicateur de page : la pastille active s'allonge avec un rebond, les autres restent rondes. */
@Composable
private fun PageDots(pagerState: PagerState, count: Int, colors: List<Color>) {
    if (count <= 1) return
    val reduce = LocalReduceMotion.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until count) {
            val active = pagerState.currentPage == i
            val width by animateDpAsState(
                targetValue = if (active) 30.dp else 11.dp,
                animationSpec = if (reduce) snap<Dp>() else spring<Dp>(dampingRatio = 0.45f, stiffness = 500f),
                label = "dot-width",
            )
            Box(
                Modifier
                    .height(11.dp)
                    .width(width)
                    .background(if (active) colors[i] else WgColors.Locked, RoundedCornerShape(WgRadius.Pill)),
            )
        }
    }
}

@Composable
private fun ChapterCardView(
    card: ChapterCard,
    language: Language,
    pagerState: PagerState,
    page: Int,
    onOpen: () -> Unit,
    onClaim: () -> Unit,
) {
    val theme = WgChapters.forKey(card.paletteKey)
    val locked = card.status == ChapterStatus.LOCKED
    val shape = RoundedCornerShape(34.dp)
    val lip = if (locked) WgColors.LockedDark else theme.dark
    val cardDescription = stringResource(R.string.home_chapter_title, card.index) + ", " + card.theme[language]

    JuicyPressable(
        onClick = onOpen,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 10.dp, horizontal = 2.dp)
            .testTag("chapter_card_${card.index}")
            .graphicsLayer {
                // Carrousel : la carte centrale est grande et droite, ses voisines reculent en s'inclinant légèrement.
                val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                val a = abs(offset).coerceIn(0f, 1f)
                val s = 1f - 0.13f * a
                scaleX = s
                scaleY = s
                rotationZ = offset * -4f
                translationY = a * 22.dp.toPx()
                transformOrigin = TransformOrigin(0.5f, 1f)
            },
        description = cardDescription,
        sound = if (locked) Sound.WRONG else Sound.MENU_CHANGE,
        haptic = if (locked) HapticEffect.ERROR else HapticEffect.CONFIRM,
        squash = 0.35f,
    ) { press ->
        Box(Modifier.fillMaxSize()) {
            // Lèvre 3D de la carte
            Box(Modifier.fillMaxSize().padding(top = 8.dp).background(lip, shape))
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp)
                    .graphicsLayer { translationY = press().coerceAtLeast(0f) * 8.dp.toPx() }
                    .shadow(10.dp, shape)
                    .clip(shape)
                    .background(WgColors.White),
            ) {
                IllustrationBand(card, theme, pagerState, page, locked)
                CardBody(card, theme, language, onClaim, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun IllustrationBand(card: ChapterCard, theme: ChapterTheme, pagerState: PagerState, page: Int, locked: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.40f)
            .background(if (locked) WgColors.Locked else theme.sky),
    ) {
        Image(
            painterResource(theme.illustration),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Parallaxe : l'illustration glisse un peu plus vite que la carte.
                    val offset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                    translationX = offset * 70.dp.toPx()
                    scaleX = 1.25f
                    scaleY = 1.25f
                },
            contentScale = ContentScale.Crop,
            alpha = if (locked) 0.35f else 1f,
        )
        RibbonBanner(
            text = stringResource(R.string.home_chapter_title, card.index),
            colors = if (locked) LockedRibbon else if (card.index % 2 == 1) Ribbons.Green else Ribbons.Purple,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.84f).padding(bottom = 4.dp),
            height = 48.dp,
        )
        if (locked) {
            Icon(painterResource(R.drawable.ic_lock), null, Modifier.align(Alignment.Center).size(54.dp).padding(bottom = 14.dp), tint = WgColors.White)
        }
    }
}

@Composable
private fun CardBody(card: ChapterCard, theme: ChapterTheme, language: Language, onClaim: () -> Unit, modifier: Modifier) {
    val locked = card.status == ChapterStatus.LOCKED
    Column(
        modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Titre : thème du chapitre (+ niveau en cours)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                card.theme[language],
                style = MaterialTheme.typography.headlineMedium,
                color = if (locked) WgColors.LockedInk else WgColors.Ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            val level = card.currentLevel
            Spacer(Modifier.height(4.dp))
            when {
                level != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.home_level_n, level.number), style = MaterialTheme.typography.titleMedium, color = WgColors.InkMuted)
                        DifficultyBadge(DifficultyBand.of(level.number))
                    }
                    Text(level.theme[language], style = MaterialTheme.typography.bodyLarge, color = WgColors.InkMuted, maxLines = 1)
                }
                card.status == ChapterStatus.COMPLETED -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ColorIcon(R.drawable.ic_check_green, Modifier.size(24.dp))
                        Text(stringResource(R.string.home_chapter_done), style = MaterialTheme.typography.titleMedium, color = WgColors.GreenDark)
                    }
                }
                else -> {
                    Text(
                        stringResource(R.string.home_chapter_locked_hint),
                        style = MaterialTheme.typography.bodyLarge, color = WgColors.LockedInk, textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Étoiles du chapitre
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ColorIcon(if (locked) R.drawable.ic_star_off else R.drawable.ic_star, Modifier.size(24.dp))
            Text(
                stringResource(R.string.home_chapter_stars, card.starsEarned, card.starsMax),
                style = MaterialTheme.typography.titleSmall, color = if (locked) WgColors.LockedInk else WgColors.Ink,
            )
        }

        // Progression continue vers le trophée
        val progressText = stringResource(R.string.home_gift_progress, card.completedCount, card.total)
        val progressDescription = stringResource(R.string.home_gift_description, card.completedCount, card.total)
        Column(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = progressDescription },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChunkyProgressBar(
                    fraction = card.fraction,
                    modifier = Modifier.weight(1f),
                    color = if (locked) WgColors.LockedInk else theme.color,
                    dark = if (locked) WgColors.LockedDark else theme.dark,
                    height = 20.dp,
                )
                TrophyBadge(ready = card.trophyClaimable, size = 46.dp)
            }
            Text(progressText, style = MaterialTheme.typography.labelMedium, color = WgColors.InkMuted)
        }

        // Action
        when {
            card.trophyClaimable -> WgButton(
                text = stringResource(R.string.home_trophy_claim),
                onClick = onClaim,
                modifier = Modifier.fillMaxWidth().testTag("home_trophy_claim"),
                kind = ButtonKind.Primary,
                sound = Sound.GIFT,
                haptic = HapticEffect.SUCCESS,
            )
            card.status == ChapterStatus.CURRENT -> PlayPill(stringResource(if (card.hasSnapshot) R.string.home_resume else R.string.home_play))
            else -> Spacer(Modifier.height(WgSize.Button))
        }
    }
}

/** Pastille « Jouer » de la carte courante : visuelle, la carte entière est le bouton (un seul geste, aucun faux bouton). */
@Composable
private fun PlayPill(text: String) {
    Box(Modifier.fillMaxWidth().height(WgSize.Button + WgSize.Lip).popIn(key = text)) {
        BubbleFace(Modifier.fillMaxSize(), WgColors.Green, WgColors.GreenDark, RoundedCornerShape(WgRadius.Md), { 0f }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painterResource(R.drawable.ic_play), null, Modifier.size(22.dp), tint = WgColors.White)
                Text(text, style = MaterialTheme.typography.labelLarge, color = WgColors.White)
            }
        }
    }
}
