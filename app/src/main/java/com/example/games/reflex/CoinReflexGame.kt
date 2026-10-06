package com.example.games.reflex

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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.audio.SoundManager
import com.example.model.CornerPosition
import com.example.model.Player
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class LootType(val points: Int, val color: Color, val radius: Float) {
    COIN(1, Color(0xFFFBBF24), 18f),
    GEM(3, Color(0xFFA855F7), 22f),
    STAR(5, Color(0xFF38BDF8), 24f),
    BOMB(-2, Color(0xFFEF4444), 20f)
}

data class CenterLoot(
    val id: Long,
    val x: Float,
    val y: Float,
    val type: LootType,
    val spawnTimeMs: Long
)

data class GrabArm(
    val playerId: Int,
    val originX: Float,
    val originY: Float,
    var currentX: Float,
    var currentY: Float,
    var targetX: Float,
    var targetY: Float,
    var isExtending: Boolean = false,
    var isRetracting: Boolean = false,
    var botReactMs: Long = 0
)

@Composable
fun CoinReflexGame(
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

        val cx = width / 2f
        val cy = height / 2f

        val playerScores = remember(players) {
            mutableStateMapOf<Int, Int>().apply {
                players.forEach { put(it.id, 0) }
            }
        }

        val activeLoots = remember { mutableStateListOf<CenterLoot>() }
        var nextLootId by remember { mutableStateOf(1L) }
        var nextSpawnTimeMs by remember { mutableStateOf(0L) }

        var remainingSeconds by remember { mutableIntStateOf(25) }
        var isRoundOver by remember { mutableStateOf(false) }

        val grabArms = remember(players, width, height) {
            mutableStateListOf<GrabArm>().apply {
                players.forEach { p ->
                    val (ox, oy) = when (p.corner) {
                        CornerPosition.BOTTOM_LEFT -> Pair(30f, height - 30f)
                        CornerPosition.BOTTOM_RIGHT -> Pair(width - 30f, height - 30f)
                        CornerPosition.TOP_LEFT -> Pair(30f, 30f)
                        CornerPosition.TOP_RIGHT -> Pair(width - 30f, 30f)
                    }
                    add(GrabArm(p.id, ox, oy, ox, oy, cx, cy))
                }
            }
        }

        LaunchedEffect(isPaused) {
            var lastTime = System.currentTimeMillis()
            var secondTimer = 0f
            var prevPress = playerPressStates

            while (true) {
                withInfiniteAnimationFrameMillis {
                    if (!isPaused && !isRoundOver) {
                        val nowMs = System.currentTimeMillis()
                        val dt = ((nowMs - lastTime) / 1000f).coerceIn(0.005f, 0.05f)
                        lastTime = nowMs

                        secondTimer += dt
                        if (secondTimer >= 1.0f) {
                            secondTimer = 0f
                            remainingSeconds--
                            if (remainingSeconds <= 0) {
                                isRoundOver = true
                                soundManager.playWinFanfare()
                                val highestScorerId = playerScores.maxByOrNull { it.value }?.key
                                val winner = players.find { it.id == highestScorerId }
                                onGameOver(winner)
                                return@withInfiniteAnimationFrameMillis
                            }
                        }

                        // 1. Spawn items
                        if (nowMs > nextSpawnTimeMs && activeLoots.size < 4) {
                            nextSpawnTimeMs = nowMs + 1200 + Random.nextLong(1500)
                            val type = when (Random.nextInt(10)) {
                                in 0..4 -> LootType.COIN
                                in 5..6 -> LootType.GEM
                                in 7..7 -> LootType.STAR
                                else -> LootType.BOMB
                            }
                            val spawnRadius = 80f
                            val ang = Random.nextFloat() * 6.28f
                            val lx = cx + cos(ang) * (Random.nextFloat() * spawnRadius)
                            val ly = cy + sin(ang) * (Random.nextFloat() * spawnRadius)
                            activeLoots.add(CenterLoot(nextLootId++, lx, ly, type, nowMs))
                        }

                        // 2. Player grab logic
                        grabArms.forEach { arm ->
                            val player = players.find { it.id == arm.playerId } ?: return@forEach

                            val isPressed = if (player.isBot) {
                                val goodLoot = activeLoots.find { it.type != LootType.BOMB }
                                if (goodLoot != null && nowMs > arm.botReactMs && !arm.isExtending && !arm.isRetracting) {
                                    arm.botReactMs = nowMs + 300 + Random.nextLong(200)
                                    arm.targetX = goodLoot.x
                                    arm.targetY = goodLoot.y
                                    true
                                } else false
                            } else {
                                playerPressStates[arm.playerId] ?: false
                            }

                            val wasPressed = prevPress[arm.playerId] ?: false

                            // Tap activates arm launch
                            if (isPressed && !wasPressed && !arm.isExtending && !arm.isRetracting) {
                                val target = activeLoots.minByOrNull {
                                    val ddx = it.x - arm.originX
                                    val ddy = it.y - arm.originY
                                    ddx * ddx + ddy * ddy
                                }
                                arm.targetX = target?.x ?: cx
                                arm.targetY = target?.y ?: cy
                                arm.isExtending = true
                                soundManager.playShoot()
                            }

                            // Move arm
                            val armSpeed = 750f
                            if (arm.isExtending) {
                                val dx = arm.targetX - arm.currentX
                                val dy = arm.targetY - arm.currentY
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist < 18f) {
                                    arm.isExtending = false
                                    arm.isRetracting = true

                                    // Check loot grab
                                    val grabbed = activeLoots.find {
                                        val ldx = it.x - arm.currentX
                                        val ldy = it.y - arm.currentY
                                        ldx * ldx + ldy * ldy < 32f * 32f
                                    }
                                    if (grabbed != null) {
                                        activeLoots.remove(grabbed)
                                        val cur = playerScores[arm.playerId] ?: 0
                                        playerScores[arm.playerId] = (cur + grabbed.type.points).coerceAtLeast(0)
                                        if (grabbed.type == LootType.BOMB) {
                                            soundManager.playExplosion()
                                        } else {
                                            soundManager.playCoin()
                                        }
                                    }
                                } else {
                                    arm.currentX += (dx / dist) * armSpeed * dt
                                    arm.currentY += (dy / dist) * armSpeed * dt
                                }
                            } else if (arm.isRetracting) {
                                val dx = arm.originX - arm.currentX
                                val dy = arm.originY - arm.currentY
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist < 18f) {
                                    arm.currentX = arm.originX
                                    arm.currentY = arm.originY
                                    arm.isRetracting = false
                                } else {
                                    arm.currentX += (dx / dist) * armSpeed * dt
                                    arm.currentY += (dy / dist) * armSpeed * dt
                                }
                            }
                        }

                        prevPress = playerPressStates
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1B4B))) {
            // Center ring
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF312E81), Color(0xFF1E1B4B)),
                    center = Offset(cx, cy),
                    radius = 160f
                ),
                radius = 160f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = 155f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
            )

            // Draw Loot items in center
            activeLoots.forEach { loot ->
                drawCircle(
                    color = loot.type.color,
                    radius = loot.type.radius,
                    center = Offset(loot.x, loot.y)
                )
                drawCircle(
                    color = Color.White,
                    radius = loot.type.radius * 0.5f,
                    center = Offset(loot.x, loot.y)
                )
            }

            // Draw Grab arms & cables
            grabArms.forEach { arm ->
                val p = players.find { it.id == arm.playerId } ?: return@forEach

                // Cable line
                drawLine(
                    color = p.color,
                    start = Offset(arm.originX, arm.originY),
                    end = Offset(arm.currentX, arm.currentY),
                    strokeWidth = 6f
                )

                // Arm Grab Hand
                drawCircle(
                    color = p.color,
                    radius = 16f,
                    center = Offset(arm.currentX, arm.currentY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8f,
                    center = Offset(arm.currentX, arm.currentY)
                )
            }
        }
    }
}
