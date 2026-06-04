package com.pedroaba.tccmobile.backend.online

import com.pedroaba.tccmobile.backend.model.BiometricDataMessage
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import kotlin.math.round

fun buildBiometricDataMessage(
    sessionId: String,
    userId: String,
    telemetryState: TelemetryState,
    timestampMs: Long
): BiometricDataMessage? {
    val location = telemetryState.latestLocationPoint
    val acceleration = telemetryState.latestAccelerationSample
    val biofeedback = telemetryState.latestBiofeedbackSample

    if (location == null && acceleration == null && biofeedback?.bpm == null) return null

    val speedKmH = roundToTwoDecimals((location?.speedMetersPerSecond ?: 0.0) * 3.6)
    val pace = if (speedKmH > 0.0) roundToOneDecimal(60.0 / speedKmH) else 0.0

    return BiometricDataMessage(
        sessionId = sessionId,
        userId = userId,
        timestamp = maxOf(
            timestampMs,
            location?.timestampMs ?: 0L,
            acceleration?.timestampMs ?: 0L,
            biofeedback?.timestampMs ?: 0L
        ),
        bpm = biofeedback?.bpm?.coerceIn(1, 220) ?: 1,
        cadence = 0.0,
        speed = speedKmH,
        pace = pace,
        accumulatedDistance = roundToFourDecimals(telemetryState.session.totalDistanceMeters / 1_000.0),
        accumulatedCalories = 0.0
    )
}

private fun roundToOneDecimal(value: Double): Double = round(value * 10.0) / 10.0

private fun roundToTwoDecimals(value: Double): Double = round(value * 100.0) / 100.0

private fun roundToFourDecimals(value: Double): Double = round(value * 10_000.0) / 10_000.0
