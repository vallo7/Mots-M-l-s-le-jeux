package com.motsmeles.game.presentation.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val WgTypography = Typography().let { base ->
    base.copy(
        headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, lineHeight = 38.sp),
        titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
        bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    )
}

@Composable
fun WordGameTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = WgColors.Coral,
        onPrimary = WgColors.Ink,
        secondary = WgColors.Teal,
        onSecondary = WgColors.Ink,
        tertiary = WgColors.Plum,
        background = WgColors.Cream,
        onBackground = WgColors.Ink,
        surface = WgColors.Surface,
        onSurface = WgColors.Ink,
        onSurfaceVariant = WgColors.InkMuted,
        outline = WgColors.Outline,
        error = WgColors.Error,
    )
    MaterialTheme(colorScheme = scheme, typography = WgTypography, content = content)
}

/** Vrai si l'utilisateur a supprimé les animations dans les réglages système (échelle = 0). */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
