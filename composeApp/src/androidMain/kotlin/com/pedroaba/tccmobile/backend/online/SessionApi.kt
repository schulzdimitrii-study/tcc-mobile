package com.pedroaba.tccmobile.backend.online

import com.pedroaba.tccmobile.backend.http.BackendHttpClient
import com.pedroaba.tccmobile.backend.http.bearerAuth
import com.pedroaba.tccmobile.backend.http.safeApiCall
import com.pedroaba.tccmobile.backend.model.StartSessionRequest
import com.pedroaba.tccmobile.backend.model.StartSessionResponse
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class SessionApi(
    private val backendHttpClient: BackendHttpClient
) {
    suspend fun startSession(
        token: String,
        request: StartSessionRequest = StartSessionRequest()
    ): Result<StartSessionResponse> = safeApiCall {
        backendHttpClient.client.post("/sessions/iniciar") {
            bearerAuth(token)
            setBody(request)
        }.body()
    }

    suspend fun endSession(
        token: String,
        sessionId: String
    ): Result<Unit> = safeApiCall {
        backendHttpClient.client.post("/sessions/$sessionId/encerrar") {
            bearerAuth(token)
        }
        Unit
    }
}
