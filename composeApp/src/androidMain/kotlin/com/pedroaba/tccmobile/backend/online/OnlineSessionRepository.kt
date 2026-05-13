package com.pedroaba.tccmobile.backend.online

import com.pedroaba.tccmobile.backend.model.StartSessionRequest
import com.pedroaba.tccmobile.game.models.GameSnapshot
import com.pedroaba.tccmobile.game.telemetry.model.TelemetryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnlineSessionRepository(
    private val sessionApi: SessionApi,
    private val stompWebSocketClient: StompWebSocketClient,
    private val currentTimeMsProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val _state = MutableStateFlow(RemoteSessionState())
    val state: StateFlow<RemoteSessionState> = _state.asStateFlow()

    private var lastTelemetrySentAtMs: Long = 0L

    suspend fun startSession(token: String): Result<String> {
        val currentState = _state.value
        if (currentState.sessionId != null && currentState.status != RemoteSessionStatus.IDLE) {
            return Result.success(currentState.sessionId)
        }

        _state.value = _state.value.copy(
            status = RemoteSessionStatus.STARTING,
            errorMessage = null
        )

        return sessionApi.startSession(token, StartSessionRequest()).fold(
            onSuccess = { response ->
                _state.value = _state.value.onSessionStarted(response.sessionId)
                stompWebSocketClient.connect(
                    sessionId = response.sessionId,
                    onConnected = {
                        _state.value = _state.value.onSocketConnected()
                    },
                    onLeaderboard = { leaderboard ->
                        _state.value = _state.value.onLeaderboardUpdated(leaderboard)
                    },
                    onFailure = { message ->
                        _state.value = _state.value.onRealtimeFailure(message)
                    }
                )
                Result.success(response.sessionId)
            },
            onFailure = { error ->
                val message = error.message ?: "Nao foi possivel iniciar a sessao online."
                _state.value = _state.value.onSessionStartFailed(message)
                Result.failure(error)
            }
        )
    }

    suspend fun endSession(token: String): Result<Unit> {
        val sessionId = _state.value.sessionId
            ?: return Result.failure(IllegalStateException("Nenhuma sessao online em andamento."))

        _state.value = _state.value.onSessionEndRequested()

        return sessionApi.endSession(token, sessionId).fold(
            onSuccess = {
                stompWebSocketClient.disconnect()
                lastTelemetrySentAtMs = 0L
                _state.value = _state.value.onSessionEnded()
                Result.success(Unit)
            },
            onFailure = { error ->
                val message = error.message ?: "Nao foi possivel encerrar a sessao online."
                _state.value = _state.value.onSessionEndFailed(message)
                Result.failure(error)
            }
        )
    }

    fun sendTelemetry(
        userId: String,
        telemetryState: TelemetryState,
        snapshot: GameSnapshot
    ): Boolean {
        val sessionId = _state.value.sessionId ?: return false
        if (!stompWebSocketClient.isConnected()) return false

        val now = currentTimeMsProvider()
        if (now - lastTelemetrySentAtMs < 1_000L) return false

        val message = buildBiometricDataMessage(
            sessionId = sessionId,
            userId = userId,
            telemetryState = telemetryState,
            snapshot = snapshot,
            timestampMs = now
        ) ?: return false

        val sent = stompWebSocketClient.sendBiometricData(message)
        if (sent) {
            lastTelemetrySentAtMs = now
        }
        return sent
    }

    fun clear() {
        stompWebSocketClient.disconnect()
    }
}
