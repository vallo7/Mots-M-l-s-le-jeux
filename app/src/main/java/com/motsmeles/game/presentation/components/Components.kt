package com.motsmeles.game.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.ui.DifficultyBand
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSize
import com.motsmeles.game.presentation.theme.WgSpacing

/** Élément cliquable « classique » (lignes de paramètres…) : mêmes retours gelée / son / haptique que les boutons bulle. */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    stateText: String? = null,
    role: Role = Role.Button,
    sound: Sound? = Sound.CLICK,
    haptic: HapticEffect? = HapticEffect.SELECTION,
    content: @Composable BoxScope.() -> Unit,
) {
    JuicyPressable(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        description = description,
        stateText = stateText,
        role = role,
        sound = sound,
        haptic = haptic,
        squash = 0.45f,
    ) { content() }
}

/**
 * Interrupteur de la maquette (piste grise, pouce blanc, jaune une fois activé). Le pouce glisse avec un rebond et
 * s'étire brièvement : la zone tactile est la ligne entière (voir les paramètres).
 */
@Composable
fun WgSwitchVisual(checked: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val offset by animateDpAsState(
        targetValue = if (checked) 25.dp else 3.dp,
        animationSpec = if (reduce) snap<Dp>() else spring<Dp>(dampingRatio = 0.42f, stiffness = 520f),
        label = "switch-thumb",
    )
    Box(
        modifier
            .size(width = 56.dp, height = 32.dp)
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(if (checked) WgColors.Yellow else WgColors.Locked),
    ) {
        Box(
            Modifier
                .offset(x = offset, y = 3.dp)
                .size(26.dp)
                .background(WgColors.White, CircleShape),
        )
    }
}

@Composable
fun DifficultyBadge(band: DifficultyBand, modifier: Modifier = Modifier) {
    val (label, background, foreground) = when (band) {
        DifficultyBand.EASY -> Triple(R.string.band_easy, WgColors.Teal, WgColors.White)
        DifficultyBand.MEDIUM -> Triple(R.string.band_medium, WgColors.Yellow, WgColors.Ink)
        DifficultyBand.HARD -> Triple(R.string.band_hard, WgColors.Pink, WgColors.White)
        DifficultyBand.FINAL -> Triple(R.string.band_final, WgColors.Purple, WgColors.White)
    }
    Row(
        modifier
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (band == DifficultyBand.FINAL) ColorIcon(R.drawable.ic_crown, Modifier.size(14.dp))
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = foreground, maxLines = 1)
    }
}

/** Étoiles obtenues (dorées) et manquantes (lavande) ; la valeur est aussi annoncée aux lecteurs d'écran. */
@Composable
fun StarsRow(stars: Int, size: Dp, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val description = resources.getQuantityString(R.plurals.stars_count, stars, stars)
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        for (i in 0 until 3) {
            ColorIcon(if (i < stars) R.drawable.ic_star else R.drawable.ic_star_off, Modifier.size(size))
        }
    }
}

/** Trophée de fin de chapitre (remplace le cadeau). Quand il est prêt, il saute en boucle ; statique si animations réduites. */
@Composable
fun TrophyBadge(ready: Boolean, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val wobble = rememberWobble(ready)
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                rotationZ = wobble
                val s = if (ready) 1.08f else 1f
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.Center,
    ) {
        ColorIcon(R.drawable.ic_trophy, Modifier.fillMaxSize())
    }
}

/** Solde d'indices : ampoule + nombre, dans une pastille blanche cerclée de lavande ; le nombre rebondit quand il change. */
@Composable
fun HintChip(count: Int, modifier: Modifier = Modifier) {
    val text = LocalContext.current.resources.getQuantityString(R.plurals.hints_count, count, count)
    val bounce = rememberBounce(count)
    Row(
        modifier
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(WgColors.White)
            .border(2.dp, WgColors.Outline, RoundedCornerShape(WgRadius.Pill))
            .padding(start = 6.dp, end = 14.dp, top = 3.dp, bottom = 3.dp)
            .semantics(mergeDescendants = true) { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ColorIcon(R.drawable.ic_bulb, Modifier.size(26.dp))
        Text(
            count.toString(),
            modifier = Modifier.graphicsLayer {
                scaleX = bounce
                scaleY = bounce
            },
            style = MaterialTheme.typography.titleMedium,
            color = WgColors.Ink,
            maxLines = 1,
        )
    }
}

/** Barre de titre des écrans secondaires. */
@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = WgSpacing.Sm, vertical = WgSpacing.Xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BubbleIconButton(R.drawable.ic_back, stringResource(R.string.cd_back), onBack, sound = Sound.MENU_CHANGE)
        Text(
            text = title,
            modifier = Modifier.weight(1f).padding(horizontal = WgSpacing.Sm).semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            color = WgColors.Ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(WgSize.MinTouch))
    }
}

@Composable
fun FillBox(modifier: Modifier = Modifier, color: Color = WgColors.Background, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(color), content = content)
}

/** Face « kawaii » posée sur une étoile gagnée (yeux + sourire), comme la maquette de victoire. */
@Composable
fun StarFace(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val s = size.minDimension
        val ink = WgColors.Ink
        drawCircle(ink, s * 0.045f, androidx.compose.ui.geometry.Offset(s * 0.41f, s * 0.54f))
        drawCircle(ink, s * 0.045f, androidx.compose.ui.geometry.Offset(s * 0.59f, s * 0.54f))
        val smile = androidx.compose.ui.graphics.Path().apply {
            moveTo(s * 0.44f, s * 0.62f)
            quadraticBezierTo(s * 0.5f, s * 0.70f, s * 0.56f, s * 0.62f)
        }
        drawPath(smile, ink, style = androidx.compose.ui.graphics.drawscope.Stroke(width = s * 0.032f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}
