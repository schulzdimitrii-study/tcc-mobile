package com.pedroaba.tccmobile.backend.online

import com.pedroaba.tccmobile.backend.model.LeaderboardEntryDto
import com.pedroaba.tccmobile.backend.model.LeaderboardResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteSessionStateTest {

    @Test
    fun `session start success stores session id and clears previous error`() {
        val current = RemoteSessionState(
            status = RemoteSessionStatus.ERROR,
            errorMessage = "Falha anterior"
        )

        val updated = current.onSessionStarted("session-1")

        assertEquals(RemoteSessionStatus.CONNECTING, updated.status)
        assertEquals("session-1", updated.sessionId)
        assertEquals(null, updated.errorMessage)
    }

    @Test
    fun `session end failure preserves session for retry`() {
        val current = RemoteSessionState(
            sessionId = "session-1",
            status = RemoteSessionStatus.ACTIVE
        )

        val updated = current.onSessionEndFailed("Nao foi possivel encerrar")

        assertEquals(RemoteSessionStatus.ERROR, updated.status)
        assertEquals("session-1", updated.sessionId)
        assertEquals("Nao foi possivel encerrar", updated.errorMessage)
    }

    @Test
    fun `leaderboard update keeps active session data`() {
        val leaderboard = LeaderboardResponse(
            sessionId = "session-1",
            userRank = 2,
            hordeVirtualDistanceKm = 0.75,
            entries = listOf(
                LeaderboardEntryDto(userId = "u1", rank = 1, distanceKm = 1.2),
                LeaderboardEntryDto(userId = "u2", rank = 2, distanceKm = 1.0)
            )
        )

        val updated = RemoteSessionState(
            sessionId = "session-1",
            status = RemoteSessionStatus.CONNECTING
        ).onLeaderboardUpdated(leaderboard)

        assertEquals(RemoteSessionStatus.ACTIVE, updated.status)
        assertEquals(leaderboard, updated.leaderboard)
    }
}
