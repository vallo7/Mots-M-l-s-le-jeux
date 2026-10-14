package com.motsmeles.game

import com.motsmeles.game.domain.content.ValidationIssue
import com.motsmeles.game.domain.engine.MonotonicClock
import com.motsmeles.game.domain.model.LevelCatalog
import com.motsmeles.game.domain.ports.LevelRepository
import com.motsmeles.game.presentation.startup.StartupException
import com.motsmeles.game.presentation.startup.StartupSequence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

class StartupSequenceTest {
    private class Clock(var now: Long = 0) : MonotonicClock {
        override fun nowMillis() = now
    }

    private val empty = LevelCatalog(emptyList())
    private val okRepo = object : LevelRepository { override suspend fun loadCatalog() = empty }

    private fun <T> runNow(block: suspend () -> T): T {
        var result: Result<T>? = null
        block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
        return result!!.getOrThrow()
    }

    private fun sequence(
        repo: LevelRepository = okRepo,
        clock: Clock = Clock(),
        validate: suspend (LevelCatalog) -> List<ValidationIssue> = { emptyList() },
        profile: suspend () -> Unit = { },
        audio: suspend () -> Unit = { },
        wait: suspend (Long) -> Unit = { clock.now += it },
    ) = StartupSequence(repo, clock, validate, profile, audio, wait)

    @Test fun `runs every real step in order and reports progress`() {
        val order = mutableListOf<String>()
        val steps = mutableListOf<Pair<Float, StartupSequence.Step>>()
        val seq = sequence(profile = { order += "profile" }, audio = { order += "audio" })
        runNow { seq.run { p, s -> steps += p to s } }
        assertEquals(listOf("profile", "audio"), order)
        assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f, 1f), steps.map { it.first })
        assertEquals(StartupSequence.Step.READY, steps.last().second)
    }

    @Test fun `waits for the minimum duration exactly once after the last step`() {
        val waits = mutableListOf<Long>()
        val clock = Clock()
        runNow { sequence(clock = clock, wait = { waits += it; clock.now += it }).run { _, _ -> } }
        assertEquals(listOf(2_000L), waits)
    }

    @Test fun `only the remaining time is waited when loading was slow`() {
        val clock = Clock(); val waits = mutableListOf<Long>()
        val slowRepo = object : LevelRepository {
            override suspend fun loadCatalog(): LevelCatalog { clock.now += 1_700; return empty }
        }
        runNow { sequence(repo = slowRepo, clock = clock, wait = { waits += it }).run { _, _ -> } }
        assertEquals(listOf(300L), waits)
    }

    @Test fun `no wait when loading already took two seconds`() {
        val clock = Clock(); val waits = mutableListOf<Long>()
        val slowRepo = object : LevelRepository {
            override suspend fun loadCatalog(): LevelCatalog { clock.now += 2_500; return empty }
        }
        runNow { sequence(repo = slowRepo, clock = clock, wait = { waits += it }).run { _, _ -> } }
        assertTrue(waits.isEmpty())
    }

    @Test fun `invalid catalog stops startup with the issues`() {
        val bad = listOf(ValidationIssue("c1-l01", "GRID_MISMATCH", "x"))
        val e = runCatching { runNow { sequence(validate = { bad }).run { _, _ -> } } }.exceptionOrNull()
        assertTrue(e is StartupException)
        assertEquals(bad, (e as StartupException).issues)
    }

    @Test fun `repository or profile failure is reported as startup failure`() {
        val failing = object : LevelRepository { override suspend fun loadCatalog(): LevelCatalog = error("boom") }
        assertTrue(runCatching { runNow { sequence(repo = failing).run { _, _ -> } } }.exceptionOrNull() is StartupException)
        assertTrue(runCatching { runNow { sequence(profile = { error("disque") }).run { _, _ -> } } }.exceptionOrNull() is StartupException)
    }

    @Test fun `a failing audio preload never blocks the game`() {
        val catalog = runNow { sequence(audio = { error("codec") }).run { _, _ -> } }
        assertEquals(empty, catalog)
    }
}
