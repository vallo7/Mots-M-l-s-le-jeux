package com.motsmeles.game.presentation.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.motsmeles.game.BuildConfig
import com.motsmeles.game.R
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.ports.Sound
import com.motsmeles.game.presentation.AppState
import com.motsmeles.game.presentation.AppViewModel
import com.motsmeles.game.presentation.components.ButtonKind
import com.motsmeles.game.presentation.components.ConfirmPopup
import com.motsmeles.game.presentation.components.Pressable
import com.motsmeles.game.presentation.components.PopupCard
import com.motsmeles.game.presentation.components.PopupHost
import com.motsmeles.game.presentation.components.Ribbons
import com.motsmeles.game.presentation.components.WgButton
import com.motsmeles.game.presentation.components.WgSwitchVisual
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgRadius
import com.motsmeles.game.presentation.theme.WgSpacing

/**
 * Paramètres : popup de la maquette « pause » (ruban en titre, bouton fermer rose, lignes avec interrupteur jaune).
 * Tous les réglages s'appliquent immédiatement et sont conservés. La langue se change ici (plus sur l'accueil).
 */
@Composable
fun SettingsPopup(visible: Boolean, state: AppState, vm: AppViewModel) {
    val settings = state.settings
    var confirmRestore by remember { mutableStateOf(false) }
    PopupHost(visible = visible && !confirmRestore, onDismiss = vm::onBack) {
        PopupCard(
            title = stringResource(R.string.settings_title),
            ribbon = Ribbons.Yellow,
            modifier = Modifier.testTag("settings_popup"),
            onClose = vm::onBack,
            closeDescription = stringResource(R.string.cd_close),
        ) {
            Column(
                Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                LanguageRow(state.language) { language -> vm.updateSettings { it.copy(language = language) } }
                ToggleRow(R.drawable.ic_sound, stringResource(R.string.settings_sound), null, settings.soundEnabled, "toggle_sound") { value ->
                    vm.updateSettings { it.copy(soundEnabled = value) }
                }
                ToggleRow(R.drawable.ic_music, stringResource(R.string.settings_music), null, settings.musicEnabled, "toggle_music") { value ->
                    vm.updateSettings { it.copy(musicEnabled = value) }
                }
                ToggleRow(R.drawable.ic_vibrate, stringResource(R.string.settings_haptics), null, settings.hapticsEnabled, "toggle_haptics") { value ->
                    vm.updateSettings { it.copy(hapticsEnabled = value) }
                }
                SectionTitle(stringResource(R.string.settings_accessibility))
                ToggleRow(
                    R.drawable.ic_motion,
                    stringResource(R.string.settings_reduce_motion),
                    stringResource(R.string.settings_reduce_motion_hint),
                    settings.reduceMotion,
                    "toggle_reduce_motion",
                ) { value -> vm.updateSettings { it.copy(reduceMotion = value) } }
                ToggleRow(R.drawable.ic_text, stringResource(R.string.settings_large_text), null, settings.largeText, "toggle_large_text") { value ->
                    vm.updateSettings { it.copy(largeText = value) }
                }
                Spacer(Modifier.height(WgSpacing.Sm))
                WgButton(
                    text = stringResource(R.string.settings_credits),
                    onClick = vm::openCredits,
                    modifier = Modifier.fillMaxWidth().testTag("row_credits"),
                    kind = ButtonKind.Ghost,
                    icon = R.drawable.ic_info,
                    sound = Sound.MENU_CHANGE,
                )
                if (vm.privacyOptionsRequired) {
                    WgButton(
                        text = stringResource(R.string.settings_privacy_options),
                        onClick = vm::openPrivacyOptions,
                        modifier = Modifier.fillMaxWidth().testTag("row_privacy"),
                        kind = ButtonKind.Ghost,
                    )
                }
                WgButton(
                    text = stringResource(R.string.settings_restore),
                    onClick = { confirmRestore = true },
                    modifier = Modifier.fillMaxWidth().testTag("settings_restore"),
                    kind = ButtonKind.Secondary,
                )
            }
        }
    }
    ConfirmPopup(
        visible = visible && confirmRestore,
        title = stringResource(R.string.settings_restore_title),
        body = stringResource(R.string.settings_restore_body),
        confirmText = stringResource(R.string.settings_restore_confirm),
        dismissText = stringResource(R.string.dialog_cancel),
        onConfirm = {
            confirmRestore = false
            vm.restoreDefaultSettings()
        },
        onDismiss = { confirmRestore = false },
        ribbon = Ribbons.Purple,
        tag = "restore_popup",
    )
}

@Composable
fun CreditsPopup(visible: Boolean, vm: AppViewModel) {
    PopupHost(visible = visible, onDismiss = vm::onBack) {
        PopupCard(
            title = stringResource(R.string.credits_title),
            ribbon = Ribbons.Blue,
            modifier = Modifier.testTag("credits_popup"),
            onClose = vm::onBack,
            closeDescription = stringResource(R.string.cd_close),
        ) {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(WgSpacing.Md),
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = WgColors.Ink)
                Text(stringResource(R.string.credits_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelLarge, color = WgColors.InkMuted)
                CreditsSection(R.string.credits_section_game, R.string.credits_game_body)
                CreditsSection(R.string.credits_section_audio, R.string.credits_audio_body)
                CreditsSection(R.string.credits_section_libs, R.string.credits_libs_body)
                CreditsSection(R.string.credits_section_ads, R.string.credits_ads_body)
                CreditsSection(R.string.credits_section_privacy, R.string.credits_privacy_body)
            }
        }
    }
}

@Composable
private fun CreditsSection(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(WgSpacing.Xs)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = WgColors.Ink, modifier = Modifier.semantics { heading() })
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = WgColors.InkMuted)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
        color = WgColors.InkMuted,
    )
}

/** Ligne de réglage : icône jaune, libellé, interrupteur. Toute la ligne est cliquable (rôle « interrupteur »). */
@Composable
private fun ToggleRow(@DrawableRes icon: Int, title: String, subtitle: String?, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    val stateText = stringResource(if (checked) R.string.settings_state_on else R.string.settings_state_off)
    Pressable(
        onClick = { onChange(!checked) },
        modifier = Modifier.fillMaxWidth().testTag(tag),
        role = Role.Switch,
        description = title,
        stateText = stateText,
        sound = Sound.CLICK,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(icon), null, Modifier.size(26.dp), tint = WgColors.YellowDark)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = WgColors.Ink)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = WgColors.InkMuted)
            }
            WgSwitchVisual(checked)
        }
    }
}

@Composable
private fun LanguageRow(current: Language, onSelect: (Language) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.ic_globe), null, Modifier.size(26.dp), tint = WgColors.YellowDark)
        Text(
            stringResource(R.string.settings_language),
            Modifier.weight(1f).padding(horizontal = 12.dp),
            style = MaterialTheme.typography.titleMedium,
            color = WgColors.Ink,
        )
        LanguageOption("FR", current == Language.FR, "lang_fr") { onSelect(Language.FR) }
        Spacer(Modifier.size(6.dp))
        LanguageOption("EN", current == Language.EN, "lang_en") { onSelect(Language.EN) }
    }
}

@Composable
private fun LanguageOption(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    val stateText = stringResource(if (selected) R.string.settings_state_on else R.string.settings_state_off)
    Pressable(
        onClick = onClick,
        modifier = Modifier.testTag(tag),
        role = Role.RadioButton,
        description = label,
        stateText = stateText,
        sound = Sound.MENU_CHANGE,
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(WgRadius.Pill))
                .background(if (selected) WgColors.Purple else WgColors.Track)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) WgColors.White else WgColors.Ink)
        }
    }
}
