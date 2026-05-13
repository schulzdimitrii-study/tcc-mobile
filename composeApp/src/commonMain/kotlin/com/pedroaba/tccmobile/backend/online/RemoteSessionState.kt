package com.pedroaba.tccmobile.backend.online

import com.pedroaba.tccmobile.backend.model.LeaderboardResponse

enum class RemoteSessionStatus {
    IDLE,
    STARTING,
    CONNECTING,
    ACTIVE,
    ENDING,
    ERROR
}

data class RemoteSessionState(
    val sessionId: String? = null,
    val status: RemoteSessionStatus = RemoteSessionStatus.IDLE,
    val leaderboard: LeaderboardResponse? = null,
    val errorMessage: String? = null
) {
    fun onSessionStarted(newSessionId: String): RemoteSessionState = copy(
        sessionId = newSessionId,
        status = RemoteSessionStatus.CONNECTING,
        leaderboard = null,
        errorMessage = null
    )

    fun onSocketConnected(): RemoteSessionState = copy(
        status = RemoteSessionStatus.ACTIVE,
        errorMessage = null
    )

    fun onSessionStartFailed(message: String): RemoteSessionState = copy(
        sessionId = null,
        status = RemoteSessionStatus.ERROR,
        errorMessage = message
    )

    fun onSessionEndRequested(): RemoteSessionState = copy(
        status = RemoteSessionStatus.ENDING,
        errorMessage = null
    )

    fun onSessionEnded(): RemoteSessionState = RemoteSessionState()

    fun onSessionEndFailed(message: String): RemoteSessionState = copy(
        status = RemoteSessionStatus.ERROR,
        errorMessage = message
    )

    fun onLeaderboardUpdated(newLeaderboard: LeaderboardResponse): RemoteSessionState = copy(
        status = RemoteSessionStatus.ACTIVE,
        leaderboard = newLeaderboard,
        errorMessage = null
    )

    fun onRealtimeFailure(message: String): RemoteSessionState = copy(
        status = RemoteSessionStatus.ERROR,
        errorMessage = message
    )
}
