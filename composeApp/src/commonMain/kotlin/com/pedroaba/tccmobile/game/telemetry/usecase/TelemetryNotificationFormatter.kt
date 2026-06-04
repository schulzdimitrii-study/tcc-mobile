package com.pedroaba.tccmobile.game.telemetry.usecase

import com.pedroaba.tccmobile.game.telemetry.model.TelemetrySessionStatus
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import kotlin.math.roundToInt

data class TelemetryNotificationContent(
    val title: String,
    val body: String
)

class TelemetryNotificationFormatter {
    fun format(state: TelemetryState): TelemetryNotificationContent {
        val statusLabel = when (state.session.status) {
            TelemetrySessionStatus.RUNNING -> "running"
            TelemetrySessionStatus.PAUSED -> "paused"
            TelemetrySessionStatus.STOPPED -> "stopped"
            TelemetrySessionStatus.IDLE -> "idle"
        }

        val speed = state.latestLocationPoint?.speedMetersPerSecond?.let { oneDecimal(it) } ?: "0.0"
        val samples = state.session.sampleCount

        return TelemetryNotificationContent(
            title = "Session $statusLabel",
            body = "Provider speed $speed m/s • Raw samples $samples"
        )
    }

    private fun oneDecimal(value: Double): String {
        return (((value * 10.0).roundToInt()) / 10.0).toString()
    }
}
