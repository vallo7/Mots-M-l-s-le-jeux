package com.motsmeles.game.presentation.screens

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
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.motsmeles.game.BuildConfig
import com.motsmeles.game.R
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.domain.settings.AppSettings
import com.motsmeles.game.presentation.AppState
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.components.*
import com.motsmeles.game.presentation.theme.*

/** Paramètres : tous les réglages sont persistés et appliqués immédiatement. Pas de musique : aucun fichier n'est livré. */
@Composable
fun SettingsScreen(state: AppState, vm: AppViewModel) {
    val settings = state.settings
    var confirmRestore by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().testTag("settings_screen")) {
        FillBox {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                ScreenTopBar(stringResource(R.string.settings_title), vm::onBack)
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = WgSpacing.Md, vertical = WgSpacing.Sm),
                    verticalArrangement = Arrangement.spacedBy(WgSpacing.Sm),
                ) {
                    SectionTitle(stringResource(R.string.settings_language))
                    LanguageChoice(state.language) { language -> vm.updateSettings { it.copy(language = language) } }
                    Spacer(Modifier.height(WgSpacing.Sm))
                    ToggleRow(stringResource(R.string.settings_sound), null, settings.soundEnabled, "toggle_sound") { value ->
                        vm.updateSettings { it.copy(soundEnabled = value) }
                    }
                    ToggleRow(stringResource(R.string.settings_haptics), null, settings.hapticsEnabled, "toggle_haptics") { value ->
                        vm.updateSettings { it.copy(hapticsEnabled = value) }
                    }
                    Spacer(Modifier.height(WgSpacing.Sm))
                    SectionTitle(stringResource(R.string.settings_accessibility))
                    ToggleRow(
                        stringResource(R.string.settings_reduce_motion),
                        stringResource(R.string.settings_reduce_motion_hint),
                        settings.reduceMotion,
                        "toggle_reduce_motion",
                    ) { value -> vm.updateSettings { it.copy(reduceMotion = value) } }
                    ToggleRow(stringResource(R.string.settings_large_text), null, settings.largeText, "toggle_large_text") { value ->
                        vm.updateSettings { it.copy(largeText = value) }
                    }
                    Spacer(Modifier.height(WgSpacing.Sm))
                    NavRow(stringResource(R.string.settings_credits), "row_credits", vm::openCredits)
                    if (vm.privacyOptionsRequired) {
                        NavRow(stringResource(R.string.settings_privacy_options), "row_privacy", vm::openPrivacyOptions)
                    }
                    Spacer(Modifier.height(WgSpacing.Md))
                    WgButton(
                        text = stringResource(R.string.settings_restore),
                        onClick = { confirmRestore = true },
                        modifier = Modifier.fillMaxWidth().testTag("settings_restore"),
                        kind = ButtonKind.Secondary,
                    )
                    Spacer(Modifier.height(WgSpacing.Lg))
                }
            }
        }
        ConfirmSheet(
            visible = confirmRestore,
            title = stringResource(R.string.settings_restore_title),
            body = stringResource(R.string.settings_restore_body),
            confirmText = stringResource(R.string.settings_restore_confirm),
            dismissText = stringResource(R.string.dialog_cancel),
            onConfirm = {
                confirmRestore = false
                vm.restoreDefaultSettings()
            },
            onDismiss = { confirmRestore = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = WgColors.InkMuted,
        modifier = Modifier.padding(top = WgSpacing.Sm).semantics { heading() },
    )
}

/** Ligne à interrupteur : toute la ligne est la zone tactile (≥ 64 dp). */
@Composable
private fun ToggleRow(title: String, subtitle: String?, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val state = stringResource(if (checked) R.string.settings_state_on else R.string.settings_state_off)
    Pressable(
        onClick = { onChange(!checked) },
        modifier = Modifier
            .testTag(tag)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(WgRadius.Md))
            .background(WgColors.Surface)
            .padding(WgSpacing.Md),
        description = title,
        stateText = state,
        role = Role.Switch,
    ) {
        Row(Modifier.fillMaxWidth().align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(end = WgSpacing.Md)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = WgColors.Ink)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = WgColors.InkMuted)
            }
            WgSwitchVisual(checked)
        }
    }
}

@Composable
private fun NavRow(title: String, tag: String, onClick: () -> Unit) {
    Pressable(
        onClick = onClick,
        modifier = Modifier
            .testTag(tag)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .clip(RoundedCornerShape(WgRadius.Md))
            .background(WgColors.Surface)
            .padding(WgSpacing.Md),
        sound = Sound.MENU_CHANGE,
    ) {
        Row(Modifier.fillMaxWidth().align(Alignment.Center), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(WgSpacing.Md)) {
            Icon(painterResource(R.drawable.ic_info), null, Modifier.size(24.dp), tint = WgColors.InkMuted)
            Text(title, style = MaterialTheme.typography.bodyLarge, color = WgColors.Ink, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun LanguageChoice(current: Language, onSelect: (Language) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(WgSpacing.Sm)) {
        LanguageOption(stringResource(R.string.language_fr_short), current == Language.FR, "language_fr", Modifier.weight(1f)) { onSelect(Language.FR) }
        LanguageOption(stringResource(R.string.language_en_short), current == Language.EN, "language_en", Modifier.weight(1f)) { onSelect(Language.EN) }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(WgRadius.Md)
    Pressable(
        onClick = onClick,
        modifier = modifier
            .testTag(tag)
            .height(WgSize.Button)
            .clip(shape)
            .background(if (selected) WgColors.Coral else WgColors.Surface)
            .border(2.dp, if (selected) WgColors.Ink else WgColors.Outline, shape),
        description = label,
        stateText = stringResource(if (selected) R.string.settings_state_on else R.string.settings_state_off),
        role = Role.RadioButton,
        sound = Sound.MENU_CHANGE,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = WgColors.Ink, modifier = Modifier.align(Alignment.Center))
    }
}

/** Crédits et mentions légales (texte local, aucun lien externe). */
@Composable
fun CreditsScreen(vm: AppViewModel) {
    FillBox(Modifier.testTag("credits_screen")) {
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            ScreenTopBar(stringResource(R.string.credits_title), vm::onBack)
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = WgSpacing.Lg, vertical = WgSpacing.Sm),
                verticalArrangement = Arrangement.spacedBy(WgSpacing.Md),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, color = WgColors.Ink)
                Text(stringResource(R.string.credits_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelLarge, color = WgColors.InkMuted)
                CreditsSection(R.string.credits_section_game, R.string.credits_game_body)
                CreditsSection(R.string.credits_section_audio, R.string.credits_audio_body)
                CreditsSection(R.string.credits_section_libs, R.string.credits_libs_body)
                CreditsSection(R.string.credits_section_ads, R.string.credits_ads_body)
                CreditsSection(R.string.credits_section_privacy, R.string.credits_privacy_body)
                Spacer(Modifier.height(WgSpacing.Lg))
            }
        }
    }
}

@Composable
private fun CreditsSection(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(WgSpacing.Xs)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = WgColors.Ink, modifier = Modifier.semantics { heading() })
        Text(stringResource(body), style = MaterialTheme.typography.bodyLarge, color = WgColors.InkMuted)
    }
}
