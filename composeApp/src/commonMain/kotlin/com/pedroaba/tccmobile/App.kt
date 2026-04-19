package com.pedroaba.tccmobile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import com.pedroaba.tccmobile.game.models.BiofeedbackSample
import com.pedroaba.tccmobile.game.models.SessionConfig
import com.pedroaba.tccmobile.game.simulation.BiofeedbackSimulator
import com.pedroaba.tccmobile.game.simulation.SimulationMode
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFBB86FC),
            secondary = Color(0xFF03DAC6),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onSurface = Color.White
        )
    ) {
        val gameController = remember { GameController() }
        val snapshot by gameController.snapshot.collectAsState()
        val isSceneLoading by gameController.isSceneLoading.collectAsState()
        val lastSample by gameController.lastSample.collectAsState()
        val isActive by gameController.isActive.collectAsState()
        
        var bpm by remember { mutableStateOf(120f) }
        var cadence by remember { mutableStateOf(170f) }
        var simulationMode by remember { mutableStateOf(SimulationMode.INTERVAL) }
        
        // Simulation loop
        LaunchedEffect(gameController.isActive, simulationMode, bpm, cadence) {
            while (isActive) {
                val elapsedSeconds = snapshot.elapsedSeconds.toInt()
                val sample = BiofeedbackSimulator.generateSample(
                    mode = simulationMode,
                    elapsedSeconds = elapsedSeconds,
                    manualBpm = bpm.toInt(),
                    manualCadence = cadence.toInt(),
                    config = debugSessionConfig,
                    timestampMs = (elapsedSeconds + 1L) * 1000L
                )

                GameDebugLogger.log(
                    tag = "simulation-loop",
                    "mode" to simulationMode.label,
                    "elapsedSeconds" to elapsedSeconds,
                    "bpm" to sample.bpm,
                    "cadence" to sample.cadence,
                    "timestampMs" to sample.timestampMs
                )

                gameController.sendBiofeedback(
                    sample
                )
                delay(1000) // 1Hz update
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
                        "DEBUG CONTROLS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    SimulationModeSelector(
                        selectedMode = simulationMode,
                        onModeSelected = { simulationMode = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (simulationMode == SimulationMode.CUSTOM) {
                        DebugSlider(
                            label = "BPM",
                            value = bpm,
                            range = 60f..200f,
                            onValueChange = { bpm = it }
                        )

                        DebugSlider(
                            label = "Cadence",
                            value = cadence,
                            range = 50f..200f,
                            onValueChange = { cadence = it }
                        )
                    } else {
                        SimulationTelemetryCard(
                            simulationMode = simulationMode,
                            sample = lastSample
                        )
                    }

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
                                gameController.stopSession()
                            } else {
                                gameController.startSession(debugSessionConfig)
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
                        sample = lastSample,
                        snapshot = snapshot
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "SESSION STATUS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    StatusDisplay(snapshot = snapshot)
                }
            }
        }
    }
}

private val debugSessionConfig = SessionConfig(
    goalDistance = 1000.0,
    sessionDurationSeconds = 90.0,
    initialDistance = 500.0,
    chaseRatePerSecond = 24.0,
    escapeRatePerSecond = 18.0
)

@Composable
fun DebugSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("${value.toInt()}", fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SimulationModeSelector(
    selectedMode: SimulationMode,
    onModeSelected: (SimulationMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SimulationMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.label) }
            )
        }
    }
}

@Composable
fun SimulationTelemetryCard(
    simulationMode: SimulationMode,
    sample: BiofeedbackSample?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatusRow("Simulation", simulationMode.label)
            StatusRow("Input BPM", sample?.bpm?.toString() ?: "--")
            StatusRow("Input Cadence", sample?.cadence?.toString() ?: "--")
            StatusRow("Sample Time", sample?.timestampMs?.toString() ?: "--")
        }
    }
}

@Composable
fun SessionSignalCard(
    sample: BiofeedbackSample?,
    snapshot: com.pedroaba.tccmobile.game.models.GameSnapshot
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatusRow("Current BPM", sample?.bpm?.toString() ?: "--")
            StatusRow("Current Cadence", sample?.cadence?.toString() ?: "--")
            StatusRow("Runner Vel.", formatOneDecimal(snapshot.runnerVelocity))
            StatusRow("Horde Vel.", formatOneDecimal(snapshot.hordeVelocity))
            StatusRow("Elapsed", "${snapshot.elapsedSeconds.roundToInt()}s")
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
