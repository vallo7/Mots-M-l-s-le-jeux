package com.motsmeles.game.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.presentation.feedback.FeedbackController

val LocalReduceMotion = compositionLocalOf { false }
val LocalLanguage = compositionLocalOf { Language.EN }
val LocalFeedback = staticCompositionLocalOf<FeedbackController> { error("FeedbackController non fourni") }
