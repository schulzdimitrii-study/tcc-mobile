package com.pedroaba.tccmobile.backend.http

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders

class BackendApiException(message: String) : Exception(message)

suspend inline fun <reified T> safeApiCall(crossinline block: suspend () -> T): Result<T> = runCatching {
    try {
        block()
    } catch (error: ClientRequestException) {
        throw BackendApiException(error.response.bodyAsText().ifBlank { "Requisicao invalida ao backend" })
    } catch (error: ServerResponseException) {
        throw BackendApiException(error.response.bodyAsText().ifBlank { "Backend indisponivel no momento" })
    }
}

fun HttpRequestBuilder.bearerAuth(token: String) {
    headers.append(HttpHeaders.Authorization, "Bearer $token")
}
