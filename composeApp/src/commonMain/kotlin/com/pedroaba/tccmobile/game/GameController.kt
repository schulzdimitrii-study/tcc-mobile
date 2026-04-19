package com.pedroaba.tccmobile.game

import com.pedroaba.tccmobile.game.debug.GameDebugLogger
import com.pedroaba.tccmobile.game.logic.RunCalculator
import com.pedroaba.tccmobile.game.models.BiofeedbackSample
import com.pedroaba.tccmobile.game.models.GameSnapshot
import com.pedroaba.tccmobile.game.models.SessionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameController {
    private val calculator = RunCalculator()
    
    private val _snapshot = MutableStateFlow(GameSnapshot())
    val snapshot: StateFlow<GameSnapshot> = _snapshot.asStateFlow()

    private val _isSceneLoading = MutableStateFlow(true)
    val isSceneLoading: StateFlow<Boolean> = _isSceneLoading.asStateFlow()

    private val _sessionConfig = MutableStateFlow(SessionConfig())
    val sessionConfig: StateFlow<SessionConfig> = _sessionConfig.asStateFlow()

    private val _lastSample = MutableStateFlow<BiofeedbackSample?>(null)
    val lastSample: StateFlow<BiofeedbackSample?> = _lastSample.asStateFlow()
    
    private var currentConfig: SessionConfig? = null
    private var _isActive: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun onSceneLoadingStarted() {
        _isSceneLoading.value = true
        GameDebugLogger.log(
            tag = "scene-loading",
            "state" to "started"
        )
    }

    fun onSceneLoaded() {
        _isSceneLoading.value = false
        GameDebugLogger.log(
            tag = "scene-loading",
            "state" to "completed"
        )
    }

    fun startSession(config: SessionConfig) {
        currentConfig = config
        _sessionConfig.value = config
        _isActive.value = true
        _snapshot.value = GameSnapshot(
            distance = config.initialDistance,
            hordePressure = 1.0,
            risk = 1.0,
            result = "running"
        )
        GameDebugLogger.log(
            tag = "session",
            "event" to "started",
            "initialDistance" to config.initialDistance,
            "goalDistance" to config.goalDistance,
            "durationSeconds" to config.sessionDurationSeconds
        )
    }

    fun stopSession() {
        _isActive.value = false
        GameDebugLogger.log(
            tag = "session",
            "event" to "stopped",
            "distance" to _snapshot.value.distance,
            "elapsedSeconds" to _snapshot.value.elapsedSeconds,
            "result" to _snapshot.value.result
        )
    }

    fun sendBiofeedback(sample: BiofeedbackSample) {
        if (!_isActive.value) return
        
        val config = currentConfig ?: return
        val current = _snapshot.value
        _lastSample.value = sample

        GameDebugLogger.log(
            tag = "simulation-sample",
            "timestampMs" to sample.timestampMs,
            "bpm" to sample.bpm,
            "cadence" to sample.cadence
        )
        
        val nextSnapshot = calculator.calculateSnapshot(
            currentDistance = current.distance,
            elapsedSeconds = current.elapsedSeconds,
            deltaSeconds = 1.0,
            sample = sample,
            config = config
        )
        
        _snapshot.value = nextSnapshot

        GameDebugLogger.log(
            tag = "snapshot",
            "distance" to nextSnapshot.distance,
            "performance" to nextSnapshot.performanceScore,
            "risk" to nextSnapshot.risk,
            "pressure" to nextSnapshot.hordePressure,
            "runnerVelocity" to nextSnapshot.runnerVelocity,
            "hordeVelocity" to nextSnapshot.hordeVelocity,
            "elapsedSeconds" to nextSnapshot.elapsedSeconds,
            "result" to nextSnapshot.result
        )
        
        if (nextSnapshot.result == "escaped" || nextSnapshot.result == "caught") {
            GameDebugLogger.log(
                tag = "session-terminal",
                "result" to nextSnapshot.result,
                "distance" to nextSnapshot.distance,
                "elapsedSeconds" to nextSnapshot.elapsedSeconds
            )
            stopSession()
        }
    }
}
