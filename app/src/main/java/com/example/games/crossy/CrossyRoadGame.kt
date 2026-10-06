package com.example.games.crossy

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
import com.example.audio.SoundManager
import com.example.model.Player
import kotlin.random.Random

enum class LaneType {
    GRASS,
    ROAD,
    RIVER,
    FINISH
}

data class TrafficVehicle(
    var x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val speed: Float,
    val color: Color
)

data class ChickenRacer(
    val playerId: Int,
    var x: Float,
    var laneIndex: Int = 0,
    var respawnTimerMs: Long = 0,
    var isDazed: Boolean = false,
    var botNextHopMs: Long = 0
)

@Composable
fun CrossyRoadGame(
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

        val totalLanes = 8
        val laneHeight = height / (totalLanes + 1f)

        // Lane setup: 0=Start, 1=Road, 2=Road, 3=Grass, 4=Road, 5=River, 6=Road, 7=Finish
        val laneTypes = listOf(
            LaneType.GRASS,   // 0: Start safe zone (bottom)
            LaneType.ROAD,    // 1: Cars
            LaneType.ROAD,    // 2: Trucks
            LaneType.GRASS,   // 3: Rest stop
            LaneType.ROAD,    // 4: Fast sports cars
            LaneType.RIVER,   // 5: River logs
            LaneType.ROAD,    // 6: Final traffic
            LaneType.FINISH   // 7: Golden finish
        )

        val vehicles = remember(width, height) {
            mutableStateListOf<TrafficVehicle>().apply {
                // Lane 1 vehicles (Road)
                val l1Y = height - (laneHeight * 2.5f)
                add(TrafficVehicle(50f, l1Y, 55f, laneHeight * 0.7f, 150f, Color(0xFFEF4444)))
                add(TrafficVehicle(width * 0.6f, l1Y, 55f, laneHeight * 0.7f, 150f, Color(0xFF3B82F6)))

                // Lane 2 vehicles (Trucks)
                val l2Y = height - (laneHeight * 3.5f)
                add(TrafficVehicle(100f, l2Y, 85f, laneHeight * 0.7f, -120f, Color(0xFFF59E0B)))
                add(TrafficVehicle(width * 0.75f, l2Y, 85f, laneHeight * 0.7f, -120f, Color(0xFF8B5CF6)))

                // Lane 4 vehicles (Sports cars)
                val l4Y = height - (laneHeight * 5.5f)
                add(TrafficVehicle(0f, l4Y, 50f, laneHeight * 0.7f, 210f, Color(0xFFEC4899)))
                add(TrafficVehicle(width * 0.5f, l4Y, 50f, laneHeight * 0.7f, 210f, Color(0xFF10B981)))

                // Lane 5 River logs
                val l5Y = height - (laneHeight * 6.5f)
                add(TrafficVehicle(20f, l5Y, 90f, laneHeight * 0.75f, 90f, Color(0xFF92400E)))
                add(TrafficVehicle(width * 0.65f, l5Y, 90f, laneHeight * 0.75f, 90f, Color(0xFF92400E)))

                // Lane 6 vehicles
                val l6Y = height - (laneHeight * 7.5f)
                add(TrafficVehicle(width, l6Y, 60f, laneHeight * 0.7f, -180f, Color(0xFFFBBF24)))
                add(TrafficVehicle(width * 0.4f, l6Y, 60f, laneHeight * 0.7f, -180f, Color(0xFF38BDF8)))
            }
        }

        val chickens = remember(players, width, height) {
            mutableStateListOf<ChickenRacer>().apply {
                val stepX = width / (players.size + 1)
                players.forEachIndexed { i, p ->
                    add(ChickenRacer(p.id, stepX * (i + 1), laneIndex = 0))
                }
            }
        }

        var isRoundOver by remember { mutableStateOf(false) }

        LaunchedEffect(isPaused) {
            var lastTime = System.currentTimeMillis()
            var prevPress = playerPressStates

            while (true) {
                withInfiniteAnimationFrameMillis {
                    if (!isPaused && !isRoundOver) {
                        val nowMs = System.currentTimeMillis()
                        val dt = ((nowMs - lastTime) / 1000f).coerceIn(0.005f, 0.05f)
                        lastTime = nowMs

                        // 1. Move vehicles
                        vehicles.forEach { v ->
                            v.x += v.speed * dt
                            if (v.speed > 0 && v.x > width + 20f) {
                                v.x = -v.width - 20f
                            } else if (v.speed < 0 && v.x < -v.width - 20f) {
                                v.x = width + 20f
                            }
                        }

                        // 2. Chicken updates
                        chickens.forEach { c ->
                            val player = players.find { it.id == c.playerId } ?: return@forEach

                            val isPressed = if (player.isBot) {
                                if (nowMs > c.botNextHopMs && !c.isDazed) {
                                    c.botNextHopMs = nowMs + 450 + Random.nextLong(300)
                                    true
                                } else false
                            } else {
                                playerPressStates[c.playerId] ?: false
                            }

                            val wasPressed = prevPress[c.playerId] ?: false

                            // Hop forward on tap
                            if (isPressed && !wasPressed && !c.isDazed) {
                                c.laneIndex++
                                soundManager.playBounce()

                                // Win check
                                if (c.laneIndex >= totalLanes - 1) {
                                    isRoundOver = true
                                    soundManager.playWinFanfare()
                                    onGameOver(player)
                                    return@forEach
                                }
                            }

                            // Daze timer
                            if (c.isDazed) {
                                c.respawnTimerMs -= (dt * 1000).toLong()
                                if (c.respawnTimerMs <= 0) {
                                    c.isDazed = false
                                }
                            }

                            // Collision check with vehicles in this lane
                            val cY = height - (laneHeight * (c.laneIndex + 1.5f))
                            val currentLaneType = laneTypes.getOrElse(c.laneIndex) { LaneType.GRASS }

                            if (currentLaneType == LaneType.ROAD && !c.isDazed) {
                                vehicles.forEach { v ->
                                    if (Math.abs(v.y - cY) < laneHeight * 0.4f) {
                                        if (c.x in (v.x - 14f)..(v.x + v.width + 14f)) {
                                            // Hit by car!
                                            c.isDazed = true
                                            c.respawnTimerMs = 800
                                            c.laneIndex = (c.laneIndex - 2).coerceAtLeast(0)
                                            soundManager.playExplosion()
                                        }
                                    }
                                }
                            } else if (currentLaneType == LaneType.RIVER && !c.isDazed) {
                                // Must be on a log
                                val onLog = vehicles.any { v ->
                                    Math.abs(v.y - cY) < laneHeight * 0.4f &&
                                            c.x in (v.x - 10f)..(v.x + v.width + 10f)
                                }
                                if (!onLog) {
                                    // Fell in water!
                                    c.isDazed = true
                                    c.respawnTimerMs = 800
                                    c.laneIndex = 3 // Safe grass
                                    soundManager.playBounce()
                                }
                            }
                        }

                        prevPress = playerPressStates
                    }
                }
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw Lanes
            for (lane in 0 until totalLanes) {
                val laneY = height - (laneHeight * (lane + 2f))
                val type = laneTypes.getOrElse(lane) { LaneType.GRASS }

                val laneColor = when (type) {
                    LaneType.GRASS -> Color(0xFF15803D)
                    LaneType.ROAD -> Color(0xFF334155)
                    LaneType.RIVER -> Color(0xFF0284C7)
                    LaneType.FINISH -> Color(0xFFCA8A04)
                }

                drawRect(
                    color = laneColor,
                    topLeft = Offset(0f, laneY),
                    size = Size(width, laneHeight)
                )

                if (type == LaneType.ROAD) {
                    // Dashed road divider
                    var dx = 0f
                    while (dx < width) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.4f),
                            topLeft = Offset(dx, laneY + laneHeight / 2f - 2f),
                            size = Size(20f, 4f)
                        )
                        dx += 40f
                    }
                }
            }

            // Draw Vehicles & Logs
            vehicles.forEach { v ->
                drawRoundRect(
                    color = v.color,
                    topLeft = Offset(v.x, v.y),
                    size = Size(v.width, v.height),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            // Draw Finish Trophy line
            val finishY = height - (laneHeight * (totalLanes + 1f))
            drawRect(
                color = Color(0xFFFBBF24),
                topLeft = Offset(0f, finishY),
                size = Size(width, laneHeight)
            )

            // Draw Chickens
            chickens.forEach { c ->
                val p = players.find { it.id == c.playerId } ?: return@forEach
                val cY = height - (laneHeight * (c.laneIndex + 1.5f))

                // Chicken body
                drawCircle(
                    color = if (c.isDazed) Color.Gray else p.color,
                    radius = 16f,
                    center = Offset(c.x, cY)
                )
                // Beak
                drawCircle(
                    color = Color(0xFFF59E0B),
                    radius = 5f,
                    center = Offset(c.x, cY - 12f)
                )
                // Crown
                drawCircle(
                    color = Color.Red,
                    radius = 4f,
                    center = Offset(c.x, cY - 16f)
                )
            }
        }
    }
}
