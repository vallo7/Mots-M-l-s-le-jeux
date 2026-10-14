package com.motsmeles.game.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.ui.DifficultyBand
import com.motsmeles.game.presentation.theme.LocalFeedback
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgMotion
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSize
import com.motsmeles.game.presentation.theme.WgSpacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Base de tout élément cliquable : état pressé animé (échelle, rebond contrôlé), son et haptique immédiats,
 * action déclenchée après un micro-délai (~90 ms), garde anti double-clic, état focus visible, sans fondu.
 */
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
    val feedback = LocalFeedback.current
    val reduce = LocalReduceMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.94f else 1f,
        animationSpec = if (reduce) snap<Float>() else spring<Float>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press-scale",
    )
    val scope = rememberCoroutineScope()
    val latestOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(modifier)
            .then(if (focused) Modifier.border(2.dp, WgColors.Ink, RoundedCornerShape(WgRadius.Md)) else Modifier)
            .semantics {
                if (description != null) contentDescription = description
                if (stateText != null) stateDescription = stateText
            }
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, role = role) {
                if (feedback.tryAcquireClick()) {
                    sound?.let { feedback.play(it) }
                    haptic?.let { feedback.perform(it) }
                    scope.launch {
                        delay(WgMotion.PressDelayMs)
                        latestOnClick()
                    }
                }
            },
        content = content,
    )
}

enum class ButtonKind { Primary, Secondary, Ghost }

@Composable
fun WgButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.Primary,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    sound: Sound? = Sound.CLICK,
    haptic: HapticEffect? = HapticEffect.CONFIRM,
) {
    val shape = RoundedCornerShape(WgRadius.Md)
    val container = when {
        !enabled -> WgColors.Outline
        kind == ButtonKind.Primary -> WgColors.Coral
        kind == ButtonKind.Secondary -> WgColors.Surface
        else -> Color.Transparent
    }
    val contentColor = if (enabled) WgColors.Ink else WgColors.InkMuted
    val outline = if (kind == ButtonKind.Secondary && enabled) Modifier.border(2.dp, WgColors.Outline, shape) else Modifier
    Pressable(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = WgSize.Button)
            .clip(shape)
            .background(container)
            .then(outline)
            .padding(horizontal = WgSpacing.Lg),
        enabled = enabled,
        sound = sound,
        haptic = haptic,
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(WgSpacing.Sm),
        ) {
            if (icon != null) Icon(painterResource(icon), null, Modifier.size(22.dp), tint = contentColor)
            Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
fun WgIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = WgColors.Surface,
    tint: Color = WgColors.Ink,
    sound: Sound? = Sound.CLICK,
    haptic: HapticEffect? = HapticEffect.SELECTION,
) {
    Pressable(
        onClick = onClick,
        modifier = modifier.size(WgSize.MinTouch).clip(CircleShape).background(container),
        description = description,
        sound = sound,
        haptic = haptic,
    ) {
        Icon(painterResource(icon), null, Modifier.align(Alignment.Center).size(24.dp), tint = tint)
    }
}

/** Aspect d'un interrupteur (la zone tactile est la ligne entière, voir les paramètres). Le pouce glisse avec un léger rebond. */
@Composable
fun WgSwitchVisual(checked: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val offset by animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        animationSpec = if (reduce) snap<Dp>() else spring<Dp>(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "switch-thumb",
    )
    Box(
        modifier
            .size(width = 56.dp, height = 32.dp)
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(if (checked) WgColors.Teal else WgColors.Outline),
    ) {
        Box(Modifier.offset(x = offset, y = 4.dp).size(24.dp).clip(CircleShape).background(WgColors.Surface))
    }
}

@Composable
fun DifficultyBadge(band: DifficultyBand, modifier: Modifier = Modifier) {
    val (label, background, foreground) = when (band) {
        DifficultyBand.EASY -> Triple(R.string.band_easy, Color(0xFFD5F2EE), WgColors.Ink)
        DifficultyBand.MEDIUM -> Triple(R.string.band_medium, Color(0xFFFFF0CC), WgColors.Ink)
        DifficultyBand.HARD -> Triple(R.string.band_hard, Color(0xFFFFE0D7), WgColors.Ink)
        DifficultyBand.FINAL -> Triple(R.string.band_final, WgColors.Plum, Color.White)
    }
    Row(
        modifier
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (band == DifficultyBand.FINAL) Icon(painterResource(R.drawable.ic_crown), null, Modifier.size(14.dp), tint = foreground)
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = foreground, maxLines = 1)
    }
}

@Composable
fun StarsRow(stars: Int, size: Dp, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
    val description = resources.getQuantityString(R.plurals.stars_count, stars, stars)
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 0 until 3) {
            val earned = i < stars
            Icon(
                painterResource(if (earned) R.drawable.ic_star else R.drawable.ic_star_outline),
                null,
                Modifier.size(size),
                tint = if (earned) WgColors.Sun else WgColors.InkMuted,
            )
        }
    }
}

/** Barre segmentée qui se remplit niveau après niveau (progression vers le cadeau du chapitre). */
@Composable
fun ProgressSegments(completed: Int, total: Int, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until total) {
            val fill by animateFloatAsState(
                targetValue = if (i < completed) 1f else 0f,
                animationSpec = if (reduce) snap<Float>() else spring<Float>(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                label = "segment",
            )
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(WgColors.Outline)) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = fill
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .background(WgColors.Teal),
                )
            }
        }
    }
}

/** Icône de cadeau : rebondit en boucle quand il est prêt à être ouvert (statique si animations réduites). */
@Composable
fun GiftBadge(ready: Boolean, modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val bounce = rememberInfiniteTransition(label = "gift").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 1400
                0f at 0
                1f at 220 using FastOutSlowInEasing
                0f at 560 using FastOutSlowInEasing
                0f at 1400
            },
        ),
        label = "gift-bounce",
    )
    Box(
        modifier
            .size(44.dp)
            .graphicsLayer {
                if (ready && !reduce) translationY = -8.dp.toPx() * bounce.value
            }
            .clip(CircleShape)
            .background(if (ready) WgColors.Sun else WgColors.Outline),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(R.drawable.ic_gift), null, Modifier.size(24.dp), tint = WgColors.Ink)
    }
}

@Composable
fun HintChip(count: Int, modifier: Modifier = Modifier) {
    val text = LocalContext.current.resources.getQuantityString(R.plurals.hints_count, count, count)
    Row(
        modifier
            .clip(RoundedCornerShape(WgRadius.Pill))
            .background(WgColors.Surface)
            .border(2.dp, WgColors.Outline, RoundedCornerShape(WgRadius.Pill))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(painterResource(R.drawable.ic_hint), null, Modifier.size(18.dp), tint = WgColors.Sun)
        Text(text, style = MaterialTheme.typography.labelLarge, color = WgColors.Ink, maxLines = 1)
    }
}

@Composable
fun ScreenTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = WgSpacing.Md, vertical = WgSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WgIconButton(R.drawable.ic_back, stringResource(R.string.cd_back), onBack, sound = Sound.MENU_CHANGE)
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
fun FillBox(modifier: Modifier = Modifier, color: Color = WgColors.Cream, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxSize().background(color), content = content)
}
