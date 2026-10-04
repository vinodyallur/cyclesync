package dev.cyclesync.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyPolicyTest {
    private val policy = SafetyPolicy()
    private val recommendation = Recommendation(3, 600, 0.8, "Within limits")

    @Test
    fun allowsFreshHighQualityRecommendation() {
        val decision = policy.evaluate(input(quality = 0.9), recommendation)

        assertTrue(decision is SafetyDecision.Allowed)
        assertEquals(6, (decision as SafetyDecision.Allowed).checks.size)
    }

    @Test
    fun blocksLowQualitySignal() {
        val decision = policy.evaluate(input(quality = 0.2), recommendation)

        assertTrue(decision is SafetyDecision.Blocked)
    }

    @Test
    fun blocksStaleTelemetry() {
        val stale = input(quality = 0.9, capturedAtMs = System.currentTimeMillis() - 20_000)
        val decision = policy.evaluate(stale, recommendation)

        assertTrue(decision is SafetyDecision.Blocked)
    }

    private fun input(
        quality: Double,
        capturedAtMs: Long = System.currentTimeMillis(),
    ) = SessionInput(
        painScore = 6,
        comfortScore = 5,
        telemetry = Telemetry(quality, 0.2, 76.0, 98.0, 90, 1, capturedAtMs),
        previousAcceptedLevel = 2,
        sessionActive = true,
    )
}
