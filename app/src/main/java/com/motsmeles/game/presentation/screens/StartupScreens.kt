package com.motsmeles.game.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.motsmeles.game.R
import com.motsmeles.game.presentation.components.FillBox
import com.motsmeles.game.presentation.theme.WgColors
import com.motsmeles.game.presentation.theme.WgSpacing

/** Affiché si les données de niveaux sont absentes ou invalides (jamais en usage normal : elles sont validées au build). */
@Composable
fun StartupErrorScreen() {
    FillBox(Modifier.testTag("startup_error")) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(WgSpacing.Xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(stringResource(R.string.loading_error_title), style = MaterialTheme.typography.headlineLarge, color = WgColors.Error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(WgSpacing.Md))
            Text(stringResource(R.string.loading_error_body), style = MaterialTheme.typography.bodyLarge, color = WgColors.Ink, textAlign = TextAlign.Center)
        }
    }
}
