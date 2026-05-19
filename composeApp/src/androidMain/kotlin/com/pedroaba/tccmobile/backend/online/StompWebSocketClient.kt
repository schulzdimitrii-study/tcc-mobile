package com.pedroaba.tccmobile.backend.online

import com.pedroaba.tccmobile.backend.BackendConfig
import com.pedroaba.tccmobile.backend.model.BiometricDataMessage
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

    fun connect(
        sessionId: String,
        onConnected: () -> Unit,
        onLeaderboard: (LeaderboardResponse) -> Unit,
        onFailure: (String) -> Unit
    ) {
        disconnect()
        currentSessionId = sessionId
        webSocket = okHttpClient.newWebSocket(
            Request.Builder().url(BackendConfig.webSocketUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
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
                    parseFrames(text).forEach { frame ->
                        when (frame.command) {
                            "CONNECTED" -> {
                                connected.set(true)
                                currentSessionId?.let { activeSessionId ->
                                    webSocket.send(
                                        buildFrame(
                                            command = "SUBSCRIBE",
                                            headers = mapOf(
                                                "id" to "leaderboard-$activeSessionId",
                                                "destination" to "/topic/session/$activeSessionId/leaderboard"
                                            )
                                        )
                                    )
                                    onConnected()
                                }
                            }

                            "MESSAGE" -> {
                                runCatching {
                                    json.decodeFromString<LeaderboardResponse>(frame.body)
                                }.onSuccess(onLeaderboard).onFailure {
                                    onFailure("Nao foi possivel ler o leaderboard em tempo real.")
                                }
                            }

                            "ERROR" -> {
                                connected.set(false)
                                onFailure(frame.body.ifBlank { "Falha na conexao em tempo real." })
                            }
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    connected.set(false)
                    onFailure(t.message ?: "Falha ao conectar ao leaderboard em tempo real.")
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    connected.set(false)
                }
            }
        )
    }

    fun sendBiometricData(message: BiometricDataMessage): Boolean {
        if (!connected.get()) return false
        val frame = buildFrame(
            command = "SEND",
            headers = mapOf(
                "destination" to "/app/treino/dados",
                "content-type" to "application/json"
            ),
            body = json.encodeToString(message)
        )
        return webSocket?.send(frame) == true
    }

    fun isConnected(): Boolean = connected.get()

    fun disconnect() {
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

    private fun parseFrames(text: String): List<StompFrame> {
        return text
            .split('\u0000')
            .filter { it.isNotBlank() }
            .mapNotNull { rawFrame ->
                val lines = rawFrame.lines()
                if (lines.isEmpty()) return@mapNotNull null

                val command = lines.first().trim()
                val bodyIndex = lines.indexOfFirst { it.isBlank() }
                val body = if (bodyIndex >= 0) {
                    lines.drop(bodyIndex + 1).joinToString("\n").trim()
                } else {
                    ""
                }
                StompFrame(command = command, body = body)
            }
    }
}

private data class StompFrame(
    val command: String,
    val body: String
)
