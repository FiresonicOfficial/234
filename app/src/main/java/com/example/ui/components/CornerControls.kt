package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CornerPosition
import com.example.model.Player

@Composable
fun CornerPlayerButton(
    player: Player,
    isPressed: Boolean,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    customActionLabel: String? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "button_scale"
    )

    // Invert rotation for top players sitting opposite the screen
    val rotation = when (player.corner) {
        CornerPosition.TOP_LEFT, CornerPosition.TOP_RIGHT -> 180f
        CornerPosition.BOTTOM_LEFT, CornerPosition.BOTTOM_RIGHT -> 0f
    }

    Box(
        modifier = modifier
            .size(112.dp)
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.radialGradient(
                    colors = if (isPressed) {
                        listOf(player.color, player.secondaryColor)
                    } else {
                        listOf(player.secondaryColor, player.color.copy(alpha = 0.85f))
                    }
                )
            )
            .border(
                width = if (isPressed) 4.dp else 2.5.dp,
                color = if (isPressed) Color.White else Color.White.copy(alpha = 0.6f),
                shape = RoundedCornerShape(28.dp)
            )
            .testTag("player_${player.id}_action_btn")
            .then(
                if (!player.isBot) {
                    Modifier.pointerInput(player.id) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onPressChanged(true)
                            do {
                                val event = awaitPointerEvent()
                                val isStillDown = event.changes.any { it.pressed }
                            } while (isStillDown)
                            onPressChanged(false)
                        }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .rotate(rotation)
                .padding(6.dp)
        ) {
            Text(
                text = player.avatarEmoji,
                fontSize = 30.sp
            )
            Text(
                text = if (player.isBot) "CPU" else (customActionLabel ?: player.name),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            if (player.isBot) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "AUTO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Yellow
                    )
                }
            }
        }
    }
}

@Composable
fun CornerControlsOverlay(
    players: List<Player>,
    playerPressStates: Map<Int, Boolean>,
    onPlayerPressChange: (playerId: Int, isPressed: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String? = null
) {
    Box(modifier = modifier.fillMaxSize()) {
        players.forEach { player ->
            val isPressed = playerPressStates[player.id] ?: false

            val alignModifier = when (player.corner) {
                CornerPosition.TOP_LEFT -> Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 48.dp)
                CornerPosition.TOP_RIGHT -> Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 48.dp)
                CornerPosition.BOTTOM_LEFT -> Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 20.dp)
                CornerPosition.BOTTOM_RIGHT -> Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 20.dp)
            }

            CornerPlayerButton(
                player = player,
                isPressed = isPressed,
                onPressChanged = { pressed -> onPlayerPressChange(player.id, pressed) },
                modifier = alignModifier,
                customActionLabel = actionLabel
            )
        }
    }
}
