package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.audio.SoundManager
import com.example.model.CornerPosition
import com.example.model.GameMode
import com.example.model.GameType
import com.example.model.Player
import com.example.ui.screens.GameHostScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.TournamentPodiumScreen
import com.example.ui.theme.MyApplicationTheme

enum class ScreenState {
    MENU,
    IN_GAME,
    TOURNAMENT_PODIUM
}

class MainActivity : ComponentActivity() {

    private lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        soundManager = SoundManager(this)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A)
                ) {
                    PartyGamesApp(soundManager = soundManager)
                }
            }
        }
    }
}

@Composable
fun PartyGamesApp(soundManager: SoundManager) {
    var screenState by remember { mutableStateOf(ScreenState.MENU) }
    var playerCount by remember { mutableIntStateOf(4) }
    var currentGameType by remember { mutableStateOf(GameType.TANK) }
    var currentGameMode by remember { mutableStateOf(GameMode.QUICK_PLAY) }

    // Tournament state
    var tournamentTotalRounds by remember { mutableIntStateOf(5) }
    var tournamentCurrentRoundIndex by remember { mutableIntStateOf(0) }
    val tournamentPlaylist = remember { mutableStateListOf<GameType>() }

    // 4 Default Players
    val allPlayers = remember {
        mutableStateListOf(
            Player(
                id = 1,
                name = "Kırmızı",
                color = Color(0xFFEF4444),
                secondaryColor = Color(0xFF991B1B),
                avatarEmoji = "🦊",
                corner = CornerPosition.BOTTOM_LEFT,
                isBot = false
            ),
            Player(
                id = 2,
                name = "Mavi",
                color = Color(0xFF3B82F6),
                secondaryColor = Color(0xFF1D4ED8),
                avatarEmoji = "🦈",
                corner = CornerPosition.BOTTOM_RIGHT,
                isBot = false
            ),
            Player(
                id = 3,
                name = "Yeşil",
                color = Color(0xFF10B981),
                secondaryColor = Color(0xFF065F46),
                avatarEmoji = "🦖",
                corner = CornerPosition.TOP_LEFT,
                isBot = false
            ),
            Player(
                id = 4,
                name = "Sarı",
                color = Color(0xFFF59E0B),
                secondaryColor = Color(0xFFB45309),
                avatarEmoji = "⚡",
                corner = CornerPosition.TOP_RIGHT,
                isBot = false
            )
        )
    }

    // Active players subset
    val activePlayers = remember(playerCount, allPlayers.toList()) {
        allPlayers.take(playerCount)
    }

    // Back handling
    BackHandler(enabled = screenState != ScreenState.MENU) {
        soundManager.playClick()
        screenState = ScreenState.MENU
    }

    when (screenState) {
        ScreenState.MENU -> {
            MainMenuScreen(
                players = allPlayers,
                playerCount = playerCount,
                soundManager = soundManager,
                onPlayerCountChanged = { newCount ->
                    playerCount = newCount
                },
                onStartQuickPlay = { gameType ->
                    currentGameType = gameType
                    currentGameMode = GameMode.QUICK_PLAY
                    // Reset single match scores
                    for (i in 0 until allPlayers.size) {
                        allPlayers[i] = allPlayers[i].copy(score = 0)
                    }
                    screenState = ScreenState.IN_GAME
                },
                onStartTournament = { totalRounds ->
                    tournamentTotalRounds = totalRounds
                    tournamentCurrentRoundIndex = 1
                    currentGameMode = GameMode.TOURNAMENT

                    // Reset tournament points
                    for (i in 0 until allPlayers.size) {
                        allPlayers[i] = allPlayers[i].copy(score = 0, tournamentPoints = 0)
                    }

                    // Create randomized game playlist
                    tournamentPlaylist.clear()
                    val available = GameType.values().toList()
                    repeat(totalRounds) {
                        tournamentPlaylist.add(available.random())
                    }

                    currentGameType = tournamentPlaylist.firstOrNull() ?: GameType.TANK
                    screenState = ScreenState.IN_GAME
                },
                onUpdatePlayerBot = { playerId, isBot ->
                    val idx = allPlayers.indexOfFirst { it.id == playerId }
                    if (idx != -1) {
                        allPlayers[idx] = allPlayers[idx].copy(isBot = isBot)
                    }
                }
            )
        }

        ScreenState.IN_GAME -> {
            val roundTitle = if (currentGameMode == GameMode.TOURNAMENT) {
                "Tur $tournamentCurrentRoundIndex / $tournamentTotalRounds"
            } else null

            GameHostScreen(
                gameType = currentGameType,
                gameMode = currentGameMode,
                players = activePlayers,
                soundManager = soundManager,
                roundTitle = roundTitle,
                onRoundCompleted = { winner ->
                    if (currentGameMode == GameMode.TOURNAMENT) {
                        // Award tournament points (Winner gets 3, others 1 or 0)
                        if (winner != null) {
                            val wIdx = allPlayers.indexOfFirst { it.id == winner.id }
                            if (wIdx != -1) {
                                allPlayers[wIdx] = allPlayers[wIdx].copy(
                                    tournamentPoints = allPlayers[wIdx].tournamentPoints + 3,
                                    score = allPlayers[wIdx].score + 1
                                )
                            }
                        }

                        if (tournamentCurrentRoundIndex >= tournamentTotalRounds) {
                            // Tournament finished! Show podium
                            screenState = ScreenState.TOURNAMENT_PODIUM
                        } else {
                            // Next round
                            tournamentCurrentRoundIndex++
                            currentGameType = tournamentPlaylist.getOrElse(tournamentCurrentRoundIndex - 1) {
                                GameType.values().random()
                            }
                        }
                    } else {
                        // Quick play rematch or score bump
                        if (winner != null) {
                            val wIdx = allPlayers.indexOfFirst { it.id == winner.id }
                            if (wIdx != -1) {
                                allPlayers[wIdx] = allPlayers[wIdx].copy(score = allPlayers[wIdx].score + 1)
                            }
                        }
                    }
                },
                onExitToMenu = {
                    soundManager.playClick()
                    screenState = ScreenState.MENU
                }
            )
        }

        ScreenState.TOURNAMENT_PODIUM -> {
            TournamentPodiumScreen(
                players = activePlayers,
                onReturnToMenu = {
                    soundManager.playClick()
                    screenState = ScreenState.MENU
                }
            )
        }
    }
}
