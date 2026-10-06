package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.audio.SoundManager
import com.example.model.GameMode
import com.example.model.GameType
import com.example.model.Player

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    players: List<Player>,
    playerCount: Int,
    soundManager: SoundManager,
    onPlayerCountChanged: (Int) -> Unit,
    onStartQuickPlay: (GameType) -> Unit,
    onStartTournament: (totalRounds: Int) -> Unit,
    onUpdatePlayerBot: (playerId: Int, isBot: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSettingsSheet by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(soundManager.isSoundEnabled()) }
    var hapticEnabled by remember { mutableStateOf(soundManager.isHapticEnabled()) }
    var tournamentRounds by remember { mutableIntStateOf(5) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Quick Play, 1: Tournament

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E1B4B), Color(0xFF0F172A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val next = !soundEnabled
                            soundEnabled = next
                            soundManager.setSoundEnabled(next)
                            if (next) soundManager.playClick()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("btn_toggle_sound")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Ses",
                            tint = if (soundEnabled) Color(0xFFFBBF24) else Color.Gray
                        )
                    }

                    IconButton(
                        onClick = {
                            val next = !hapticEnabled
                            hapticEnabled = next
                            soundManager.setHapticEnabled(next)
                            if (next) soundManager.vibrate(50)
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .testTag("btn_toggle_vibration")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Vibration,
                            contentDescription = "Titreşim",
                            tint = if (hapticEnabled) Color(0xFF38BDF8) else Color.Gray
                        )
                    }
                }

                IconButton(
                    onClick = {
                        soundManager.playClick()
                        showSettingsSheet = true
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .testTag("btn_open_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ayarlar",
                        tint = Color.White
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title Banner
            Text(
                text = "🎮 2 3 4 OYUNCU 🎮",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFBBF24),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Aynı Cihazda Çok Oyunculu Parti!",
                fontSize = 13.sp,
                color = Color.LightGray,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(14.dp))

            // Player Count Selector (2, 3, 4)
            Text(
                text = "OYUNCU SAYISI SEÇ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E293B))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(2, 3, 4).forEach { count ->
                    val isSelected = playerCount == count
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFF3B82F6) else Color.Transparent,
                        label = "tab_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable {
                                soundManager.playClick()
                                onPlayerCountChanged(count)
                            }
                            .padding(vertical = 10.dp)
                            .testTag("btn_player_count_$count"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$count Kişi",
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.LightGray
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Mode Tabs: [ Hızlı Oyun ] [ Turnuva ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) Color(0xFF10B981) else Color.Transparent)
                        .clickable {
                            soundManager.playClick()
                            selectedTab = 0
                        }
                        .padding(vertical = 8.dp)
                        .testTag("tab_quick_play"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Hızlı Oyun",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) Color(0xFFF59E0B) else Color.Transparent)
                        .clickable {
                            soundManager.playClick()
                            selectedTab = 1
                        }
                        .padding(vertical = 8.dp)
                        .testTag("tab_tournament"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Turnuva Modu",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Tab Content
            if (selectedTab == 0) {
                // Mini Games Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(GameType.values()) { game ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    soundManager.playClick()
                                    onStartQuickPlay(game)
                                }
                                .testTag("card_game_${game.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1E293B)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, game.primaryColor.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = game.iconEmoji, fontSize = 28.sp)
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(game.primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Oyna",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                Text(
                                    text = game.titleTr,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = game.descTr,
                                    fontSize = 11.sp,
                                    color = Color.LightGray,
                                    maxLines = 2,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                // Tournament Setup
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.5.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🏆 BÜYÜK KUPA TURNUVASI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Rastgele mini oyunlar oynanır, puanlar toplanır ve şampiyon kupa kürsüsüne çıkar!",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Toplam Tur Sayısı",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf(3, 5, 7).forEach { rounds ->
                                val isSelected = tournamentRounds == rounds
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFFF59E0B) else Color(0xFF0F172A))
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            soundManager.playClick()
                                            tournamentRounds = rounds
                                        }
                                        .padding(horizontal = 20.dp, vertical = 10.dp)
                                        .testTag("btn_tournament_rounds_$rounds"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$rounds Tur",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            soundManager.playWinFanfare()
                            onStartTournament(tournamentRounds)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_start_tournament"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                    ) {
                        Text(
                            text = "Turnuvayı Başlat! 🏆",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Settings / Players & Bots BottomSheet
        if (showSettingsSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                sheetState = sheetState,
                containerColor = Color(0xFF1E1B4B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "⚙️ Oyuncular & Bot Ayarları",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    Text(
                        text = "İstediğin oyuncu yuvasını bilgisayar (Bot / CPU) yapabilirsin. Tek başına veya 2 kişi botlara karşı oynamak için idealdir!",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        players.take(playerCount).forEach { player ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(player.color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = player.avatarEmoji, fontSize = 20.sp)
                                    }
                                    Column {
                                        Text(
                                            text = player.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (player.isBot) "Bilgisayar (Bot)" else "İnsan Oyuncu",
                                            fontSize = 11.sp,
                                            color = if (player.isBot) Color(0xFFFBBF24) else Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "BOT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (player.isBot) Color.Yellow else Color.Gray,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Switch(
                                        checked = player.isBot,
                                        onCheckedChange = { isBot ->
                                            soundManager.playClick()
                                            onUpdatePlayerBot(player.id, isBot)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = Color(0xFF10B981)
                                        ),
                                        modifier = Modifier.testTag("switch_bot_${player.id}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
