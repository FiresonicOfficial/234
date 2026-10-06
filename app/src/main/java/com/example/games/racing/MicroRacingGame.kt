package com.example.games.racing

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.example.audio.SoundManager
import com.example.model.Player
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class Racer(
    val playerId: Int,
    var x: Float,
    var y: Float,
    var angleDeg: Float,
    var speed: Float = 0f,
    var lapsCompleted: Int = 0,
    var currentCheckpoint: Int = 0,
    var isBoosting: Boolean = false,
    var boostRemainingMs: Long = 0,
    var skidTrail: MutableList<Offset> = mutableListOf()
)

@Composable
fun MicroRacingGame(
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

        val targetLaps = 3

        // Circuit waypoints in clockwise direction
        val cx = width / 2f
        val cy = height / 2f
        val rx = width * 0.36f
        val ry = height * 0.32f

        val checkpoints = remember(width, height) {
            listOf(
                Offset(cx, cy + ry),              // 0: Bottom (Finish line)
                Offset(cx - rx, cy + ry * 0.6f),  // 1: Bottom-left
                Offset(cx - rx, cy - ry * 0.6f),  // 2: Top-left
                Offset(cx, cy - ry),              // 3: Top
                Offset(cx + rx, cy - ry * 0.6f),  // 4: Top-right
                Offset(cx + rx, cy + ry * 0.6f)   // 5: Bottom-right
            )
        }

        // Boost pads
        val boostPads = remember(width, height) {
            listOf(
                Offset(cx - rx, cy),
                Offset(cx + rx, cy)
            )
        }

        val racers = remember(players, width, height) {
            mutableStateListOf<Racer>().apply {
                val startY = cy + ry
                val spacing = 22f
                players.forEachIndexed { i, p ->
                    val sx = cx - 50f + (i * spacing)
                    add(Racer(p.id, sx, startY + (i % 2) * 16f, angleDeg = 180f))
                }
            }
        }

        var isFinished by remember { mutableStateOf(false) }

        LaunchedEffect(isPaused) {
            var lastTime = System.currentTimeMillis()

            while (true) {
                withInfiniteAnimationFrameMillis {
                    if (!isPaused && !isFinished) {
                        val nowMs = System.currentTimeMillis()
                        val dt = ((nowMs - lastTime) / 1000f).coerceIn(0.005f, 0.05f)
                        lastTime = nowMs

                        racers.forEach { racer ->
                            val player = players.find { it.id == racer.playerId } ?: return@forEach

                            val isPressed = if (player.isBot) {
                                // AI follows next checkpoint
                                val targetCp = checkpoints[racer.currentCheckpoint % checkpoints.size]
                                val angleToCpRad = atan2(targetCp.y - racer.y, targetCp.x - racer.x)
                                val angleToCpDeg = Math.toDegrees(angleToCpRad.toDouble()).toFloat()
                                val diff = (angleToCpDeg - racer.angleDeg + 540f) % 360f - 180f
                                // Turn if needed
                                diff > 10f
                            } else {
                                playerPressStates[racer.playerId] ?: false
                            }

                            // Steer right when button is pressed, otherwise slight counter steer
                            if (isPressed) {
                                racer.angleDeg = (racer.angleDeg + 185f * dt) % 360f
                            } else {
                                racer.angleDeg = (racer.angleDeg - 25f * dt) % 360f
                            }

                            // Speed logic
                            val maxSpeed = if (racer.isBoosting) 310f else 190f
                            racer.speed = (racer.speed + 160f * dt).coerceAtMost(maxSpeed)

                            if (racer.isBoosting) {
                                racer.boostRemainingMs -= (dt * 1000).toLong()
                                if (racer.boostRemainingMs <= 0) {
                                    racer.isBoosting = false
                                }
                            }

                            // Move racer
                            val rad = Math.toRadians(racer.angleDeg.toDouble())
                            racer.x += (cos(rad) * racer.speed * dt).toFloat()
                            racer.y += (sin(rad) * racer.speed * dt).toFloat()

                            // Bounds check
                            racer.x = racer.x.coerceIn(20f, width - 20f)
                            racer.y = racer.y.coerceIn(20f, height - 20f)

                            // Skid mark trail
                            if (isPressed && Random.nextFloat() < 0.35f) {
                                racer.skidTrail.add(Offset(racer.x, racer.y))
                                if (racer.skidTrail.size > 25) racer.skidTrail.removeAt(0)
                            }

                            // Check boost pads
                            boostPads.forEach { pad ->
                                val distSq = (racer.x - pad.x) * (racer.x - pad.x) + (racer.y - pad.y) * (racer.y - pad.y)
                                if (distSq < 30f * 30f && !racer.isBoosting) {
                                    racer.isBoosting = true
                                    racer.boostRemainingMs = 900
                                    soundManager.playShoot()
                                }
                            }

                            // Check checkpoints
                            val nextCpIdx = racer.currentCheckpoint % checkpoints.size
                            val targetCp = checkpoints[nextCpIdx]
                            val distToCp = (racer.x - targetCp.x) * (racer.x - targetCp.x) + (racer.y - targetCp.y) * (racer.y - targetCp.y)
                            if (distToCp < 70f * 70f) {
                                racer.currentCheckpoint++
                                if (nextCpIdx == 0 && racer.currentCheckpoint > checkpoints.size) {
                                    racer.lapsCompleted++
                                    soundManager.playCoin()
                                    if (racer.lapsCompleted >= targetLaps) {
                                        isFinished = true
                                        soundManager.playWinFanfare()
                                        onGameOver(player)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF1E293B))) {
            // Track outer ring
            drawRoundRect(
                color = Color(0xFF334155),
                topLeft = Offset(cx - rx - 55f, cy - ry - 55f),
                size = Size((rx * 2) + 110f, (ry * 2) + 110f),
                cornerRadius = CornerRadius(90f, 90f)
            )

            // Inner infield grass / island
            drawRoundRect(
                color = Color(0xFF0F172A),
                topLeft = Offset(cx - rx + 45f, cy - ry + 45f),
                size = Size((rx * 2) - 90f, (ry * 2) - 90f),
                cornerRadius = CornerRadius(60f, 60f)
            )

            // Track center dashed line
            drawRoundRect(
                color = Color.White.copy(alpha = 0.3f),
                topLeft = Offset(cx - rx, cy - ry),
                size = Size(rx * 2, ry * 2),
                cornerRadius = CornerRadius(75f, 75f),
                style = Stroke(width = 3f)
            )

            // Finish Line at bottom
            val finishY = cy + ry
            for (i in 0 until 8) {
                val fColor = if (i % 2 == 0) Color.White else Color.Black
                drawRect(
                    color = fColor,
                    topLeft = Offset(cx - 30f + (i * 8f), finishY - 35f),
                    size = Size(8f, 70f)
                )
            }

            // Draw Boost pads
            boostPads.forEach { pad ->
                drawCircle(color = Color(0xFFFBBF24), radius = 22f, center = pad)
                drawCircle(color = Color(0xFFEF4444), radius = 14f, center = pad)
            }

            // Draw skid trails
            racers.forEach { r ->
                r.skidTrail.forEach { p ->
                    drawCircle(color = Color.Black.copy(alpha = 0.45f), radius = 3.5f, center = p)
                }
            }

            // Draw Cars
            racers.forEach { r ->
                val p = players.find { it.id == r.playerId } ?: return@forEach
                rotate(degrees = r.angleDeg, pivot = Offset(r.x, r.y)) {
                    // Car body
                    drawRoundRect(
                        color = p.color,
                        topLeft = Offset(r.x - 18f, r.y - 10f),
                        size = Size(36f, 20f),
                        cornerRadius = CornerRadius(5f, 5f)
                    )
                    // Windshield
                    drawRoundRect(
                        color = Color(0xFFE2E8F0),
                        topLeft = Offset(r.x + 2f, r.y - 7f),
                        size = Size(9f, 14f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                    // Spoiler
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(r.x - 18f, r.y - 11f),
                        size = Size(4f, 22f)
                    )
                    // Headlights
                    drawCircle(color = Color.Yellow, radius = 2.5f, center = Offset(r.x + 18f, r.y - 6f))
                    drawCircle(color = Color.Yellow, radius = 2.5f, center = Offset(r.x + 18f, r.y + 6f))
                }
            }
        }
    }
}
