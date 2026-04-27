package com.pedroaba.tccmobile.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val token: String = "",
    val userId: String = "",
    val name: String = "",
    val email: String = ""
) {
    fun isValid(): Boolean = token.isNotBlank() && userId.isNotBlank()
}