package dev.cyclesync.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationEngineTest {
    private val engine = BaselineRecommendationEngine()

    @Test
    fun recommendationStaysInsidePrototypeBounds() {
        val recommendation = engine.recommend(input(pain = 10, comfort = 10, previous = 6))

        assertTrue(recommendation.suggestedLevel in 1..6)
        assertTrue(recommendation.durationSeconds in 30..900)
        assertTrue(recommendation.confidence in 0.0..1.0)
    }

    @Test
    fun lowComfortPreventsAggressiveIncrease() {
        val comfortable = engine.recommend(input(pain = 8, comfort = 8, previous = 3))
        val uncomfortable = engine.recommend(input(pain = 8, comfort = 1, previous = 3))

        assertTrue(uncomfortable.suggestedLevel < comfortable.suggestedLevel)
    }

    @Test
    fun moderateSessionKeepsPreviousLevel() {
        val recommendation = engine.recommend(input(pain = 4, comfort = 5, previous = 3))

        assertEquals(3, recommendation.suggestedLevel)
    }

    private fun input(pain: Int, comfort: Int, previous: Int?) = SessionInput(
        painScore = pain,
        comfortScore = comfort,
        telemetry = Telemetry(0.9, 0.2, 76.0, 98.0, 90, 1, System.currentTimeMillis()),
        previousAcceptedLevel = previous,
        sessionActive = true,
    )
}
