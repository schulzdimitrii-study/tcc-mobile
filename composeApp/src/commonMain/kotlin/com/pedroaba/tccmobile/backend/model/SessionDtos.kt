package com.pedroaba.tccmobile.backend.model

import kotlinx.serialization.Serializable

@Serializable
data class StartSessionRequest(
    val hordeId: String? = null,
    val trainType: String = "RUN"
)

@Serializable
data class StartSessionResponse(
    val sessionId: String
)

@Serializable
data class BiometricDataMessage(
    val sessionId: String,
    val userId: String,
    val timestamp: Long,
    val bpm: Int,
    val cadence: Double,
    val speed: Double,
    val pace: Double,
    val accumulatedDistance: Double,
    val accumulatedCalories: Double
)

@Serializable
data class LeaderboardEntryDto(
    val userId: String,
    val rank: Int,
    val distanceKm: Double
)

@Serializable
data class LeaderboardResponse(
    val sessionId: String,
    val userRank: Int,
    val hordeVirtualDistanceKm: Double?,
    val entries: List<LeaderboardEntryDto>
)
