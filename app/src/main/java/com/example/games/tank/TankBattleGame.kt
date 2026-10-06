package com.example.games.tank

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.audio.SoundManager
import com.example.model.Player
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class TankState(
    val playerId: Int,
    var x: Float,
    var y: Float,
    var angleDeg: Float,
    var isAlive: Boolean = true,
    var lastShotTimeMs: Long = 0,
    var botNextDecisionTimeMs: Long = 0,
    var botHoldingButton: Boolean = false
)

data class Bullet(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val ownerId: Int,
    var bouncesLeft: Int = 2
)

data class TankParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1.0f,
    val color: Color
)

data class Obstacle(val x: Float, val y: Float, val width: Float, val height: Float)

@Composable
fun TankBattleGame(
    players: List<Player>,
    playerPressStates: Map<Int, Boolean>,
    isPaused: Boolean,
    soundManager: SoundManager,
    onGameOver: (winner: Player?) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val arenaWidth = constraints.maxWidth.toFloat().coerceAtLeast(300f)
        val arenaHeight = constraints.maxHeight.toFloat().coerceAtLeast(400f)

        // Arena obstacles in middle
        val obstacles = remember(arenaWidth, arenaHeight) {
            val cx = arenaWidth / 2f
            val cy = arenaHeight / 2f
            listOf(
                Obstacle(cx - 30f, cy - 30f, 60f, 60f),
                Obstacle(cx - 140f, cy - 100f, 40f, 40f),
                Obstacle(cx + 100f, cy - 100f, 40f, 40f),
                Obstacle(cx - 140f, cy + 60f, 40f, 40f),
                Obstacle(cx + 100f, cy + 60f, 40f, 40f)
            )
        }

        val tanks = remember(players, arenaWidth, arenaHeight) {
            mutableStateListOf<TankState>().apply {
                val padX = arenaWidth * 0.22f
                val padY = arenaHeight * 0.22f
                val spawnPoints = listOf(
                    Triple(padX, arenaHeight - padY, 0f),                 // Bottom-left
                    Triple(arenaWidth - padX, arenaHeight - padY, 180f), // Bottom-right
                    Triple(padX, padY, 90f),                             // Top-left
                    Triple(arenaWidth - padX, padY, 270f)                // Top-right
                )
                players.forEachIndexed { i, p ->
                    val sp = spawnPoints.getOrElse(i) { Triple(arenaWidth / 2, arenaHeight / 2, 0f) }
                    add(TankState(p.id, sp.first, sp.second, sp.third))
                }
            }
        }

        val bullets = remember { mutableStateListOf<Bullet>() }
        val particles = remember { mutableStateListOf<TankParticle>() }
        var isRoundOver by remember { mutableStateOf(false) }

        // Game loop
        LaunchedEffect(isPaused) {
            var lastTime = System.currentTimeMillis()
            var prevPressStates = playerPressStates

            while (true) {
                withInfiniteAnimationFrameMillis { nowNanos ->
                    if (!isPaused && !isRoundOver) {
                        val nowMs = System.currentTimeMillis()
                        val dt = ((nowMs - lastTime) / 1000f).coerceIn(0.005f, 0.05f)
                        lastTime = nowMs

                        // 1. Tank logic
                        tanks.forEach { tank ->
                            if (!tank.isAlive) return@forEach
                            val player = players.find { it.id == tank.playerId } ?: return@forEach

                            val isPressed = if (player.isBot) {
                                // Simple Bot AI
                                if (nowMs > tank.botNextDecisionTimeMs) {
                                    tank.botNextDecisionTimeMs = nowMs + 120 + Random.nextLong(200)
                                    val target = tanks.filter { it.isAlive && it.playerId != tank.playerId }
                                        .minByOrNull { (it.x - tank.x) * (it.x - tank.x) + (it.y - tank.y) * (it.y - tank.y) }
                                    if (target != null) {
                                        val targetAngleRad = atan2(target.y - tank.y, target.x - tank.x)
                                        val targetAngleDeg = Math.toDegrees(targetAngleRad.toDouble()).toFloat()
                                        val diff = (targetAngleDeg - tank.angleDeg + 540f) % 360f - 180f
                                        if (Math.abs(diff) < 30f) {
                                            // Shoot if in sight
                                            if (nowMs - tank.lastShotTimeMs > 600) {
                                                tank.botHoldingButton = false
                                            } else {
                                                tank.botHoldingButton = true
                                            }
                                        } else {
                                            tank.botHoldingButton = false // rotate
                                        }
                                    }
                                }
                                tank.botHoldingButton
                            } else {
                                playerPressStates[tank.playerId] ?: false
                            }

                            val wasPressed = prevPressStates[tank.playerId] ?: false

                            if (isPressed) {
                                // Move forward
                                val rad = Math.toRadians(tank.angleDeg.toDouble())
                                val speed = 140f * dt
                                val nx = tank.x + (cos(rad) * speed).toFloat()
                                val ny = tank.y + (sin(rad) * speed).toFloat()

                                // Boundary check
                                val margin = 26f
                                val validX = nx.coerceIn(margin, arenaWidth - margin)
                                val validY = ny.coerceIn(margin, arenaHeight - margin)

                                // Obstacle collision
                                val collides = obstacles.any { obs ->
                                    validX in (obs.x - 20f)..(obs.x + obs.width + 20f) &&
                                            validY in (obs.y - 20f)..(obs.y + obs.height + 20f)
                                }
                                if (!collides) {
                                    tank.x = validX
                                    tank.y = validY
                                }
                            } else {
                                // Rotate when idle
                                tank.angleDeg = (tank.angleDeg + 120f * dt) % 360f
                            }

                            // Tap/Press release or periodic fire
                            val justPressed = isPressed && !wasPressed
                            if (justPressed && nowMs - tank.lastShotTimeMs > 450) {
                                tank.lastShotTimeMs = nowMs
                                val rad = Math.toRadians(tank.angleDeg.toDouble())
                                val barrelDist = 28f
                                val bx = tank.x + (cos(rad) * barrelDist).toFloat()
                                val by = tank.y + (sin(rad) * barrelDist).toFloat()
                                val bSpeed = 360f
                                bullets.add(
                                    Bullet(
                                        x = bx,
                                        y = by,
                                        vx = (cos(rad) * bSpeed).toFloat(),
                                        vy = (sin(rad) * bSpeed).toFloat(),
                                        ownerId = tank.playerId
                                    )
                                )
                                soundManager.playShoot()
                            }
                        }

                        prevPressStates = playerPressStates

                        // 2. Bullets update
                        val bulletIterator = bullets.iterator()
                        while (bulletIterator.hasNext()) {
                            val b = bulletIterator.next()
                            b.x += b.vx * dt
                            b.y += b.vy * dt

                            // Wall bounce
                            var bounced = false
                            if (b.x < 12f || b.x > arenaWidth - 12f) {
                                b.vx = -b.vx
                                b.x = b.x.coerceIn(12f, arenaWidth - 12f)
                                bounced = true
                            }
                            if (b.y < 12f || b.y > arenaHeight - 12f) {
                                b.vy = -b.vy
                                b.y = b.y.coerceIn(12f, arenaHeight - 12f)
                                bounced = true
                            }

                            // Obstacle bounce
                            obstacles.forEach { obs ->
                                if (b.x in obs.x..(obs.x + obs.width) && b.y in obs.y..(obs.y + obs.height)) {
                                    b.vx = -b.vx
                                    b.vy = -b.vy
                                    bounced = true
                                }
                            }

                            if (bounced) {
                                b.bouncesLeft--
                                soundManager.playBounce()
                                if (b.bouncesLeft <= 0) {
                                    bulletIterator.remove()
                                    continue
                                }
                            }

                            // Hit tanks
                            var hit = false
                            tanks.forEach { tank ->
                                if (tank.isAlive) {
                                    val distSq = (tank.x - b.x) * (tank.x - b.x) + (tank.y - b.y) * (tank.y - b.y)
                                    if (distSq < 26f * 26f) {
                                        tank.isAlive = false
                                        hit = true
                                        soundManager.playExplosion()

                                        // Explosion particles
                                        val pColor = players.find { it.id == tank.playerId }?.color ?: Color.Red
                                        repeat(25) {
                                            val ang = Random.nextFloat() * 2 * PI.toFloat()
                                            val spd = 60f + Random.nextFloat() * 180f
                                            particles.add(
                                                TankParticle(
                                                    x = tank.x,
                                                    y = tank.y,
                                                    vx = cos(ang) * spd,
                                                    vy = sin(ang) * spd,
                                                    color = pColor
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            if (hit) {
                                bulletIterator.remove()
                            }
                        }

                        // 3. Particles
                        val pIter = particles.iterator()
                        while (pIter.hasNext()) {
                            val p = pIter.next()
                            p.x += p.vx * dt
                            p.y += p.vy * dt
                            p.alpha -= 1.8f * dt
                            if (p.alpha <= 0f) {
                                pIter.remove()
                            }
                        }

                        // 4. Check win condition
                        val aliveTanks = tanks.filter { it.isAlive }
                        if (aliveTanks.size <= 1 && players.size > 1) {
                            isRoundOver = true
                            val winnerTank = aliveTanks.firstOrNull()
                            val winnerPlayer = players.find { it.id == winnerTank?.playerId }
                            soundManager.playWinFanfare()
                            onGameOver(winnerPlayer)
                        }
                    }
                }
            }
        }

        // Draw Canvas
        Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            // Draw grid lines
            val step = 40f
            var gx = 0f
            while (gx < size.width) {
                drawLine(Color(0xFF1E293B), Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
                gx += step
            }
            var gy = 0f
            while (gy < size.height) {
                drawLine(Color(0xFF1E293B), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
                gy += step
            }

            // Draw arena border
            drawRect(
                color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                topLeft = Offset(8f, 8f),
                size = Size(size.width - 16f, size.height - 16f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )

            // Draw obstacles
            obstacles.forEach { obs ->
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(obs.x, obs.y),
                    size = Size(obs.width, obs.height),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(obs.x + 3f, obs.y + 3f),
                    size = Size(obs.width - 6f, obs.height - 6f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            // Draw Bullets
            bullets.forEach { b ->
                drawCircle(color = Color(0xFFFBBF24), radius = 5.5f, center = Offset(b.x, b.y))
                drawCircle(color = Color.White, radius = 3.5f, center = Offset(b.x, b.y))
            }

            // Draw Tanks
            tanks.forEach { tank ->
                if (tank.isAlive) {
                    val p = players.find { it.id == tank.playerId } ?: return@forEach
                    rotate(degrees = tank.angleDeg, pivot = Offset(tank.x, tank.y)) {
                        // Tank body
                        drawRoundRect(
                            color = p.color,
                            topLeft = Offset(tank.x - 20f, tank.y - 15f),
                            size = Size(40f, 30f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                        // Tracks
                        drawRoundRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(tank.x - 22f, tank.y - 18f),
                            size = Size(44f, 6f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        drawRoundRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(tank.x - 22f, tank.y + 12f),
                            size = Size(44f, 6f),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                        // Turret
                        drawCircle(color = p.secondaryColor, radius = 10f, center = Offset(tank.x, tank.y))
                        // Barrel
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(tank.x, tank.y - 3f),
                            size = Size(22f, 6f)
                        )
                    }
                }
            }

            // Draw Particles
            particles.forEach { pt ->
                drawCircle(
                    color = pt.color.copy(alpha = pt.alpha.coerceIn(0f, 1f)),
                    radius = 4f * pt.alpha,
                    center = Offset(pt.x, pt.y)
                )
            }
        }
    }
}
