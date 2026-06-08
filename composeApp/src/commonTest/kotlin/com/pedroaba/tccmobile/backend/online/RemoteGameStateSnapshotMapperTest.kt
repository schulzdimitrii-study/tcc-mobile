package com.pedroaba.tccmobile.backend.online

import com.pedroaba.tccmobile.backend.model.GameStateResponse
import com.pedroaba.tccmobile.backend.model.GameStatusDto
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteGameStateSnapshotMapperTest {

    @Test
    fun `maps backend game state units into local game snapshot`() {
        val gameState = GameStateResponse(
            sessionId = "session-1",
            userId = "user-1",
            playerPosition = 0.45,
            hordePosition = 0.30,
            distanceToGoal = 0.55,
            distancePlayerToHorde = 0.15,
            playerSpeed = 10.8,
            hordeSpeed = 8.4,
            raceProgress = 45.0,
            gameStatus = GameStatusDto.RUNNING
        )

        val snapshot = gameState.toGameSnapshot(elapsedSeconds = 12.0)

        assertEquals(450.0, snapshot.distance, absoluteTolerance = 0.0001)
        assertEquals(3.0, snapshot.runnerVelocity, absoluteTolerance = 0.0001)
        assertEquals(2.3333, snapshot.hordeVelocity, absoluteTolerance = 0.0001)
        assertEquals(0.45, snapshot.performanceScore, absoluteTolerance = 0.0001)
        assertEquals(0.7, snapshot.hordePressure, absoluteTolerance = 0.0001)
        assertEquals(0.7, snapshot.risk, absoluteTolerance = 0.0001)
        assertEquals(12.0, snapshot.elapsedSeconds, absoluteTolerance = 0.0001)
        assertEquals("running", snapshot.result)
    }

    @Test
    fun `maps terminal backend game statuses`() {
        val caught = gameState(GameStatusDto.CAUGHT).toGameSnapshot()
        val escaped = gameState(GameStatusDto.ESCAPED).toGameSnapshot()

        assertEquals("caught", caught.result)
        assertEquals("escaped", escaped.result)
    }

    private fun gameState(status: GameStatusDto): GameStateResponse = GameStateResponse(
        sessionId = "session-1",
        userId = "user-1",
        playerPosition = 1.0,
        hordePosition = 1.0,
        distanceToGoal = 0.0,
        distancePlayerToHorde = 0.0,
        playerSpeed = 0.0,
        hordeSpeed = 0.0,
        raceProgress = 100.0,
        gameStatus = status
    )
}
