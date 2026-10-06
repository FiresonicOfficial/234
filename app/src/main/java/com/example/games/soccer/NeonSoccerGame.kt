package com.example.games.soccer

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.audio.SoundManager
import com.example.model.Player
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class Striker(
    val playerId: Int,
    var x: Float,
    var y: Float,
    val homeX: Float,
    val homeY: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isKicking: Boolean = false
)

data class Ball(
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
)

@Composable
fun NeonSoccerGame(
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

        val pitchPadX = 24f
        val pitchPadY = 50f
        val pitchWidth = width - (pitchPadX * 2)
        val pitchHeight = height - (pitchPadY * 2)

        val goalWidth = pitchWidth * 0.44f
        val goalLeft = pitchPadX + (pitchWidth - goalWidth) / 2f
        val goalRight = goalLeft + goalWidth

        var scoreTeamBottom by remember { mutableIntStateOf(0) }
        var scoreTeamTop by remember { mutableIntStateOf(0) }

        val ball = remember(width, height) {
            mutableStateOf(Ball(width / 2f, height / 2f))
        }

        val strikers = remember(players, width, height) {
            mutableStateListOf<Striker>().apply {
                val cx = width / 2f
                val cy = height / 2f
                val bottomHomeY = cy + (pitchHeight * 0.32f)
                val topHomeY = cy - (pitchHeight * 0.32f)

                when (players.size) {
                    2 -> {
                        // Player 1 Bottom, Player 2 Top
                        add(Striker(players[0].id, cx, bottomHomeY, cx, bottomHomeY))
                        add(Striker(players[1].id, cx, topHomeY, cx, topHomeY))
                    }
                    3 -> {
                        // 2 Bottom, 1 Top
                        add(Striker(players[0].id, cx - 60f, bottomHomeY, cx - 60f, bottomHomeY))
                        add(Striker(players[1].id, cx + 60f, bottomHomeY, cx + 60f, bottomHomeY))
                        add(Striker(players[2].id, cx, topHomeY, cx, topHomeY))
                    }
                    else -> {
                        // 4 players: 2 Bottom (P1, P2) vs 2 Top (P3, P4)
                        add(Striker(players[0].id, cx - 65f, bottomHomeY, cx - 65f, bottomHomeY))
                        add(Striker(players[1].id, cx + 65f, bottomHomeY, cx + 65f, bottomHomeY))
                        add(Striker(players[2].id, cx - 65f, topHomeY, cx - 65f, topHomeY))
                        add(Striker(players[3].id, cx + 65f, topHomeY, cx + 65f, topHomeY))
                    }
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

                        val b = ball.value

                        // 1. Strikers AI & Movement
                        strikers.forEach { s ->
                            val player = players.find { it.id == s.playerId } ?: return@forEach

                            val isPressed = if (player.isBot) {
                                // Bot kicks when ball is on their half or near them
                                val isBottomTeam = s.homeY > height / 2f
                                if (isBottomTeam) {
                                    b.y > height / 2f - 40f
                                } else {
                                    b.y < height / 2f + 40f
                                }
                            } else {
                                playerPressStates[s.playerId] ?: false
                            }

                            s.isKicking = isPressed

                            // Target position: ball if pressed, otherwise home
                            val tx = if (isPressed) b.x else s.homeX
                            val ty = if (isPressed) b.y else s.homeY

                            val dx = tx - s.x
                            val dy = ty - s.y
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist > 3f) {
                                val moveSpeed = if (isPressed) 260f else 170f
                                val moveStep = moveSpeed * dt
                                s.x += (dx / dist) * minOf(dist, moveStep)
                                s.y += (dy / dist) * minOf(dist, moveStep)
                            }

                            // Keep strikers on pitch
                            s.x = s.x.coerceIn(pitchPadX + 24f, pitchPadX + pitchWidth - 24f)
                            s.y = s.y.coerceIn(pitchPadY + 24f, pitchPadY + pitchHeight - 24f)

                            // Striker ball hit
                            val bdx = b.x - s.x
                            val bdy = b.y - s.y
                            val bdist = sqrt(bdx * bdx + bdy * bdy)
                            val hitRadius = 38f
                            if (bdist < hitRadius && bdist > 0.001f) {
                                val nx = bdx / bdist
                                val ny = bdy / bdist
                                val kickSpeed = 440f
                                b.vx = nx * kickSpeed
                                b.vy = ny * kickSpeed
                                soundManager.playBounce()
                            }
                        }

                        // 2. Ball physics
                        b.vx *= (1f - 0.7f * dt)
                        b.vy *= (1f - 0.7f * dt)
                        b.x += b.vx * dt
                        b.y += b.vy * dt

                        // Side wall bounce
                        if (b.x < pitchPadX + 14f) {
                            b.x = pitchPadX + 14f
                            b.vx = -b.vx
                            soundManager.playBounce()
                        } else if (b.x > pitchPadX + pitchWidth - 14f) {
                            b.x = pitchPadX + pitchWidth - 14f
                            b.vx = -b.vx
                            soundManager.playBounce()
                        }

                        // Top & Bottom wall / Goal logic
                        // Top Goal check
                        if (b.y < pitchPadY + 12f) {
                            if (b.x in goalLeft..goalRight) {
                                // Bottom team scores!
                                scoreTeamBottom++
                                soundManager.playGoal()
                                b.x = width / 2f
                                b.y = height / 2f
                                b.vx = 0f
                                b.vy = 0f
                            } else {
                                b.y = pitchPadY + 12f
                                b.vy = -b.vy
                                soundManager.playBounce()
                            }
                        }

                        // Bottom Goal check
                        if (b.y > pitchPadY + pitchHeight - 12f) {
                            if (b.x in goalLeft..goalRight) {
                                // Top team scores!
                                scoreTeamTop++
                                soundManager.playGoal()
                                b.x = width / 2f
                                b.y = height / 2f
                                b.vx = 0f
                                b.vy = 0f
                            } else {
                                b.y = pitchPadY + pitchHeight - 12f
                                b.vy = -b.vy
                                soundManager.playBounce()
                            }
                        }

                        // Win condition: first to 3
                        if (scoreTeamBottom >= 3 || scoreTeamTop >= 3) {
                            isRoundOver = true
                            soundManager.playWinFanfare()
                            val winner = if (scoreTeamBottom >= 3) {
                                players.firstOrNull { it.corner == com.example.model.CornerPosition.BOTTOM_LEFT || it.corner == com.example.model.CornerPosition.BOTTOM_RIGHT }
                            } else {
                                players.firstOrNull { it.corner == com.example.model.CornerPosition.TOP_LEFT || it.corner == com.example.model.CornerPosition.TOP_RIGHT }
                            } ?: players.first()
                            onGameOver(winner)
                        }
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF022C22))) {
            // Pitch background
            drawRoundRect(
                color = Color(0xFF064E3B),
                topLeft = Offset(pitchPadX, pitchPadY),
                size = Size(pitchWidth, pitchHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Pitch boundary lines
            drawRoundRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = Offset(pitchPadX, pitchPadY),
                size = Size(pitchWidth, pitchHeight),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 3f)
            )

            // Center line
            drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(pitchPadX, height / 2f),
                end = Offset(pitchPadX + pitchWidth, height / 2f),
                strokeWidth = 3f
            )

            // Center circle
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = 48f,
                center = Offset(width / 2f, height / 2f),
                style = Stroke(width = 3f)
            )

            // Goals
            // Top goal
            drawRect(
                color = Color(0xFFEF4444).copy(alpha = 0.6f),
                topLeft = Offset(goalLeft, pitchPadY - 18f),
                size = Size(goalWidth, 18f)
            )
            // Bottom goal
            drawRect(
                color = Color(0xFF3B82F6).copy(alpha = 0.6f),
                topLeft = Offset(goalLeft, pitchPadY + pitchHeight),
                size = Size(goalWidth, 18f)
            )

            // Draw Strikers
            strikers.forEach { s ->
                val p = players.find { it.id == s.playerId } ?: return@forEach
                drawCircle(
                    color = p.color,
                    radius = 22f,
                    center = Offset(s.x, s.y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 16f,
                    center = Offset(s.x, s.y),
                    style = Stroke(width = 3f)
                )
            }

            // Draw Soccer Ball
            val b = ball.value
            drawCircle(
                color = Color.White,
                radius = 15f,
                center = Offset(b.x, b.y)
            )
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 7f,
                center = Offset(b.x, b.y)
            )
        }
    }
}
