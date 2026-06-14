package com.pedroaba.tccmobile.backend.online

import kotlin.math.ceil

data class LatencyMetricsSummary(
    val count: Int,
    val meanMs: Double,
    val medianMs: Long,
    val p95Ms: Long,
    val maxMs: Long
)

class LatencyMetricsCollector {
    private val endToEndSamplesMs = mutableListOf<Long>()

    fun recordEndToEndLatency(valueMs: Long) {
        if (valueMs >= 0) {
            endToEndSamplesMs += valueMs
        }
    }

    fun summary(): LatencyMetricsSummary? {
        if (endToEndSamplesMs.isEmpty()) return null

        val sorted = endToEndSamplesMs.sorted()
        val count = sorted.size
        val mean = sorted.average()
        val median = sorted[count / 2]
        val p95Index = (ceil(count * 0.95).toInt() - 1).coerceIn(0, count - 1)

        return LatencyMetricsSummary(
            count = count,
            meanMs = mean,
            medianMs = median,
            p95Ms = sorted[p95Index],
            maxMs = sorted.last()
        )
    }

    fun clear() {
        endToEndSamplesMs.clear()
    }
}
