package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Player
import com.example.ui.components.ConfettiEffect

@Composable
fun TournamentPodiumScreen(
    players: List<Player>,
    onReturnToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedPlayers = players.sortedByDescending { it.tournamentPoints }
    val champion = sortedPlayers.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF312E81))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        ConfettiEffect()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Text(
                    text = "🏆 TURNUVA ŞAMPİYONU! 🏆",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFBBF24),
                    textAlign = TextAlign.Center
                )
                if (champion != null) {
                    Text(
                        text = "${champion.name} Kupayı Kaldırdı!",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Podium Visual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd Place
                if (sortedPlayers.size > 1) {
                    PodiumColumn(
                        player = sortedPlayers[1],
                        place = "2.🥈",
                        podiumHeight = 130.dp,
                        podiumColor = Color(0xFF94A3B8)
                    )
                }

                // 1st Place (Champion)
                if (sortedPlayers.isNotEmpty()) {
                    PodiumColumn(
                        player = sortedPlayers[0],
                        place = "1.🥇",
                        podiumHeight = 180.dp,
                        podiumColor = Color(0xFFF59E0B)
                    )
                }

                // 3rd Place
                if (sortedPlayers.size > 2) {
                    PodiumColumn(
                        player = sortedPlayers[2],
                        place = "3.🥉",
                        podiumHeight = 90.dp,
                        podiumColor = Color(0xFFB45309)
                    )
                }
            }

            // Standings List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Turnuva Genel Sıralaması",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )

                sortedPlayers.forEachIndexed { idx, p ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (idx == 0) Color(0xFFFBBF24) else Color.White
                            )
                            Text(text = p.avatarEmoji, fontSize = 22.sp)
                            Text(
                                text = p.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.color
                            )
                        }
                        Text(
                            text = "${p.tournamentPoints} Puan",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }

            // Exit Button
            Button(
                onClick = onReturnToMenu,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_podium_menu"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                Icon(Icons.Default.Home, contentDescription = "Menü")
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "Ana Menüye Dön",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    player: Player,
    place: String,
    podiumHeight: androidx.compose.ui.unit.Dp,
    podiumColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 6.dp)
    ) {
        // Player Avatar
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(player.color)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = player.avatarEmoji, fontSize = 28.sp)
        }

        Text(
            text = player.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        // Block
        Box(
            modifier = Modifier
                .size(width = 84.dp, height = podiumHeight)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(podiumColor)
                .border(
                    2.dp,
                    Color.White.copy(alpha = 0.4f),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = place,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "${player.tournamentPoints} P",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
