package com.pedroaba.tccmobile.auth.http

import android.util.Log
import com.pedroaba.tccmobile.auth.model.AuthResponse
import com.pedroaba.tccmobile.auth.model.LoginRequest
import com.pedroaba.tccmobile.auth.model.RegisterRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "AuthHttpClient"
private const val BASE_URL = "http://10.0.2.2:8080"

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

class AuthHttpClient {

    private val urlLogin = URL("$BASE_URL/auth/login")
    private val urlRegister = URL("$BASE_URL/auth/register")

    suspend fun login(request: LoginRequest): Result<AuthResponse> = runCatching {
        Log.d(TAG, "Login request: $request")

        val connection = urlLogin.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        connection.outputStream.use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.write(json.encodeToString(request))
            }
        }

        val responseCode = connection.responseCode
        Log.d(TAG, "Login response code: $responseCode")

        if (responseCode == 200) {
            connection.inputStream.bufferedReader().use { reader ->
                json.decodeFromString<AuthResponse>(reader.readText())
            }
        } else {
            throw Exception("HTTP $responseCode: ${connection.errorStream?.bufferedReader()?.readText()}")
        }.also {
            Log.d(TAG, "Login success: ${it.email}")
        }
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponse> = runCatching {
        Log.d(TAG, "Register request: $request")

        val connection = urlRegister.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        connection.outputStream.use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.write(json.encodeToString(request))
            }
        }

        val responseCode = connection.responseCode
        Log.d(TAG, "Register response code: $responseCode")

        if (responseCode == 201) {
            connection.inputStream.bufferedReader().use { reader ->
                json.decodeFromString<AuthResponse>(reader.readText())
            }
        } else {
            throw Exception("HTTP $responseCode: ${connection.errorStream?.bufferedReader()?.readText()}")
        }.also {
            Log.d(TAG, "Register success: ${it.email}")
        }
    }

    fun close() {
        // No-op for HttpURLConnection
    }
}