package com.example.model

import androidx.compose.ui.graphics.Color

enum class CornerPosition {
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP_LEFT,
    TOP_RIGHT
}

data class Player(
    val id: Int,
    val name: String,
    val color: Color,
    val secondaryColor: Color,
    val avatarEmoji: String,
    val corner: CornerPosition,
    val isBot: Boolean = false,
    val score: Int = 0,
    val tournamentPoints: Int = 0
)

enum class GameType(
    val id: String,
    val titleTr: String,
    val titleEn: String,
    val descTr: String,
    val descEn: String,
    val iconEmoji: String,
    val primaryColor: Color
) {
    TANK(
        id = "tank",
        titleTr = "Tank Savaşı",
        titleEn = "Tank Battle",
        descTr = "Mermiler duvardan seker! Ayakta kalan son tank kazanır.",
        descEn = "Bullets ricochet off walls! Last tank standing wins.",
        iconEmoji = "💣",
        primaryColor = Color(0xFFEF4444)
    ),
    RACING(
        id = "racing",
        titleTr = "Sürat Pisti",
        titleEn = "Micro Racing",
        descTr = "Basılı tutarak virajı al! 3 turu ilk tamamlayan kazanır.",
        descEn = "Hold to steer into corners! First to complete 3 laps wins.",
        iconEmoji = "🏎️",
        primaryColor = Color(0xFF3B82F6)
    ),
    SUMO(
        id = "sumo",
        titleTr = "Bumper Sumo",
        titleEn = "Bumper Sumo",
        descTr = "Rakipleri platformdan dışarı it! Alan zamanla daralır.",
        descEn = "Push rivals off the ring! The platform shrinks over time.",
        iconEmoji = "🥊",
        primaryColor = Color(0xFFF59E0B)
    ),
    SOCCER(
        id = "soccer",
        titleTr = "Neon Futbol",
        titleEn = "Neon Soccer",
        descTr = "Topu rakip kalelere gönder! İlk 3 golü atan kazanır.",
        descEn = "Kick the glowing ball into goals! First to 3 goals wins.",
        iconEmoji = "⚽",
        primaryColor = Color(0xFF10B981)
    ),
    CROSSY(
        id = "crossy",
        titleTr = "Tavuk Geçidi",
        titleEn = "Crossy Chicken",
        descTr = "Trafiği ve nehirleri aş, karşı kıyıya ilk ulaşan ol!",
        descEn = "Dodge speedy traffic and rivers, reach the finish line first!",
        iconEmoji = "🐔",
        primaryColor = Color(0xFFEC4899)
    ),
    REFLEX(
        id = "reflex",
        titleTr = "Altın Kapmaca",
        titleEn = "Coin Rush",
        descTr = "Ortadaki altınları ilk sen kap, bombalardan kaçın!",
        descEn = "Grab coins as fast as you can, beware of dangerous bombs!",
        iconEmoji = "🪙",
        primaryColor = Color(0xFF8B5CF6)
    )
}

enum class GameMode {
    QUICK_PLAY,
    TOURNAMENT
}

data class TournamentState(
    val totalRounds: Int = 5,
    val currentRound: Int = 1,
    val selectedGames: List<GameType> = emptyList(),
    val isCompleted: Boolean = false
)

enum class AppLanguage {
    TR,
    EN
}
