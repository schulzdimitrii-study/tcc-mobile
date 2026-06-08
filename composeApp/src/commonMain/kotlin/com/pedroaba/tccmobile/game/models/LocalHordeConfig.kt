package com.pedroaba.tccmobile.game.models

import kotlin.math.round

enum class LocalHordeDifficulty {
    EASY,
    MEDIUM,
    HARD
}

data class LocalHordeConfig(
    val distanceKm: Double = 1.0,
    val difficulty: LocalHordeDifficulty = LocalHordeDifficulty.MEDIUM
) {
    fun toSessionConfig(): SessionConfig {
        val baseConfig = SessionConfig()
        val difficultyMultiplier = when (difficulty) {
            LocalHordeDifficulty.EASY -> 0.85
            LocalHordeDifficulty.MEDIUM -> 1.0
            LocalHordeDifficulty.HARD -> 1.2
        }

        return baseConfig.copy(
            goalDistance = distanceKm.coerceAtLeast(0.0) * 1_000.0,
            chaseRatePerSecond = roundToTwoDecimals(baseConfig.chaseRatePerSecond * difficultyMultiplier)
        )
    }
}

private fun roundToTwoDecimals(value: Double): Double = round(value * 100.0) / 100.0
