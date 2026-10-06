package com.example.games.sumo

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.audio.SoundManager
import com.example.model.Player
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class SumoFighter(
    val playerId: Int,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var angleDeg: Float = 0f,
    var isAlive: Boolean = true,
    var isDashing: Boolean = false,
    var botNextDecisionMs: Long = 0,
    var botDashActive: Boolean = false
)

@Composable
fun BumperSumoGame(
    players: List<Player>,
    playerPressStates: Map<Int, Boolean>,
    isPaused: Boolean,
    soundManager: SoundManager,
    onGameOver: (winner: Player?) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat().coerceAtLeast(300f)
        val height = constraints.maxHeight.toFloat().coerceAtLeast(400f)

        val arenaCenter = remember(width, height) { Offset(width / 2f, height / 2f) }
        var ringRadius by remember(width, height) {
            mutableFloatStateOf((minOf(width, height) * 0.44f).coerceAtLeast(140f))
        }

        val fighterRadius = 24f

        val fighters = remember(players, width, height) {
            mutableStateListOf<SumoFighter>().apply {
                val initDist = ringRadius * 0.55f
                val step = 360f / players.size
                players.forEachIndexed { i, p ->
                    val ang = Math.toRadians((i * step).toDouble())
                    val fx = arenaCenter.x + (cos(ang) * initDist).toFloat()
                    val fy = arenaCenter.y + (sin(ang) * initDist).toFloat()
                    add(SumoFighter(p.id, fx, fy, angleDeg = (i * step + 180f) % 360f))
                }
            }
        }

        var isRoundOver by remember { mutableStateOf(false) }

        LaunchedEffect(isPaused) {
            var lastTime = System.currentTimeMillis()

            while (true) {
                withInfiniteAnimationFrameMillis {
                    if (!isPaused && !isRoundOver) {
                        val nowMs = System.currentTimeMillis()
                        val dt = ((nowMs - lastTime) / 1000f).coerceIn(0.005f, 0.05f)
                        lastTime = nowMs

                        // Shrink ring slowly
                        if (ringRadius > 85f) {
                            ringRadius -= 6f * dt
                        }

                        // 1. Update each fighter
                        fighters.forEach { f ->
                            if (!f.isAlive) return@forEach
                            val player = players.find { it.id == f.playerId } ?: return@forEach

                            val isPressed = if (player.isBot) {
                                if (nowMs > f.botNextDecisionMs) {
                                    f.botNextDecisionMs = nowMs + 100 + Random.nextLong(200)
                                    // Target closest alive opponent or center
                                    val target = fighters.filter { it.isAlive && it.playerId != f.playerId }
                                        .minByOrNull { (it.x - f.x) * (it.x - f.x) + (it.y - f.y) * (it.y - f.y) }
                                    val tx = target?.x ?: arenaCenter.x
                                    val ty = target?.y ?: arenaCenter.y
                                    val targetAngle = Math.toDegrees(atan2(ty - f.y, tx - f.x).toDouble()).toFloat()
                                    val diff = (targetAngle - f.angleDeg + 540f) % 360f - 180f
                                    f.botDashActive = Math.abs(diff) < 40f
                                }
                                f.botDashActive
                            } else {
                                playerPressStates[f.playerId] ?: false
                            }

                            f.isDashing = isPressed

                            if (isPressed) {
                                // Thrust in direction
                                val rad = Math.toRadians(f.angleDeg.toDouble())
                                val thrust = 480f * dt
                                f.vx += (cos(rad) * thrust).toFloat()
                                f.vy += (sin(rad) * thrust).toFloat()
                            } else {
                                // Rotate when coasting
                                f.angleDeg = (f.angleDeg + 160f * dt) % 360f
                            }

                            // Friction / ice drift
                            f.vx *= (1f - 1.2f * dt)
                            f.vy *= (1f - 1.2f * dt)

                            f.x += f.vx * dt
                            f.y += f.vy * dt

                            // Check fell off ring
                            val distFromCenter = sqrt(
                                (f.x - arenaCenter.x) * (f.x - arenaCenter.x) +
                                        (f.y - arenaCenter.y) * (f.y - arenaCenter.y)
                            )
                            if (distFromCenter > ringRadius + fighterRadius * 0.8f) {
                                f.isAlive = false
                                soundManager.playExplosion()
                            }
                        }

                        // 2. Collision between fighters
                        for (i in 0 until fighters.size) {
                            for (j in i + 1 until fighters.size) {
                                val f1 = fighters[i]
                                val f2 = fighters[j]
                                if (f1.isAlive && f2.isAlive) {
                                    val dx = f2.x - f1.x
                                    val dy = f2.y - f1.y
                                    val dist = sqrt(dx * dx + dy * dy)
                                    val minDist = fighterRadius * 2
                                    if (dist < minDist && dist > 0.001f) {
                                        // Normal
                                        val nx = dx / dist
                                        val ny = dy / dist

                                        // Separate
                                        val overlap = minDist - dist
                                        f1.x -= nx * overlap * 0.5f
                                        f1.y -= ny * overlap * 0.5f
                                        f2.x += nx * overlap * 0.5f
                                        f2.y += ny * overlap * 0.5f

                                        // Bounce impulse
                                        val bounceSpeed = 280f
                                        f1.vx -= nx * bounceSpeed
                                        f1.vy -= ny * bounceSpeed
                                        f2.vx += nx * bounceSpeed
                                        f2.vy += ny * bounceSpeed

                                        soundManager.playBounce()
                                    }
                                }
                            }
                        }

                        // 3. Win check
                        val aliveCount = fighters.filter { it.isAlive }
                        if (aliveCount.size <= 1 && players.size > 1) {
                            isRoundOver = true
                            val winnerFighter = aliveCount.firstOrNull()
                            val winnerPlayer = players.find { it.id == winnerFighter?.playerId }
                            soundManager.playWinFanfare()
                            onGameOver(winnerPlayer)
                        }
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
            // Abyss background
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF1E1B4B), Color(0xFF020617)),
                    center = arenaCenter,
                    radius = ringRadius * 1.6f
                ),
                radius = ringRadius * 1.5f,
                center = arenaCenter
            )

            // Outer danger border
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = 0.5f),
                radius = ringRadius + 6f,
                center = arenaCenter,
                style = Stroke(width = 4f)
            )

            // Platform Ring
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                    center = arenaCenter,
                    radius = ringRadius
                ),
                radius = ringRadius,
                center = arenaCenter
            )

            // Inner Ring decal
            drawCircle(
                color = Color.White.copy(alpha = 0.25f),
                radius = ringRadius * 0.6f,
                center = arenaCenter,
                style = Stroke(width = 3f)
            )

            // Draw Fighters
            fighters.forEach { f ->
                if (f.isAlive) {
                    val p = players.find { it.id == f.playerId } ?: return@forEach

                    // Jet Thruster flame if dashing
                    if (f.isDashing) {
                        rotate(degrees = f.angleDeg + 180f, pivot = Offset(f.x, f.y)) {
                            drawCircle(
                                color = Color(0xFFF59E0B),
                                radius = 9f,
                                center = Offset(f.x + fighterRadius + 6f, f.y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 5f,
                                center = Offset(f.x + fighterRadius + 4f, f.y)
                            )
                        }
                    }

                    // Fighter Circle
                    drawCircle(
                        color = p.color,
                        radius = fighterRadius,
                        center = Offset(f.x, f.y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = fighterRadius - 4f,
                        center = Offset(f.x, f.y),
                        style = Stroke(width = 3f)
                    )

                    // Direction arrow / eye
                    rotate(degrees = f.angleDeg, pivot = Offset(f.x, f.y)) {
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(f.x + fighterRadius * 0.5f, f.y)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 3f,
                            center = Offset(f.x + fighterRadius * 0.6f, f.y)
                        )
                    }
                }
            }
        }
    }
}
