package com.motsmeles.game.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.motsmeles.game.R
import com.motsmeles.game.presentation.UiMessage
import com.motsmeles.game.presentation.resolve
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgMotion
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSpacing

/**
 * Hôte de popup : le voile reste jusqu'à la fin de l'animation de sortie, la carte rétrécit en sortant (aucun fondu).
 * L'entrée « pop » est assurée par [PopupCard].
 */
@Composable
fun PopupHost(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    AnimatedVisibility(visible = visible, modifier = modifier, enter = EnterTransition.None, exit = ExitTransition.None) {
        PopupScrim(onDismiss = onDismiss) {
            Box(
                Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 26.dp)
                    .fillMaxWidth()
                    .animateEnterExit(enter = EnterTransition.None, exit = scaleOut(tween(WgMotion.QuickMs + 40), targetScale = 0.6f)),
                contentAlignment = Alignment.Center,
                content = content,
            )
        }
    }
}

/** Popup de confirmation : ruban en titre, texte, deux boutons bulles. */
@Composable
fun ConfirmPopup(
    visible: Boolean,
    title: String,
    body: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    ribbon: RibbonColors = Ribbons.Red,
    tag: String = "confirm_popup",
) {
    PopupHost(visible = visible, modifier = modifier, onDismiss = onDismiss) {
        PopupCard(title = title, ribbon = ribbon, modifier = Modifier.testTag(tag), onClose = onDismiss, closeDescription = stringResource(R.string.dialog_cancel)) {
            Text(body, style = MaterialTheme.typography.bodyLarge, color = WgColors.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WgSpacing.Sm)) {
                WgButton(dismissText, onDismiss, Modifier.weight(1f), kind = ButtonKind.Secondary)
                WgButton(confirmText, onConfirm, Modifier.weight(1f), kind = ButtonKind.Danger)
            }
        }
    }
}

/**
 * Popup « regarde une pub, gagne un indice » (maquette de pause : libellé, pastille +1, gros bouton vert lecture).
 * Aucune annonce n'est jamais obligatoire : « Plus tard » ferme sans rien perdre.
 */
@Composable
fun RewardedOfferPopup(
    visible: Boolean,
    title: String,
    body: String,
    watchDescription: String,
    laterText: String,
    onWatch: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PopupHost(visible = visible, modifier = modifier, onDismiss = onLater) {
        PopupCard(title = title, ribbon = Ribbons.Purple, modifier = Modifier.testTag("rewarded_popup"), onClose = onLater, closeDescription = laterText) {
            Text(body, style = MaterialTheme.typography.bodyLarge, color = WgColors.Ink, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WgRadius.Pill))
                    .background(WgColors.Track)
                    .padding(start = 18.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorIcon(R.drawable.ic_bulb, Modifier.size(44.dp))
                Text(stringResource(R.string.rewarded_plus_one), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.headlineMedium, color = WgColors.YellowDark)
                Spacer(Modifier.weight(1f))
                BubbleButton(
                    onClick = onWatch,
                    modifier = Modifier.size(width = 84.dp, height = 56.dp),
                    color = WgColors.Green,
                    dark = WgColors.GreenDark,
                    shape = RoundedCornerShape(WgRadius.Pill),
                    description = watchDescription,
                    haptic = HapticEffectConfirm,
                ) {
                    androidx.compose.material3.Icon(
                        androidx.compose.ui.res.painterResource(R.drawable.ic_play), null, Modifier.size(28.dp), tint = WgColors.White,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            WgButton(laterText, onLater, Modifier.fillMaxWidth(), kind = ButtonKind.Ghost)
        }
    }
}

private val HapticEffectConfirm = com.motsmeles.game.domain.ports.HapticEffect.CONFIRM

/** Message bref en haut de l'écran : tombe en rebondissant puis remonte en glissant (jamais de fondu). */
@Composable
fun MessageHost(message: UiMessage?, modifier: Modifier = Modifier) {
    var last by remember { mutableStateOf<UiMessage?>(null) }
    if (message != null) last = message
    val resources = LocalContext.current.resources
    Box(
        modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(top = WgSpacing.Sm),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.55f, stiffness = 420f)) { fullHeight -> -fullHeight * 2 },
            exit = slideOutVertically(animationSpec = tween(WgMotion.QuickMs)) { fullHeight -> -fullHeight * 2 },
        ) {
            val shown = last
            if (shown != null) {
                Text(
                    text = shown.resolve(resources),
                    modifier = Modifier
                        .testTag("message")
                        .padding(horizontal = WgSpacing.Md)
                        .clip(RoundedCornerShape(WgRadius.Pill))
                        .background(WgColors.PurpleDeep)
                        .padding(horizontal = WgSpacing.Lg, vertical = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    color = WgColors.White,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
