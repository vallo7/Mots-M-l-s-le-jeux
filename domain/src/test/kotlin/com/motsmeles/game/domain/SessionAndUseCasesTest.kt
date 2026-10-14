package com.motsmeles.game.domain

import com.motsmeles.game.domain.engine.GameSession
import com.motsmeles.game.domain.engine.GridGeometry
import com.motsmeles.game.domain.engine.GameTimer
import com.motsmeles.game.domain.engine.MonotonicClock
import com.motsmeles.game.domain.engine.SessionEvent
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.model.Language
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.ports.AdProvider
import com.motsmeles.game.domain.ports.ProgressRepository
import com.motsmeles.game.domain.ports.RewardedAdResult
import com.motsmeles.game.domain.ports.UnavailableReason
import com.motsmeles.game.domain.progress.LevelStatus
import com.motsmeles.game.domain.progress.ProgressRules
import com.motsmeles.game.domain.progress.ProgressState
import com.motsmeles.game.domain.progress.RandomSource
import com.motsmeles.game.domain.settings.AppSettings
import com.motsmeles.game.domain.ui.DifficultyBand
import com.motsmeles.game.domain.ui.HomeModelBuilder
import com.motsmeles.game.domain.ui.MonumentBuilder
import com.motsmeles.game.domain.ui.TimeFormat
import com.motsmeles.game.domain.usecases.GameUseCases
import com.motsmeles.game.domain.usecases.HintUseResult
import com.motsmeles.game.domain.usecases.RewardOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private fun <T> runNow(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

private class FakeClock(var now: Long = 0) : MonotonicClock { override fun nowMillis() = now }

private class MemoryProgress(var state: ProgressState = ProgressState(), var failWrites: Boolean = false) : ProgressRepository {
    override suspend fun read() = state
    override suspend fun update(transform: (ProgressState) -> ProgressState): ProgressState {
        if (failWrites) error("disque plein")
        state = transform(state)
        return state
    }
}

private class FakeAds(
    var reason: UnavailableReason? = null,
    var rewarded: RewardedAdResult = RewardedAdResult.Rewarded("ad-1"),
    var interstitialShown: Int = 0,
) : AdProvider {
    override suspend fun rewardedUnavailableReason() = reason
    override suspend fun showRewarded() = rewarded
    override suspend fun showInterstitialIfReady(): Boolean { interstitialShown++; return true }
}

class SessionAndUseCasesTest {
    private val catalog = Fixtures.stubCatalog()
    private val level = catalog.levels.first()
    private val low = RandomSource { from, _ -> from }
    private val high = RandomSource { _, until -> until - 1 }

    private fun session(clock: FakeClock = FakeClock()) = GameSession(level, Language.EN, clock)

    // ---- GameSession
    @Test fun `timer starts at first interaction not at creation`() {
        val clock = FakeClock()
        val s = session(clock)
        clock.now += 5_000
        assertEquals(0, s.elapsedMillis())
        s.submit(Fixtures.SUN.take(2))
        clock.now += 2_000
        assertEquals(2_000, s.elapsedMillis())
    }

    @Test fun `solving all words marks the session solved and wrong tries are counted`() {
        val s = session()
        assertTrue(s.submit(Fixtures.SUN.take(2)) is SessionEvent.Wrong)
        assertEquals(SessionEvent.Found(0, false), s.submit(Fixtures.SUN))
        assertEquals(SessionEvent.Found(1, true), s.submit(Fixtures.SKY))
        assertTrue(s.isSolved)
        assertEquals(1, s.state.wrongSelections)
        assertTrue(s.submit(Fixtures.SUN) is SessionEvent.Ignored)
    }

    @Test fun `snapshot and restore keep words hints and elapsed time`() {
        val clock = FakeClock()
        val a = session(clock)
        a.submit(Fixtures.SUN)
        clock.now += 4_000
        a.pause()
        val snap = a.snapshot()
        val b = session(clock)
        b.restore(snap)
        assertEquals(setOf(0), b.state.foundWordIndices)
        assertEquals(4_000, b.elapsedMillis())
        assertFalse(b.isRunning)
        b.resume()
        clock.now += 1_000
        assertEquals(5_000, b.elapsedMillis())
    }

    @Test fun `restore ignores invalid indices`() {
        val s = session()
        s.restore(com.motsmeles.game.domain.progress.GameSnapshot(setOf(0, 99), listOf(1, 77), -3, 10))
        assertEquals(setOf(0), s.state.foundWordIndices)
        assertEquals(1, s.state.hintsUsed)
        assertEquals(0, s.state.wrongSelections)
    }

    @Test fun `restart resets grid and timer`() {
        val clock = FakeClock()
        val s = session(clock)
        s.submit(Fixtures.SUN)
        clock.now += 3_000
        s.restart()
        assertTrue(s.state.foundWordIndices.isEmpty())
        assertFalse(s.hasStarted)
        assertEquals(0, s.elapsedMillis())
    }

    // ---- GridGeometry
    @Test fun `cellAt clamps to the board`() {
        assertEquals(Cell(1, 2), GridGeometry.cellAt(250f, 130f, 100f, 3))
        assertEquals(Cell(0, 0), GridGeometry.cellAt(-40f, -5f, 100f, 3))
        assertEquals(Cell(2, 2), GridGeometry.cellAt(900f, 900f, 100f, 3))
    }

    @Test fun `snapPath aligns on horizontal vertical and diagonal`() {
        assertEquals(listOf(Cell(0, 0)), GridGeometry.snapPath(Cell(0, 0), Cell(0, 0), 5))
        assertEquals((0..3).map { Cell(2, it) }, GridGeometry.snapPath(Cell(2, 0), Cell(2, 3), 5))
        assertEquals((0..3).map { Cell(it, 4) }, GridGeometry.snapPath(Cell(0, 4), Cell(3, 4), 5))
        assertEquals((0..2).map { Cell(it, it) }, GridGeometry.snapPath(Cell(0, 0), Cell(2, 2), 5))
        assertEquals(listOf(Cell(3, 3), Cell(2, 2), Cell(1, 1)), GridGeometry.snapPath(Cell(3, 3), Cell(1, 1), 5))
        // presque horizontal -> horizontal
        assertEquals((0..3).map { Cell(0, it) }, GridGeometry.snapPath(Cell(0, 0), Cell(1, 3), 5))
    }

    @Test fun `snapPath never leaves the grid`() {
        val path = GridGeometry.snapPath(Cell(3, 3), Cell(4, 4), 5)
        assertTrue(path.all { it.row in 0..4 && it.col in 0..4 })
        val corner = GridGeometry.snapPath(Cell(4, 4), Cell(4, 4), 5)
        assertEquals(1, corner.size)
    }

    @Test fun `time and band formatting`() {
        assertEquals("0:00", TimeFormat.mmss(0))
        assertEquals("1:23", TimeFormat.mmss(83_999))
        assertEquals("10:00", TimeFormat.mmss(600_000))
        assertEquals("0:00", TimeFormat.mmss(-5))
        assertEquals(DifficultyBand.EASY, DifficultyBand.of(3))
        assertEquals(DifficultyBand.MEDIUM, DifficultyBand.of(4))
        assertEquals(DifficultyBand.HARD, DifficultyBand.of(9))
        assertEquals(DifficultyBand.FINAL, DifficultyBand.of(10))
    }

    @Test fun `settings follow the system language until chosen`() {
        assertEquals(Language.FR, AppSettings().resolveLanguage("fr"))
        assertEquals(Language.EN, AppSettings().resolveLanguage("de"))
        assertEquals(Language.EN, AppSettings(language = Language.EN).resolveLanguage("fr"))
    }

    // ---- Home / Monument
    @Test fun `monument shows statuses stars and gift state`() {
        val done = ProgressRules.completeLevel(ProgressState(), "c1-l01", 2, 200_000, null)
        val chapters = MonumentBuilder.build(catalog, done)
        val first = chapters[0].levels
        assertEquals(LevelStatus.COMPLETED, first[0].status)
        assertEquals(2, first[0].stars)
        assertEquals(LevelStatus.AVAILABLE, first[1].status)
        assertEquals(LevelStatus.LOCKED, first[2].status)
        assertTrue(first.last().isFinal)
        assertEquals(1, chapters[0].completedCount)
        assertFalse(chapters[0].giftClaimable)
    }

    @Test fun `home highlights the claimable gift before the current chapter`() {
        val all1 = (1..10).fold(ProgressState(hintBalance = 2)) { s, n -> ProgressRules.completeLevel(s, "c1-l%02d".format(n), 3, 1, null) }
        val home = HomeModelBuilder.build(catalog, all1, Language.EN)
        assertEquals("c2-l01", home.currentLevel!!.id)
        assertTrue(home.giftClaimable)
        assertEquals(1, home.giftChapter!!.index)
        assertEquals(2, home.hintBalance)
        val everything = (1..10).fold(all1) { s, n -> ProgressRules.completeLevel(s, "c2-l%02d".format(n), 3, 1, null) }
        assertTrue(HomeModelBuilder.build(catalog, everything, Language.EN).allCompleted)
    }

    // ---- GameUseCases
    @Test fun `startLevel offers an interstitial on the second distinct level only`() {
        val repo = MemoryProgress(); val ads = FakeAds()
        val uc = GameUseCases(catalog, repo, ads, low)
        assertFalse(runNow { uc.startLevel(catalog.levels[0]) })
        assertFalse(runNow { uc.startLevel(catalog.levels[0]) })
        assertTrue(runNow { uc.startLevel(catalog.levels[1]) })
        assertEquals(1, ads.interstitialShown)
    }

    @Test fun `completeLevel gives stars from time persists and is idempotent`() {
        val clock = FakeClock(); val repo = MemoryProgress()
        val uc = GameUseCases(catalog, repo, FakeAds(), low)
        val s = session(clock)
        s.submit(Fixtures.SUN); clock.now += 100_000; s.submit(Fixtures.SKY)
        val r = runNow { uc.completeLevel(s) }
        assertEquals(3, r.stars)
        assertEquals("c1-l02", r.nextLevelId)
        assertTrue(repo.state.completed.containsKey("c1-l01"))
        val again = runNow { uc.completeLevel(s) }
        assertEquals(r.stars, again.stars)
        assertEquals(1, repo.state.completed.size)
    }

    @Test fun `slow victory gets one star and clears the saved game`() {
        val clock = FakeClock(); val repo = MemoryProgress()
        val uc = GameUseCases(catalog, repo, FakeAds(), low)
        val s = session(clock)
        s.submit(Fixtures.SUN); runNow { uc.saveSnapshot(s) }
        assertEquals(1, repo.state.inProgress.size)
        clock.now += 600_000; s.submit(Fixtures.SKY)
        assertEquals(1, runNow { uc.completeLevel(s) }.stars)
        assertTrue(repo.state.inProgress.isEmpty())
    }

    @Test fun `completeLevel refuses an unsolved session`() {
        val uc = GameUseCases(catalog, MemoryProgress(), FakeAds(), low)
        assertTrue(runCatching { runNow { uc.completeLevel(session()) } }.isFailure)
    }

    @Test fun `hint costs one only when revealed`() {
        val repo = MemoryProgress(ProgressState(hintBalance = 2))
        val uc = GameUseCases(catalog, repo, FakeAds(), low)
        val s = session()
        val r = runNow { uc.useHint(s) } as HintUseResult.Used
        assertEquals(1, r.newBalance)
        assertEquals(1, repo.state.hintBalance)
        assertEquals(1, s.state.hintsUsed)
    }

    @Test fun `no balance means no hint no deduction no reveal`() {
        val repo = MemoryProgress(ProgressState(hintBalance = 0))
        val s = session()
        assertEquals(HintUseResult.NoBalance, runNow { GameUseCases(catalog, repo, FakeAds(), low).useHint(s) })
        assertEquals(0, s.state.hintsUsed)
    }

    @Test fun `failed save neither deducts nor reveals`() {
        val repo = MemoryProgress(ProgressState(hintBalance = 2), failWrites = true)
        val s = session()
        assertEquals(HintUseResult.Failed, runNow { GameUseCases(catalog, repo, FakeAds(), low).useHint(s) })
        assertEquals(2, repo.state.hintBalance)
        assertEquals(0, s.state.hintsUsed)
    }

    @Test fun `gift is granted once`() {
        val done = (1..10).fold(ProgressState()) { st, n -> ProgressRules.completeLevel(st, "c1-l%02d".format(n), 3, 1, null) }
        val repo = MemoryProgress(done)
        val uc = GameUseCases(catalog, repo, FakeAds(), high)
        assertEquals(3, runNow { uc.claimGift(1) })
        assertNull(runNow { uc.claimGift(1) })
        assertEquals(3, repo.state.hintBalance)
        assertNull(runNow { uc.claimGift(2) })
    }

    @Test fun `rewarded ad grants hints after confirmation and never twice`() {
        val repo = MemoryProgress(); val ads = FakeAds(rewarded = RewardedAdResult.Rewarded("show-1"))
        val uc = GameUseCases(catalog, repo, ads, high)
        assertEquals(RewardOutcome.Granted(2), runNow { uc.watchRewardedAd() })
        assertEquals(RewardOutcome.Duplicate, runNow { uc.watchRewardedAd() })
        assertEquals(2, repo.state.hintBalance)
    }

    @Test fun `unavailable dismissed or failed ads change nothing`() {
        val repo = MemoryProgress(ProgressState(hintBalance = 0))
        val ads = FakeAds(reason = UnavailableReason.OFFLINE)
        val uc = GameUseCases(catalog, repo, ads, high)
        assertEquals(RewardOutcome.Unavailable(UnavailableReason.OFFLINE), runNow { uc.watchRewardedAd() })
        ads.reason = null; ads.rewarded = RewardedAdResult.Dismissed
        assertEquals(RewardOutcome.Dismissed, runNow { uc.watchRewardedAd() })
        ads.rewarded = RewardedAdResult.Unavailable(UnavailableReason.NO_FILL)
        assertEquals(RewardOutcome.Unavailable(UnavailableReason.NO_FILL), runNow { uc.watchRewardedAd() })
        assertEquals(0, repo.state.hintBalance)
    }

    @Test fun `timer restore is paused until resume`() {
        val clock = FakeClock(); val t = GameTimer(clock)
        t.restoreElapsed(7_000)
        clock.now += 50_000
        assertEquals(7_000, t.elapsedMillis())
        assertNotNull(t); t.resume(); clock.now += 1_000
        assertEquals(8_000, t.elapsedMillis())
    }
}
