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

/** Typographie plus grasse : titres en Black, libellés en ExtraBold, texte courant en Bold. */
private val WgTypography = Typography().let { base ->
    base.copy(
        headlineLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 40.sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 34.sp),
        titleLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 24.sp),
        titleSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, lineHeight = 14.sp),
    )
}

@Composable
fun WordGameTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = WgColors.Purple,
        onPrimary = WgColors.White,
        secondary = WgColors.Teal,
        onSecondary = WgColors.White,
        tertiary = WgColors.Pink,
        background = WgColors.Background,
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
