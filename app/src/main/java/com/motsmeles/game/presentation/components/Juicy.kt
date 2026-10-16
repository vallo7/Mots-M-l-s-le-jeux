package com.motsmeles.game.presentation.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.motsmeles.game.domain.ports.HapticEffect
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.presentation.theme.LocalFeedback
import com.motsmeles.game.presentation.theme.LocalReduceMotion
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgMotion
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSize
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

// =================================================================================================================
//  Pression « gelée » : écrasement à l'appui, rebond élastique au relâchement (jamais de fondu)
// =================================================================================================================

/**
 * Base de tout élément cliquable. La progression d'appui [press] vaut 0 au repos et 1 à fond ; au relâchement le
 * ressort sous-amorti la fait passer sous 0 (l'élément s'étire un instant) avant de se poser : effet gelée.
 * Son et haptique immédiats, action après un micro-délai, garde anti double-clic, focus visible.
 */
@Composable
fun JuicyPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null,
    stateText: String? = null,
    role: Role = Role.Button,
    sound: Sound? = Sound.CLICK,
    haptic: HapticEffect? = HapticEffect.SELECTION,
    squash: Float = 1f,
    content: @Composable BoxScope.(press: () -> Float) -> Unit,
) {
    val feedback = LocalFeedback.current
    val reduce = LocalReduceMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = if (reduce) snap<Float>() else spring<Float>(dampingRatio = 0.36f, stiffness = 620f),
        label = "press",
    )
    val scope = rememberCoroutineScope()
    val latestOnClick by rememberUpdatedState(onClick)
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = 1f + 0.07f * squash * press
                scaleY = 1f - 0.11f * squash * press
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
    ) {
        content { press }
    }
}

// =================================================================================================================
//  Boutons « bulle » : face colorée + lèvre foncée qui s'écrase à l'appui, reflet en haut
// =================================================================================================================

/** Face d'un bouton bulle. La taille vient de [modifier] (hauteur imposée, largeur libre ou imposée). */
@Composable
fun BubbleFace(
    modifier: Modifier,
    color: Color,
    dark: Color,
    shape: Shape,
    press: () -> Float,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val lip = WgSize.Lip
    Box(modifier, propagateMinConstraints = true) {
        Box(Modifier.matchParentSize().padding(top = lip).background(dark, shape))
        Box(
            Modifier
                .padding(bottom = lip)
                .graphicsLayer { translationY = press().coerceAtLeast(0f) * lip.toPx() }
                .background(color, shape)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.26f),
                        topLeft = Offset(w * 0.09f, h * 0.09f),
                        size = Size(w * 0.82f, h * 0.30f),
                        cornerRadius = CornerRadius(h * 0.15f),
                    )
                },
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

enum class ButtonKind { Primary, Secondary, Danger, Ghost }

private data class BubbleColors(val face: Color, val dark: Color, val content: Color)

private fun colorsFor(kind: ButtonKind, enabled: Boolean): BubbleColors = when {
    !enabled -> BubbleColors(WgColors.Locked, WgColors.LockedDark, WgColors.LockedInk)
    kind == ButtonKind.Primary -> BubbleColors(WgColors.Green, WgColors.GreenDark, WgColors.White)
    kind == ButtonKind.Secondary -> BubbleColors(WgColors.Purple, WgColors.PurpleDark, WgColors.White)
    kind == ButtonKind.Danger -> BubbleColors(WgColors.Red, WgColors.RedDark, WgColors.White)
    else -> BubbleColors(WgColors.White, WgColors.Outline, WgColors.Ink)
}

/** Bouton large avec libellé (et icône optionnelle). Plus compact qu'avant : 46 dp de face, zone tactile conservée. */
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
    val colors = colorsFor(kind, enabled)
    JuicyPressable(
        onClick = onClick,
        modifier = modifier.height(WgSize.Button + WgSize.Lip),
        enabled = enabled,
        sound = sound,
        haptic = haptic,
    ) { press ->
        BubbleFace(
            modifier = Modifier.fillMaxSize(),
            color = colors.face,
            dark = colors.dark,
            shape = RoundedCornerShape(WgRadius.Md),
            press = press,
        ) {
            Row(
                Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                if (icon != null) Icon(painterResource(icon), null, Modifier.size(20.dp), tint = colors.content)
                Text(
                    text,
                    style = MaterialTheme.typography.labelLarge.copy(
                        shadow = if (kind == ButtonKind.Ghost || !enabled) null else Shadow(colors.dark, Offset(0f, 3f), 0f),
                    ),
                    color = colors.content,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                )
            }
        }
    }
}

/** Bouton bulle à contenu libre : la taille (hauteur incluse) vient de [modifier] ; lèvre comprise. */
@Composable
fun BubbleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = WgColors.Purple,
    dark: Color = WgColors.PurpleDark,
    shape: Shape = RoundedCornerShape(WgRadius.Md),
    enabled: Boolean = true,
    description: String? = null,
    sound: Sound? = Sound.CLICK,
    haptic: HapticEffect? = HapticEffect.CONFIRM,
    content: @Composable BoxScope.() -> Unit,
) {
    JuicyPressable(onClick = onClick, modifier = modifier, enabled = enabled, description = description, sound = sound, haptic = haptic) { press ->
        BubbleFace(Modifier.fillMaxSize(), color, dark, shape, press, content = content)
    }
}

/** Bouton rond compact avec glyphe teinté. Face de [visualSize] ; la zone tactile reste de 48 dp. */
@Composable
fun BubbleIconButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = WgColors.Purple,
    dark: Color = WgColors.PurpleDark,
    tint: Color = WgColors.White,
    visualSize: Dp = WgSize.IconButton,
    enabled: Boolean = true,
    sound: Sound? = Sound.CLICK,
    haptic: HapticEffect? = HapticEffect.SELECTION,
) {
    JuicyPressable(
        onClick = onClick,
        modifier = modifier.size(WgSize.MinTouch),
        enabled = enabled,
        description = description,
        sound = sound,
        haptic = haptic,
    ) { press ->
        BubbleFace(
            modifier = Modifier.align(Alignment.Center).size(visualSize, visualSize + WgSize.Lip),
            color = color,
            dark = dark,
            shape = CircleShape,
            press = press,
        ) {
            Icon(painterResource(icon), null, Modifier.size(visualSize * 0.52f), tint = tint)
        }
    }
}

/** Gros bouton rond cerclé de blanc (maquettes de résultat : accueil, niveaux, suivant). */
@Composable
fun RingButton(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = WgColors.Purple,
    dark: Color = WgColors.PurpleDark,
    size: Dp = 62.dp,
    sound: Sound? = Sound.CLICK,
    haptic: HapticEffect? = HapticEffect.CONFIRM,
) {
    JuicyPressable(
        onClick = onClick,
        modifier = modifier.size(size),
        description = description,
        sound = sound,
        haptic = haptic,
    ) { press ->
        Box(
            Modifier
                .fillMaxSize()
                .shadow(6.dp, CircleShape)
                .background(WgColors.White, CircleShape)
                .padding(5.dp),
        ) {
            BubbleFace(Modifier.fillMaxSize(), color, dark, CircleShape, press) {
                Icon(painterResource(icon), null, Modifier.size(size * 0.42f), tint = WgColors.White)
            }
        }
    }
}

/** Icône multicolore (trophée, étoile, ampoule…) : jamais teintée. */
@Composable
fun ColorIcon(@DrawableRes icon: Int, modifier: Modifier = Modifier, description: String? = null) {
    Image(painterResource(icon), description, modifier)
}

// =================================================================================================================
//  Entrée en « pop » : l'élément grandit depuis rien avec rebond (aucun fondu)
// =================================================================================================================

/** Grandit de 0 à 1 avec un rebond ressort et une petite montée. [delayMs] décale l'entrée (cascade). */
@Composable
fun Modifier.popIn(delayMs: Int = 0, rise: Dp = 18.dp, key: Any = Unit, damping: Float = 0.52f, stiffness: Float = 330f): Modifier {
    val reduce = LocalReduceMotion.current
    val progress = remember(key) { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(key) {
        if (!reduce) {
            delay(delayMs.toLong())
            progress.animateTo(1f, spring<Float>(dampingRatio = damping, stiffness = stiffness))
        }
    }
    val risePx = with(LocalDensity.current) { rise.toPx() }
    return this.graphicsLayer {
        val p = progress.value
        scaleX = p
        scaleY = p
        translationY = (1f - p) * risePx
    }
}

/** Petit rebond de pastille (compteurs qui changent) : le facteur monte à ~1,3 puis se pose. */
@Composable
fun rememberBounce(trigger: Any?): Float {
    val reduce = LocalReduceMotion.current
    val anim = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (!reduce) {
            anim.snapTo(1.35f)
            anim.animateTo(1f, spring<Float>(dampingRatio = 0.35f, stiffness = 500f))
        }
    }
    return anim.value
}

// =================================================================================================================
//  Rubans (bannières de popup) — couleurs d'après la planche fournie
// =================================================================================================================

data class RibbonColors(val color: Color, val dark: Color, val deep: Color)

object Ribbons {
    val Pink = RibbonColors(Color(0xFFFF3D8B), Color(0xFFD62A73), Color(0xFFA81A57))
    val Blue = RibbonColors(Color(0xFF2EB8FF), Color(0xFF1F9AE0), Color(0xFF1573B0))
    val Purple = RibbonColors(Color(0xFF7B5CFF), Color(0xFF6342E6), Color(0xFF4630B5))
    val Yellow = RibbonColors(Color(0xFFFFC81E), Color(0xFFF2A100), Color(0xFFC27E00))
    val Green = RibbonColors(Color(0xFF5CD62F), Color(0xFF3FB41B), Color(0xFF2A8A10))
    val Red = RibbonColors(Color(0xFFFF4B55), Color(0xFFDB2F3A), Color(0xFFA8202A))
}

/** Ruban à deux queues fendues, plis et bande courbée ; le texte est posé sur la bande. */
@Composable
fun RibbonBanner(
    text: String,
    colors: RibbonColors,
    modifier: Modifier = Modifier,
    height: Dp = 58.dp,
    textColor: Color = WgColors.White,
) {
    Box(modifier.height(height), contentAlignment = Alignment.TopCenter) {
        Canvas(Modifier.fillMaxSize()) { drawRibbon(colors) }
        Box(
            Modifier.fillMaxWidth().height(height * 0.74f).padding(horizontal = height * 0.66f),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                style = MaterialTheme.typography.titleLarge.copy(shadow = Shadow(colors.deep, Offset(0f, 4f), 0f)),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun DrawScope.drawRibbon(c: RibbonColors) {
    val w = size.width
    val h = size.height
    val tail = h * 0.62f
    val bandL = tail * 0.80f
    val bandR = w - bandL
    val over = tail * 0.35f
    val curve = h * 0.06f
    val top = curve
    val bandH = h * 0.70f
    val ty0 = h * 0.22f
    val ty1 = h * 0.97f
    val notch = tail * 0.38f
    val midY = (ty0 + ty1) / 2f

    val left = Path().apply {
        moveTo(bandL + over, ty0); lineTo(0f, ty0); lineTo(notch, midY); lineTo(0f, ty1); lineTo(bandL + over, ty1); close()
    }
    val right = Path().apply {
        moveTo(bandR - over, ty0); lineTo(w, ty0); lineTo(w - notch, midY); lineTo(w, ty1); lineTo(bandR - over, ty1); close()
    }
    drawPath(left, c.dark)
    drawPath(right, c.dark)
    drawPath(Path().apply { moveTo(bandL, top + bandH); lineTo(bandL + over, top + bandH); lineTo(bandL + over, ty1); close() }, c.deep)
    drawPath(Path().apply { moveTo(bandR, top + bandH); lineTo(bandR - over, top + bandH); lineTo(bandR - over, ty1); close() }, c.deep)

    fun band(dy: Float): Path = Path().apply {
        val steps = 24
        for (i in 0..steps) {
            val x = bandL + (bandR - bandL) * i / steps
            val y = top - curve * sin(PI.toFloat() * i / steps) + dy
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        for (i in steps downTo 0) {
            val x = bandL + (bandR - bandL) * i / steps
            val y = top + bandH - curve * 0.8f * sin(PI.toFloat() * i / steps) + dy
            lineTo(x, y)
        }
        close()
    }
    drawPath(band(h * 0.05f), c.deep)
    val face = band(0f)
    drawPath(face, c.color)
    clipPath(face) {
        drawRect(
            brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.Transparent), startY = 0f, endY = h * 0.34f),
            topLeft = Offset(bandL, 0f),
            size = Size(bandR - bandL, h * 0.34f),
        )
    }
}

// =================================================================================================================
//  Popups : voile qui s'ouvre en cercle, carte qui rebondit, ruban en titre
// =================================================================================================================

/**
 * Voile + conteneur de popup. Le voile s'ouvre depuis le centre en cercle grandissant (aucun fondu) ; il intercepte
 * tous les gestes pour que rien ne se déclenche derrière. [onDismiss] = tap en dehors de la carte (null : ignoré).
 */
@Composable
fun PopupScrim(
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduce = LocalReduceMotion.current
    val reveal = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduce) reveal.animateTo(1f, tween(260, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)))
    }
    val latestDismiss by rememberUpdatedState(onDismiss)
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val maxR = kotlin.math.hypot(size.width, size.height)
                drawCircle(WgColors.Scrim, radius = maxR * reveal.value, center = center)
            }
            .pointerInput(Unit) { detectTapGestures { latestDismiss?.invoke() } },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Carte de popup : ruban en titre à cheval sur le bord haut, bouton fermer rose dans le coin, contenu en colonne.
 * [containerColor] blanc par défaut (violet pour la victoire).
 */
@Composable
fun PopupCard(
    title: String,
    ribbon: RibbonColors,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    closeDescription: String = "",
    containerColor: Color = WgColors.White,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(30.dp)
    Box(modifier.popIn(key = title).pointerInput(Unit) { detectTapGestures { } }) {
        Column(
            Modifier
                .padding(top = 30.dp)
                .fillMaxWidth()
                .shadow(16.dp, shape)
                .background(containerColor, shape)
                .padding(start = 20.dp, end = 20.dp, top = 44.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        RibbonBanner(title, ribbon, Modifier.align(Alignment.TopCenter).fillMaxWidth(0.86f))
        if (onClose != null) {
            BubbleIconButton(
                icon = com.motsmeles.game.R.drawable.ic_close,
                description = closeDescription,
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = 22.dp),
                color = WgColors.Pink,
                dark = WgColors.PinkDark,
                visualSize = 34.dp,
                sound = Sound.MENU_CHANGE,
            )
        }
    }
}

// =================================================================================================================
//  Barre de progression continue (chapitre) : remplissage fluide, reflet qui balaie, rebond à l'arrivée
// =================================================================================================================

/**
 * Barre continue (pas de segments). Quand [animateFrom] est fourni, la barre part de cette valeur et se remplit
 * jusqu'à [fraction] avec une courbe d'accélération douce, puis « souffle » (petit rebond d'épaisseur) en arrivant.
 * Un reflet diagonal balaie le remplissage en boucle (animations réduites : barre statique).
 */
@Composable
fun ChunkyProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = WgColors.Green,
    dark: Color = WgColors.GreenDark,
    height: Dp = 22.dp,
    animateFrom: Float? = null,
    animationMs: Int = 1200,
    startDelayMs: Int = 0,
    onFinished: (() -> Unit)? = null,
) {
    val reduce = LocalReduceMotion.current
    val target = fraction.coerceIn(0f, 1f)
    val progress = remember { Animatable(if (reduce) target else (animateFrom ?: target).coerceIn(0f, 1f)) }
    val squash = remember { Animatable(0f) }
    val latestFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(target) {
        if (reduce) {
            progress.snapTo(target)
        } else {
            delay(startDelayMs.toLong())
            if (progress.value != target) {
                progress.animateTo(target, tween(animationMs, easing = CubicBezierEasing(0.25f, 0.9f, 0.3f, 1f)))
                squash.snapTo(1f)
                squash.animateTo(0f, spring<Float>(dampingRatio = 0.3f, stiffness = 380f))
            }
            latestFinished?.invoke()
        }
    }
    val sweep = if (reduce) null else rememberInfiniteTransition(label = "bar-shine").animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "bar-shine-pos",
    )
    Canvas(
        modifier
            .height(height)
            .fillMaxWidth()
            .graphicsLayer { scaleY = 1f + 0.22f * squash.value },
    ) {
        val h = size.height
        val w = size.width
        val r = CornerRadius(h / 2f)
        drawRoundRect(WgColors.Track, Offset.Zero, size, r)
        val fillW = (w * progress.value).let { if (progress.value > 0.001f) it.coerceAtLeast(h) else 0f }
        if (fillW > 0f) {
            drawRoundRect(dark, Offset(0f, h * 0.08f), Size(fillW, h * 0.92f), r)
            val body = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, fillW, h * 0.92f, r)) }
            drawPath(body, color)
            clipPath(body) {
                drawRoundRect(
                    Color.White.copy(alpha = 0.30f), Offset(h * 0.35f, h * 0.12f), Size((fillW - h * 0.7f).coerceAtLeast(0f), h * 0.24f), CornerRadius(h * 0.12f),
                )
                val s = sweep?.value
                if (s != null) {
                    val cx = fillW * s
                    val band = h * 1.1f
                    val stripe = Path().apply {
                        moveTo(cx - band, h); lineTo(cx - band * 0.4f, 0f); lineTo(cx + band * 0.4f, 0f); lineTo(cx - band * 0.2f, h); close()
                    }
                    drawPath(stripe, Color.White.copy(alpha = 0.30f))
                }
            }
            drawCircle(Color.White.copy(alpha = 0.55f), h * 0.12f, Offset(fillW - h * 0.4f, h * 0.34f))
        }
    }
}

/** Petite bosse d'attention (rotation de part et d'autre) répétée toutes les ~3 s : appelle l'œil sans être agaçante. */
@Composable
fun rememberWobble(active: Boolean): Float {
    val reduce = LocalReduceMotion.current
    if (!active || reduce) return 0f
    val t = rememberInfiniteTransition(label = "wobble").animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                0f at 0
                -14f at 110 using FastOutSlowInEasing
                12f at 230 using FastOutSlowInEasing
                -8f at 350 using FastOutSlowInEasing
                5f at 470 using FastOutSlowInEasing
                0f at 600 using FastOutSlowInEasing
                0f at 3200
            },
        ),
        label = "wobble-angle",
    )
    return t.value
}
