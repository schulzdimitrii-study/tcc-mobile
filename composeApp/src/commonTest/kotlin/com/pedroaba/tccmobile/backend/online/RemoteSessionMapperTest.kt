package com.pedroaba.tccmobile.backend.online

import com.pedroaba.tccmobile.backend.model.BiometricDataMessage
import com.pedroaba.tccmobile.game.telemetry.model.BiofeedbackSample
import com.pedroaba.tccmobile.game.telemetry.model.LocationPoint
import com.pedroaba.tccmobile.game.telemetry.model.MovementSession
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemoteSessionMapperTest {

    @Test
    fun `maps telemetry into backend biometric payload`() {
        val telemetryState = TelemetryState(
            session = MovementSession(totalDistanceMeters = 420.0),
            latestLocationPoint = LocationPoint(
                timestampMs = 5_000L,
                latitude = -22.0,
                longitude = -45.0,
                speedMetersPerSecond = 3.4
            ),
            latestBiofeedbackSample = BiofeedbackSample(
                timestampMs = 5_200L,
                bpm = 148
            )
        )

        val message = buildBiometricDataMessage(
            sessionId = "session-123",
            userId = "user-456",
            telemetryState = telemetryState,
            timestampMs = 1L
        )

        assertEquals(
            BiometricDataMessage(
                sessionId = "session-123",
                userId = "user-456",
                timestamp = 5_200L,
                bpm = 148,
                cadence = 0.0,
                speed = 12.24,
                pace = 4.9,
                accumulatedDistance = 0.42,
                accumulatedCalories = 0.0
            ),
            message
        )
    }

    @Test
    fun `maps latency metadata into backend biometric payload`() {
        val telemetryState = TelemetryState(
            session = MovementSession(totalDistanceMeters = 420.0),
            latestBiofeedbackSample = BiofeedbackSample(
                timestampMs = 5_200L,
                bpm = 148
            )
        )

        val message = buildBiometricDataMessage(
            sessionId = "session-123",
            userId = "user-456",
            telemetryState = telemetryState,
            timestampMs = 1L,
            latencyTraceId = "trace-789",
            clientSentAtElapsedMs = 12_345L
        )

        assertEquals("trace-789", message?.latencyTraceId)
        assertEquals(12_345L, message?.clientSentAtElapsedMs)
    }

    @Test
    fun `returns null when there is no signal`() {
        val message = buildBiometricDataMessage(
            sessionId = "session-123",
            userId = "user-456",
            telemetryState = TelemetryState(),
            timestampMs = 1L
        )

        assertNull(message)
    }
}
