package com.pedroaba.tccmobile.game.telemetry.model

data class TelemetryAvailability(
    val hasLocationPermission: Boolean = false,
    val isLocationEnabled: Boolean = false,
    val hasMotionSensor: Boolean = false,
    val hasWatch: Boolean = false,
    val issues: Set<TelemetryIssue> = emptySet()
)

data class TelemetryState(
    val session: MovementSession = MovementSession(),
    val latestLocationPoint: LocationPoint? = null,
    val latestAccelerationSample: AccelerationSample? = null,
    val latestBiofeedbackSample: BiofeedbackSample? = null,
    val strategy: TelemetryStrategy = TelemetryStrategy.MOVEMENT_ONLY,
    val availability: TelemetryAvailability = TelemetryAvailability()
)
