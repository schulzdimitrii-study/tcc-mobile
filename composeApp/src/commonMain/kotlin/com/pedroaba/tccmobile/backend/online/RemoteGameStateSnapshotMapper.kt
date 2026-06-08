package com.pedroaba.tccmobile.backend.online

import com.pedroaba.tccmobile.backend.model.GameStateResponse
import com.pedroaba.tccmobile.backend.model.GameStatusDto
import com.pedroaba.tccmobile.game.models.GameSnapshot

private const val METERS_PER_KILOMETER = 1_000.0
private const val KILOMETERS_PER_HOUR_TO_METERS_PER_SECOND = 3.6
private const val FULL_PRESSURE_DISTANCE_KM = 0.5

fun GameStateResponse.toGameSnapshot(elapsedSeconds: Double = 0.0): GameSnapshot {
    val pressure = when (gameStatus) {
        GameStatusDto.CAUGHT -> 1.0
        GameStatusDto.ESCAPED -> 0.0
        GameStatusDto.RUNNING -> (1.0 - distancePlayerToHorde / FULL_PRESSURE_DISTANCE_KM)
            .coerceIn(0.0, 1.0)
    }

    return GameSnapshot(
        distance = (playerPosition * METERS_PER_KILOMETER).coerceAtLeast(0.0),
        hordePressure = pressure,
        risk = pressure,
        performanceScore = (raceProgress / 100.0).coerceIn(0.0, 1.0),
        runnerVelocity = (playerSpeed / KILOMETERS_PER_HOUR_TO_METERS_PER_SECOND).coerceAtLeast(0.0),
        hordeVelocity = (hordeSpeed / KILOMETERS_PER_HOUR_TO_METERS_PER_SECOND).coerceAtLeast(0.0),
        elapsedSeconds = elapsedSeconds,
        result = when (gameStatus) {
            GameStatusDto.RUNNING -> "running"
            GameStatusDto.CAUGHT -> "caught"
            GameStatusDto.ESCAPED -> "escaped"
        }
    )
}
