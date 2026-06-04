package com.pedroaba.tccmobile.backend.online

import android.util.Log
import com.pedroaba.tccmobile.backend.BackendConfig
import com.pedroaba.tccmobile.backend.model.BiometricDataMessage
import com.pedroaba.tccmobile.backend.model.GameStateResponse
import com.pedroaba.tccmobile.backend.model.LeaderboardResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class StompWebSocketClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private var webSocket: WebSocket? = null
    private var currentSessionId: String? = null
    private val connected = AtomicBoolean(false)
    private val intentionalDisconnect = AtomicBoolean(false)

    fun connect(
        sessionId: String,
        onConnected: () -> Unit,
        onLeaderboard: (LeaderboardResponse) -> Unit,
        onGameState: (GameStateResponse) -> Unit,
        onFailure: (String) -> Unit
    ) {
        disconnect()
        intentionalDisconnect.set(false)
        currentSessionId = sessionId
        webSocket = okHttpClient.newWebSocket(
            Request.Builder().url(BackendConfig.webSocketUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(REALTIME_LOG_TAG, "socket_open url=${BackendConfig.webSocketUrl}")
                    webSocket.send(
                        buildFrame(
                            command = "CONNECT",
                            headers = mapOf(
                                "accept-version" to "1.2",
                                "heart-beat" to "0,0"
                            )
                        )
                    )
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d(REALTIME_LOG_TAG, "socket_raw_message=${text.take(LOG_BODY_LIMIT)}")
                    parseFrames(text).forEach { frame ->
                        Log.d(
                            REALTIME_LOG_TAG,
                            "stomp_frame command=${frame.command} destination=${frame.headers["destination"] ?: "--"} body=${frame.body.take(LOG_BODY_LIMIT)}"
                        )
                        when (frame.command) {
                            "CONNECTED" -> {
                                connected.set(true)
                                currentSessionId?.let { activeSessionId ->
                                    Log.d(REALTIME_LOG_TAG, "stomp_connected sessionId=$activeSessionId")
                                    subscribeToSessionTopics(webSocket, activeSessionId)
                                    onConnected()
                                }
                            }

                            "MESSAGE" -> {
                                when (frame.headers["destination"]) {
                                    "/topic/session/$sessionId/leaderboard" -> {
                                        runCatching {
                                            json.decodeFromString<LeaderboardResponse>(frame.body)
                                        }.onSuccess { leaderboard ->
                                            Log.d(
                                                REALTIME_LOG_TAG,
                                                "leaderboard_received sessionId=${leaderboard.sessionId} rank=${leaderboard.userRank} entries=${leaderboard.entries.size} horde=${leaderboard.hordeVirtualDistanceKm} behind=${leaderboard.isBehindHorde} distanceToHorde=${leaderboard.distanceToHorde}"
                                            )
                                            val decodedEntries = leaderboard.entries.joinToString(
                                                prefix = "[",
                                                postfix = "]"
                                            ) { entry ->
                                                "{userId=${entry.userId},rank=${entry.rank},distanceKm=${entry.distanceKm},cardiacZone=${entry.cardiacZone}}"
                                            }
                                            Log.d(
                                                REALTIME_LOG_TAG,
                                                "DECODED_LEADERBOARD sessionId=${leaderboard.sessionId} userRank=${leaderboard.userRank} hordeVirtualDistanceKm=${leaderboard.hordeVirtualDistanceKm} isBehindHorde=${leaderboard.isBehindHorde} distanceToHordeKm=${leaderboard.distanceToHorde} entries=$decodedEntries"
                                            )
                                            onLeaderboard(leaderboard)
                                        }.onFailure {
                                            Log.e(REALTIME_LOG_TAG, "leaderboard_decode_failed body=${frame.body.take(LOG_BODY_LIMIT)}", it)
                                            onFailure("Nao foi possivel ler o leaderboard em tempo real.")
                                        }
                                    }

                                    "/topic/session/$sessionId/game-state" -> {
                                        runCatching {
                                            json.decodeFromString<GameStateResponse>(frame.body)
                                        }.onSuccess { gameState ->
                                            Log.d(
                                                REALTIME_LOG_TAG,
                                                "game_state_received sessionId=${gameState.sessionId} userId=${gameState.userId} player=${gameState.playerPosition} horde=${gameState.hordePosition} toGoal=${gameState.distanceToGoal} playerToHorde=${gameState.distancePlayerToHorde} playerSpeed=${gameState.playerSpeed} hordeSpeed=${gameState.hordeSpeed} progress=${gameState.raceProgress} status=${gameState.gameStatus}"
                                            )
                                            Log.d(
                                                REALTIME_LOG_TAG,
                                                "DECODED_GAME_STATE sessionId=${gameState.sessionId} userId=${gameState.userId} playerPositionKm=${gameState.playerPosition} hordePositionKm=${gameState.hordePosition} distanceToGoalKm=${gameState.distanceToGoal} distancePlayerToHordeKm=${gameState.distancePlayerToHorde} playerSpeedKmh=${gameState.playerSpeed} hordeSpeedKmh=${gameState.hordeSpeed} raceProgressPercent=${gameState.raceProgress} gameStatus=${gameState.gameStatus}"
                                            )
                                            onGameState(gameState)
                                        }.onFailure {
                                            Log.e(REALTIME_LOG_TAG, "game_state_decode_failed body=${frame.body.take(LOG_BODY_LIMIT)}", it)
                                            onFailure("Nao foi possivel ler o estado do jogo em tempo real.")
                                        }
                                    }
                                }
                            }

                            "ERROR" -> {
                                connected.set(false)
                                Log.e(REALTIME_LOG_TAG, "stomp_error body=${frame.body.take(LOG_BODY_LIMIT)}")
                                onFailure(frame.body.ifBlank { "Falha na conexao em tempo real." })
                            }
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    connected.set(false)
                    if (intentionalDisconnect.get()) return
                    Log.e(REALTIME_LOG_TAG, "socket_failure code=${response?.code} message=${t.message}", t)
                    onFailure(t.message ?: "Falha ao conectar ao leaderboard em tempo real.")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    connected.set(false)
                    Log.d(REALTIME_LOG_TAG, "socket_closed code=$code reason=$reason")
                }
            }
        )
    }

    fun sendBiometricData(message: BiometricDataMessage): Boolean {
        if (!connected.get()) return false
        Log.d(
            REALTIME_LOG_TAG,
            "telemetry_send sessionId=${message.sessionId} userId=${message.userId} timestamp=${message.timestamp} bpm=${message.bpm} cadence=${message.cadence} speed=${message.speed} pace=${message.pace} distance=${message.accumulatedDistance} calories=${message.accumulatedCalories}"
        )
        val frame = buildFrame(
            command = "SEND",
            headers = mapOf(
                "destination" to "/app/train/data",
                "content-type" to "application/json"
            ),
            body = json.encodeToString(message)
        )
        return webSocket?.send(frame) == true
    }

    fun isConnected(): Boolean = connected.get()

    fun disconnect() {
        intentionalDisconnect.set(true)
        connected.set(false)
        webSocket?.send(buildFrame(command = "DISCONNECT"))
        webSocket?.close(1000, "client disconnect")
        webSocket = null
        currentSessionId = null
    }

    private fun buildFrame(
        command: String,
        headers: Map<String, String> = emptyMap(),
        body: String = ""
    ): String {
        val headerBlock = buildString {
            append(command)
            append('\n')
            headers.forEach { (key, value) ->
                append(key)
                append(':')
                append(value)
                append('\n')
            }
            append('\n')
            append(body)
            append('\u0000')
        }
        return headerBlock
    }

    private fun subscribeToSessionTopics(webSocket: WebSocket, sessionId: String) {
        Log.d(REALTIME_LOG_TAG, "subscribe_leaderboard destination=/topic/session/$sessionId/leaderboard")
        webSocket.send(
            buildFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "leaderboard-$sessionId",
                    "destination" to "/topic/session/$sessionId/leaderboard"
                )
            )
        )
        Log.d(REALTIME_LOG_TAG, "subscribe_game_state destination=/topic/session/$sessionId/game-state")
        webSocket.send(
            buildFrame(
                command = "SUBSCRIBE",
                headers = mapOf(
                    "id" to "game-state-$sessionId",
                    "destination" to "/topic/session/$sessionId/game-state"
                )
            )
        )
    }

    private companion object {
        const val REALTIME_LOG_TAG = "TccRealtime"
        const val LOG_BODY_LIMIT = 1_000
    }
}

internal fun parseFrames(text: String): List<StompFrame> {
    return text
        .split('\u0000')
        .filter { it.isNotBlank() }
        .mapNotNull { rawFrame ->
            val lines = rawFrame.lines()
            if (lines.isEmpty()) return@mapNotNull null

            val command = lines.first().trim()
            val bodyIndex = lines.indexOfFirst { it.isBlank() }
            val headers = if (bodyIndex > 1) {
                lines.subList(1, bodyIndex)
                    .mapNotNull { line ->
                        val separator = line.indexOf(':')
                        if (separator <= 0) {
                            null
                        } else {
                            line.substring(0, separator) to line.substring(separator + 1)
                        }
                    }
                    .toMap()
            } else {
                emptyMap()
            }
            val body = if (bodyIndex >= 0) {
                lines.drop(bodyIndex + 1).joinToString("\n").trim()
            } else {
                ""
            }
            StompFrame(command = command, headers = headers, body = body)
        }
}

internal data class StompFrame(
    val command: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String
)
