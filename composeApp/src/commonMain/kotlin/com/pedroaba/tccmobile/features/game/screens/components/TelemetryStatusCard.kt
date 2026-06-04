package com.pedroaba.tccmobile.features.game.screens.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.backend.online.RemoteSessionState
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import kotlin.math.roundToInt

@Composable
fun TelemetryStatusCard(
    telemetryState: TelemetryState,
    remoteSessionState: RemoteSessionState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "MOVEMENT METRICS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val location = telemetryState.latestLocationPoint
            val acceleration = telemetryState.latestAccelerationSample
            val gameState = remoteSessionState.gameState

            StatusRow("Raw GPS", location?.provider ?: "--")
            StatusRow("GPS Accuracy", location?.accuracyMeters?.let { "${formatOneDecimal(it)} m" } ?: "--")
            StatusRow("Provider Speed", location?.speedMetersPerSecond?.let(::formatOneDecimal) ?: "--")
            StatusRow("Raw Acceleration", acceleration?.magnitudeMetersPerSecondSquared?.let(::formatOneDecimal) ?: "--")
            StatusRow("Backend Distance", gameState?.playerPosition?.let { "${formatOneDecimal(it)} km" } ?: "--")
            StatusRow("Backend Speed", gameState?.playerSpeed?.let(::formatOneDecimal) ?: "--")
            StatusRow(
                "Horde Distance",
                gameState?.hordePosition?.let { "${formatOneDecimal(it)} km" } ?: "--"
            )

            val issuesLabel = telemetryState.availability.issues
                .filterNot { it.name == "WATCH_UNAVAILABLE" }
                .joinToString()
                .ifEmpty { "none" }

            Spacer(modifier = Modifier.height(8.dp))
            StatusRow("Issues", issuesLabel)
        }
    }
}

private fun formatOneDecimal(value: Double): String {
    return ((value * 10.0).roundToInt() / 10.0).toString()
}
