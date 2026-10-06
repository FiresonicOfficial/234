package com.example

import androidx.compose.ui.graphics.Color
import com.example.model.CornerPosition
import com.example.model.GameMode
import com.example.model.GameType
import com.example.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGameTypesAvailable() {
        val games = GameType.values()
        assertEquals(6, games.size)
        assertTrue(games.any { it.id == "tank" })
        assertTrue(games.any { it.id == "racing" })
        assertTrue(games.any { it.id == "sumo" })
        assertTrue(games.any { it.id == "soccer" })
        assertTrue(games.any { it.id == "crossy" })
        assertTrue(games.any { it.id == "reflex" })
    }

    @Test
    fun testPlayerInitialization() {
        val player1 = Player(
            id = 1,
            name = "Kırmızı",
            color = Color(0xFFEF4444),
            secondaryColor = Color(0xFF991B1B),
            avatarEmoji = "🦊",
            corner = CornerPosition.BOTTOM_LEFT,
            isBot = false,
            score = 0,
            tournamentPoints = 0
        )
        assertEquals(1, player1.id)
        assertFalse(player1.isBot)
        assertEquals("Kırmızı", player1.name)
        assertEquals(CornerPosition.BOTTOM_LEFT, player1.corner)
    }

    @Test
    fun testTournamentScoring() {
        var player = Player(
            id = 1,
            name = "Kırmızı",
            color = Color(0xFFEF4444),
            secondaryColor = Color(0xFF991B1B),
            avatarEmoji = "🦊",
            corner = CornerPosition.BOTTOM_LEFT
        )
        // Award win points
        player = player.copy(tournamentPoints = player.tournamentPoints + 3, score = player.score + 1)
        assertEquals(3, player.tournamentPoints)
        assertEquals(1, player.score)
    }
}
