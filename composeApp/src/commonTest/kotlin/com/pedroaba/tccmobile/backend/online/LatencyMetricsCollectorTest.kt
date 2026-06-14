package com.pedroaba.tccmobile.backend.online

import kotlin.test.Test
import kotlin.test.assertEquals

class LatencyMetricsCollectorTest {

    @Test
    fun `summarizes latency samples`() {
        val collector = LatencyMetricsCollector()

        listOf(10L, 20L, 30L, 40L, 100L).forEach { sample ->
            collector.recordEndToEndLatency(sample)
        }

        val summary = collector.summary()!!

        assertEquals(5, summary.count)
        assertEquals(40.0, summary.meanMs)
        assertEquals(30L, summary.medianMs)
        assertEquals(100L, summary.p95Ms)
        assertEquals(100L, summary.maxMs)
    }
}
