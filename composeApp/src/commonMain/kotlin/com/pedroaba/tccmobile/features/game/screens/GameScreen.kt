package com.pedroaba.tccmobile.features.game.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.backend.model.GameStateResponse
import com.pedroaba.tccmobile.backend.model.GameStatusDto
import com.pedroaba.tccmobile.backend.model.LeaderboardResponse
import com.pedroaba.tccmobile.backend.online.RemoteSessionState
import com.pedroaba.tccmobile.backend.online.RemoteSessionStatus
import com.pedroaba.tccmobile.game.GameController
import com.pedroaba.tccmobile.game.KorgeGameView
import com.pedroaba.tccmobile.game.debug.GameDebugLogger
import com.pedroaba.tccmobile.game.models.GameSnapshot
import com.pedroaba.tccmobile.game.models.SessionConfig
import com.pedroaba.tccmobile.game.telemetry.model.TelemetrySessionStatus
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import com.pedroaba.tccmobile.features.game.screens.components.SessionSignalCard
import com.pedroaba.tccmobile.features.game.screens.components.TelemetryStatusCard
import com.pedroaba.tccmobile.theme.AppTheme
import com.pedroaba.tccmobile.theme.TccMobileTheme
import com.pedroaba.tccmobile.ui.components.AppCallout
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.round
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    telemetryStateFlow: StateFlow<TelemetryState>? = null,
    remoteSessionState: RemoteSessionState = RemoteSessionState(),
    currentUserId: String = "",
    gameSessionConfig: SessionConfig = defaultGameSessionConfig,
    shouldAutoStartSession: Boolean = false,
    currentTimeMsProvider: () -> Long = { 0L },
    onSnapshotChanged: (GameSnapshot) -> Unit = {},
    onStartTelemetrySession: (() -> Unit)? = null,
    onStopTelemetrySession: (() -> Unit)? = null
) {
    TccMobileTheme {
        val gameController = remember { GameController(timeProviderMs = currentTimeMsProvider) }
        val snapshot by gameController.snapshot.collectAsState()
        val isSceneLoading by gameController.isSceneLoading.collectAsState()
        val lastEscapeMetrics by gameController.lastEscapeMetrics.collectAsState()
        val sessionConfig by gameController.sessionConfig.collectAsState()
        val isActive by gameController.isActive.collectAsState()
        val telemetryState = telemetryStateFlow?.collectAsState()?.value ?: TelemetryState()
        val isRemotePreparing = remoteSessionState.status == RemoteSessionStatus.STARTING ||
            remoteSessionState.status == RemoteSessionStatus.CONNECTING
        val hasRemoteSession = remoteSessionState.sessionId != null ||
            remoteSessionState.status == RemoteSessionStatus.STARTING ||
            remoteSessionState.status == RemoteSessionStatus.CONNECTING ||
            remoteSessionState.status == RemoteSessionStatus.ACTIVE ||
            remoteSessionState.status == RemoteSessionStatus.ENDING
        val authoritativeSnapshot = remoteSessionState.toAuthoritativeSnapshot(
            currentUserId = currentUserId,
            goalDistanceMeters = sessionConfig.goalDistance,
            elapsedSeconds = snapshot.elapsedSeconds
        )
        val isWaitingForRemoteGameState = hasRemoteSession &&
            remoteSessionState.status == RemoteSessionStatus.ACTIVE &&
            authoritativeSnapshot == null
        val canUseSessionButton = !isSceneLoading && !isRemotePreparing

        LaunchedEffect(snapshot) {
            onSnapshotChanged(snapshot)
        }

        LaunchedEffect(isSceneLoading, shouldAutoStartSession) {
            if (shouldAutoStartSession && !isSceneLoading) {
                onStartTelemetrySession?.invoke()
            }
        }

        LaunchedEffect(telemetryState.latestEscapeMetrics) {
            telemetryState.latestEscapeMetrics?.let { metrics ->
                if (hasRemoteSession) {
                    gameController.recordEscapeMetrics(metrics)
                } else {
                    gameController.applyEscapeMetrics(metrics)
                }
            }
        }

        LaunchedEffect(authoritativeSnapshot, remoteSessionState.gameState?.gameStatus) {
            authoritativeSnapshot?.let { nextSnapshot ->
                gameController.applyAuthoritativeSnapshot(
                    snapshot = nextSnapshot,
                    isRunning = remoteSessionState.gameState?.gameStatus
                        ?.let { it == GameStatusDto.RUNNING }
                        ?: hasRemoteSession
                )
            }
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

        LaunchedEffect(snapshot.result, isActive, telemetryState.session.status, remoteSessionState.gameState?.gameStatus, hasRemoteSession) {
            val telemetryRunning = telemetryState.session.status == TelemetrySessionStatus.RUNNING ||
                telemetryState.session.status == TelemetrySessionStatus.PAUSED
            val remoteTerminal = remoteSessionState.gameState?.gameStatus == GameStatusDto.CAUGHT ||
                remoteSessionState.gameState?.gameStatus == GameStatusDto.ESCAPED

            if (telemetryRunning && remoteTerminal) {
                onStopTelemetrySession?.invoke()
            } else if (!hasRemoteSession && !isActive && telemetryRunning && (snapshot.result == "escaped" || snapshot.result == "caught")) {
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

                    if (isSceneLoading || isRemotePreparing || isWaitingForRemoteGameState) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppTheme.colors.background.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (isRemotePreparing) {
                                        remoteSessionState.status.loadingLabel()
                                    } else if (isWaitingForRemoteGameState) {
                                        "Aguardando estado do servidor..."
                                    } else {
                                        "Carregando sprites..."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    GameProgressCard(
                        snapshot = snapshot,
                        goalDistanceMeters = sessionConfig.goalDistance,
                        remoteSessionState = remoteSessionState,
                        currentUserId = currentUserId,
                        preferRemoteState = hasRemoteSession
                    )

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
                            if (!canUseSessionButton) {
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
                        enabled = canUseSessionButton,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive)
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isRemotePreparing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                        }
                        Text(
                            when {
                                isRemotePreparing -> remoteSessionState.status.buttonLabel()
                                isActive -> "STOP SESSION"
                                else -> "START SESSION"
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    remoteSessionState.errorMessage?.let {
                        AppCallout(text = it)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    SessionSignalCard(
                        telemetryState = telemetryState,
                        snapshot = snapshot,
                        remoteSessionState = remoteSessionState,
                        preferRemoteState = hasRemoteSession
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TelemetryStatusCard(
                        telemetryState = telemetryState,
                        lastEscapeMetricsLabel = lastEscapeMetrics?.movementScore?.let { "${(it * 100).roundToInt()}%" } ?: "--",
                        remoteSessionState = remoteSessionState
                    )
                }
            }
        }
    }
}

private val defaultGameSessionConfig = SessionConfig(
    goalDistance = 1000.0,
    sessionDurationSeconds = 90.0,
    initialDistance = 500.0,
    chaseRatePerSecond = 24.0,
    escapeRatePerSecond = 18.0
)

@Composable
private fun GameProgressCard(
    snapshot: GameSnapshot,
    goalDistanceMeters: Double,
    remoteSessionState: RemoteSessionState,
    currentUserId: String,
    preferRemoteState: Boolean = false
) {
    val remoteLeaderboard = remoteSessionState.leaderboard
    val remoteGameState = remoteSessionState.gameState
    val remoteUserDistanceMeters = remoteLeaderboard
        ?.entries
        ?.firstOrNull { it.userId == currentUserId }
        ?.distanceKm
        ?.let { it * 1_000.0 }
    val remoteHordeDistanceMeters = remoteLeaderboard
        ?.hordeVirtualDistanceKm
        ?.let { it * 1_000.0 }
    val remoteGoalDistanceMeters = remoteGameState
        ?.let { (it.playerPosition + it.distanceToGoal).takeIf { value -> value > 0.0 } }
        ?.let { it * 1_000.0 }
    val effectiveGoalDistanceMeters = remoteGoalDistanceMeters ?: goalDistanceMeters
    val runnerDistanceMeters = remoteGameState?.playerPosition?.let { it * 1_000.0 }
        ?: remoteUserDistanceMeters
        ?: if (preferRemoteState) null else snapshot.distance.coerceAtLeast(0.0)
    val hordeDistanceMeters = remoteGameState?.hordePosition?.let { it * 1_000.0 }
        ?: remoteHordeDistanceMeters
        ?: if (preferRemoteState) null else 0.0
    val runnerProgress = ((runnerDistanceMeters ?: 0.0) / effectiveGoalDistanceMeters)
        .coerceIn(0.0, 1.0)
        .toFloat()
    val hordeProgress = ((hordeDistanceMeters ?: 0.0) / effectiveGoalDistanceMeters)
        .coerceIn(0.0, 1.0)
        .toFloat()
    val distanceLabel = runnerDistanceMeters?.let { "${roundToOneDecimal(it)} m" } ?: "--"
    val goalLabel = "${roundToTwoDecimals(effectiveGoalDistanceMeters / 1_000.0)} km"
    val hordeLabel = if (hordeDistanceMeters != null) {
        "${roundToOneDecimal(hordeDistanceMeters)} m"
    } else {
        "--"
    }
    val distanceToHordeMeters = remoteGameState?.distancePlayerToHorde?.let { it * 1_000.0 }
        ?: remoteLeaderboard?.distanceToHorde?.let { kotlin.math.abs(it * 1_000.0) }
    val distanceToHordeLabel = distanceToHordeMeters?.let { "${roundToOneDecimal(it)} m" }
    val isBehindHorde = remoteGameState?.let { it.hordePosition >= it.playerPosition }
        ?: remoteLeaderboard?.isBehindHorde

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = AppTheme.colors.card,
        border = BorderStroke(1.dp, AppTheme.colors.border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "PROGRESSO",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            RacePositionBar(
                runnerProgress = runnerProgress,
                hordeProgress = hordeProgress
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Você: $distanceLabel / $goalLabel · Horda: $hordeLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            remoteGameState?.let { gameState ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Estado remoto: ${gameState.gameStatus.name.lowercase()} · ${roundToOneDecimal(gameState.raceProgress)}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            distanceToHordeLabel?.let { label ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isBehindHorde == true) {
                        "Atrás da horda por $label"
                    } else {
                        "À frente da horda por $label"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RacePositionBar(
    runnerProgress: Float,
    hordeProgress: Float
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        val markerSize = 18.dp
        val usableWidth = maxWidth - markerSize
        val runnerOffset = usableWidth * runnerProgress
        val hordeOffset = usableWidth * hordeProgress

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
                .background(AppTheme.colors.border)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(runnerProgress)
                .height(6.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        PositionMarker(
            color = Color(0xFF22C55E),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = hordeOffset)
        )
        PositionMarker(
            color = Color(0xFF38BDF8),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = runnerOffset)
        )
        Text(
            text = "🏁",
            modifier = Modifier.align(Alignment.CenterEnd),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PositionMarker(
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private fun roundToOneDecimal(value: Double): Double = round(value * 10.0) / 10.0

private fun roundToTwoDecimals(value: Double): Double = round(value * 100.0) / 100.0

private fun RemoteSessionState.toAuthoritativeSnapshot(
    currentUserId: String,
    goalDistanceMeters: Double,
    elapsedSeconds: Double
): GameSnapshot? {
    gameState?.let { return it.toAuthoritativeSnapshot(elapsedSeconds) }
    return leaderboard?.toAuthoritativeSnapshot(
        currentUserId = currentUserId,
        goalDistanceMeters = goalDistanceMeters,
        elapsedSeconds = elapsedSeconds
    )
}

private fun GameStateResponse.toAuthoritativeSnapshot(elapsedSeconds: Double): GameSnapshot {
    val pressure = pressureFromDistanceToHordeKm(distancePlayerToHorde)

    return GameSnapshot(
        distance = (playerPosition * 1_000.0).coerceAtLeast(0.0),
        hordePressure = pressure,
        risk = pressure,
        performanceScore = (raceProgress / 100.0).coerceIn(0.0, 1.0),
        runnerVelocity = playerSpeed,
        hordeVelocity = hordeSpeed,
        elapsedSeconds = elapsedSeconds,
        result = when (gameStatus) {
            GameStatusDto.RUNNING -> "running"
            GameStatusDto.CAUGHT -> "caught"
            GameStatusDto.ESCAPED -> "escaped"
        }
    )
}

private fun LeaderboardResponse.toAuthoritativeSnapshot(
    currentUserId: String,
    goalDistanceMeters: Double,
    elapsedSeconds: Double
): GameSnapshot? {
    val playerDistanceMeters = entries
        .firstOrNull { it.userId == currentUserId }
        ?.distanceKm
        ?.let { it * 1_000.0 }
        ?: return null
    val hordeDistanceMeters = hordeVirtualDistanceKm?.let { it * 1_000.0 }
    val distanceToHordeKm = distanceToHorde
        ?: hordeDistanceMeters?.let { kotlin.math.abs((playerDistanceMeters - it) / 1_000.0) }
    val pressure = distanceToHordeKm?.let(::pressureFromDistanceToHordeKm) ?: 0.0

    return GameSnapshot(
        distance = playerDistanceMeters.coerceAtLeast(0.0),
        hordePressure = pressure,
        risk = pressure,
        performanceScore = (playerDistanceMeters / goalDistanceMeters)
            .takeIf { goalDistanceMeters > 0.0 }
            ?.coerceIn(0.0, 1.0)
            ?: 0.0,
        runnerVelocity = 0.0,
        hordeVelocity = 0.0,
        elapsedSeconds = elapsedSeconds,
        result = "running"
    )
}

private fun pressureFromDistanceToHordeKm(distanceToHordeKm: Double): Double {
    val distanceToHordeMeters = distanceToHordeKm * 1_000.0
    return when {
        distanceToHordeMeters <= 0.0 -> 1.0
        else -> (1.0 - (distanceToHordeMeters / 500.0)).coerceIn(0.0, 1.0)
    }
}

private fun RemoteSessionStatus.loadingLabel(): String = when (this) {
    RemoteSessionStatus.STARTING -> "Abrindo sessao no servidor..."
    RemoteSessionStatus.CONNECTING -> "Conectando com o servidor..."
    else -> "Preparando sessao..."
}

private fun RemoteSessionStatus.buttonLabel(): String = when (this) {
    RemoteSessionStatus.STARTING -> "STARTING SERVER SESSION"
    RemoteSessionStatus.CONNECTING -> "CONNECTING SERVER"
    else -> "WAITING SERVER"
}
