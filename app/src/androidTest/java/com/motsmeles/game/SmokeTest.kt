package com.motsmeles.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Parcours de fumée sur émulateur (workflow « Instrumented tests », lancement manuel). */
@RunWith(AndroidJUnit4::class)
class SmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String, timeoutMillis: Long = 30_000) {
        rule.waitUntil(timeoutMillis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun coldStartLoadsThenShowsTheChapterCarouselWithRequiredButtons() {
        waitForTag("chapter_pager")
        rule.onNodeWithTag("home_settings").assertIsDisplayed()
        rule.onNodeWithTag("home_monument").assertIsDisplayed()
        rule.onNodeWithTag("chapter_card_1").assertIsDisplayed()
        // Plus de bouton de langue sur l'accueil : il est dans les paramètres.
        assert(rule.onAllNodesWithTag("home_language").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun tappingTheCurrentChapterOpensTheCurrentLevelWithWordsGridHintAndBannerArea() {
        waitForTag("chapter_card_1")
        rule.onNodeWithTag("chapter_card_1").performClick()
        waitForTag("game_screen")
        waitForTag("letter_grid")
        rule.onNodeWithTag("words_panel").assertIsDisplayed()
        rule.onNodeWithTag("hint_button").assertIsDisplayed()
        rule.onNodeWithTag("ad_banner_area").assertIsDisplayed()
        // Redémarrer ne demande aucune confirmation : la fenêtre n'existe plus.
        rule.onNodeWithTag("game_restart").performClick()
        assert(rule.onAllNodesWithTag("confirm_popup").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun monumentAndSettingsOpenFromHome() {
        waitForTag("home_monument")
        rule.onNodeWithTag("home_monument").performClick()
        waitForTag("monument_screen")
        rule.onNodeWithTag("level_c1-l01").assertIsDisplayed()
    }

    @Test
    fun settingsOpenAsAPopupWithTheLanguageChoice() {
        waitForTag("home_settings")
        rule.onNodeWithTag("home_settings").performClick()
        waitForTag("settings_popup")
        rule.onNodeWithTag("lang_fr").assertIsDisplayed()
        rule.onNodeWithTag("lang_en").assertIsDisplayed()
        rule.onNodeWithTag("toggle_music").assertIsDisplayed()
    }
}
