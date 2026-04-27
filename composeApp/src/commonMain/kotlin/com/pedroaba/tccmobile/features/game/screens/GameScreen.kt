package com.pedroaba.tccmobile.features.game.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pedroaba.tccmobile.game.GameController
import com.pedroaba.tccmobile.game.KorgeGameView
import com.pedroaba.tccmobile.game.debug.GameDebugLogger
import com.pedroaba.tccmobile.game.models.SessionConfig
import com.pedroaba.tccmobile.game.telemetry.model.TelemetrySessionStatus
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import com.pedroaba.tccmobile.features.game.screens.components.SessionSignalCard
import com.pedroaba.tccmobile.features.game.screens.components.StatusDisplay
import com.pedroaba.tccmobile.features.game.screens.components.TelemetryStatusCard
import com.pedroaba.tccmobile.theme.AppTheme
import com.pedroaba.tccmobile.theme.TccMobileTheme
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    telemetryStateFlow: StateFlow<TelemetryState>? = null,
    currentTimeMsProvider: () -> Long = { 0L },
    onStartTelemetrySession: (() -> Unit)? = null,
    onStopTelemetrySession: (() -> Unit)? = null
) {
    TccMobileTheme {
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
                                .background(AppTheme.colors.background.copy(alpha = 0.8f)),
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
                                MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
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