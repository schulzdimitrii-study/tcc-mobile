package com.pedroaba.tccmobile.game

import com.pedroaba.tccmobile.game.debug.GameDebugLogger
import com.pedroaba.tccmobile.game.models.GameSnapshot
import com.pedroaba.tccmobile.game.models.SessionConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GameController(
    private val timeProviderMs: () -> Long = { 0L }
) {
    private val _snapshot = MutableStateFlow(GameSnapshot())
    val snapshot: StateFlow<GameSnapshot> = _snapshot.asStateFlow()

    private val _isSceneLoading = MutableStateFlow(true)
    val isSceneLoading: StateFlow<Boolean> = _isSceneLoading.asStateFlow()

    private val _sessionConfig = MutableStateFlow(SessionConfig())
    val sessionConfig: StateFlow<SessionConfig> = _sessionConfig.asStateFlow()

    private var currentConfig: SessionConfig? = null
    private var _isActive: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private var sessionStartedAtMs: Long? = null

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
        sessionStartedAtMs = timeProviderMs()
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
        sessionStartedAtMs = null
        GameDebugLogger.log(
            tag = "session",
            "event" to "stopped",
            "distance" to _snapshot.value.distance,
            "elapsedSeconds" to _snapshot.value.elapsedSeconds,
            "result" to _snapshot.value.result
        )
    }

    fun applyAuthoritativeSnapshot(snapshot: GameSnapshot, isRunning: Boolean) {
        _isActive.value = isRunning
        _snapshot.value = snapshot

        GameDebugLogger.log(
            tag = "authoritative-snapshot",
            "distance" to snapshot.distance,
            "performance" to snapshot.performanceScore,
            "risk" to snapshot.risk,
            "pressure" to snapshot.hordePressure,
            "runnerVelocity" to snapshot.runnerVelocity,
            "hordeVelocity" to snapshot.hordeVelocity,
            "result" to snapshot.result
        )
    }
}
