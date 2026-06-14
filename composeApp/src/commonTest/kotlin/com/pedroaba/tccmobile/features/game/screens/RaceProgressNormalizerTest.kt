package com.pedroaba.tccmobile.features.game.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class RaceProgressNormalizerTest {
    @Test
    fun `normalizes backend race progress percent for visual positioning`() {
        assertEquals(0f, normalizeRaceProgressPercent(null))
        assertEquals(0f, normalizeRaceProgressPercent(0.0))
        assertEquals(0.45f, normalizeRaceProgressPercent(45.0))
        assertEquals(1f, normalizeRaceProgressPercent(100.0))
        assertEquals(1f, normalizeRaceProgressPercent(150.0))
    }

    @Test
    fun `invalid backend race progress becomes zero`() {
        assertEquals(0f, normalizeRaceProgressPercent(-1.0))
        assertEquals(0f, normalizeRaceProgressPercent(Double.NaN))
        assertEquals(0f, normalizeRaceProgressPercent(Double.POSITIVE_INFINITY))
        assertEquals(0f, normalizeRaceProgressPercent(Double.NEGATIVE_INFINITY))
    }
}
