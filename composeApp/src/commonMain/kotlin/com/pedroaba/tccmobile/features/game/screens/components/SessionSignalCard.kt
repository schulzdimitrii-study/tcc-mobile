package com.pedroaba.tccmobile.features.game.screens.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.backend.model.GameStateResponse
import com.pedroaba.tccmobile.backend.online.RemoteSessionState
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import kotlin.math.roundToInt

@Composable
fun SessionSignalCard(
    telemetryState: TelemetryState,
    remoteSessionState: RemoteSessionState,
    remoteGameState: GameStateResponse? = remoteSessionState.gameState
) {
    val runnerVelocityLabel = remoteGameState
        ?.playerSpeed
        ?.let(::formatOneDecimal)
        ?: "--"
    val hordeVelocityLabel = remoteGameState
        ?.hordeSpeed
        ?.let(::formatOneDecimal)
        ?: "--"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatusRow("Session", telemetryState.session.status.name)
            StatusRow("Online Sync", remoteSessionState.status.name)
            StatusRow("Remote Session", remoteSessionState.sessionId ?: "--")
            StatusRow("Provider Speed", telemetryState.latestLocationPoint?.speedMetersPerSecond?.let(::formatOneDecimal) ?: "--")
            StatusRow("Raw Acceleration", telemetryState.latestAccelerationSample?.magnitudeMetersPerSecondSquared?.let(::formatOneDecimal) ?: "--")
            StatusRow("Runner Vel.", runnerVelocityLabel)
            StatusRow("Horde Vel.", hordeVelocityLabel)
            StatusRow("Remote Status", remoteGameState?.gameStatus?.name ?: "--")
            remoteSessionState.leaderboard?.let {
                StatusRow("Your Rank", "#${it.userRank}")
            }
        }
    }
}

private fun formatOneDecimal(value: Double): String {
    return ((value * 10.0).roundToInt() / 10.0).toString()
}
