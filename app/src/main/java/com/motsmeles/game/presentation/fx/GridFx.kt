package com.motsmeles.game.presentation.fx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.motsmeles.game.domain.model.Cell
import com.motsmeles.game.domain.ui.Motion
import com.motsmeles.game.presentation.theme.WgColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Mot trouvé à afficher : cases + couleur de capsule. */
data class FoundHighlight(val cells: List<Cell>, val color: Color)

/**
 * Moteur d'effets de la grille. Tout est évalué à partir d'un temps écoulé (ressorts analytiques de [Motion]) :
 * aucune animation par case, un seul Canvas, une seule boucle d'images qui s'arrête quand plus rien ne bouge.
 *
 * Règle de style : AUCUN fondu. Les effets grandissent, rebondissent, tournent, glissent et RÉTRÉCISSENT jusqu'à
 * disparaître (particules, anneaux), ils ne changent jamais d'opacité dans le temps. Les coordonnées sont en
 * « unités de case » (colonne + 0,5 ; ligne + 0,5), indépendantes de la taille en pixels.
 */
class GridFx(val n: Int, var reduceMotion: Boolean) {
    /** Horloge d'images (ms). Lue pendant le dessin : sa modification invalide uniquement le Canvas. */
    var clockMs: Long by mutableLongStateOf(0L)
        private set

    /** Incrémenté à chaque nouvel effet : relance la boucle d'images si elle s'était arrêtée. */
    var wake: Int by mutableIntStateOf(0)
        private set

    // ---- effets (listes simples : modifiées uniquement sur le fil principal)
    private class Pop(val col: Int, val row: Int, val strength: Float) { var startMs = UNSET }
    private class WordWave(val cells: List<Cell>, val color: Color) { var startMs = UNSET }
    private class Ring(val x: Float, val y: Float, val color: Color, val maxR: Float, val width: Float) { var startMs = UNSET }
    private class Spark(
        val x: Float, val y: Float, val vx: Float, val vy: Float, val life: Long,
        val color: Color, val size: Float, val sparkle: Boolean, val spin: Float,
    ) { var startMs = UNSET }
    private class Leaving(val a: Offset, val b: Offset, val color: Color, val wrong: Boolean, val cells: List<Cell>) { var startMs = UNSET }
    private class Shuffle(val perm1: IntArray, val perm2: IntArray, val delays: LongArray, val spins: FloatArray, val arcs: FloatArray) {
        var startMs = UNSET
    }

    private val pops = ArrayList<Pop>()
    private val waves = ArrayList<WordWave>()
    private val rings = ArrayList<Ring>()
    private val sparks = ArrayList<Spark>()
    private val leaving = ArrayList<Leaving>()
    private var shuffle: Shuffle? = null
    private val foundStart = HashMap<String, Long>()
    private val knownFound = HashSet<String>()
    private var foundInitialised = false
    private var knownHints: Set<Cell> = emptySet()
    private var previousSelection: List<Cell> = emptyList()
    private var lastPath: List<Cell> = emptyList()
    private var pendingRelease: Long = NONE   // NONE : rien en attente ; UNSET : en attente d'horodatage
    private var pendingReleaseAt = 0L
    private var wrongSince = NONE
    private var wrongCells: List<Cell> = emptyList()
    private var random = Random(7)

    // ---- tracé en direct
    private var tipX = 0f
    private var tipY = 0f
    private var tracing = false
    private var lastTick = 0L

    /** Verrou de saisie pendant le mélange (horodatage `SystemClock.uptimeMillis`, posé par l'écran). */
    var inputLockedUntil: Long = 0L

    val isShuffling: Boolean get() = shuffle != null

    private fun bump() {
        wake++
    }

    // =========================================================================================== événements (fil principal)
    /** Compare l'ensemble des mots trouvés au précédent : un nouveau mot déclenche l'effet de victoire, un mot retiré se rétracte. */
    fun onFoundChanged(found: List<FoundHighlight>) {
        val current = found.associateBy { key(it.cells) }
        if (!foundInitialised) {
            foundInitialised = true
            knownFound.clear()
            knownFound.addAll(current.keys)
            return
        }
        for ((k, h) in current) {
            if (k in knownFound) continue
            knownFound.add(k)
            if (reduceMotion) continue
            foundStart[k] = UNSET
            pendingRelease = NONE
            spawnFound(h)
        }
        val gone = knownFound.filter { it !in current }
        for (k in gone) {
            knownFound.remove(k)
            foundStart.remove(k)
        }
        if (gone.isNotEmpty() && !reduceMotion) bump()
    }

    /** Mots retirés d'un coup (redémarrage) : on garde leurs capsules pour les rétracter. */
    fun onFoundCleared(previous: List<FoundHighlight>) {
        if (reduceMotion) return
        previous.forEach { h ->
            leaving.add(Leaving(center(h.cells.first()), center(h.cells.last()), h.color, wrong = false, cells = h.cells))
        }
        if (previous.isNotEmpty()) bump()
    }

    fun onSelectionChanged(selection: List<Cell>) {
        if (reduceMotion) {
            previousSelection = selection
            if (selection.isNotEmpty()) lastPath = selection
            tracing = selection.isNotEmpty()
            if (tracing) snapTip(selection.last())
            bump()
            return
        }
        if (selection.isNotEmpty()) {
            if (previousSelection.isEmpty()) {
                tipX = selection.first().col + 0.5f
                tipY = selection.first().row + 0.5f
                pendingRelease = NONE
            }
            // Chaque case nouvellement atteinte « pop » ; la première rebondit plus fort.
            val fresh = selection.filter { it !in previousSelection }
            fresh.forEachIndexed { i, c ->
                pops.add(Pop(c.col, c.row, if (previousSelection.isEmpty() && i == 0) 1.25f else 1f))
                spawnTrail(c)
            }
            lastPath = selection
            tracing = true
        } else if (previousSelection.isNotEmpty()) {
            tracing = false
            pendingRelease = UNSET // attend le verdict (trouvé / faux) quelques images avant de rétracter
        }
        previousSelection = selection
        bump()
    }

    /** Mauvais mot : la capsule rouge se rétracte en tremblant. */
    fun onWrong() {
        if (reduceMotion) return
        pendingRelease = NONE
        if (lastPath.size >= 2) {
            leaving.add(Leaving(center(lastPath.first()), center(lastPath.last()), WgColors.Red, wrong = true, cells = lastPath))
            wrongCells = lastPath
            wrongSince = UNSET
        }
        bump()
    }

    /** Indices déjà présents à l'ouverture (partie reprise) : pas d'effet de révélation. */
    fun primeHints(cells: List<Cell>) {
        knownHints = cells.toSet()
    }

    fun onHintCells(cells: List<Cell>) {
        val fresh = cells.filter { it !in knownHints }
        knownHints = cells.toSet()
        if (reduceMotion || fresh.isEmpty()) return
        fresh.forEach { c ->
            rings.add(Ring(c.col + 0.5f, c.row + 0.5f, WgColors.Yellow, maxR = 1.9f, width = 0.22f))
            repeat(6) { sparks.add(sparkle(c.col + 0.5f, c.row + 0.5f, WgColors.Yellow, speed = 3.5f)) }
        }
        bump()
    }

    /** Redémarrage : les lettres se mélangent (deux arcs de permutation) puis se reposent avec un rebond. */
    fun onRestart(token: Int) {
        if (reduceMotion) return
        val total = n * n
        val rnd = Random(token * 7919 + 13)
        val maxDist = hypot(n / 2f, n / 2f)
        shuffle = Shuffle(
            perm1 = derangement(total, rnd),
            perm2 = derangement(total, rnd),
            delays = LongArray(total) { i ->
                val dx = (i % n + 0.5f) - n / 2f
                val dy = (i / n + 0.5f) - n / 2f
                (hypot(dx, dy) / maxDist * 150f + rnd.nextFloat() * 70f).toLong()
            },
            spins = FloatArray(total) { (if (rnd.nextBoolean()) 1f else -1f) * (1 + rnd.nextInt(2)) * 360f },
            arcs = FloatArray(total) { (rnd.nextFloat() * 2f - 1f) * 1.4f },
        )
        rings.add(Ring(n / 2f, n / 2f, WgColors.Purple, maxR = n * 0.75f, width = 0.35f))
        repeat(14) { sparks.add(sparkle(n / 2f, n / 2f, if (it % 2 == 0) WgColors.Blue else WgColors.Pink, speed = n * 0.9f)) }
        pendingRelease = NONE
        bump()
    }

    // =========================================================================================== boucle d'images
    fun tick(now: Long) {
        // Horodatage des effets créés alors que la boucle était à l'arrêt.
        pops.forEach { if (it.startMs == UNSET) it.startMs = now }
        waves.forEach { if (it.startMs == UNSET) it.startMs = now }
        rings.forEach { if (it.startMs == UNSET) it.startMs = now }
        sparks.forEach { if (it.startMs == UNSET) it.startMs = now }
        leaving.forEach { if (it.startMs == UNSET) it.startMs = now }
        shuffle?.let { if (it.startMs == UNSET) it.startMs = now }
        foundStart.keys.toList().forEach { if (foundStart[it] == UNSET) foundStart[it] = now }
        if (wrongSince == UNSET) wrongSince = now
        if (pendingRelease == UNSET) {
            pendingRelease = now
            pendingReleaseAt = now
        }

        // Verdict absent au bout de ~2 images : simple relâchement, la capsule se rétracte sans bruit.
        if (pendingRelease >= 0L && now - pendingReleaseAt >= 40L) {
            pendingRelease = NONE
            if (lastPath.size >= 2) {
                leaving.add(Leaving(center(lastPath.first()), center(lastPath.last()), WgColors.Purple, wrong = false, cells = lastPath).also { it.startMs = now })
            }
        }

        // Pointe de la capsule : suit le doigt avec un amortissement critique (jamais de saut de case en case).
        val dt = if (lastTick == 0L) 0f else ((now - lastTick) / 1000f).coerceIn(0f, 0.05f)
        lastTick = now
        if (tracing && lastPath.isNotEmpty()) {
            val target = center(previousSelection.lastOrNull() ?: lastPath.last())
            val k = 1f - exp(-30f * dt)
            tipX += (target.x - tipX) * k
            tipY += (target.y - tipY) * k
        }

        // Nettoyage des effets terminés.
        pops.removeAll { it.startMs != UNSET && now - it.startMs > POP_MS }
        waves.removeAll { it.startMs != UNSET && now - it.startMs > WAVE_MS + it.cells.size * WAVE_STAGGER_MS }
        rings.removeAll { it.startMs != UNSET && now - it.startMs > RING_MS }
        sparks.removeAll { it.startMs != UNSET && now - it.startMs > it.life }
        leaving.removeAll { it.startMs != UNSET && now - it.startMs > LEAVE_MS }
        shuffle?.let { if (it.startMs != UNSET && now - it.startMs > SHUFFLE_TOTAL_MS) shuffle = null }
        if (wrongSince >= 0L && now - wrongSince > WRONG_WIGGLE_MS) {
            wrongSince = NONE
            wrongCells = emptyList()
        }
        clockMs = now
    }

    /** Vrai tant qu'il reste quelque chose à animer (la boucle d'images s'arrête sinon). */
    fun hasWork(now: Long): Boolean =
        pops.isNotEmpty() || waves.isNotEmpty() || rings.isNotEmpty() || sparks.isNotEmpty() || leaving.isNotEmpty() ||
            shuffle != null || tracing || pendingRelease != NONE || wrongSince != NONE ||
            foundStart.values.any { it == UNSET || now - it < FOUND_GROW_MS }

    // =========================================================================================== fabrication d'effets
    private fun spawnFound(h: FoundHighlight) {
        waves.add(WordWave(h.cells, h.color))
        val a = h.cells.first()
        val b = h.cells.last()
        rings.add(Ring(a.col + 0.5f, a.row + 0.5f, h.color, maxR = 1.7f, width = 0.2f))
        rings.add(Ring(b.col + 0.5f, b.row + 0.5f, h.color, maxR = 2.1f, width = 0.26f))
        val count = min(26, 10 + h.cells.size * 2)
        repeat(count) { i ->
            val c = h.cells[random.nextInt(h.cells.size)]
            val color = when (i % 4) {
                0 -> h.color
                1 -> WgColors.White
                2 -> WgColors.Yellow
                else -> lerp(h.color, WgColors.White, 0.5f)
            }
            sparks.add(sparkle(c.col + 0.5f, c.row + 0.5f, color, speed = 4.5f, sparkleShape = i % 3 != 0))
        }
        bump()
    }

    private fun spawnTrail(c: Cell) {
        repeat(2) { sparks.add(sparkle(c.col + 0.5f, c.row + 0.5f, WgColors.White, speed = 1.8f, sparkleShape = false, small = true)) }
    }

    private fun sparkle(x: Float, y: Float, color: Color, speed: Float, sparkleShape: Boolean = true, small: Boolean = false): Spark {
        val angle = random.nextFloat() * 2f * PI.toFloat()
        val v = speed * (0.45f + random.nextFloat() * 0.75f)
        return Spark(
            x = x, y = y, vx = cos(angle) * v, vy = sin(angle) * v - speed * 0.25f,
            life = (520 + random.nextInt(380)).toLong(), color = color,
            size = (if (small) 0.07f else 0.12f) + random.nextFloat() * (if (small) 0.05f else 0.12f),
            sparkle = sparkleShape, spin = (random.nextFloat() - 0.5f) * 540f,
        )
    }

    private fun derangement(size: Int, rnd: Random): IntArray {
        val p = IntArray(size) { it }
        for (attempt in 0 until 8) {
            for (i in size - 1 downTo 1) {
                val j = rnd.nextInt(i + 1)
                val t = p[i]; p[i] = p[j]; p[j] = t
            }
            if (p.indices.none { p[it] == it }) return p
        }
        // Repli : décalage circulaire (aucune case ne reste en place).
        return IntArray(size) { (it + 1 + size / 3) % size }
    }

    private fun center(c: Cell) = Offset(c.col + 0.5f, c.row + 0.5f)
    private fun snapTip(c: Cell) { tipX = c.col + 0.5f; tipY = c.row + 0.5f }
    private fun key(cells: List<Cell>) = "${cells.first().row},${cells.first().col}-${cells.last().row},${cells.last().col}"

    // =========================================================================================== dessin
    /**
     * Dessine toute la grille (tuiles, capsules, lettres animées, anneaux, particules).
     * [whiteLetters] : cases dont la lettre passe en blanc (sous une capsule colorée).
     */
    fun DrawScope.drawAll(
        grid: List<String>,
        found: List<FoundHighlight>,
        selection: List<Cell>,
        selectionColor: Color,
        hintCells: List<Cell>,
        glyphs: Map<Char, TextLayoutResult>,
        fontPx: Float,
        ink: Color,
        tileColor: Color,
    ) {
        val now = clockMs
        val cs = size.width / n

        // 1) Tuiles très discrètes (lisibilité de la grille)
        val gap = cs * 0.07f
        for (r in 0 until n) for (c in 0 until n) {
            drawRoundRect(
                color = tileColor,
                topLeft = Offset(c * cs + gap, r * cs + gap),
                size = Size(cs - gap * 2, cs - gap * 2),
                cornerRadius = CornerRadius(cs * 0.22f),
            )
        }

        // 2) Capsules rétractées (mot retiré ou tracé relâché / faux)
        leaving.forEach { drawLeaving(it, now, cs) }

        // 3) Capsules des mots trouvés (elles « poussent » avec un rebond à leur création)
        val covered = HashSet<Long>()
        found.forEach { h ->
            val start = foundStart[key(h.cells)]
            val t = if (start == null || start == UNSET) 99f else ((now - start) / 1000f)
            val grow = if (start == UNSET) 0f else min(1.08f, Motion.spring(t, 15f, 0.55f))
            val pulse = 1f + 0.24f * Motion.bump(t / 0.36f)
            drawCapsule(center(h.cells.first()), center(h.cells.last()), h.color, cs, grow, pulse)
            h.cells.forEach { covered.add(cellKey(it)) }
        }

        // 4) Capsule du tracé en direct : serpent élastique, léger « souffle » de l'épaisseur
        if (selection.isNotEmpty()) {
            val a = center(selection.first())
            val b = if (reduceMotion) center(selection.last()) else Offset(tipX, tipY)
            val breathe = 1f + 0.05f * sin(now / 105f)
            drawCapsule(a, b, selectionColor, cs, 1f, breathe, extra = 0.04f)
            selection.forEach { covered.add(cellKey(it)) }
        }

        // 5) Cases d'indice : anneau qui bat + halo
        hintCells.forEach { c ->
            val beat = 1f + 0.12f * sin(now / 170f + c.col)
            val cx = (c.col + 0.5f) * cs
            val cy = (c.row + 0.5f) * cs
            drawCircle(WgColors.Yellow.copy(alpha = 0.28f), cs * 0.44f * beat, Offset(cx, cy))
            drawCircle(WgColors.YellowDark, cs * 0.46f * beat, Offset(cx, cy), style = Stroke(width = cs * 0.09f))
        }

        // 6) Lettres : pop à la sélection, vague à la victoire, secousse à l'erreur, mélange au redémarrage
        for (r in 0 until n) for (c in 0 until n) {
            val ch = grid[r][c]
            val layout = glyphs[ch] ?: continue
            val idx = r * n + c
            var px = c + 0.5f
            var py = r + 0.5f
            var scale = 1f
            var rot = 0f

            pops.forEach { p ->
                if (p.col == c && p.row == r && p.startMs != UNSET) {
                    val t = (now - p.startMs) / 1000f
                    scale *= 1f + 0.5f * p.strength * (1f - Motion.spring(t, 20f, 0.4f))
                }
            }
            waves.forEach { w ->
                val i = w.cells.indexOfFirst { it.row == r && it.col == c }
                if (i >= 0 && w.startMs != UNSET) {
                    val t = (now - w.startMs - i * WAVE_STAGGER_MS) / 1000f / 0.32f
                    if (t in 0f..1f) {
                        val b = Motion.bump(t)
                        py -= 0.34f * b
                        scale *= 1f + 0.38f * b
                    }
                }
            }
            if (wrongSince >= 0L && wrongCells.any { it.row == r && it.col == c }) {
                val t = (now - wrongSince) / 1000f
                rot += 11f * sin(t * 46f) * exp(-t * 7f)
            }
            shuffle?.let { s ->
                if (s.startMs != UNSET) {
                    val t = now - s.startMs - s.delays[idx]
                    if (t > 0) {
                        val home = Offset(px, py)
                        val p1 = slot(s.perm1[idx])
                        val p2 = slot(s.perm2[idx])
                        val arc = s.arcs[idx]
                        val pos: Offset
                        val lift: Float
                        when {
                            t < LEG1_MS -> {
                                val u = Motion.easeInOutCubic(t / LEG1_MS.toFloat())
                                pos = arcPoint(home, p1, arc, u); lift = Motion.bump(t / LEG1_MS.toFloat())
                                rot = s.spins[idx] * 0.5f * u
                            }
                            t < LEG1_MS + LEG2_MS -> {
                                val u = Motion.easeInOutCubic((t - LEG1_MS) / LEG2_MS.toFloat())
                                pos = arcPoint(p1, p2, -arc, u); lift = Motion.bump((t - LEG1_MS) / LEG2_MS.toFloat())
                                rot = s.spins[idx] * (0.5f + 0.5f * u)
                            }
                            else -> {
                                val u = (t - LEG1_MS - LEG2_MS) / 1000f
                                val k = Motion.spring(u, 17f, 0.5f)
                                pos = Offset(p2.x + (home.x - p2.x) * k, p2.y + (home.y - p2.y) * k)
                                lift = 0.35f * (1f - min(1f, k))
                                rot = 0f
                            }
                        }
                        px = pos.x; py = pos.y
                        scale *= 1f + 0.3f * lift
                    }
                }
            }
            val white = cellKey(Cell(r, c)) in covered
            drawLetter(layout, px * cs, py * cs, scale, rot, if (white) Color.White else ink, fontPx)
        }

        // 7) Anneaux d'onde de choc (leur épaisseur s'amincit : aucun fondu)
        rings.forEach { ring ->
            if (ring.startMs == UNSET) return@forEach
            val u = ((now - ring.startMs) / RING_MS.toFloat()).coerceIn(0f, 1f)
            val e = Motion.easeOutCubic(u)
            val w = ring.width * cs * (1f - u)
            if (w > 0.5f) drawCircle(ring.color, ring.maxR * cs * e, Offset(ring.x * cs, ring.y * cs), style = Stroke(width = w))
        }

        // 8) Particules : bulles et étincelles qui rétrécissent jusqu'à disparaître
        sparks.forEach { s ->
            if (s.startMs == UNSET) return@forEach
            val t = (now - s.startMs) / 1000f
            val u = ((now - s.startMs) / s.life.toFloat()).coerceIn(0f, 1f)
            val px = (s.x + s.vx * t) * cs
            val py = (s.y + s.vy * t + 5.5f * t * t) * cs
            val r = s.size * cs * (1f - Motion.easeOutCubic(u) * 0.95f)
            if (r < 0.6f) return@forEach
            if (s.sparkle) drawSparkle(Offset(px, py), r * 1.7f, s.color, s.spin * t) else drawCircle(s.color, r, Offset(px, py))
        }
    }

    private fun slot(i: Int) = Offset(i % n + 0.5f, i / n + 0.5f)

    private fun arcPoint(a: Offset, b: Offset, arc: Float, u: Float): Offset {
        val mx = (a.x + b.x) / 2f
        val my = (a.y + b.y) / 2f
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = max(0.001f, hypot(dx, dy))
        val cx = mx - dy / len * arc * min(len, 4f)
        val cy = my + dx / len * arc * min(len, 4f)
        return Offset(Motion.quad(a.x, cx, b.x, u), Motion.quad(a.y, cy, b.y, u))
    }

    private fun cellKey(c: Cell): Long = c.row.toLong() * 1000L + c.col

    private fun DrawScope.drawLetter(layout: TextLayoutResult, cx: Float, cy: Float, s: Float, rot: Float, color: Color, fontPx: Float) {
        val topY = -(layout.firstBaseline - fontPx * 0.36f)
        withTransform({
            translate(cx, cy)
            if (rot != 0f) rotate(rot, Offset.Zero)
            if (s != 1f) scale(s, s, Offset.Zero)
        }) {
            drawText(layout, color = color, topLeft = Offset(-layout.size.width / 2f, topY))
        }
    }

    /** Capsule « bulle » : lèvre foncée dessous, corps coloré, reflet clair au-dessus. [grow] ∈ [0, ~1,1] : longueur visible. */
    private fun DrawScope.drawCapsule(a: Offset, b: Offset, color: Color, cs: Float, grow: Float, thickness: Float, extra: Float = 0f) {
        val ax = a.x * cs
        val ay = a.y * cs
        val bx = ax + (b.x * cs - ax) * grow
        val by = ay + (b.y * cs - ay) * grow
        val w = cs * (0.78f + extra) * thickness
        val dark = lerp(color, Color.Black, 0.28f)
        val lip = cs * 0.07f
        drawLine(dark, Offset(ax, ay + lip), Offset(bx, by + lip), w, StrokeCap.Round)
        drawLine(color, Offset(ax, ay), Offset(bx, by), w, StrokeCap.Round)
        drawLine(
            lerp(color, Color.White, 0.55f),
            Offset(ax - cs * 0.04f, ay - w * 0.2f), Offset(bx - cs * 0.04f, by - w * 0.2f),
            w * 0.16f, StrokeCap.Round,
        )
    }

    private fun DrawScope.drawLeaving(l: Leaving, now: Long, cs: Float) {
        if (l.startMs == UNSET) return
        val u = ((now - l.startMs) / LEAVE_MS.toFloat()).coerceIn(0f, 1f)
        val e = Motion.easeInOutCubic(u)
        val ax = l.a.x * cs
        val ay = l.a.y * cs
        val bx = ax + (l.b.x * cs - ax) * (1f - e)
        val by = ay + (l.b.y * cs - ay) * (1f - e)
        val shake = if (l.wrong) 0.18f * cs * sin(u * 50f) * (1f - u) else 0f
        val w = cs * 0.78f * (1f - e * 0.85f)
        drawLine(
            lerp(l.color, Color.Black, 0.28f), Offset(ax + shake, ay + cs * 0.07f), Offset(bx + shake, by + cs * 0.07f), w, StrokeCap.Round,
        )
        drawLine(l.color, Offset(ax + shake, ay), Offset(bx + shake, by), w, StrokeCap.Round)
    }

    private fun DrawScope.drawSparkle(c: Offset, r: Float, color: Color, spinDeg: Float) {
        val path = Path()
        for (i in 0 until 8) {
            val a = (i * 45f + spinDeg) * PI.toFloat() / 180f
            val rr = if (i % 2 == 0) r else r * 0.32f
            val x = c.x + cos(a) * rr
            val y = c.y + sin(a) * rr
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color)
    }

    companion object {
        /** Effet créé mais pas encore horodaté (la boucle d'images lui donnera son temps de départ). */
        const val UNSET = Long.MIN_VALUE
        /** Aucun effet en attente. */
        const val NONE = Long.MIN_VALUE + 1
        private const val POP_MS = 600L
        private const val WAVE_MS = 420L
        private const val WAVE_STAGGER_MS = 42L
        private const val RING_MS = 520L
        private const val LEAVE_MS = 260L
        private const val FOUND_GROW_MS = 700L
        private const val WRONG_WIGGLE_MS = 520L
        private const val LEG1_MS = 330L
        private const val LEG2_MS = 330L
        private const val LEG3_MS = 520L
        /** Durée maximale du mélange : retard maximal par case (~220 ms) + trois étapes. */
        const val SHUFFLE_TOTAL_MS = 220L + LEG1_MS + LEG2_MS + LEG3_MS + 80L
    }
}
