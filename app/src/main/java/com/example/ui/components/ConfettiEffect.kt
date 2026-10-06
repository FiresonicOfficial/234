package com.example.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var width: Float,
    var height: Float,
    val color: Color
)

@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(
        Color(0xFFEF4444),
        Color(0xFF3B82F6),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEC4899),
        Color(0xFF8B5CF6)
    )
) {
    val particles = remember {
        mutableStateListOf<ConfettiParticle>().apply {
            repeat(90) {
                add(
                    ConfettiParticle(
                        x = Random.nextFloat() * 1000f,
                        y = -Random.nextFloat() * 400f,
                        vx = (Random.nextFloat() - 0.5f) * 6f,
                        vy = 4f + Random.nextFloat() * 8f,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 12f,
                        width = 12f + Random.nextFloat() * 10f,
                        height = 8f + Random.nextFloat() * 6f,
                        color = colors[Random.nextInt(colors.size)]
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                particles.forEach { p ->
                    p.x += p.vx
                    p.y += p.vy
                    p.rotation += p.rotationSpeed
                    if (p.y > 2200f) {
                        p.y = -50f
                        p.x = Random.nextFloat() * 1000f
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            drawRect(
                color = p.color,
                topLeft = Offset(p.x, p.y),
                size = Size(p.width, p.height)
            )
        }
    }
}
