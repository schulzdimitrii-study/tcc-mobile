package com.pedroaba.tccmobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.pedroaba.tccmobile.game.GameController
import com.pedroaba.tccmobile.game.KorgeGameView
import com.pedroaba.tccmobile.game.debug.GameDebugLogger
import com.pedroaba.tccmobile.game.models.SessionConfig
import com.pedroaba.tccmobile.game.telemetry.model.TelemetrySessionStatus
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

@Composable
@Preview
fun App(
    telemetryStateFlow: StateFlow<TelemetryState>? = null,
    hasLocationPermission: Boolean = false,
    currentTimeMsProvider: () -> Long = { 0L },
    onRequestLocationPermission: (() -> Unit)? = null,
    onStartTelemetrySession: (() -> Unit)? = null,
    onPauseTelemetrySession: (() -> Unit)? = null,
    onResumeTelemetrySession: (() -> Unit)? = null,
    onStopTelemetrySession: (() -> Unit)? = null
) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onSurface = Color.White
        )
    ) {
        val gameController = remember { GameController(timeProviderMs = currentTimeMsProvider) }
        val snapshot by gameController.snapshot.collectAsState()
        val isSceneLoading by gameController.isSceneLoading.collectAsState()
        val lastEscapeMetrics by gameController.lastEscapeMetrics.collectAsState()
        val isActive by gameController.isActive.collectAsState()
        val telemetryState = telemetryStateFlow?.collectAsState()?.value ?: TelemetryState()

        LaunchedEffect(telemetryState.latestEscapeMetrics) {
            telemetryState.latestEscapeMetrics?.let(gameController::applyEscapeMetrics)
        }

        LaunchedEffect(telemetryState.session.status) {
            when (telemetryState.session.status) {
                TelemetrySessionStatus.RUNNING -> {
                    if (!isActive) {
                        gameController.startSession(gameSessionConfig)
                    }
                }
                TelemetrySessionStatus.IDLE,
                TelemetrySessionStatus.STOPPED -> {
                    if (isActive) {
                        gameController.stopSession()
                    }
                }
                TelemetrySessionStatus.PAUSED -> Unit
            }
        }

        LaunchedEffect(snapshot.result, isActive, telemetryState.session.status) {
            val telemetryRunning = telemetryState.session.status == TelemetrySessionStatus.RUNNING ||
                telemetryState.session.status == TelemetrySessionStatus.PAUSED

            if (!isActive && telemetryRunning && (snapshot.result == "escaped" || snapshot.result == "caught")) {
                onStopTelemetrySession?.invoke()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Game View Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black)
                ) {
                    KorgeGameView(
                        controller = gameController,
                        isActive = isActive,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isSceneLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xCC121212)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Carregando sprites...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Debug & Status Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "SESSION STATUS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    StatusDisplay(snapshot = snapshot)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "GAME SESSION",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isSceneLoading) {
                                GameDebugLogger.log(
                                    "start-button",
                                )

                                return@Button
                            }

                            if (isActive) {
                                onStopTelemetrySession?.invoke()
                            } else {
                                onStartTelemetrySession?.invoke()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive)
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(if (isActive) "STOP SESSION" else "START SESSION")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SessionSignalCard(
                        telemetryState = telemetryState,
                        snapshot = snapshot
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TelemetryStatusCard(
                        telemetryState = telemetryState,
                        lastEscapeMetricsLabel = lastEscapeMetrics?.movementScore?.let { "${(it * 100).roundToInt()}%" } ?: "--"
                    )
                }
            }
        }
    }
}

private val gameSessionConfig = SessionConfig(
    goalDistance = 1000.0,
    sessionDurationSeconds = 90.0,
    initialDistance = 500.0,
    chaseRatePerSecond = 24.0,
    escapeRatePerSecond = 18.0
)

@Composable
fun SessionSignalCard(
    telemetryState: TelemetryState,
    snapshot: com.pedroaba.tccmobile.game.models.GameSnapshot
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatusRow("Session", telemetryState.session.status.name)
            StatusRow("Telemetry Speed", telemetryState.latestSample?.speedMetersPerSecond?.let(::formatOneDecimal) ?: "--")
            StatusRow("Telemetry Distance", telemetryState.latestSample?.totalDistanceMeters?.roundToInt()?.toString() ?: "--")
            StatusRow("Runner Vel.", formatOneDecimal(snapshot.runnerVelocity))
            StatusRow("Horde Vel.", formatOneDecimal(snapshot.hordeVelocity))
            StatusRow("Elapsed", "${snapshot.elapsedSeconds.roundToInt()}s")
        }
    }
}

@Composable
fun TelemetryStatusCard(
    telemetryState: TelemetryState,
    lastEscapeMetricsLabel: String
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

            val sample = telemetryState.latestSample
            val metrics = telemetryState.latestEscapeMetrics

            StatusRow("Distance", sample?.totalDistanceMeters?.roundToInt()?.let { "${it}m" } ?: "--")
            StatusRow("Speed", sample?.speedMetersPerSecond?.let(::formatOneDecimal) ?: "--")
            StatusRow("Acceleration", sample?.effectiveAccelerationMetersPerSecondSquared?.let(::formatOneDecimal) ?: "--")
            StatusRow("Confidence", sample?.movementConfidence?.let { "${(it * 100).roundToInt()}%" } ?: "--")
            StatusRow("Escape Score", lastEscapeMetricsLabel)
            StatusRow("Normalized Speed", metrics?.normalizedSpeed?.let { "${(it * 100).roundToInt()}%" } ?: "--")
            StatusRow("Normalized Distance", metrics?.normalizedDistance?.let { "${(it * 100).roundToInt()}%" } ?: "--")

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

@Composable
fun StatusDisplay(snapshot: com.pedroaba.tccmobile.game.models.GameSnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatusRow("Distance", "${snapshot.distance.toInt()}m")
            StatusRow("Score", "${(snapshot.performanceScore * 100).toInt()}%")
            StatusRow("Risk", "${(snapshot.risk * 100).toInt()}%")
            StatusRow("Horde Pressure", "${(snapshot.hordePressure * 100).toInt()}%")
            StatusRow("Result", snapshot.result.uppercase(), 
                color = when(snapshot.result) {
                    "escaped" -> Color.Green
                    "caught" -> Color.Red
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}
