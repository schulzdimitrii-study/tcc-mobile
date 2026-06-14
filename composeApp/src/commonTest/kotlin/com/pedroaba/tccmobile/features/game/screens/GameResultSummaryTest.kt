package com.pedroaba.tccmobile.features.game.screens

import com.pedroaba.tccmobile.backend.model.GameStateResponse
import com.pedroaba.tccmobile.backend.model.GameStatusDto
import com.pedroaba.tccmobile.game.models.GameSnapshot
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import kotlin.test.Test
import kotlin.test.assertEquals

class GameResultSummaryTest {

    @Test
    fun `builds victory summary from local snapshot`() {
        val summary = buildGameResultSummary(
            snapshot = GameSnapshot(
                distance = 1.234,
                hordePressure = 0.27,
                risk = 0.18,
                performanceScore = 0.82,
                runnerVelocity = 3.2,
                hordeVelocity = 2.7,
                elapsedSeconds = 125.0,
                result = "escaped"
            ),
            telemetryState = TelemetryState(),
            remoteGameState = null
        )

        assertEquals("Vitória", summary.title)
        assertEquals("Você escapou da horda.", summary.subtitle)
        assertEquals("2min 05s", summary.stats[0].value)
        assertEquals("1.23 km", summary.stats[1].value)
        assertEquals("82%", summary.stats[2].value)
        assertEquals("18%", summary.stats[3].value)
    }

    @Test
    fun `prefers remote race values when available`() {
        val summary = buildGameResultSummary(
            snapshot = GameSnapshot(
                distance = 0.9,
                performanceScore = 0.5,
                runnerVelocity = 1.5,
                hordeVelocity = 1.4,
                elapsedSeconds = 60.0,
                result = "escaped"
            ),
            telemetryState = TelemetryState(),
            remoteGameState = GameStateResponse(
                sessionId = "session",
                userId = "user",
                playerPosition = 2.5,
                hordePosition = 1.9,
                distanceToGoal = 0.0,
                distancePlayerToHorde = 0.6,
                playerSpeed = 3.4,
                hordeSpeed = 3.0,
                raceProgress = 100.0,
                gameStatus = GameStatusDto.ESCAPED
            )
        )

        assertEquals("2.50 km", summary.stats[1].value)
        assertEquals("100%", summary.stats[2].value)
        assertEquals("0.60 km", summary.details[0].value)
        assertEquals("3.40 m/s", summary.details[1].value)
    }
}
