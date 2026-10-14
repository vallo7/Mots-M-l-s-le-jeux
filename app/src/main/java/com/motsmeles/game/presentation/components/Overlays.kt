package com.motsmeles.game.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.motsmeles.game.presentation.UiMessage
import com.motsmeles.game.presentation.resolve
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgMotion
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSpacing

/**
 * Panneau modal qui glisse depuis le bas (aucun fondu). Le voile apparaît instantanément et un tap dessus ferme.
 */
@Composable
fun SheetOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "sheet",
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(modifier.fillMaxSize()) {
        if (visible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(WgColors.Scrim)
                    .clickable(interactionSource = interaction, indication = null, onClick = onDismiss),
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(animationSpec = tween(WgMotion.SlowMs)) { fullHeight -> fullHeight },
            exit = slideOutVertically(animationSpec = tween(WgMotion.QuickMs)) { fullHeight -> fullHeight },
        ) {
            Column(
                modifier = Modifier
                    .testTag(tag)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(WgSpacing.Md)
                    .clip(RoundedCornerShape(WgRadius.Lg))
                    .background(WgColors.Surface)
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(WgSpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(WgSpacing.Md),
                content = content,
            )
        }
    }
}

@Composable
fun ConfirmSheet(
    visible: Boolean,
    title: String,
    body: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "confirm_sheet",
) {
    SheetOverlay(visible = visible, onDismiss = onDismiss, modifier = modifier, tag = tag) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = WgColors.Ink, modifier = Modifier.semantics { heading() })
        Text(body, style = MaterialTheme.typography.bodyLarge, color = WgColors.InkMuted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WgSpacing.Sm)) {
            WgButton(dismissText, onDismiss, Modifier.weight(1f), kind = ButtonKind.Secondary)
            WgButton(confirmText, onConfirm, Modifier.weight(1f))
        }
    }
}

/** Message bref en haut de l'écran : glisse puis disparaît (la disparition est un glissement, pas un fondu). */
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
            enter = slideInVertically(animationSpec = tween(WgMotion.StandardMs)) { fullHeight -> -fullHeight },
            exit = slideOutVertically(animationSpec = tween(WgMotion.QuickMs)) { fullHeight -> -fullHeight },
        ) {
            val shown = last
            if (shown != null) {
                Text(
                    text = shown.resolve(resources),
                    modifier = Modifier
                        .testTag("message")
                        .padding(horizontal = WgSpacing.Md)
                        .clip(RoundedCornerShape(WgRadius.Pill))
                        .background(WgColors.Ink)
                        .padding(horizontal = WgSpacing.Lg, vertical = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyLarge,
                    color = WgColors.Cream,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
