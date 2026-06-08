package com.pedroaba.tccmobile.backend.model

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class StartSessionRequestTest {

    @Test
    fun `serializes selected goal distance in kilometers`() {
        val json = Json.encodeToString(
            StartSessionRequest(
                hordeId = "horde-1",
                goalDistanceKm = 3.0
            )
        )

        val element = Json.parseToJsonElement(json).jsonObject

        assertEquals("horde-1", element["hordeId"]?.jsonPrimitive?.content)
        assertEquals(3.0, element["goalDistanceKm"]?.jsonPrimitive?.double)
    }
}
