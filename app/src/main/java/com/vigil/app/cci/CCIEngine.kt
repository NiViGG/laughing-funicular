package com.vigil.app.cci

import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Bundled simplified Kotlin port of the CCI (Cortical Coherence Index) algorithm.
 *
 * The full reference implementation is in Python (see project root, `cci/`).
 * For a demo APK without a live EEG link, we approximate the algorithm by:
 *   1. Simulating two short multi-channel EEG-like signals.
 *   2. Computing the average normalized cross-correlation between channel pairs.
 *   3. Thresholding the average correlation to produce a coherence verdict.
 *
 * The threshold (DEFAULT_THRESHOLD) matches the reference implementation's demo value.
 * For a real Bluetooth EEG link, replace [simulateChannels] with sampled buffers.
 */
object CCIEngine {

    const val DEFAULT_THRESHOLD = 0.55

    data class Result(
        val coherent: Boolean,
        val score: Double,
        val perChannelScores: List<Double>
    )

    /**
     * Run a single CCI check.
     *
     * @param forceCoherent when non-null, bypasses computation and produces a result
     *   matching the requested outcome (used by validation / simulation mode).
     */
    fun runCheck(forceCoherent: Boolean? = null, rng: Random = Random.Default): Result {
        if (forceCoherent != null) {
            val s = if (forceCoherent) 0.78 + rng.nextDouble() * 0.15
                    else 0.18 + rng.nextDouble() * 0.20
            val per = List(3) { (s + (rng.nextDouble() - 0.5) * 0.08).coerceIn(0.0, 1.0) }
            return Result(forceCoherent, s, per)
        }
        val channels = simulateChannels(numChannels = 4, samples = 256, rng = rng)
        val pairs = mutableListOf<Double>()
        for (i in channels.indices) {
            for (j in i + 1 until channels.size) {
                pairs += abs(normalizedCrossCorrelation(channels[i], channels[j]))
            }
        }
        val score = if (pairs.isEmpty()) 0.0 else pairs.average()
        return Result(score >= DEFAULT_THRESHOLD, score, pairs)
    }

    private fun abs(d: Double) = if (d < 0) -d else d

    private fun simulateChannels(numChannels: Int, samples: Int, rng: Random): List<DoubleArray> {
        // Shared low-frequency component (correlation source) + per-channel noise.
        // The amplitude of the shared component drives whether the channels appear coherent.
        val sharedAmp = if (rng.nextDouble() < 0.5) 0.2 else 0.9
        val freq = 0.05 + rng.nextDouble() * 0.05
        val phaseJitterMax = if (sharedAmp > 0.5) 0.15 else 1.0
        return List(numChannels) { _ ->
            val phase = rng.nextDouble() * phaseJitterMax
            DoubleArray(samples) { n ->
                sharedAmp * sin(2.0 * Math.PI * freq * n + phase) +
                        (1.0 - sharedAmp) * (rng.nextDouble() - 0.5) * 2.0 +
                        (rng.nextDouble() - 0.5) * 0.4
            }
        }
    }

    private fun normalizedCrossCorrelation(a: DoubleArray, b: DoubleArray): Double {
        require(a.size == b.size)
        var sa = 0.0; var sb = 0.0
        for (i in a.indices) { sa += a[i]; sb += b[i] }
        val ma = sa / a.size
        val mb = sb / b.size
        var num = 0.0; var da = 0.0; var db = 0.0
        for (i in a.indices) {
            val xa = a[i] - ma
            val xb = b[i] - mb
            num += xa * xb
            da += xa * xa
            db += xb * xb
        }
        val denom = sqrt(da * db)
        return if (denom == 0.0) 0.0 else num / denom
    }
}
