package com.pedroaba.tccmobile.game.telemetry.usecase

import com.pedroaba.tccmobile.game.telemetry.model.MovementSession
import com.pedroaba.tccmobile.game.telemetry.model.LocationPoint
import com.pedroaba.tccmobile.game.telemetry.model.TelemetrySessionStatus
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import kotlin.test.Test
import kotlin.test.assertEquals

class TelemetryNotificationFormatterTest {
    private val formatter = TelemetryNotificationFormatter()

    @Test
    fun formatsForegroundNotificationContentFromTelemetryState() {
        val content = formatter.format(
            TelemetryState(
                session = MovementSession(
                    status = TelemetrySessionStatus.RUNNING,
                    sampleCount = 12
                ),
                latestLocationPoint = LocationPoint(
                    timestampMs = 1_000L,
                    latitude = 0.0,
                    longitude = 0.0,
                    speedMetersPerSecond = 2.75
                )
            )
        )

        assertEquals("Session running", content.title)
        assertEquals("Provider speed 2.8 m/s • Raw samples 12", content.body)
    }
}
