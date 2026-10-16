package com.motsmeles.game.domain

import com.motsmeles.game.domain.engine.Praise
import com.motsmeles.game.domain.progress.AdMoment
import com.motsmeles.game.domain.progress.ReturnAdPolicy
import com.motsmeles.game.domain.progress.LevelStatus
import com.motsmeles.game.domain.progress.ProgressRules
import com.motsmeles.game.domain.progress.ProgressState
import com.motsmeles.game.domain.progress.RandomSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRulesTest {
    private val catalog = Fixtures.stubCatalog()
    private val low = RandomSource { from, _ -> from }
    private val high = RandomSource { _, until -> until - 1 }

    private fun completeChapter(state: ProgressState, chapter: Int, stars: Int = 3): ProgressState =
        (1..10).fold(state) { s, n ->
            ProgressRules.completeLevel(s, "c$chapter-l%02d".format(n), stars, 60_000, Praise.NICE)
        }

    // --- indices initiaux
    @Test fun `first launch grants exactly two hints once`() {
        val once = ProgressRules.initialize(ProgressState())
        assertEquals(2, once.hintBalance)
        assertTrue(once.initialHintsGranted)
        assertEquals(once, ProgressRules.initialize(once))
        assertEquals(2, ProgressRules.initialize(ProgressRules.initialize(once)).hintBalance)
    }

    @Test fun `hint spend never goes below zero`() {
        assertNull(ProgressRules.spendHint(ProgressState(hintBalance = 0)))
        assertEquals(1, ProgressRules.spendHint(ProgressState(hintBalance = 2))!!.hintBalance)
    }

    // --- progression et verrouillage
    @Test fun `only the first uncompleted level is available`() {
        val s = ProgressState()
        assertEquals(LevelStatus.AVAILABLE, ProgressRules.statusOf(catalog, s, "c1-l01"))
        assertEquals(LevelStatus.LOCKED, ProgressRules.statusOf(catalog, s, "c1-l02"))
        val next = ProgressRules.completeLevel(s, "c1-l01", 3, 1000, Praise.NICE)
        assertEquals(LevelStatus.COMPLETED, ProgressRules.statusOf(catalog, next, "c1-l01"))
        assertEquals(LevelStatus.AVAILABLE, ProgressRules.statusOf(catalog, next, "c1-l02"))
        assertEquals("c1-l02", ProgressRules.currentLevel(catalog, next)!!.id)
    }

    @Test fun `completing a level twice changes nothing`() {
        val once = ProgressRules.completeLevel(ProgressState(), "c1-l01", 3, 1000, Praise.GREAT)
        val twice = ProgressRules.completeLevel(once, "c1-l01", 1, 999_000, Praise.LEGENDARY)
        assertEquals(once, twice)
        assertEquals(3, twice.completed.getValue("c1-l01").stars)
    }

    @Test fun `current level moves to next chapter and ends with null`() {
        val ch1 = completeChapter(ProgressState(), 1)
        assertEquals("c2-l01", ProgressRules.currentLevel(catalog, ch1)!!.id)
        assertNull(ProgressRules.currentLevel(catalog, completeChapter(ch1, 2)))
    }

    @Test fun `perfect streak counts consecutive three star levels`() {
        var s = ProgressRules.completeLevel(ProgressState(), "c1-l01", 3, 1, null)
        s = ProgressRules.completeLevel(s, "c1-l02", 3, 1, null)
        assertEquals(2, s.perfectStreak)
        s = ProgressRules.completeLevel(s, "c1-l03", 2, 1, null)
        assertEquals(0, s.perfectStreak)
    }

    // --- cadeau de chapitre
    @Test fun `gift is not claimable before the chapter is complete`() {
        val partial = ProgressRules.completeLevel(ProgressState(), "c1-l01", 3, 1, null)
        assertFalse(ProgressRules.isGiftClaimable(catalog, partial, 1))
        assertNull(ProgressRules.claimGift(catalog, partial, 1, low))
        assertEquals(1, ProgressRules.chapterCompletedCount(catalog, partial, 1))
    }

    @Test fun `gift gives one to three hints and only once per chapter`() {
        val done = completeChapter(ProgressState(), 1)
        assertTrue(ProgressRules.isGiftClaimable(catalog, done, 1))
        val claim = ProgressRules.claimGift(catalog, done, 1, high)!!
        assertEquals(3, claim.amount)
        assertEquals(3, claim.state.hintBalance)
        assertNull(ProgressRules.claimGift(catalog, claim.state, 1, high))
        assertEquals(1, ProgressRules.claimGift(catalog, done, 1, low)!!.amount)
        assertFalse(ProgressRules.isGiftClaimable(catalog, done, 2))
    }

    // --- publicité récompensée
    @Test fun `rewarded ad grants exactly one hint whatever the random source`() {
        assertEquals(1, ProgressRules.grantRewardedHints(ProgressState(), "ad-1", low)!!.amount)
        assertEquals(1, ProgressRules.grantRewardedHints(ProgressState(), "ad-1", high)!!.amount)
        assertEquals(1, ProgressRules.grantRewardedHints(ProgressState(hintBalance = 4), "ad-9", high)!!.state.hintBalance - 4)
    }

    @Test fun `same reward id is never granted twice`() {
        val first = ProgressRules.grantRewardedHints(ProgressState(), "ad-1", high)!!
        assertNull(ProgressRules.grantRewardedHints(first.state, "ad-1", high))
        assertNotNull(ProgressRules.grantRewardedHints(first.state, "ad-2", high))
    }

    @Test fun `blank reward id is refused and random is not drawn`() {
        var draws = 0
        val counting = RandomSource { f, _ -> draws++; f }
        assertNull(ProgressRules.grantRewardedHints(ProgressState(), " ", counting))
        assertNull(ProgressRules.grantRewardedHints(ProgressRules.grantRewardedHints(ProgressState(), "x", low)!!.state, "x", counting))
        assertEquals(0, draws)
    }

    @Test fun `remembered reward ids are bounded`() {
        val s = (1..100).fold(ProgressState()) { st, i -> ProgressRules.grantRewardedHints(st, "ad-$i", low)!!.state }
        assertEquals(ProgressRules.REMEMBERED_REWARD_IDS, s.grantedRewardIds.size)
        assertEquals("ad-100", s.grantedRewardIds.last())
    }

    // --- publicité de retour dans l'application
    private val moment = AdMoment()

    @Test fun `return ad is shown after a long enough absence`() {
        assertTrue(ReturnAdPolicy.shouldShow(30_000, null, cameBackFromAd = false, sessionStarted = true, moment = moment))
    }

    @Test fun `return ad is never shown for a very short absence`() {
        assertFalse(ReturnAdPolicy.shouldShow(ReturnAdPolicy.MIN_AWAY_MS - 1, null, false, true, moment))
        assertTrue(ReturnAdPolicy.shouldShow(ReturnAdPolicy.MIN_AWAY_MS, null, false, true, moment))
    }

    @Test fun `return ad respects the cooldown`() {
        assertFalse(ReturnAdPolicy.shouldShow(60_000, ReturnAdPolicy.COOLDOWN_MS - 1, false, true, moment))
        assertTrue(ReturnAdPolicy.shouldShow(60_000, ReturnAdPolicy.COOLDOWN_MS, false, true, moment))
    }

    @Test fun `return ad is never shown after a cold start or when coming back from another ad`() {
        assertFalse(ReturnAdPolicy.shouldShow(60_000, null, cameBackFromAd = false, sessionStarted = false, moment = moment))
        assertFalse(ReturnAdPolicy.shouldShow(60_000, null, cameBackFromAd = true, sessionStarted = true, moment = moment))
    }

    @Test fun `return ad is blocked at sensitive moments`() {
        assertTrue(ReturnAdPolicy.canShowNow(AdMoment()))
        assertFalse(ReturnAdPolicy.canShowNow(AdMoment(isFirstRender = true)))
        assertFalse(ReturnAdPolicy.canShowNow(AdMoment(isGridBeingTouched = true)))
        assertFalse(ReturnAdPolicy.canShowNow(AdMoment(isResolvingWin = true)))
        assertFalse(ReturnAdPolicy.canShowNow(AdMoment(isSensitiveActionPending = true)))
        assertFalse(ReturnAdPolicy.shouldShow(60_000, null, false, true, AdMoment(isResolvingWin = true)))
    }
}
