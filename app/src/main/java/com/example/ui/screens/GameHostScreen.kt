package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.audio.SoundManager
import com.example.games.crossy.CrossyRoadGame
import com.example.games.racing.MicroRacingGame
import com.example.games.reflex.CoinReflexGame
import com.example.games.soccer.NeonSoccerGame
import com.example.games.sumo.BumperSumoGame
import com.example.games.tank.TankBattleGame
import com.example.model.GameMode
import com.example.model.GameType
import com.example.model.Player
import com.example.ui.components.CornerControlsOverlay
import com.example.ui.components.GameOverOverlay
import com.example.ui.components.GameScoreHeader
import com.example.ui.components.PauseMenuOverlay
import kotlinx.coroutines.delay

@Composable
fun GameHostScreen(
    gameType: GameType,
    gameMode: GameMode,
    players: List<Player>,
    soundManager: SoundManager,
    roundTitle: String? = null,
    onRoundCompleted: (winner: Player?) -> Unit,
    onExitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    var countdownValue by remember { mutableIntStateOf(3) }
    var isCountingDown by remember { mutableStateOf(true) }
    var isPaused by remember { mutableStateOf(false) }
    var winnerPlayer by remember { mutableStateOf<Player?>(null) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameInstanceKey by remember { mutableIntStateOf(0) }

    // Multi-touch press states for each player
    val playerPressStates = remember {
        mutableStateMapOf<Int, Boolean>().apply {
            players.forEach { put(it.id, false) }
        }
    }

    // Countdown sequence
    LaunchedEffect(gameInstanceKey) {
        countdownValue = 3
        isCountingDown = true
        isGameOver = false
        winnerPlayer = null

        soundManager.playCountdown(false)
        delay(800)
        countdownValue = 2
        soundManager.playCountdown(false)
        delay(800)
        countdownValue = 1
        soundManager.playCountdown(false)
        delay(800)
        countdownValue = 0 // "BAŞLA!"
        soundManager.playCountdown(true)
        delay(500)
        isCountingDown = false
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF020617))) {
        // Active Mini Game Canvas
        key(gameInstanceKey) {
            when (gameType) {
                GameType.TANK -> TankBattleGame(
                    players = players,
                    playerPressStates = playerPressStates,
                    isPaused = isPaused || isCountingDown,
                    soundManager = soundManager,
                    onGameOver = { winner ->
                        winnerPlayer = winner
                        isGameOver = true
                    }
                )
                GameType.RACING -> MicroRacingGame(
                    players = players,
                    playerPressStates = playerPressStates,
                    isPaused = isPaused || isCountingDown,
                    soundManager = soundManager,
                    onGameOver = { winner ->
                        winnerPlayer = winner
                        isGameOver = true
                    }
                )
                GameType.SUMO -> BumperSumoGame(
                    players = players,
                    playerPressStates = playerPressStates,
                    isPaused = isPaused || isCountingDown,
                    soundManager = soundManager,
                    onGameOver = { winner ->
                        winnerPlayer = winner
                        isGameOver = true
                    }
                )
                GameType.SOCCER -> NeonSoccerGame(
                    players = players,
                    playerPressStates = playerPressStates,
                    isPaused = isPaused || isCountingDown,
                    soundManager = soundManager,
                    onGameOver = { winner ->
                        winnerPlayer = winner
                        isGameOver = true
                    }
                )
                GameType.CROSSY -> CrossyRoadGame(
                    players = players,
                    playerPressStates = playerPressStates,
                    isPaused = isPaused || isCountingDown,
                    soundManager = soundManager,
                    onGameOver = { winner ->
                        winnerPlayer = winner
                        isGameOver = true
                    }
                )
                GameType.REFLEX -> CoinReflexGame(
                    players = players,
                    playerPressStates = playerPressStates,
                    isPaused = isPaused || isCountingDown,
                    soundManager = soundManager,
                    onGameOver = { winner ->
                        winnerPlayer = winner
                        isGameOver = true
                    }
                )
            }
        }

        // Corner Player Action Buttons
        CornerControlsOverlay(
            players = players,
            playerPressStates = playerPressStates,
            onPlayerPressChange = { playerId, pressed ->
                playerPressStates[playerId] = pressed
            }
        )

        // Top Header
        GameScoreHeader(
            gameType = gameType,
            players = players,
            liveScores = players.associate { it.id to it.score },
            onPauseClick = { isPaused = true },
            onRestartClick = { gameInstanceKey++ },
            roundTitle = roundTitle,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Countdown Overlay
        if (isCountingDown) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (countdownValue > 0) "$countdownValue" else "BAŞLA! 🔥",
                    fontSize = if (countdownValue > 0) 90.sp else 60.sp,
                    fontWeight = FontWeight.Black,
                    color = if (countdownValue > 0) Color.White else Color(0xFFFBBF24)
                )
            }
        }

        // Pause Menu
        if (isPaused) {
            PauseMenuOverlay(
                onResume = { isPaused = false },
                onRestart = {
                    isPaused = false
                    gameInstanceKey++
                },
                onExitToMenu = onExitToMenu
            )
        }

        // Game Over Overlay
        if (isGameOver) {
            GameOverOverlay(
                winner = winnerPlayer,
                players = players,
                isTournament = gameMode == GameMode.TOURNAMENT,
                onRematch = {
                    isGameOver = false
                    gameInstanceKey++
                },
                onNextTournamentRound = {
                    isGameOver = false
                    onRoundCompleted(winnerPlayer)
                },
                onExitToMenu = onExitToMenu
            )
        }
    }
}
